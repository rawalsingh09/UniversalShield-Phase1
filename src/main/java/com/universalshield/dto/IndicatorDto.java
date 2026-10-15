package com.universalshield.dto;

/** An indicator as returned by the API. */
public record IndicatorDto(String code, String group, String title, String explanation, double weight,
                           String detectorId, String source, String evidence) {
}
