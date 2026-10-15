package com.universalshield.engine;

import com.universalshield.engine.input.MessageInput;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskEngineTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    static final FraudEvent EVENT = FraudEvent.ofMessage(new MessageInput("a message with enough words", "SMS", null), null);

    /** Configurable fake detector. */
    record FakeModule(String id, boolean available, ResultSource source, List<Indicator> indicators) implements DetectionModule {
        @Override public String displayName() { return id; }
        @Override public String version() { return "9.9"; }
        @Override public String description() { return "fake"; }
        @Override public Set<ScanCategory> categories() { return Set.of(ScanCategory.MESSAGE); }
        @Override public ResultSource sourceType() { return source; }
        @Override public boolean appliesTo(FraudEvent e) { return true; }
        @Override public DetectorResult analyze(FraudEvent e) {
            return new DetectorResult(id, "9.9", source, indicators, List.of("note from " + id), true);
        }
    }

    static Indicator ind(String group, double w, ResultSource s) {
        return new Indicator("C_" + group, group, "Title " + group, "Expl " + group, w, "fake", s, "ev");
    }

    RiskEngine engine(DetectionModule... modules) {
        return new RiskEngine(List.of(modules), RiskThresholds.DEFAULT, new RecommendationCatalog(), CLOCK);
    }

    @Test
    void producesCompleteStructuredResult() {
        FraudResult r = engine(new FakeModule("m", true, ResultSource.LOCAL_HEURISTIC,
                List.of(ind("OTP_REQUEST", 0.7), ind("URGENCY", 0.25)))).evaluate(EVENT);
        assertThat(r.resultId()).isNotNull();
        assertThat(r.category()).isEqualTo(ScanCategory.MESSAGE);
        assertThat(r.riskScore()).isEqualTo(78); // 1 - 0.3*0.75 = 0.775
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.evidenceStrength()).isEqualTo(EvidenceStrength.MODERATE);
        assertThat(r.timestamp()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(r.rulesetVersion()).isEqualTo(RiskEngine.RULESET_VERSION);
        assertThat(r.detectorVersions()).containsEntry("m", "9.9");
        assertThat(r.sources()).containsExactly(ResultSource.LOCAL_HEURISTIC);
        assertThat(r.reasons()).hasSize(3);
        assertThat(r.recommendations()).isNotEmpty();
        assertThat(r.recommendations().get(0).priority()).isLessThanOrEqualTo(r.recommendations().get(1).priority());
        assertThat(r.notes()).contains("note from m");
        assertThat(r.disclaimers()).contains(RiskEngine.DISCLAIMER_GENERAL, RiskEngine.DISCLAIMER_HIGH,
                RiskEngine.DISCLAIMER_NO_EXTERNAL);
    }

    @Test
    void heuristicOnlyScoresAreCappedBelowCertainty() {
        FraudResult r = engine(new FakeModule("m", true, ResultSource.LOCAL_HEURISTIC,
                List.of(ind("A", 1.0), ind("B", 1.0)))).evaluate(EVENT);
        assertThat(r.riskScore()).isEqualTo(RiskEngine.MAX_SCORE_WITHOUT_EXTERNAL);
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void verifiedExternalEvidenceCanReach100AndRemovesNoExternalDisclaimer() {
        FraudResult r = engine(new FakeModule("rep", true, ResultSource.VERIFIED_EXTERNAL,
                List.of(ind("REPUTATION", 1.0, ResultSource.VERIFIED_EXTERNAL)))).evaluate(EVENT);
        assertThat(r.riskScore()).isEqualTo(100);
        assertThat(r.disclaimers()).doesNotContain(RiskEngine.DISCLAIMER_NO_EXTERNAL);
    }

    static Indicator ind(String group, double w) {
        return ind(group, w, ResultSource.LOCAL_HEURISTIC);
    }

    @Test
    void lowResultCarriesNoGuaranteeDisclaimerAndLimitedEvidence() {
        FraudResult r = engine(new FakeModule("m", true, ResultSource.LOCAL_HEURISTIC, List.of())).evaluate(EVENT);
        assertThat(r.riskScore()).isZero();
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(r.disclaimers()).contains(RiskEngine.DISCLAIMER_LOW, RiskEngine.DISCLAIMER_LIMITED);
        assertThat(r.reasons()).containsExactly("No known warning signs matched the information provided.");
        assertThat(r.recommendations().get(0).action()).contains("No strong warning signs");
    }

    @Test
    void unavailableModulesAreSkipped() {
        FraudResult r = engine(new FakeModule("on", true, ResultSource.LOCAL_HEURISTIC, List.of()),
                new FakeModule("off", false, ResultSource.TRAINED_MODEL, List.of(ind("X", 0.9)))).evaluate(EVENT);
        assertThat(r.detectorVersions()).containsOnlyKeys("on");
        assertThat(r.riskScore()).isZero();
    }

    @Test
    void failsClearlyWhenNoDetectorApplies() {
        assertThatThrownBy(() -> engine().evaluate(EVENT)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void indicatorsFromSeveralDetectorsAreMergedAndSortedByWeight() {
        FraudResult r = engine(new FakeModule("a", true, ResultSource.LOCAL_HEURISTIC, List.of(ind("A", 0.2))),
                new FakeModule("b", true, ResultSource.TRAINED_MODEL, List.of(ind("B", 0.5, ResultSource.TRAINED_MODEL)))).evaluate(EVENT);
        assertThat(r.indicators()).extracting(Indicator::weight).containsExactly(0.5, 0.2);
        assertThat(r.sources()).containsExactlyInAnyOrder(ResultSource.LOCAL_HEURISTIC, ResultSource.TRAINED_MODEL);
    }

    @Test
    void recommendationsAreDeduplicatedAndLimited() {
        var many = List.of(ind("CONTACTS", 0.2), ind("LOCATION", 0.2), ind("MICROPHONE", 0.2), ind("OTP_REQUEST", 0.7),
                ind("REMOTE_ACCESS", 0.6), ind("THREAT", 0.4), ind("URGENCY", 0.2), ind("SECRECY", 0.4), ind("PRIZE_LURE", 0.4));
        FraudResult r = engine(new FakeModule("m", true, ResultSource.LOCAL_HEURISTIC, many)).evaluate(EVENT);
        assertThat(r.recommendations()).hasSizeLessThanOrEqualTo(6).doesNotHaveDuplicates();
    }
}
