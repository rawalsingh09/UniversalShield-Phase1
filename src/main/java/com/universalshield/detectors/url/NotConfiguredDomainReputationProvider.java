package com.universalshield.detectors.url;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Default provider: honestly reports that no external reputation data is available. */
@Component
@ConditionalOnMissingBean(value = DomainReputationProvider.class, ignored = NotConfiguredDomainReputationProvider.class)
public class NotConfiguredDomainReputationProvider implements DomainReputationProvider {

    @Override
    public ReputationVerdict lookup(String registrableDomain) {
        return ReputationVerdict.notConfigured();
    }
}
