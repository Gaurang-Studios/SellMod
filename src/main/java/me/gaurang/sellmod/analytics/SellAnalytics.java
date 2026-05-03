package me.gaurang.sellmod.analytics;

import me.gaurang.sellmod.config.ModConfig;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SellAnalytics {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Set<String> SESSION_SENT = ConcurrentHashMap.newKeySet();

    private static final Path CLIENT_ID_FILE = Path.of(
            System.getProperty("user.home"),
            ".minecraft",
            "config",
            "sellmod",
            "analytics",
            "client_id.txt"
    );

    private static volatile String cachedClientId;

    private SellAnalytics() {
    }

    public static void recordUsage(Minecraft client, String modVersion) {
        try {
            if (client == null || client.player == null) return;
            if (!ModConfig.INSTANCE.analyticsEnabled) return;

            String endpoint = ModConfig.INSTANCE.analyticsEndpoint;
            if (endpoint == null || endpoint.isBlank()) return;

            String server = resolveServerName(client);
            if (server == null || server.isBlank()) return;

            String software = resolveServerSoftware(client);
            if (software == null || software.isBlank()) software = "unknown";

            String version = modVersion == null || modVersion.isBlank() ? "unknown" : modVersion;
            String clientId = getOrCreateClientId();
            if (clientId == null || clientId.isBlank()) return;

            String dedupeKey = clientId + "|" + server + "|" + software + "|" + version;
            if (!SESSION_SENT.add(dedupeKey)) return;

            String json = buildJson(clientId, server, software, version, System.currentTimeMillis());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(res -> {
                        System.out.println("Analytics status: " + res.statusCode());
                    })
                    .exceptionally(ex -> {
                        SESSION_SENT.remove(dedupeKey);
                        return null;
                    });
        } catch (Exception ignored) {
        }
    }

    private static String getOrCreateClientId() {
        if (cachedClientId != null && !cachedClientId.isBlank()) {
            return cachedClientId;
        }

        synchronized (SellAnalytics.class) {
            if (cachedClientId != null && !cachedClientId.isBlank()) {
                return cachedClientId;
            }

            try {
                if (Files.exists(CLIENT_ID_FILE)) {
                    String loaded = Files.readString(CLIENT_ID_FILE, StandardCharsets.UTF_8).trim();
                    if (!loaded.isBlank()) {
                        cachedClientId = loaded;
                        return loaded;
                    }
                }

                Files.createDirectories(CLIENT_ID_FILE.getParent());
                String id = UUID.randomUUID().toString();
                Files.writeString(CLIENT_ID_FILE, id, StandardCharsets.UTF_8);
                cachedClientId = id;
                return id;
            } catch (IOException e) {
                return null;
            }
        }
    }

    private static String resolveServerName(Minecraft client) {
        Object serverEntry = invokeNoArgs(client, "getCurrentServerEntry", "getCurrentServer", "getCurrentServerData");
        if (serverEntry == null) {
            return null;
        }

        String server = readStringMember(serverEntry, "address", "ip", "host", "name");
        if (server == null || server.isBlank()) {
            server = serverEntry.toString();
        }

        return server == null ? null : server.trim();
    }

    private static String resolveServerSoftware(Minecraft client) {
        try {
            Object player = client.player;
            if (player == null) return null;

            Object connection = readFieldValue(player, "connection");
            if (connection == null) return null;

            String brand = readStringMember(connection, "getServerBrand", "getBrand", "getBrandName");
            if (brand != null && !brand.isBlank()) {
                return brand.trim();
            }

            brand = readStringMember(connection, "serverBrand", "brand", "brandName");
            if (brand != null && !brand.isBlank()) {
                return brand.trim();
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Object invokeNoArgs(Object target, String... methodNames) {
        if (target == null) return null;

        for (String name : methodNames) {
            try {
                Method method = target.getClass().getMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return null;
    }

    private static String readStringMember(Object target, String... names) {
        if (target == null) return null;

        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                method.setAccessible(true);
                Object value = method.invoke(target);
                if (value != null) return String.valueOf(value);
            } catch (ReflectiveOperationException ignored) {
            }

            try {
                Field field = findField(target.getClass(), name);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value != null) return String.valueOf(value);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return null;
    }

    private static Object readFieldValue(Object target, String fieldName) {
        if (target == null) return null;

        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) return null;
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static String buildJson(String clientId, String server, String software, String version, long timestamp) {
        return "{"
                + "\"clientId\":\"" + escapeJson(clientId) + "\","
                + "\"server\":\"" + escapeJson(server) + "\","
                + "\"software\":\"" + escapeJson(software) + "\","
                + "\"version\":\"" + escapeJson(version) + "\","
                + "\"timestamp\":" + timestamp
                + "}";
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder(input.length() + 16);
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }
}