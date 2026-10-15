package com.universalshield.engine;

/**
 * One matched warning sign.
 *
 * @param code        stable machine-readable identifier, e.g. MSG_OTP_REQUEST
 * @param group       correlation group; indicators in the same group are not added together
 * @param title       short human-readable name
 * @param explanation why this sign matters
 * @param weight      contribution in (0, 1]; see docs/SCORING.md
 * @param detectorId  detector that produced it
 * @param source      heuristic, model or external data
 * @param evidence    short excerpt of what matched (never full sensitive input)
 */
public record Indicator(String code, String group, String title, String explanation,
                        double weight, String detectorId, ResultSource source, String evidence) {

    public Indicator {
        if (weight <= 0 || weight > 1) {
            throw new IllegalArgumentException("Indicator weight must be in (0, 1]");
        }
    }
}
