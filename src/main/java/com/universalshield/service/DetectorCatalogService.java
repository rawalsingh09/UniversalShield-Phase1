package com.universalshield.service;

import com.universalshield.domain.DetectorMetadata;
import com.universalshield.dto.DetectorInfo;
import com.universalshield.engine.DetectionModule;
import com.universalshield.engine.RiskEngine;
import com.universalshield.repository.DetectorMetadataRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Lists detectors and records their current versions in the detector_metadata table at startup. */
@Service
public class DetectorCatalogService {

    private final RiskEngine engine;
    private final DetectorMetadataRepository repo;

    public DetectorCatalogService(RiskEngine engine, DetectorMetadataRepository repo) {
        this.engine = engine;
        this.repo = repo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncMetadata() {
        for (DetectionModule m : engine.modules()) {
            DetectorMetadata md = repo.findById(m.id()).orElseGet(() -> new DetectorMetadata(m.id()));
            md.update(m.displayName(), m.version(), categories(m), m.sourceType().name(), m.description());
            repo.save(md);
        }
    }

    public List<DetectorInfo> list() {
        return engine.modules().stream()
                .sorted(Comparator.comparing(DetectionModule::id))
                .map(m -> new DetectorInfo(m.id(), m.displayName(), m.version(),
                        m.categories().stream().map(Enum::name).sorted().toList(), m.sourceType().name(),
                        m.available(), m.description()))
                .toList();
    }

    private static String categories(DetectionModule m) {
        return m.categories().stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }
}
