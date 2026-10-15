package com.universalshield.ml;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Converts text into features for the Naive Bayes model. Shared by training and prediction
 * so both see identical features.
 */
public final class Tokenizer {

    private Tokenizer() {
    }

    public static List<String> tokenize(String text, MlTask task) {
        String norm = normalize(text);
        List<String> out = new ArrayList<>();
        if (task == MlTask.URL) {
            for (String t : norm.split("[^a-z0-9]+")) {
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
            out.add("len:" + Math.min(norm.length() / 25, 8));
            out.add("dots:" + Math.min(norm.chars().filter(c -> c == '.').count(), 6));
            out.add("hyph:" + Math.min(norm.chars().filter(c -> c == '-').count(), 4));
            out.add(norm.startsWith("https://") ? "scheme:https" : norm.startsWith("http://") ? "scheme:http" : "scheme:none");
            return out;
        }
        List<String> words = new ArrayList<>();
        for (String t : norm.split("[^a-z0-9]+")) {
            if (t.length() >= 2) {
                words.add(t);
            }
        }
        out.addAll(words);
        for (int i = 0; i + 1 < words.size(); i++) {
            out.add(words.get(i) + "_" + words.get(i + 1));
        }
        return out;
    }

    /** Lower-cases, maps every digit to 0 and collapses whitespace. Also used for de-duplication. */
    public static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[0-9]", "0").replaceAll("\\s+", " ").trim();
    }
}
