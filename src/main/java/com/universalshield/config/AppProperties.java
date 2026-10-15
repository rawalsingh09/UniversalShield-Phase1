package com.universalshield.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Type-safe view of the {@code universalshield.*} settings in application.yml.
 * Secrets (hash secret, demo password) come from environment variables.
 */
@ConfigurationProperties(prefix = "universalshield")
public record AppProperties(
        @DefaultValue Risk risk,
        @DefaultValue Security security,
        @DefaultValue Retention retention,
        @DefaultValue Demo demo) {

    public record Risk(@DefaultValue Thresholds thresholds) {
    }

    public record Thresholds(@DefaultValue("30") int medium, @DefaultValue("60") int high,
                             @DefaultValue("85") int critical) {
    }

    /**
     * @param hashSecret          key for HMAC-hashing subjects (recipient, domain, number) in history
     * @param corsAllowedOrigins  extra origins allowed to call the API (empty = same origin only)
     */
    public record Security(@DefaultValue("change-me-dev-only-secret") String hashSecret,
                           @DefaultValue List<String> corsAllowedOrigins,
                           @DefaultValue RateLimit rateLimit) {
    }

    public record RateLimit(@DefaultValue("true") boolean enabled,
                            @DefaultValue("10") int authPerMinute,
                            @DefaultValue("30") int scansPerMinute) {
    }

    /**
     * @param defaultDays  retention for new users
     * @param maxDays      upper bound users can choose
     * @param cron         when the purge job runs
     */
    public record Retention(@DefaultValue("90") int defaultDays, @DefaultValue("365") int maxDays,
                            @DefaultValue("0 30 3 * * *") String cron) {
    }

    /** Demonstration account and scans, created only when enabled (dev profile). */
    public record Demo(@DefaultValue("false") boolean seed,
                       @DefaultValue("demo@universalshield.local") String email,
                       @DefaultValue("") String password) {
    }
}
