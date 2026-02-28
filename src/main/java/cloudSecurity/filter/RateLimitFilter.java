package cloudSecurity.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.quarkus.logging.Log;

/**
 * Rate limiting filter (RNF04).
 * Limits each client to a configurable number of requests per minute (default 100).
 * Uses client IP (X-Forwarded-For or remote address) as key.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 10) // Run before auth
public class RateLimitFilter implements ContainerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final long CLEANUP_INTERVAL_REQUESTS = 500;
    private static final long MAX_AGE_MS = 120_000L;

    @ConfigProperty(name = "app.rate-limit.requests-per-minute", defaultValue = "100")
    int maxRequestsPerMinute;

    @ConfigProperty(name = "app.rate-limit.enabled", defaultValue = "true")
    boolean enabled;

    private final ConcurrentHashMap<String, RequestWindow> windowsByKey = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong(0);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!enabled) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        if (!path.startsWith("api/") && !path.startsWith("/api/")) {
            return;
        }

        String clientKey = resolveClientKey(requestContext);
        RequestWindow window = updateWindow(clientKey);

        if (!window.allowed) {
            Log.debugf("Rate limit exceeded for client: %s", maskKey(clientKey));
            requestContext.abortWith(
                    Response.status(429)
                            .entity(Map.of(
                                    "error", "Too Many Requests",
                                    "message", "Rate limit exceeded. Try again later."))
                            .build());
        }

        maybeCleanup();
    }

    private String resolveClientKey(ContainerRequestContext requestContext) {
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remoteAddr = requestContext.getHeaderString("X-Real-IP");
        if (remoteAddr != null && !remoteAddr.isBlank()) {
            return remoteAddr;
        }
        Object peer = requestContext.getProperty("javax.servlet.request.remote_addr");
        if (peer != null) {
            return peer.toString();
        }
        return "unknown";
    }

    private RequestWindow updateWindow(String clientKey) {
        long now = System.currentTimeMillis();
        RequestWindow next = windowsByKey.compute(clientKey, (k, current) -> {
            if (current == null || (now - current.windowStartMs) >= WINDOW_MS) {
                return new RequestWindow(now, 1, true);
            }
            if (current.count >= maxRequestsPerMinute) {
                return new RequestWindow(current.windowStartMs, current.count, false);
            }
            return new RequestWindow(current.windowStartMs, current.count + 1, true);
        });
        return next;
    }

    private void maybeCleanup() {
        if (requestCount.incrementAndGet() % CLEANUP_INTERVAL_REQUESTS != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        windowsByKey.entrySet().removeIf(e -> (now - e.getValue().windowStartMs) > MAX_AGE_MS);
    }

    private static String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "***";
        }
        return key.substring(0, 3) + "***" + key.substring(key.length() - 2);
    }

    private static final class RequestWindow {
        final long windowStartMs;
        final int count;
        final boolean allowed;

        RequestWindow(long windowStartMs, int count, boolean allowed) {
            this.windowStartMs = windowStartMs;
            this.count = count;
            this.allowed = allowed;
        }
    }
}
