package com.universalshield.engine;

/**
 * Coarse risk band derived from the 0-100 risk score using configurable thresholds.
 * A HIGH or CRITICAL level means "strong warning signs", never "fraud proven".
 * LOW means "few warning signs found", never "safe".
 */
public enum RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL;

    /** Maps a score to a level. Thresholds are inclusive lower bounds. */
    public static RiskLevel fromScore(int score, RiskThresholds thresholds) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100");
        }
        if (score >= thresholds.critical()) {
            return CRITICAL;
        }
        if (score >= thresholds.high()) {
            return HIGH;
        }
        if (score >= thresholds.medium()) {
            return MEDIUM;
        }
        return LOW;
    }
}
