package com.universalshield.service;

import com.universalshield.domain.Scan;
import com.universalshield.domain.ScanIndicator;
import com.universalshield.dto.IndicatorDto;
import com.universalshield.dto.ScanResponse;
import com.universalshield.dto.ScanSummary;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts persisted scans into API DTOs. */
@Component
public class ScanMapper {

    public static final String SCORING_METHOD = "Indicators are grouped by correlation; only the strongest weight per group "
            + "counts. Groups are combined with a noisy-OR: score = 100 x (1 - product(1 - weight)). "
            + "See About > Methodology.";

    public ScanResponse toResponse(Scan s) {
        List<IndicatorDto> indicators = s.getIndicators().stream().map(ScanMapper::toDto).toList();
        return new ScanResponse(s.getPublicId(), s.getCategory().name(), s.getRiskScore(), s.getRiskLevel().name(),
                s.getEvidenceStrength().name(), s.getReasons(), indicators, s.getRecommendations(), s.getCreatedAt(),
                s.getRulesetVersion(), parseVersions(s.getDetectorVersions()), Arrays.asList(s.getSources().split(",")),
                s.getNotes(), s.getDisclaimers(), s.getInputExcerpt(), s.isDemo(), SCORING_METHOD);
    }

    public ScanSummary toSummary(Scan s) {
        String excerpt = s.getInputExcerpt();
        if (excerpt != null && excerpt.length() > 120) {
            excerpt = excerpt.substring(0, 119) + "…";
        }
        String top = s.getIndicators().isEmpty() ? null : s.getIndicators().get(0).getTitle();
        return new ScanSummary(s.getPublicId(), s.getCategory().name(), s.getRiskScore(), s.getRiskLevel().name(),
                s.getEvidenceStrength().name(), s.getCreatedAt(), excerpt, s.getIndicators().size(), top, s.isDemo());
    }

    static IndicatorDto toDto(ScanIndicator i) {
        return new IndicatorDto(i.getCode(), i.getGroup(), i.getTitle(), i.getExplanation(), i.getWeight(),
                i.getDetectorId(), i.getSource().name(), i.getEvidence());
    }

    static String formatVersions(Map<String, String> versions) {
        StringBuilder sb = new StringBuilder();
        versions.forEach((k, v) -> sb.append(sb.isEmpty() ? "" : ";").append(k).append('=').append(v));
        return sb.length() > 400 ? sb.substring(0, 400) : sb.toString();
    }

    static Map<String, String> parseVersions(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        if (s == null || s.isBlank()) {
            return m;
        }
        for (String part : s.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                m.put(part.substring(0, eq), part.substring(eq + 1));
            }
        }
        return m;
    }
}
