package com.universalshield.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Statistics computed live from the signed-in user's own stored scans. */
public record DashboardSummary(long totalScans, Map<String, Long> byCategory, Map<String, Long> byRiskLevel,
                               List<ScanSummary> recentScans, List<ScanSummary> recentHighRisk,
                               long demoScans, Instant oldestScanAt, int retentionDays, String statisticsNote) {
}
