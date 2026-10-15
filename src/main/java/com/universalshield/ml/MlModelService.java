package com.universalshield.ml;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads trained models produced by the offline training pipeline from
 * {@code <model-dir>/<task>/model.json} and {@code metadata.json}. Only JSON data is loaded,
 * never executable code. If a model is missing or invalid, ML is reported as unavailable and the
 * heuristic detectors keep working.
 */
@Service
public class MlModelService {

    private static final Logger log = LoggerFactory.getLogger(MlModelService.class);
    static final long MAX_MODEL_BYTES = 20L * 1024 * 1024;

    public record LoadedModel(NaiveBayesModel model, ModelMetadata metadata) {
    }

    private final ObjectMapper mapper;
    private final boolean enabled;
    private final Path modelDir;
    private final Map<MlTask, LoadedModel> models = new EnumMap<>(MlTask.class);
    private final Map<MlTask, String> status = new EnumMap<>(MlTask.class);

    public MlModelService(ObjectMapper mapper,
                          @Value("${universalshield.ml.enabled:true}") boolean enabled,
                          @Value("${universalshield.ml.model-dir:models}") String modelDir) {
        this.mapper = mapper;
        this.enabled = enabled;
        this.modelDir = Path.of(modelDir);
    }

    @PostConstruct
    public void load() {
        models.clear();
        for (MlTask task : MlTask.values()) {
            if (!enabled) {
                status.put(task, "UNAVAILABLE: ML disabled by configuration");
                continue;
            }
            Path dir = modelDir.resolve(task.dirName());
            Path modelFile = dir.resolve("model.json");
            Path metaFile = dir.resolve("metadata.json");
            try {
                if (!Files.isRegularFile(modelFile) || !Files.isRegularFile(metaFile)) {
                    status.put(task, "UNAVAILABLE: no trained model found in " + dir);
                    continue;
                }
                if (Files.size(modelFile) > MAX_MODEL_BYTES) {
                    status.put(task, "UNAVAILABLE: model file too large");
                    continue;
                }
                NaiveBayesModel.State state = mapper.readValue(modelFile.toFile(), NaiveBayesModel.State.class);
                ModelMetadata meta = mapper.readValue(metaFile.toFile(), ModelMetadata.class);
                if (!task.name().equals(state.task())) {
                    status.put(task, "UNAVAILABLE: model task mismatch");
                    continue;
                }
                models.put(task, new LoadedModel(NaiveBayesModel.fromState(state), meta));
                status.put(task, "AVAILABLE: version " + meta.modelVersion());
                log.info("Loaded {} model version {}", task, meta.modelVersion());
            } catch (Exception e) {
                status.put(task, "UNAVAILABLE: model could not be loaded");
                log.warn("Could not load {} model from {}: {}", task, dir, e.getClass().getSimpleName());
            }
        }
    }

    public Optional<LoadedModel> model(MlTask task) {
        return Optional.ofNullable(models.get(task));
    }

    public boolean anyAvailable() {
        return !models.isEmpty();
    }

    public Map<MlTask, String> status() {
        return Map.copyOf(status);
    }
}
