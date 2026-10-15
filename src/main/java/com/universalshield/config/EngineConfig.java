package com.universalshield.config;

import com.universalshield.engine.DetectionModule;
import com.universalshield.engine.RecommendationCatalog;
import com.universalshield.engine.RiskEngine;
import com.universalshield.engine.RiskThresholds;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

/** Wires the framework-free {@link RiskEngine} with every detector bean and configured thresholds. */
@Configuration
public class EngineConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RecommendationCatalog recommendationCatalog() {
        return new RecommendationCatalog();
    }

    @Bean
    public RiskThresholds riskThresholds(AppProperties props) {
        var t = props.risk().thresholds();
        return new RiskThresholds(t.medium(), t.high(), t.critical());
    }

    @Bean
    public RiskEngine riskEngine(List<DetectionModule> modules, RiskThresholds thresholds,
                                 RecommendationCatalog catalog, Clock clock) {
        return new RiskEngine(modules, thresholds, catalog, clock);
    }
}
