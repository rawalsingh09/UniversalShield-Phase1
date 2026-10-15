package com.universalshield.ml;

/** One labeled example. label: 1 = fraud/phishing, 0 = legitimate. */
public record LabeledText(String id, int label, String text) {

    public LabeledText {
        if (label != 0 && label != 1) {
            throw new IllegalArgumentException("label must be 0 or 1");
        }
    }
}
