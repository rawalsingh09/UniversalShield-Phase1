package com.universalshield.ml;

import java.util.List;
import java.util.Map;

/**
 * Contents of metadata.json, written by the training pipeline next to model.json.
 * Every number here was computed by {@code TrainingCli}; nothing is hand-entered.
 */
public record ModelMetadata(
        String modelVersion,
        String task,
        String algorithm,
        String createdAt,
        String datasetFile,
        String datasetSha256,
        String datasetDescription,
        int rowsLoaded,
        int duplicatesRemoved,
        int trainSize,
        int testSize,
        long splitSeed,
        double testRatio,
        double decisionThreshold,
        int vocabularySize,
        Map<String, Object> modelMetrics,
        Map<String, Map<String, Object>> baselineMetrics,
        List<String> limitations) {
}
