package com.universalshield.ml.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.universalshield.detectors.message.MessageFraudDetector;
import com.universalshield.detectors.url.NotConfiguredDomainReputationProvider;
import com.universalshield.detectors.url.UrlRiskDetector;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.RecommendationCatalog;
import com.universalshield.engine.RiskEngine;
import com.universalshield.engine.RiskThresholds;
import com.universalshield.engine.input.MessageInput;
import com.universalshield.engine.input.UrlInput;
import com.universalshield.ml.LabeledText;
import com.universalshield.ml.MlTask;
import com.universalshield.ml.ModelMetadata;
import com.universalshield.ml.NaiveBayesModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Offline training and evaluation pipeline. Separate from the web application: it reads a CSV,
 * splits it reproducibly, trains Naive Bayes, compares against two baselines, and writes
 * model.json, metadata.json and evaluation.md.
 *
 * <pre>
 * mvnw -q compile exec:java -Dexec.args="--task message --data data/samples/messages_synthetic.csv --out models/message"
 * </pre>
 */
public final class TrainingCli {

    static final double DECISION_THRESHOLD = 0.5;
    static final double HEURISTIC_THRESHOLD = 30;

    private TrainingCli() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> a = parseArgs(args);
        MlTask task = MlTask.valueOf(a.getOrDefault("task", "message").toUpperCase());
        Path data = Path.of(a.getOrDefault("data", "data/samples/" + task.dirName() + "s_synthetic.csv"));
        Path out = Path.of(a.getOrDefault("out", "models/" + task.dirName()));
        long seed = Long.parseLong(a.getOrDefault("seed", "42"));
        double testRatio = Double.parseDouble(a.getOrDefault("test-ratio", "0.25"));
        String version = a.getOrDefault("version",
                task.dirName() + "-nb-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.now()));
        String description = a.getOrDefault("description", "SYNTHETIC demonstration data (" + data.getFileName() + ")");

        Result r = run(task, data, out, seed, testRatio, version, description);
        System.out.println(r.report());
        System.out.println("Model written to " + out.toAbsolutePath());
    }

    public record Result(ModelMetadata metadata, String report) {
    }

    public static Result run(MlTask task, Path data, Path out, long seed, double testRatio, String version,
                             String description) throws Exception {
        List<LabeledText> rows = DatasetLoader.load(data);
        DatasetSplitter.Split split = DatasetSplitter.split(rows, testRatio, seed);
        if (split.train().isEmpty() || split.test().isEmpty()) {
            throw new IllegalArgumentException("Dataset too small to split");
        }
        NaiveBayesModel model = NaiveBayesModel.train(split.train(), task);

        int[] actual = split.test().stream().mapToInt(LabeledText::label).toArray();
        BinaryMetrics modelM = evaluate(split.test(), t -> model.predict(t, DECISION_THRESHOLD) == 1);
        long trainPos = split.train().stream().filter(e -> e.label() == 1).count();
        int majority = trainPos * 2 >= split.train().size() ? 1 : 0;
        BinaryMetrics majorityM = BinaryMetrics.of(actual, java.util.Arrays.stream(actual).map(x -> majority).toArray());
        RiskEngine heuristics = heuristicEngine(task);
        BinaryMetrics heuristicM = evaluate(split.test(), t -> heuristics.evaluate(event(task, t)).riskScore() >= HEURISTIC_THRESHOLD);

        Map<String, Map<String, Object>> baselines = new LinkedHashMap<>();
        baselines.put("majorityClass(" + majority + ")", majorityM.toMap());
        baselines.put("heuristicDetector(score>=" + (int) HEURISTIC_THRESHOLD + ")", heuristicM.toMap());

        ModelMetadata meta = new ModelMetadata(version, task.name(), "Multinomial Naive Bayes, Laplace smoothing, unigram+bigram tokens",
                Instant.now().toString(), data.getFileName().toString(), sha256(data), description, rows.size(),
                split.duplicatesRemoved() + split.conflictsRemoved(), split.train().size(), split.test().size(), seed, testRatio,
                DECISION_THRESHOLD, model.vocabularySize(), modelM.toMap(), baselines,
                List.of("Metrics describe the held-out part of this dataset only; they are not real-world accuracy.",
                        "Synthetic or small datasets produce optimistic, unstable metrics.",
                        "Naive Bayes assumes token independence and can be fooled by unusual wording.",
                        "The model is a supporting signal; its weight in the risk score is capped."));

        Files.createDirectories(out);
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(out.resolve("model.json").toFile(), model.state());
        mapper.writeValue(out.resolve("metadata.json").toFile(), meta);
        String report = report(meta, modelM, majorityM, heuristicM);
        Files.writeString(out.resolve("evaluation.md"), report);
        return new Result(meta, report);
    }

