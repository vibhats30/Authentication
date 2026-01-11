package com.auth.module.filter;

import com.auth.module.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Only apply rate limiting to authentication endpoints
        String path = request.getRequestURI();
        String clientIp = request.getRemoteAddr();

        if (path.startsWith("/api/auth/")) {
            logger.debug("Applying rate limiting for request: {} from IP: {}", path, clientIp);

            Bucket bucket = rateLimitConfig.resolveBucket(request);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                long remaining = probe.getRemainingTokens();
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));
                logger.debug("Rate limit check passed for IP: {} - {} tokens remaining", clientIp, remaining);
                filterChain.doFilter(request, response);
            } else {
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                logger.warn("Rate limit exceeded for IP: {} on path: {} - retry after {} seconds",
                    clientIp, path, waitForRefill);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
                response.getWriter().write("Too many requests - please try again later");
            }
        } else {
            logger.debug("Skipping rate limiting for non-auth endpoint: {}", path);
            filterChain.doFilter(request, response);
        }
    }
}
