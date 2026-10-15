package com.universalshield.security;

import com.universalshield.api.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {"universalshield.security.rate-limit.enabled=true",
        "universalshield.security.rate-limit.auth-per-minute=3"})
class RateLimitTest extends IntegrationTestBase {

    @Autowired RateLimiter limiter;

    @Test
    void limiterAllowsUpToLimitPerWindowThenResets() {
        var clock = new MutableClock();
        RateLimiter l = new RateLimiter(clock);
        assertThat(l.tryAcquire("k", 2)).isTrue();
        assertThat(l.tryAcquire("k", 2)).isTrue();
        assertThat(l.tryAcquire("k", 2)).isFalse();
        assertThat(l.tryAcquire("other", 2)).isTrue();
        clock.now = clock.now.plusSeconds(61);
        assertThat(l.tryAcquire("k", 2)).isTrue();
    }

    @Test
    void loginAttemptsAreLimitedWith429() throws Exception {
        limiter.reset();
        String body = toJson(Map.of("email", "rl@example.test", "password", "WrongPass123"));
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
        limiter.reset();
    }

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