    private static BinaryMetrics evaluate(List<LabeledText> test, Predicate<String> isFraud) {
        int[] actual = new int[test.size()];
        int[] pred = new int[test.size()];
        for (int i = 0; i < test.size(); i++) {
            actual[i] = test.get(i).label();
            pred[i] = isFraud.test(test.get(i).text()) ? 1 : 0;
        }
        return BinaryMetrics.of(actual, pred);
    }

    private static RiskEngine heuristicEngine(MlTask task) {
        var module = task == MlTask.MESSAGE ? new MessageFraudDetector()
                : new UrlRiskDetector(new NotConfiguredDomainReputationProvider());
        return new RiskEngine(List.of(module), RiskThresholds.DEFAULT, new RecommendationCatalog(), Clock.systemUTC());
    }

    private static FraudEvent event(MlTask task, String text) {
        return task == MlTask.MESSAGE ? FraudEvent.ofMessage(new MessageInput(text, "OTHER", null), null)
                : FraudEvent.ofUrl(new UrlInput(text), null);
    }

    static String report(ModelMetadata m, BinaryMetrics model, BinaryMetrics majority, BinaryMetrics heuristic) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Evaluation report: ").append(m.modelVersion()).append("\n\n");
        sb.append("Dataset: ").append(m.datasetDescription()).append(" (sha256 ").append(m.datasetSha256(), 0, 12).append("...)\n\n");
        sb.append("Rows loaded: ").append(m.rowsLoaded()).append(", removed as duplicates/conflicts: ").append(m.duplicatesRemoved())
                .append(", train: ").append(m.trainSize()).append(", test: ").append(m.testSize())
                .append(", seed: ").append(m.splitSeed()).append("\n\n");
        sb.append("| Classifier | Precision | Recall | F1 | FPR | TP | FP | TN | FN |\n|---|---|---|---|---|---|---|---|---|\n");
        row(sb, "Naive Bayes (p>=" + m.decisionThreshold() + ")", model);
        row(sb, "Majority-class baseline", majority);
        row(sb, "Heuristic detector baseline (score>=30)", heuristic);
        sb.append("\n**Limitations**\n\n");
        m.limitations().forEach(l -> sb.append("- ").append(l).append("\n"));
        return sb.toString();
    }

    private static void row(StringBuilder sb, String name, BinaryMetrics x) {
        sb.append(String.format(java.util.Locale.ROOT, "| %s | %.3f | %.3f | %.3f | %.3f | %d | %d | %d | %d |%n",
                name, x.precision(), x.recall(), x.f1(), x.falsePositiveRate(), x.tp(), x.fp(), x.tn(), x.fn()));
    }

    static String sha256(Path p) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));
    }

    static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") && i + 1 < args.length) {
                m.put(args[i].substring(2), args[++i]);
            } else {
                throw new IllegalArgumentException("Unexpected argument: " + args[i]
                        + ". Usage: --task message|url --data file.csv --out dir [--seed 42] [--test-ratio 0.25] [--version v]");
            }
        }
        return m;
    }
}
