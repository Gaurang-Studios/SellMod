package me.gaurang.sellmod.core.analytics;

import me.gaurang.sellmod.core.config.ModConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Version-independent analytics engine for the flat /api/events contract.
 *
 * Each HTTP request carries exactly one event as a top-level object:
 *   { "eventId": "<unique random>", "clientId": "<persisted random>",
 *     "sessionId": "<per-launch random>", "event": "<name>", "payload": {...} }
 * No sentAt, no events arrays, no per-event timestamps, no other envelope
 * fields — the backend does not accept them.
 *
 * Fully asynchronous, bounded and isolated: no network work on the game
 * thread, a bounded queue, bounded per-event retries, and every failure
 * swallowed. Analytics can never block or affect selling.
 *
 * Privacy: only the approved anonymous events and locally generated random
 * ids are sent. No server addresses, server software, versions, usernames,
 * player UUIDs, machine identifiers, inventories, GUI contents, chat,
 * coordinates, or world names are ever read or transmitted.
 */
public final class AnalyticsEngine {
    private static final int MAX_QUEUED_EVENTS = 192;
    private static final int FLUSH_TRIGGER = 12;
    private static final int MAX_EVENTS_PER_FLUSH = 32;
    private static final int MAX_EVENT_ATTEMPTS = 3;
    private static final long FLUSH_INTERVAL_SECONDS = 30;
    private static final String CLIENT_ID_FILE_NAME = "sellmod-client-id.txt";

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "sellmod-analytics");
                thread.setDaemon(true);
                return thread;
            });

    // New anonymous session id per launch; never persisted, never identity-derived.
    private static final String SESSION_ID = randomId();

    private static final Queue<PendingEvent> EVENTS = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean FLUSHING = new AtomicBoolean(false);
    private static final AtomicBoolean SESSION_RECORDED = new AtomicBoolean(false);
    private static final AtomicLong ITEMS_TRANSFERRED = new AtomicLong();
    private static final AtomicLong ITEMS_SUBMITTED = new AtomicLong();

    // Debug diagnostics: disabled by default; never logs the API key or bodies.
    private static final boolean DEBUG = Boolean.getBoolean("sellmod.analytics.debug");

    static {
        // Warm UUID's SecureRandom here — this class loads during client init
        // (off the tick loop) so per-event id generation never stalls gameplay.
        UUID.randomUUID();
    }

    private final Transport transport;
    private final Path clientIdFile;

    // Persisted anonymous client id, reused across launches for DAU/WAU/MAU.
    // Randomly generated once; never derived from user or machine identity.
    private final String clientId;

    public AnalyticsEngine(Transport transport, Path configDir) {
        this.transport = transport;
        this.clientIdFile = configDir == null ? null : configDir.resolve(CLIENT_ID_FILE_NAME);
        this.clientId = loadOrCreateClientId();
    }

    /** Starts the periodic flush cadence. Safe to call once during client init. */
    public void start() {
        SCHEDULER.scheduleWithFixedDelay(this::tryFlush,
                FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Queues session-level events once, on the first automated sell command.
     * Runtime point: SellEngine.sendCommand().
     */
    public void onSellCommand(ModConfig config) {
        if (!SESSION_RECORDED.compareAndSet(false, true)) {
            return;
        }
        if (DEBUG) System.out.println("[SellMod Analytics] onSellCommand: queuing session events");
        add(eventBody("session_started", "{}"));
        add(eventBody("transfer_method_used", "{\"method\":\"" + transferMethod(config) + "\"}"));
        add(eventBody("feature_snapshot", featureSnapshotPayload(config)));
        tryFlush();
    }

    /**
     * Counts items actually moved into the sell GUI. Aggregated into one event
     * per flush; never one request per item.
     * Runtime point: SellEngine.moveItems() (both shift and pickup paths).
     */
    public void recordItemsTransferred(int count) {
        if (enabled() && count > 0) {
            ITEMS_TRANSFERRED.addAndGet(count);
        }
    }

    /**
     * Records a submitted sell action. Submission alone counts: sales are never
     * verified through GUI changes, money, messages, packets, or inventory diffs.
     * Runtime point: SellEngine.finishTransferAction().
     */
    public void recordSellSubmitted(int itemCount, boolean throughButton) {
        if (!enabled()) {
            return;
        }
        if (itemCount > 0) {
            ITEMS_SUBMITTED.addAndGet(itemCount);
        }
        add(eventBody("sell_method_used",
                "{\"method\":\"" + (throughButton ? "button" : "close") + "\"}"));
        add(eventBody("sell_cycle_completed", "{}"));
    }

    private boolean enabled() {
        ModConfig config = ModConfig.INSTANCE;
        return config != null && config.analyticsEnabled && config.analyticsEndpoint != null
                && !config.analyticsEndpoint.isBlank();
    }

    private static String transferMethod(ModConfig config) {
        return config.transferMode == ModConfig.TransferMode.SHIFT ? "shift" : "pickup";
    }

    /**
     * Exact backend keys: burst, randomizedItemDelay, randomizedCycleDelay,
     * sellThroughButton, buttonDetection. When sell-through-button is off,
     * buttonDetection must be "none".
     */
    private static String featureSnapshotPayload(ModConfig config) {
        String detection = !config.sellThroughButton ? "none"
                : config.buttonDetectionMode == ModConfig.ButtonDetectionMode.MANUAL_MATERIAL
                ? "manual" : "automatic";
        return "{\"burst\":" + (Math.max(1, config.transferBurst) > 1)
                + ",\"randomizedItemDelay\":" + config.randomizeItemDelay
                + ",\"randomizedCycleDelay\":" + config.randomizeDelay
                + ",\"sellThroughButton\":" + config.sellThroughButton
                + ",\"buttonDetection\":\"" + detection + "\"}";
    }

    /** The exact accepted request body: five top-level fields, nothing else. */
    private String eventBody(String eventName, String payloadJson) {
        return "{\"eventId\":\"" + UUID.randomUUID()
                + "\",\"clientId\":\"" + clientId
                + "\",\"sessionId\":\"" + SESSION_ID
                + "\",\"event\":\"" + eventName
                + "\",\"payload\":" + payloadJson + "}";
    }

    private void add(String body) {
        if (!enabled()) {
            return;
        }
        enqueue(new PendingEvent(body, 0));
        if (EVENTS.size() >= FLUSH_TRIGGER) {
            tryFlush();
        }
    }

    private void enqueue(PendingEvent event) {
        EVENTS.offer(event);
        while (EVENTS.size() > MAX_QUEUED_EVENTS) {
            // Stay bounded: shed the oldest event to make room.
            EVENTS.poll();
        }
    }

    public void tryFlush() {
        if (!FLUSHING.compareAndSet(false, true)) {
            return;
        }
        List<PendingEvent> chunk;
        try {
            chunk = drainChunk();
            if (chunk.isEmpty()) {
                FLUSHING.set(false);
                return;
            }
        } catch (Exception ignored) {
            // Analytics must never propagate failures to the caller.
            FLUSHING.set(false);
            return;
        }
        sendChunk(chunk);
    }

    private List<PendingEvent> drainChunk() {
        List<PendingEvent> chunk = new ArrayList<>();
        long transferred = ITEMS_TRANSFERRED.getAndSet(0);
        if (transferred > 0) {
            chunk.add(new PendingEvent(
                    eventBody("items_transferred", "{\"count\":" + transferred + "}"), 0));
        }
        long submitted = ITEMS_SUBMITTED.getAndSet(0);
        if (submitted > 0) {
            chunk.add(new PendingEvent(
                    eventBody("items_sold", "{\"count\":" + submitted + "}"), 0));
        }
        PendingEvent event;
        while (chunk.size() < MAX_EVENTS_PER_FLUSH && (event = EVENTS.poll()) != null) {
            chunk.add(event);
        }
        return chunk;
    }

    /**
     * Sends each event as its own flat POST. The caller (game thread at most)
     * only enqueues work on the transport's dispatch executor; all network
     * activity happens off-thread. Completion is tracked with a countdown so
     * failures can be requeued with a bounded attempt count.
     */
    private void sendChunk(List<PendingEvent> chunk) {
        String endpoint;
        String apiKey;
        try {
            endpoint = ModConfig.INSTANCE.analyticsEndpoint.trim();
            apiKey = ModConfig.INSTANCE.analyticsApiKey == null
                    ? "" : ModConfig.INSTANCE.analyticsApiKey.trim();
        } catch (Exception ignored) {
            handleCompletion(new ArrayList<>(chunk));
            return;
        }
        if (DEBUG) System.out.println("[SellMod Analytics] flushing " + chunk.size()
                + " event(s) to " + endpoint);
        AtomicInteger pending = new AtomicInteger(chunk.size());
        List<PendingEvent> failed = Collections.synchronizedList(new ArrayList<>());
        for (PendingEvent event : chunk) {
            try {
                transport.postAsync(endpoint, event.body(), apiKey,
                        () -> onEventSettled(pending, failed, null),
                        () -> onEventSettled(pending, failed, event));
            } catch (Exception ignored) {
                onEventSettled(pending, failed, event);
            }
        }
    }

    private void onEventSettled(AtomicInteger pending, List<PendingEvent> failed,
                                PendingEvent failure) {
        if (failure != null) {
            failed.add(failure);
        }
        if (pending.decrementAndGet() == 0) {
            handleCompletion(failed);
        }
    }

    private void handleCompletion(List<PendingEvent> failed) {
        try {
            if (!failed.isEmpty()) {
                if (DEBUG) System.out.println("[SellMod Analytics] " + failed.size()
                        + " event(s) failed; applying bounded requeue");
                for (PendingEvent event : failed) {
                    int attempts = event.attempts() + 1;
                    if (attempts < MAX_EVENT_ATTEMPTS) {
                        enqueue(new PendingEvent(event.body(), attempts));
                    } else if (DEBUG) {
                        System.out.println("[SellMod Analytics] dropping event after "
                                + attempts + " attempts (bounded)");
                    }
                }
            } else if (DEBUG) {
                System.out.println("[SellMod Analytics] chunk delivered");
            }
        } finally {
            FLUSHING.set(false);
            // Continue draining only after a fully successful chunk; failures
            // wait for the scheduler cadence so a downed backend can never
            // cause a tight retry loop.
            if (failed.isEmpty()
                    && (!EVENTS.isEmpty() || ITEMS_TRANSFERRED.get() > 0 || ITEMS_SUBMITTED.get() > 0)) {
                tryFlush();
            }
        }
    }

    /**
     * Loads or creates the persisted anonymous client id. Random, never derived
     * from user or machine identity; reused across launches for DAU/WAU/MAU.
     * Any IO failure degrades to a per-launch id without ever blocking.
     */
    private String loadOrCreateClientId() {
        if (clientIdFile == null) {
            return randomId();
        }
        try {
            if (Files.exists(clientIdFile)) {
                String loaded = Files.readString(clientIdFile, StandardCharsets.UTF_8).trim();
                if (!loaded.isBlank()) {
                    return loaded;
                }
            }
            String id = randomId();
            Files.createDirectories(clientIdFile.getParent());
            Path tmp = clientIdFile.resolveSibling(clientIdFile.getFileName() + ".tmp");
            Files.writeString(tmp, id, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, clientIdFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, clientIdFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return id;
        } catch (IOException ignored) {
            return randomId();
        }
    }

    private static String randomId() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(Character.forDigit((b >> 4) & 0xF, 16));
            out.append(Character.forDigit(b & 0xF, 16));
        }
        return out.toString();
    }

    /** One flat event body plus its bounded attempt counter. */
    private record PendingEvent(String body, int attempts) {
    }
}
