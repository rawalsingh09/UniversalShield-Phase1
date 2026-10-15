package com.universalshield.dto;

import com.universalshield.engine.Recommendation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Full, explainable scan result. Identical for POST /api/scans and GET /api/scans/{id}. */
public record ScanResponse(
        UUID id,
        String category,
        int riskScore,
        String riskLevel,
        String evidenceStrength,
        List<String> reasons,
        List<IndicatorDto> indicators,
        List<Recommendation> recommendations,
        Instant createdAt,
        String rulesetVersion,
        Map<String, String> detectorVersions,
        List<String> sources,
        List<String> notes,
        List<String> disclaimers,
        String inputExcerpt,
        boolean demo,
        String scoringMethod) {
}
