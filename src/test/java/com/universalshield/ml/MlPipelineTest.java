package com.universalshield.ml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.ResultSource;
import com.universalshield.engine.input.MessageInput;
import com.universalshield.ml.training.BinaryMetrics;
import com.universalshield.ml.training.DatasetLoader;
import com.universalshield.ml.training.DatasetSplitter;
import com.universalshield.ml.training.TrainingCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MlPipelineTest {

    static final Path MESSAGES = Path.of("data/samples/messages_synthetic.csv");

    @Test
    void loaderReadsQuotedCsvAndValidatesHeader() throws Exception {
        List<LabeledText> rows = DatasetLoader.load(MESSAGES);
        assertThat(rows).hasSize(100);
        assertThat(rows.stream().filter(r -> r.label() == 1)).hasSize(50);
        assertThatThrownBy(() -> DatasetLoader.load(writeTemp("a,b,c\n1,2,3\n"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DatasetLoader.load(writeTemp("id,label,text\n1,x,hello\n"))).isInstanceOf(IllegalArgumentException.class);
    }

    @TempDir
    Path tmp;

    Path writeTemp(String content) throws Exception {
        Path p = tmp.resolve("d" + System.nanoTime() + ".csv");
        Files.writeString(p, content);
        return p;
    }

    @Test
    void splitIsReproducibleStratifiedAndLeakFree() throws Exception {
        List<LabeledText> rows = new java.util.ArrayList<>(DatasetLoader.load(MESSAGES));
        // near-duplicate that differs only by digits must not leak across the split
        rows.add(new LabeledText("dup", 1, rows.get(0).text().replaceAll("[0-9]", "7")));
        var a = DatasetSplitter.split(rows, 0.25, 42);
        var b = DatasetSplitter.split(rows, 0.25, 42);
        assertThat(a.train()).isEqualTo(b.train());
        assertThat(a.test()).isEqualTo(b.test());
        assertThat(a.duplicatesRemoved()).isEqualTo(1);
        Set<String> trainKeys = a.train().stream().map(r -> Tokenizer.normalize(r.text())).collect(Collectors.toSet());
        Set<String> testKeys = a.test().stream().map(r -> Tokenizer.normalize(r.text())).collect(Collectors.toSet());
        Set<String> overlap = new HashSet<>(trainKeys);
        overlap.retainAll(testKeys);
        assertThat(overlap).isEmpty();
        assertThat(a.test().stream().filter(r -> r.label() == 1).count()).isEqualTo(a.test().stream().filter(r -> r.label() == 0).count());
        var c = DatasetSplitter.split(rows, 0.25, 7);
        assertThat(c.test()).isNotEqualTo(a.test());
    }

    @Test
    void conflictingLabelsAreDropped() {
        var s = DatasetSplitter.split(List.of(new LabeledText("1", 1, "same text"), new LabeledText("2", 0, "Same  TEXT"),
                new LabeledText("3", 1, "fraud a"), new LabeledText("4", 0, "fine b")), 0.5, 1);
        assertThat(s.conflictsRemoved()).isEqualTo(1);
        assertThat(s.train().size() + s.test().size()).isEqualTo(2);
    }

    @Test
    void metricsMatchHandComputedValues() {
        // actual:    1 1 1 0 0 0 0
        // predicted: 1 1 0 1 0 0 0  -> tp2 fn1 fp1 tn3
        BinaryMetrics m = BinaryMetrics.of(new int[] {1, 1, 1, 0, 0, 0, 0}, new int[] {1, 1, 0, 1, 0, 0, 0});
        assertThat(m.tp()).isEqualTo(2);
        assertThat(m.precision()).isEqualTo(2.0 / 3);
        assertThat(m.recall()).isEqualTo(2.0 / 3);
        assertThat(m.f1()).isEqualTo(2.0 / 3);
        assertThat(m.falsePositiveRate()).isEqualTo(0.25);
        assertThat(BinaryMetrics.of(new int[] {1, 0}, new int[] {0, 0}).toMap()).containsKey("note");
    }

    @Test
    void naiveBayesLearnsSeparableData() {
        var data = List.of(new LabeledText("1", 1, "share your otp now urgent"), new LabeledText("2", 1, "send otp to verify account"),
                new LabeledText("3", 0, "see you at lunch tomorrow"), new LabeledText("4", 0, "lunch meeting moved to noon"));
        NaiveBayesModel m = NaiveBayesModel.train(data, MlTask.MESSAGE);
        assertThat(m.probabilityFraud("please share the otp")).isGreaterThan(0.5);
        assertThat(m.probabilityFraud("lunch tomorrow at noon")).isLessThan(0.5);
        assertThatThrownBy(() -> NaiveBayesModel.fromState(new NaiveBayesModel.State("MESSAGE", new int[] {0, 0}, new long[] {0, 0}, java.util.Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trainingCliWritesModelMetadataAndReportThatServiceCanLoad() throws Exception {
        Path out = tmp.resolve("models/message");
        var result = TrainingCli.run(MlTask.MESSAGE, MESSAGES, out, 42, 0.25, "test-v1", "SYNTHETIC test data");
        assertThat(Files.exists(out.resolve("model.json"))).isTrue();
        assertThat(Files.exists(out.resolve("metadata.json"))).isTrue();
        assertThat(Files.readString(out.resolve("evaluation.md"))).contains("Majority-class baseline", "Heuristic detector baseline");
        var meta = result.metadata();
        assertThat(meta.trainSize() + meta.testSize()).isEqualTo(100);
        assertThat(meta.modelMetrics()).containsKeys("precision", "recall", "f1", "falsePositiveRate", "confusionMatrix");
        assertThat(meta.baselineMetrics()).hasSize(2);
        assertThat(meta.datasetSha256()).hasSize(64);

        MlModelService service = new MlModelService(new ObjectMapper(), true, tmp.resolve("models").toString());
        service.load();
        assertThat(service.model(MlTask.MESSAGE)).isPresent();
        assertThat(service.model(MlTask.URL)).isEmpty();
        assertThat(service.status().get(MlTask.URL)).startsWith("UNAVAILABLE");

        MlTextDetector detector = new MlTextDetector(service);
        assertThat(detector.available()).isTrue();
        var r = detector.analyze(FraudEvent.ofMessage(new MessageInput(
                "URGENT your account will be blocked, share the OTP and update KYC now", "SMS", null), null));
        assertThat(r.source()).isEqualTo(ResultSource.TRAINED_MODEL);
        assertThat(r.indicators()).allSatisfy(i -> assertThat(i.weight()).isLessThanOrEqualTo(0.35));
        assertThat(r.notes()).anyMatch(n -> n.contains("not real-world accuracy"));
    }

    @Test
    void missingOrDisabledModelIsReportedUnavailable() {
        MlModelService missing = new MlModelService(new ObjectMapper(), true, tmp.resolve("nothing").toString());
        missing.load();
        assertThat(missing.anyAvailable()).isFalse();
        assertThat(new MlTextDetector(missing).available()).isFalse();
        MlModelService disabled = new MlModelService(new ObjectMapper(), false, "models");
        disabled.load();
        assertThat(disabled.status().get(MlTask.MESSAGE)).contains("disabled");
    }

    @Test
    void corruptModelFileIsRejectedSafely() throws Exception {
        Path dir = tmp.resolve("bad/message");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("model.json"), "{\"task\":\"MESSAGE\"}");
        Files.writeString(dir.resolve("metadata.json"), "{}");
        MlModelService s = new MlModelService(new ObjectMapper(), true, tmp.resolve("bad").toString());
        s.load();
        assertThat(s.model(MlTask.MESSAGE)).isEmpty();
        assertThat(s.status().get(MlTask.MESSAGE)).contains("could not be loaded");
    }
}
