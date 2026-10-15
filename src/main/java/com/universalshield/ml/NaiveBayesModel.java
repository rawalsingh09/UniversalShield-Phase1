package com.universalshield.ml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multinomial Naive Bayes binary classifier with Laplace smoothing, written in plain Java so
 * a student can read and explain every line. Label 1 = fraud, 0 = legitimate.
 *
 * <p>P(fraud | tokens) is computed in log space:
 * {@code log P(c) + sum_t log((count(t,c) + 1) / (total(c) + |V|))} and normalised with a
 * softmax over the two classes.
 */
public final class NaiveBayesModel {

    /** Serializable state (written to model.json). */
    public record State(String task, int[] docCounts, long[] tokenTotals,
                        Map<String, int[]> tokenCounts) {
    }

    private final State state;

    private NaiveBayesModel(State state) {
        this.state = state;
    }

    public static NaiveBayesModel fromState(State s) {
        if (s == null || s.docCounts() == null || s.docCounts().length != 2 || s.tokenTotals() == null
                || s.tokenTotals().length != 2 || s.tokenCounts() == null || s.docCounts()[0] + s.docCounts()[1] == 0) {
            throw new IllegalArgumentException("Invalid model state");
        }
        for (int[] c : s.tokenCounts().values()) {
            if (c == null || c.length != 2) {
                throw new IllegalArgumentException("Invalid token counts in model state");
            }
        }
        return new NaiveBayesModel(s);
    }

    public static NaiveBayesModel train(List<LabeledText> data, MlTask task) {
        int[] docs = new int[2];
        long[] totals = new long[2];
        Map<String, int[]> counts = new HashMap<>();
        for (LabeledText ex : data) {
            int y = ex.label();
            docs[y]++;
            for (String t : Tokenizer.tokenize(ex.text(), task)) {
                counts.computeIfAbsent(t, k -> new int[2])[y]++;
                totals[y]++;
            }
        }
        return fromState(new State(task.name(), docs, totals, counts));
    }

    public State state() {
        return state;
    }

    public int vocabularySize() {
        return state.tokenCounts().size();
    }

    /** Probability that the text belongs to the fraud class. */
    public double probabilityFraud(String text) {
        MlTask task = MlTask.valueOf(state.task());
        int totalDocs = state.docCounts()[0] + state.docCounts()[1];
        double v = Math.max(1, state.tokenCounts().size());
        double[] log = new double[2];
        for (int c = 0; c < 2; c++) {
            log[c] = Math.log((state.docCounts()[c] + 1.0) / (totalDocs + 2.0));
        }
        for (String t : Tokenizer.tokenize(text, task)) {
            int[] tc = state.tokenCounts().get(t);
            if (tc == null) {
                continue; // unseen tokens carry no information under this model
            }
            for (int c = 0; c < 2; c++) {
                log[c] += Math.log((tc[c] + 1.0) / (state.tokenTotals()[c] + v));
            }
        }
        double max = Math.max(log[0], log[1]);
        double e0 = Math.exp(log[0] - max);
        double e1 = Math.exp(log[1] - max);
        return e1 / (e0 + e1);
    }

    public int predict(String text, double threshold) {
        return probabilityFraud(text) >= threshold ? 1 : 0;
    }
}
