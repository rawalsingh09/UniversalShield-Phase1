package com.universalshield.ml.training;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Confusion matrix and derived metrics for a binary classifier (positive class = fraud).
 * Undefined ratios (division by zero) are reported as 0.0 and flagged in {@link #toMap()}.
 */
public record BinaryMetrics(int tp, int fp, int tn, int fn) {

    public static BinaryMetrics of(int[] actual, int[] predicted) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (int i = 0; i < actual.length; i++) {
            if (predicted[i] == 1 && actual[i] == 1) tp++;
            else if (predicted[i] == 1) fp++;
            else if (actual[i] == 1) fn++;
            else tn++;
        }
        return new BinaryMetrics(tp, fp, tn, fn);
    }

    private static double ratio(int a, int b) {
        return b == 0 ? 0.0 : (double) a / b;
    }

    public double precision() { return ratio(tp, tp + fp); }
    public double recall() { return ratio(tp, tp + fn); }
    public double falsePositiveRate() { return ratio(fp, fp + tn); }
    public double accuracy() { return ratio(tp + tn, tp + tn + fp + fn); }

    public double f1() {
        double p = precision();
        double r = recall();
        return p + r == 0 ? 0.0 : 2 * p * r / (p + r);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("precision", round(precision()));
        m.put("recall", round(recall()));
        m.put("f1", round(f1()));
        m.put("falsePositiveRate", round(falsePositiveRate()));
        m.put("accuracy", round(accuracy()));
        m.put("confusionMatrix", Map.of("tp", tp, "fp", fp, "tn", tn, "fn", fn));
        if (tp + fp == 0) {
            m.put("note", "No positive predictions; precision is undefined and reported as 0.");
        }
        return m;
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
