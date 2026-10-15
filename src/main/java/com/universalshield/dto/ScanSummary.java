package com.universalshield.dto;

import java.time.Instant;
import java.util.UUID;

/** Compact row used by the history list and dashboard. */
public record ScanSummary(UUID id, String category, int riskScore, String riskLevel, String evidenceStrength,
                          Instant createdAt, String excerpt, int indicatorCount, String topIndicator, boolean demo) {
}
