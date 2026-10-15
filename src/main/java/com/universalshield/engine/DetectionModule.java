package com.universalshield.engine;

import java.util.Set;

/**
 * Contract every detector implements. To add a detector, implement this interface and
 * register it as a Spring bean; the {@link RiskEngine} discovers it automatically.
 */
public interface DetectionModule {

    /** Stable id, e.g. "message-heuristic". */
    String id();

    String displayName();

    String version();

    String description();

    /** Categories this module is primarily built for. */
    Set<ScanCategory> categories();

    ResultSource sourceType();

    /** Whether the module is operational (e.g. an ML module without a model is not). */
    default boolean available() {
        return true;
    }

    /** Whether this module has something to say about the event. */
    boolean appliesTo(FraudEvent event);

    DetectorResult analyze(FraudEvent event);
}
