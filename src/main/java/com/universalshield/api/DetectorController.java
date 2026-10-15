package com.universalshield.api;

import com.universalshield.dto.ApiResponse;
import com.universalshield.dto.DetectorInfo;
import com.universalshield.engine.RiskEngine;
import com.universalshield.ml.MlModelService;
import com.universalshield.service.DetectorCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Public, read-only information about detectors, thresholds and ML status. */
@RestController
@Tag(name = "Detectors & methodology")
public class DetectorController {

    private final DetectorCatalogService catalog;
    private final RiskEngine engine;
    private final MlModelService ml;

    public DetectorController(DetectorCatalogService catalog, RiskEngine engine, MlModelService ml) {
        this.catalog = catalog;
        this.engine = engine;
        this.ml = ml;
    }

    @GetMapping("/api/detectors")
    @Operation(summary = "Available detection modules")
    public ApiResponse<List<DetectorInfo>> detectors() {
        return ApiResponse.ok(catalog.list());
    }

    @GetMapping("/api/methodology")
    @Operation(summary = "Scoring thresholds, ruleset version and ML model status")
    public ApiResponse<Map<String, Object>> methodology() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rulesetVersion", RiskEngine.RULESET_VERSION);
        m.put("thresholds", Map.of("medium", engine.thresholds().medium(), "high", engine.thresholds().high(),
                "critical", engine.thresholds().critical()));
        m.put("formula", "score = round(100 * (1 - product over groups of (1 - max weight in group)))");
        Map<String, Object> models = new LinkedHashMap<>();
        ml.status().forEach((task, status) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("status", status);
            ml.model(task).ifPresent(lm -> entry.put("metadata", lm.metadata()));
            models.put(task.name(), entry);
        });
        m.put("mlModels", models);
        return ApiResponse.ok(m);
    }
}
