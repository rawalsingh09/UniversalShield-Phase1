package com.universalshield.engine;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns indicators into a 0-100 score and an evidence-strength rating.
 *
 * <p>Formula (documented in docs/SCORING.md):
 * <ol>
 *   <li>Indicators are grouped by {@link Indicator#group()}. Correlated signs share a group
 *       (e.g. "act now" and "within 24 hours" are both URGENCY), and only the strongest
 *       weight in each group counts. This prevents double-counting.</li>
 *   <li>Group weights are combined with a noisy-OR: {@code score = 100 * (1 - prod(1 - g_i))}.
 *       The result is bounded in [0, 100], grows with every independent group, and has
 *       diminishing returns so many weak signs cannot exceed a few strong ones by much.</li>
 * </ol>
 */
public final class RiskScorer {

    /** Groups whose weight is below this do not count as independent evidence. */
    static final double EVIDENCE_MIN_WEIGHT = 0.15;

    public record Breakdown(int score, Map<String, Double> groupWeights) {
    }

    public Breakdown score(Collection<Indicator> indicators) {
        Map<String, Double> groups = strongestPerGroup(indicators);
        double remaining = 1.0;
        for (double g : groups.values()) {
            remaining *= (1.0 - g);
        }
        // small epsilon so values like 77.4999999 caused by floating point round as intended (77.5 -> 78)
        int score = (int) Math.round(100.0 * (1.0 - remaining) + 1e-9);
        return new Breakdown(Math.max(0, Math.min(100, score)), groups);
    }

    public EvidenceStrength evidence(Collection<Indicator> indicators, List<DetectorResult> results) {
        boolean insufficient = results.stream().anyMatch(r -> !r.inputSufficient());
        if (insufficient) {
            return EvidenceStrength.LIMITED;
        }
        Map<String, Double> groups = strongestPerGroup(indicators);
        long independent = groups.values().stream().filter(w -> w >= EVIDENCE_MIN_WEIGHT).count();
        double max = groups.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        if (independent >= 3) {
            return EvidenceStrength.STRONG;
        }
        if (independent == 2 || (independent == 1 && max >= 0.5)) {
            return EvidenceStrength.MODERATE;
        }
        return EvidenceStrength.LIMITED;
    }

    static Map<String, Double> strongestPerGroup(Collection<Indicator> indicators) {
        Map<String, Double> groups = new LinkedHashMap<>();
        for (Indicator i : indicators) {
            groups.merge(i.group(), i.weight(), Math::max);
        }
        return groups;
    }
}
