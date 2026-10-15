package com.universalshield.detectors.call;

import com.universalshield.detectors.url.ReputationVerdict;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Default provider: reports honestly that no phone-number reputation data is available. */
@Component
@ConditionalOnMissingBean(value = PhoneReputationProvider.class, ignored = NotConfiguredPhoneReputationProvider.class)
public class NotConfiguredPhoneReputationProvider implements PhoneReputationProvider {

    @Override
    public ReputationVerdict lookup(String normalizedNumber) {
        return ReputationVerdict.notConfigured();
    }
}
