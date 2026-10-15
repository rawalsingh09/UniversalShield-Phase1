package com.universalshield.detectors.call;

import com.universalshield.detectors.common.TextSignals;
import com.universalshield.detectors.url.ReputationVerdict;
import com.universalshield.engine.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyses user-entered call descriptions for impersonation, threats, remote-access
 * requests, and fake support patterns. Never records calls. No live caller-ID access.
 */
@Component
public class CallFraudDetector implements DetectionModule {

    public static final String ID = "call-heuristic";
    static final String VERSION = "1.0.0";
    static final int MIN_WORDS = 5;

    private final PhoneReputationProvider reputation;

    public CallFraudDetector(PhoneReputationProvider reputation) {
        this.reputation = reputation;
    }

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Call fraud screener"; }
    @Override public String version() { return VERSION; }
    @Override public String description() {
        return "Scans user-provided call descriptions for impersonation, threats, "
                + "remote-access requests, payment demands and fake-support patterns. "
                + "Does not access caller-ID data unless a verified provider is integrated.";
    }
    @Override public Set<ScanCategory> categories() { return Set.of(ScanCategory.CALL); }
    @Override public ResultSource sourceType() { return ResultSource.LOCAL_HEURISTIC; }

    @Override
    public boolean appliesTo(FraudEvent event) {
        return event.category() == ScanCategory.CALL && event.call() != null;
    }

    @Override
    public DetectorResult analyze(FraudEvent event) {
        var call = event.call();
        List<Indicator> indicators = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        String text = call.description();
        boolean sufficient = TextSignals.wordCount(text) >= MIN_WORDS;
        if (!sufficient) {
            notes.add("The call description is very short. Provide more detail for a better assessment.");
        }

        // Social-engineering text signals
        // Calls are live pressure situations, so key tactics weigh slightly more than in messages.
        indicators.addAll(TextSignals.match(text, TextSignals.ALL, Map.of(
                "ORG_IMPERSONATION", 0.3, "REMOTE_ACCESS_REQUEST", 0.7, "OTP_REQUEST", 0.75,
                "THREAT_LANGUAGE", 0.55, "PAYMENT_DEMAND", 0.45, "UNUSUAL_PAYMENT_METHOD", 0.5,
                "SECRECY_REQUEST", 0.45, "FAKE_SUPPORT", 0.4), ID, ""));

        // Claimed identity versus description
        if (call.callerClaimedIdentity() != null && !call.callerClaimedIdentity().isBlank()) {
            String claimed = call.callerClaimedIdentity().toLowerCase();
            for (String tag : List.of("police", "cyber cell", "cybercrime", "cbi", "ed ", "narcotics",
                    "income tax", "irs", "customs", "court", "judge", "rbi", "reserve bank", "trai")) {
                if (claimed.contains(tag)) {
                    indicators.add(new Indicator("CALL_GOV_CLAIM", "IMPERSONATION",
                            "Caller claims to be from a government or law-enforcement body",
                            "Government agencies almost never contact citizens by phone to demand payment or threaten arrest.",
                            0.55, ID, ResultSource.LOCAL_HEURISTIC, TextSignals.evidence(call.callerClaimedIdentity())));
                    break;
                }
            }
            for (String tag : List.of("bank", "customer care", "support", "fraud team", "fraud department")) {
                if (claimed.contains(tag)) {
                    indicators.add(new Indicator("CALL_BANK_CLAIM", "IMPERSONATION",
                            "Caller claims to be from a bank or support team",
                            "Real bank fraud teams do not ask for OTPs, PINs, passwords or screen sharing.",
                            0.3, ID, ResultSource.LOCAL_HEURISTIC, TextSignals.evidence(call.callerClaimedIdentity())));
                    break;
                }
            }
        }

        // Prior history
        if (event.history().priorHighRiskOfSubject() > 0) {
            indicators.add(new Indicator("CALL_KNOWN_RISKY", "KNOWN_RISKY",
                    "This caller number/identity flagged in your earlier scans",
                    "You scanned this number or identity before and received a HIGH or CRITICAL result.",
                    0.45, ID, ResultSource.LOCAL_HEURISTIC, "same caller"));
        }

        if (call.callerNumber() != null && !call.callerNumber().isBlank()) {
            String digits = call.callerNumber().replaceAll("[^0-9+]", "");
            if (digits.replace("+", "").length() < 7) {
                indicators.add(new Indicator("CALL_SHORT_NUMBER", "NUMBER_FORMAT", "Unusual number format",
                        "The number is shorter than a normal phone number. Some spoofed or masked calls show odd numbers.",
                        0.1, ID, ResultSource.LOCAL_HEURISTIC, "short number"));
            }
            ReputationVerdict v = reputation.lookup(digits);
            switch (v.status()) {
                case MALICIOUS, SUSPICIOUS -> indicators.add(new Indicator("CALL_REPUTATION", "REPUTATION",
                        "Number reported by " + v.provider(), v.detail(),
                        v.status() == ReputationVerdict.Status.MALICIOUS ? 0.85 : 0.45,
                        ID, ResultSource.VERIFIED_EXTERNAL, "provider listing"));
                case CLEAN -> notes.add(v.provider() + " has no reports for this number. That is not a guarantee.");
                default -> notes.add("Phone-number reputation: no verified provider is configured. Analysis is based on your description only.");
            }
        } else {
            notes.add("No caller number given. Analysis is based on your description only.");
        }
        notes.add("Caller ID can be spoofed: a familiar name or number on screen does not prove who is calling.");
        notes.add("UniversalShield never records calls and has no access to live caller-ID data.");
        return new DetectorResult(ID, VERSION, ResultSource.LOCAL_HEURISTIC, indicators, notes, sufficient);
    }
}
