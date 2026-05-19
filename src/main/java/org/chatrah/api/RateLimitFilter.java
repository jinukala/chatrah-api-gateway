package org.chatrah.api;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class RateLimitFilter implements ContainerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000;
    private static final long CLEANUP_INTERVAL_MS = 300_000; // 5 min

    private final Map<String, RateEntry> entries = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        if (!isRateLimited(path)) return;

        cleanupIfNeeded();

        String ip = extractIp(ctx);
        String endpoint = extractEndpoint(path);
        String key = ip + ":" + endpoint;

        RateEntry entry = entries.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateEntry(now);
            }
            return existing;
        });

        if (entry.count.incrementAndGet() > MAX_REQUESTS) {
            ctx.abortWith(Response.status(429)
                    .entity("{\"error\":\"Too many requests. Try again later.\"}")
                    .header("Content-Type", "application/json")
                    .header("Retry-After", "60")
                    .build());
        }
    }

    private String extractEndpoint(String path) {
        // Use the specific auth endpoint as key, not just "api"
        if (path.contains("auth/login")) return "login";
        if (path.contains("otp/send-reset")) return "otp-send";
        if (path.contains("otp/verify-reset")) return "otp-verify";
        if (path.contains("password/reset")) return "password-reset";
        return "other";
    }

    private boolean isRateLimited(String path) {
        return path.contains("auth/login")
                || path.contains("otp/send-reset")
                || path.contains("otp/verify-reset")
                || path.contains("password/reset");
    }

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        long last = lastCleanup.get();
        if (now - last > CLEANUP_INTERVAL_MS && lastCleanup.compareAndSet(last, now)) {
            Iterator<Map.Entry<String, RateEntry>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                if (now - it.next().getValue().windowStart > WINDOW_MS * 2) {
                    it.remove();
                }
            }
        }
    }

    private String extractIp(ContainerRequestContext ctx) {
        String forwarded = ctx.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return "unknown";
    }

    private static class RateEntry {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        RateEntry(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
