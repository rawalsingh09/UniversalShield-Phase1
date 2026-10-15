package com.universalshield.ml;

import com.universalshield.engine.DetectionModule;
import com.universalshield.engine.DetectorResult;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.Indicator;
import com.universalshield.engine.ResultSource;
import com.universalshield.engine.ScanCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Optional detector backed by a locally trained Naive Bayes model. Unavailable (and therefore
 * skipped by the engine) until a model has been trained with {@code TrainingCli}.
 *
 * <p>Its weight is capped at {@link #MAX_WEIGHT} because the model learns from the same kind of
 * wording the heuristics already match, so its evidence is correlated with theirs.
 */
@Component
public class MlTextDetector implements DetectionModule {

    public static final String ID = "ml-text-classifier";
    static final double MAX_WEIGHT = 0.35;
    static final double REPORT_THRESHOLD = 0.6;

    private final MlModelService models;

    public MlTextDetector(MlModelService models) {
        this.models = models;
    }

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Optional ML text classifier (Naive Bayes)"; }
    @Override public String version() {
        return models.model(MlTask.MESSAGE).or(() -> models.model(MlTask.URL))
                .map(m -> m.metadata().modelVersion()).orElse("not-trained");
    }
    @Override public String description() {
        return "Multinomial Naive Bayes trained locally on a labeled dataset (see docs/ML.md). "
                + "Status: " + models.status();
    }
    @Override public Set<ScanCategory> categories() { return Set.of(ScanCategory.MESSAGE, ScanCategory.URL); }
    @Override public ResultSource sourceType() { return ResultSource.TRAINED_MODEL; }
    @Override public boolean available() { return models.anyAvailable(); }

    @Override
    public boolean appliesTo(FraudEvent event) {
        return taskFor(event).flatMap(models::model).isPresent();
    }

    @Override
    public DetectorResult analyze(FraudEvent event) {
        MlTask task = taskFor(event).orElseThrow();
        MlModelService.LoadedModel loaded = models.model(task).orElseThrow();
        String text = task == MlTask.MESSAGE ? event.message().text() : event.url().url();
        double p = loaded.model().probabilityFraud(text);
        List<Indicator> indicators = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        String pct = String.format(Locale.ROOT, "%.2f", p);
        if (p >= REPORT_THRESHOLD) {
            double w = 0.1 + (MAX_WEIGHT - 0.1) * (p - REPORT_THRESHOLD) / (1 - REPORT_THRESHOLD);
            indicators.add(new Indicator("ML_" + task.name() + "_CLASSIFIER", "ml:MODEL",
                    "Trained model leans towards fraud",
                    "The locally trained model (version " + loaded.metadata().modelVersion() + ") estimated p(fraud) = " + pct
                            + ". This is a statistical estimate, not a verdict.",
                    Math.min(MAX_WEIGHT, w), ID, ResultSource.TRAINED_MODEL, "p=" + pct));
        } else {
            notes.add("Trained model (version " + loaded.metadata().modelVersion() + ") estimated p(fraud) = " + pct
                    + ", below the reporting threshold of " + REPORT_THRESHOLD + ".");
        }
        notes.add("Model trained on: " + loaded.metadata().datasetDescription()
                + ". Its test metrics describe that dataset only, not real-world accuracy.");
        return new DetectorResult(ID, loaded.metadata().modelVersion(), ResultSource.TRAINED_MODEL, indicators, notes, true);
    }

    private static Optional<MlTask> taskFor(FraudEvent e) {
        return switch (e.category()) {
            case MESSAGE -> Optional.of(MlTask.MESSAGE);
            case URL -> Optional.of(MlTask.URL);
            default -> Optional.empty();
        };
    }
}
