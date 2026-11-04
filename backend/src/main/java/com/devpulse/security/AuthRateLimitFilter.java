package com.devpulse.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_NANOS = 60_000_000_000L;
    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final boolean trustProxyHeader;

    public AuthRateLimitFilter(
            @Value("${devpulse.auth-rate-limit.enabled:true}") boolean enabled,
            @Value("${devpulse.auth-rate-limit.trust-proxy-header:false}") boolean trustProxyHeader) {
        this.enabled = enabled;
        this.trustProxyHeader = trustProxyHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!enabled || !"POST".equals(request.getMethod())
                || !("/api/auth/login".equals(path) || "/api/auth/register".equals(path))) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientAddress = request.getRemoteAddr();
        if (trustProxyHeader) {
            String forwardedAddress = request.getHeader("X-Real-IP");
            if (forwardedAddress != null && !forwardedAddress.isBlank()) {
                clientAddress = forwardedAddress.trim();
            }
        }
        String key = clientAddress + ":" + path;
        long now = System.nanoTime();
        AtomicBoolean allowed = new AtomicBoolean();
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAtNanos >= WINDOW_NANOS) {
                allowed.set(true);
                return new Window(now, 1);
            }
            synchronized (current) {
                if (current.count >= MAX_REQUESTS_PER_WINDOW) {
                    allowed.set(false);
                } else {
                    current.count++;
                    allowed.set(true);
                }
                return current;
            }
        });

        if (!allowed.get()) {
            long remainingNanos = Math.max(0, WINDOW_NANOS - (now - window.startedAtNanos));
            long retryAfterSeconds = Math.max(1, (remainingNanos + 999_999_999L) / 1_000_000_000L);
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            response.sendError(429, "Too many authentication requests");
            return;
        }

        filterChain.doFilter(request, response);

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAtNanos >= WINDOW_NANOS);
        }
    }

    private static final class Window {
        private final long startedAtNanos;
        private int count;

        private Window(long startedAtNanos, int count) {
            this.startedAtNanos = startedAtNanos;
            this.count = count;
        }
    }
}
