package com.universalshield.engine;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runs every applicable {@link DetectionModule} on a {@link FraudEvent} and combines their
 * evidence into one explainable {@link FraudResult}. Pure Java; no Spring or database access.
 */
public class RiskEngine {

    public static final String RULESET_VERSION = "2026.10-1";

    static final String DISCLAIMER_GENERAL =
            "This is an automated risk assessment of the information you provided. It is not proof of fraud or of safety.";
    static final String DISCLAIMER_LOW =
            "A low score does not guarantee safety. Scammers constantly change their wording and infrastructure.";
    static final String DISCLAIMER_HIGH =
            "A high score means strong warning signs were found. It does not prove that fraud is taking place.";
    static final String DISCLAIMER_LIMITED =
            "Evidence is limited: the input was short or incomplete, or only weak signs were found. Treat this result with extra caution.";
    static final String DISCLAIMER_NO_EXTERNAL =
            "No verified external reputation data was used. Results are based on local heuristics"
                    + " and, where available, a locally trained model.";

    /**
     * Local heuristics and models can never establish certainty, so without verified external
     * evidence the score is capped below 100.
     */
    public static final int MAX_SCORE_WITHOUT_EXTERNAL = 95;

    private final List<DetectionModule> modules;
    private final RiskThresholds thresholds;
    private final RecommendationCatalog recommendations;
    private final RiskScorer scorer = new RiskScorer();
    private final Clock clock;

    public RiskEngine(List<DetectionModule> modules, RiskThresholds thresholds,
                      RecommendationCatalog recommendations, Clock clock) {
        this.modules = List.copyOf(modules);
        this.thresholds = thresholds;
        this.recommendations = recommendations;
        this.clock = clock;
    }

    public List<DetectionModule> modules() {
        return modules;
    }

    public RiskThresholds thresholds() {
        return thresholds;
    }

    public FraudResult evaluate(FraudEvent event) {
        List<DetectorResult> results = new ArrayList<>();
        for (DetectionModule module : modules) {
            if (module.available() && module.appliesTo(event)) {
                results.add(module.analyze(event));
            }
        }
        if (results.isEmpty()) {
            throw new IllegalStateException("No detector available for category " + event.category());
        }

        List<Indicator> indicators = results.stream().flatMap(r -> r.indicators().stream())
                .sorted(Comparator.comparingDouble(Indicator::weight).reversed()).toList();
        RiskScorer.Breakdown breakdown = scorer.score(indicators);
        EvidenceStrength evidence = scorer.evidence(indicators, results);

        Map<String, String> versions = new LinkedHashMap<>();
        Set<ResultSource> sources = EnumSet.noneOf(ResultSource.class);
        List<String> notes = new ArrayList<>();
        for (DetectorResult r : results) {
            versions.put(r.detectorId(), r.detectorVersion());
            sources.add(r.source());
            notes.addAll(r.notes());
        }
        indicators.forEach(i -> sources.add(i.source()));
        int score = sources.contains(ResultSource.VERIFIED_EXTERNAL)
                ? breakdown.score() : Math.min(breakdown.score(), MAX_SCORE_WITHOUT_EXTERNAL);
        RiskLevel level = RiskLevel.fromScore(score, thresholds);

        return new FraudResult(UUID.randomUUID(), event.category(), score, level, evidence,
                reasons(indicators, breakdown), indicators, recommendations.recommend(indicators, level),
                Instant.now(clock), RULESET_VERSION, versions, sources, List.copyOf(notes),
                disclaimers(level, evidence, sources));
    }

    private static List<String> reasons(List<Indicator> indicators, RiskScorer.Breakdown breakdown) {
        List<String> reasons = new ArrayList<>();
        if (indicators.isEmpty()) {
            reasons.add("No known warning signs matched the information provided.");
            return reasons;
        }
        reasons.add(indicators.size() + " warning sign(s) matched across " + breakdown.groupWeights().size()
                + " independent group(s). Signs in the same group are counted once.");
        Set<String> seen = new java.util.HashSet<>();
        for (Indicator i : indicators) {
            if (seen.add(i.group())) {
                reasons.add(i.title() + ": " + i.explanation());
            }
        }
        return reasons;
    }

    private static List<String> disclaimers(RiskLevel level, EvidenceStrength evidence, Set<ResultSource> sources) {
        List<String> list = new ArrayList<>();
        list.add(DISCLAIMER_GENERAL);
        if (level == RiskLevel.LOW) {
            list.add(DISCLAIMER_LOW);
        }
        if (level == RiskLevel.HIGH || level == RiskLevel.CRITICAL) {
            list.add(DISCLAIMER_HIGH);
        }
        if (evidence == EvidenceStrength.LIMITED) {
            list.add(DISCLAIMER_LIMITED);
        }
        if (!sources.contains(ResultSource.VERIFIED_EXTERNAL)) {
            list.add(DISCLAIMER_NO_EXTERNAL);
        }
        return list;
    }
}
