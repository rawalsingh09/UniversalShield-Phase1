package com.universalshield.detectors.call;

import com.universalshield.detectors.url.ReputationVerdict;

/**
 * Extension point for a verified phone-number reputation service. UniversalShield ships
 * without one and never claims live caller-ID knowledge.
 */
public interface PhoneReputationProvider {

    ReputationVerdict lookup(String normalizedNumber);
}
