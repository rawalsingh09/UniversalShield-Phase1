package com.universalshield.engine;

/**
 * Inclusive lower bounds of the MEDIUM, HIGH and CRITICAL bands.
 * Configured via universalshield.risk.thresholds.* in application.yml.
 */
public record RiskThresholds(int medium, int high, int critical) {

    public static final RiskThresholds DEFAULT = new RiskThresholds(30, 60, 85);

    public RiskThresholds {
        if (!(0 < medium && medium < high && high < critical && critical <= 100)) {
            throw new IllegalArgumentException(
                    "Thresholds must satisfy 0 < medium < high < critical <= 100");
        }
    }
}
