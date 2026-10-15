package com.universalshield.ml.training;

import com.universalshield.ml.LabeledText;
import com.universalshield.ml.Tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Reproducible, leakage-aware train/test split.
 * <ol>
 *   <li>De-duplicates examples by normalised text (lower-case, digits mapped to 0), so the same
 *       message with a different code or amount cannot appear in both train and test.</li>
 *   <li>Drops examples whose normalised text appears with conflicting labels.</li>
 *   <li>Shuffles each class with a fixed seed and takes the same test fraction from each
 *       (stratified split).</li>
 * </ol>
 */
public final class DatasetSplitter {

    public record Split(List<LabeledText> train, List<LabeledText> test, int duplicatesRemoved, int conflictsRemoved) {
    }

    private DatasetSplitter() {
    }

    public static Split split(List<LabeledText> data, double testRatio, long seed) {
        if (testRatio <= 0 || testRatio >= 1) {
            throw new IllegalArgumentException("testRatio must be between 0 and 1");
        }
        Map<String, LabeledText> unique = new LinkedHashMap<>();
        Map<String, Boolean> conflict = new LinkedHashMap<>();
        int dups = 0;
        for (LabeledText ex : data) {
            String key = Tokenizer.normalize(ex.text());
            LabeledText prev = unique.putIfAbsent(key, ex);
            if (prev != null) {
                dups++;
                if (prev.label() != ex.label()) {
                    conflict.put(key, true);
                }
            }
        }
        conflict.keySet().forEach(unique::remove);

        List<LabeledText> pos = new ArrayList<>();
        List<LabeledText> neg = new ArrayList<>();
        unique.values().forEach(e -> (e.label() == 1 ? pos : neg).add(e));
        Random rnd = new Random(seed);
        Collections.shuffle(pos, rnd);
        Collections.shuffle(neg, rnd);

        List<LabeledText> train = new ArrayList<>();
        List<LabeledText> test = new ArrayList<>();
        for (List<LabeledText> cls : List.of(pos, neg)) {
            int nTest = (int) Math.round(cls.size() * testRatio);
            test.addAll(cls.subList(0, nTest));
            train.addAll(cls.subList(nTest, cls.size()));
        }
        return new Split(train, test, dups, conflict.size());
    }
}
