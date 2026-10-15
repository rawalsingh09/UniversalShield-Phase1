package com.universalshield.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScorerTest {

    private final RiskScorer scorer = new RiskScorer();

    static Indicator ind(String group, double w) {
        return new Indicator("C_" + group + w, group, "t", "e", w, "test", ResultSource.LOCAL_HEURISTIC, null);
    }

    static DetectorResult result(boolean sufficient, Indicator... inds) {
        return new DetectorResult("test", "1", ResultSource.LOCAL_HEURISTIC, List.of(inds), List.of(), sufficient);
    }

    @Test
    void noIndicatorsScoresZero() {
        assertThat(scorer.score(List.of()).score()).isZero();
    }

    @Test
    void singleIndicatorScoresItsWeight() {
        assertThat(scorer.score(List.of(ind("A", 0.5))).score()).isEqualTo(50);
    }

    @Test
    void correlatedIndicatorsInSameGroupAreNotDoubleCounted() {
        var breakdown = scorer.score(List.of(ind("URGENCY", 0.25), ind("URGENCY", 0.2), ind("URGENCY", 0.25)));
        assertThat(breakdown.score()).isEqualTo(25);
        assertThat(breakdown.groupWeights()).containsOnlyKeys("URGENCY");
    }

    @Test
    void independentGroupsCombineWithNoisyOr() {
        // 1 - (0.5 * 0.5) = 0.75
        assertThat(scorer.score(List.of(ind("A", 0.5), ind("B", 0.5))).score()).isEqualTo(75);
    }

    @Test
    void scoreStaysWithinBoundsForManyStrongIndicators() {
        var many = java.util.stream.IntStream.range(0, 30).mapToObj(i -> ind("G" + i, 1.0)).toList();
        assertThat(scorer.score(many).score()).isEqualTo(100);
    }

    @Test
    void manyWeakSignsHaveDiminishingReturns() {
        var weak = java.util.stream.IntStream.range(0, 5).mapToObj(i -> ind("W" + i, 0.1)).toList();
        int s = scorer.score(weak).score();
        assertThat(s).isLessThan(50).isEqualTo(41);
    }

    @Test
    void evidenceStrengthCountsIndependentGroups() {
        var three = List.of(ind("A", 0.3), ind("B", 0.3), ind("C", 0.3));
        assertThat(scorer.evidence(three, List.of(result(true)))).isEqualTo(EvidenceStrength.STRONG);
        var two = List.of(ind("A", 0.3), ind("B", 0.3));
        assertThat(scorer.evidence(two, List.of(result(true)))).isEqualTo(EvidenceStrength.MODERATE);
        var oneStrong = List.of(ind("A", 0.6));
        assertThat(scorer.evidence(oneStrong, List.of(result(true)))).isEqualTo(EvidenceStrength.MODERATE);
        var oneWeak = List.of(ind("A", 0.3));
        assertThat(scorer.evidence(oneWeak, List.of(result(true)))).isEqualTo(EvidenceStrength.LIMITED);
    }

    @Test
    void weakGroupsBelowMinimumDoNotCountAsEvidence() {
        var weak = List.of(ind("A", 0.1), ind("B", 0.1), ind("C", 0.1));
        assertThat(scorer.evidence(weak, List.of(result(true)))).isEqualTo(EvidenceStrength.LIMITED);
    }

    @Test
    void insufficientInputAlwaysGivesLimitedEvidence() {
        var three = List.of(ind("A", 0.6), ind("B", 0.6), ind("C", 0.6));
        assertThat(scorer.evidence(three, List.of(result(false)))).isEqualTo(EvidenceStrength.LIMITED);
    }
}
