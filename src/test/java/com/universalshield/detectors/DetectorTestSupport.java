package com.universalshield.detectors;

import com.universalshield.engine.DetectorResult;
import com.universalshield.engine.Indicator;

import java.util.List;

/** Small helpers shared by the detector tests. */
public final class DetectorTestSupport {

    private DetectorTestSupport() {
    }

    public static List<String> codes(DetectorResult r) {
        return r.indicators().stream().map(Indicator::code).toList();
    }

    public static List<String> groups(DetectorResult r) {
        return r.indicators().stream().map(Indicator::group).toList();
    }
}
