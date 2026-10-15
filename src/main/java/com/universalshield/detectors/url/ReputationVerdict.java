package com.universalshield.detectors.url;

/**
 * Answer from a reputation provider.
 *
 * @param status   NOT_CONFIGURED when no provider is integrated
 * @param provider name of the provider that answered
 * @param detail   provider-supplied detail, shown to the user verbatim
 */
public record ReputationVerdict(Status status, String provider, String detail) {

    public enum Status { NOT_CONFIGURED, UNKNOWN, CLEAN, SUSPICIOUS, MALICIOUS }

    public static ReputationVerdict notConfigured() {
        return new ReputationVerdict(Status.NOT_CONFIGURED, "none", "No verified reputation provider is configured.");
    }
}
