package com.universalshield.detectors.url;

/**
 * Extension point for a verified domain-reputation service (for example a Safe-Browsing
 * style API or a commercial threat-intelligence feed).
 *
 * <p>Implementations must look up the domain by name only. They must never make the server
 * fetch the user-supplied URL itself (SSRF risk). See docs/ARCHITECTURE.md.
 */
public interface DomainReputationProvider {

    ReputationVerdict lookup(String registrableDomain);
}
