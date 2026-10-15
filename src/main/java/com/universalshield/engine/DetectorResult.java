package com.universalshield.engine;

import java.util.List;

/**
 * Output of a single {@link DetectionModule} for one event.
 *
 * @param detectorId        the module's id
 * @param detectorVersion   the module's version
 * @param source            dominant source type of this module's evidence
 * @param indicators        matched warning signs (may be empty)
 * @param notes             neutral observations (e.g. "external reputation not configured")
 * @param inputSufficient   false when the input was too short or incomplete to judge well
 */
public record DetectorResult(String detectorId, String detectorVersion, ResultSource source,
                             List<Indicator> indicators, List<String> notes, boolean inputSufficient) {

    public DetectorResult {
        indicators = List.copyOf(indicators);
        notes = List.copyOf(notes);
    }
}
