package com.universalshield.security;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fixed-window rate limiter (one window per key per minute). Good enough for a single
 * instance. For several instances, replace with a shared store (e.g. Redis/Bucket4j); see
 * SECURITY.md.
 */
public class RateLimiter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_KEYS = 50_000;

    private record Window(long start, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Returns true if the request is allowed. */
    public boolean tryAcquire(String key, int limitPerMinute) {
        long now = clock.millis();
        if (windows.size() > MAX_KEYS) {
            windows.entrySet().removeIf(e -> now - e.getValue().start() >= WINDOW_MILLIS);
        }
        Window w = windows.compute(key, (k, old) ->
                old == null || now - old.start() >= WINDOW_MILLIS ? new Window(now, 1) : new Window(old.start(), old.count() + 1));
        return w.count() <= limitPerMinute;
    }

    public void reset() {
        windows.clear();
    }
}
