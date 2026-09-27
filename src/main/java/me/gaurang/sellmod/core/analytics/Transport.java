package me.gaurang.sellmod.core.analytics;

/**
 * Fire-and-forget analytics transport. Implementations must never block the
 * caller and must invoke {@code onFailure} on transport-level failures
 * (network error or non-2xx) and {@code onSuccess} on 2xx.
 */
public interface Transport {
    void postAsync(String endpoint, String body, String apiKey, Runnable onSuccess, Runnable onFailure);
}
