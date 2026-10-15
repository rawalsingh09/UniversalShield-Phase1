package com.universalshield.engine;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The unified, explainable output of the {@link RiskEngine}.
 */
public record FraudResult(
        UUID resultId,
        ScanCategory category,
        int riskScore,
        RiskLevel riskLevel,
        EvidenceStrength evidenceStrength,
        List<String> reasons,
        List<Indicator> indicators,
        List<Recommendation> recommendations,
        Instant timestamp,
        String rulesetVersion,
        Map<String, String> detectorVersions,
        Set<ResultSource> sources,
        List<String> notes,
        List<String> disclaimers) {
}
