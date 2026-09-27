package me.gaurang.sellmod.adapter;

import me.gaurang.sellmod.core.analytics.Transport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 26.x analytics transport. Non-blocking and isolated: request construction and
 * dispatch are offloaded to a daemon executor so the game thread never pays
 * HttpClient's first-send setup cost (~130ms) or class initialization (~1s).
 * Any transport failure (timeout, connect error, non-2xx) triggers the
 * engine's bounded requeue; 2xx counts as delivered.
 */
public class FabricTransport implements Transport {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ExecutorService DISPATCH = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sellmod-analytics-http");
        thread.setDaemon(true);
        return thread;
    });

    static {
        // Spawn the dispatch thread eagerly (class-load = client init) so the
        // first in-game flush never pays thread-creation cost on the game thread.
        DISPATCH.execute(() -> { });
    }

    @Override
    public void postAsync(String endpoint, String body, String apiKey, Runnable onSuccess, Runnable onFailure) {
        try {
            DISPATCH.execute(() -> {
                try {
                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .timeout(Duration.ofSeconds(8))
                            .header("Content-Type", "application/json");
                    if (apiKey != null && !apiKey.isBlank()) {
                        builder.header("X-API-Key", apiKey.trim());
                    }
                    HttpRequest request = builder
                            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                            .build();
                    HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                            .orTimeout(12, TimeUnit.SECONDS)
                            .whenComplete((response, error) -> {
                                if (error != null) {
                                    onFailure.run();
                                    return;
                                }
                                if (response == null) {
                                    onFailure.run();
                                    return;
                                }
                                int code = response.statusCode();
                                if (code >= 200 && code < 300) {
                                    onSuccess.run();
                                } else {
                                    onFailure.run();
                                }
                            });
                } catch (Exception ignored) {
                    onFailure.run();
                }
            });
        } catch (Exception ignored) {
            onFailure.run();
        }
    }
}
