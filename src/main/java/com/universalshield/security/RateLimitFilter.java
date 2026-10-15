package com.universalshield.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universalshield.config.AppProperties;
import com.universalshield.dto.ApiError;
import com.universalshield.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Limits login/registration attempts per client IP and scan submissions per user.
 * Responds 429 with the standard error envelope when exceeded.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;
    private final AppProperties.RateLimit config;
    private final ObjectMapper mapper;

    public RateLimitFilter(RateLimiter limiter, AppProperties.RateLimit config, ObjectMapper mapper) {
        this.limiter = limiter;
        this.config = config;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (config.enabled() && "POST".equals(req.getMethod())) {
            String path = req.getRequestURI();
            boolean allowed = true;
            if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
                allowed = limiter.tryAcquire("auth:" + req.getRemoteAddr(), config.authPerMinute());
            } else if (path.startsWith("/api/scans")) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String who = auth != null && auth.isAuthenticated() ? auth.getName() : req.getRemoteAddr();
                allowed = limiter.tryAcquire("scan:" + who, config.scansPerMinute());
            }
            if (!allowed) {
                res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                res.setHeader("Retry-After", "60");
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                mapper.writeValue(res.getOutputStream(), ApiResponse.fail(ApiError.of("RATE_LIMITED",
                        "Too many requests. Please wait a minute and try again.")));
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
