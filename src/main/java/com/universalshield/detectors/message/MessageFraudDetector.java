package com.universalshield.detectors.message;

import com.universalshield.detectors.common.TextSignals;
import com.universalshield.engine.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyses user-supplied SMS, email, chat and social-message text for social-engineering
 * tactics. Produces indicators for phishing language, impersonation, OTP requests, urgency,
 * credential harvesting, threats, prize lures and suspicious payment demands.
 */
@Component
public class MessageFraudDetector implements DetectionModule {

    public static final String ID = "message-heuristic";
    static final String VERSION = "1.0.0";
    static final int MIN_WORDS = 4;

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Message fraud detector"; }
    @Override public String version() { return VERSION; }
    @Override public String description() {
        return "Scans message text for social-engineering tactics including phishing, "
                + "impersonation, OTP requests, urgency, credential harvesting, threats and lures.";
    }
    @Override public Set<ScanCategory> categories() { return Set.of(ScanCategory.MESSAGE); }
    @Override public ResultSource sourceType() { return ResultSource.LOCAL_HEURISTIC; }

    @Override
    public boolean appliesTo(FraudEvent event) {
        return event.category() == ScanCategory.MESSAGE && event.message() != null;
    }

    @Override
    public DetectorResult analyze(FraudEvent event) {
        String text = event.message().text();
        boolean sufficient = TextSignals.wordCount(text) >= MIN_WORDS;
        List<Indicator> indicators = TextSignals.match(text, TextSignals.ALL, Map.of(), ID, "");
        List<String> notes = new ArrayList<>();
        if (!sufficient) {
            notes.add("The message is very short (" + TextSignals.wordCount(text) + " words). "
                    + "There may not be enough context for a reliable assessment.");
        }

        // Check for sender-body mismatch (e.g. sender says "SBI" but body talks about Amazon)
        String sender = event.message().sender();
        if (sender != null && !sender.isBlank()) {
            String sLower = sender.toLowerCase();
            String tLower = text.toLowerCase();
            for (String brand : List.of("sbi", "hdfc", "icici", "axis", "kotak", "paypal", "amazon",
                    "apple", "microsoft", "netflix", "flipkart", "paytm", "phonepe")) {
                if (sLower.contains(brand) && !tLower.contains(brand)) {
                    indicators.add(new Indicator("MSG_SENDER_BODY_MISMATCH", "IMPERSONATION",
                            "Sender name does not match message content",
                            "The sender claims to be '" + sender + "' but the message body does not mention that organisation.",
                            0.35, ID, ResultSource.LOCAL_HEURISTIC, TextSignals.evidence(sender)));
                    break;
                }
            }
        }

        // Free webmail sender combined with an organisation claim in the body
        if (sender != null && sender.toLowerCase().matches(".*@(gmail|yahoo|outlook|hotmail|proton|protonmail|aol|yandex|mail)\\.[a-z.]+$")
                && indicators.stream().anyMatch(i -> i.group().equals("IMPERSONATION"))) {
            indicators.add(new Indicator("MSG_FREEMAIL_ORG_CLAIM", "IMPERSONATION",
                    "Organisation claim sent from a free email account",
                    "Banks, governments and large companies send official email from their own domains, not free webmail.",
                    0.45, ID, ResultSource.LOCAL_HEURISTIC, TextSignals.evidence(sender.substring(sender.indexOf('@')))));
        }

        // Prior history of this subject
        if (event.history().priorHighRiskOfSubject() > 0) {
            indicators.add(new Indicator("MSG_KNOWN_RISKY_SENDER", "KNOWN_RISKY",
                    "Sender flagged in your earlier scans",
                    "You scanned messages from this sender before and received a HIGH or CRITICAL result.",
                    0.45, ID, ResultSource.LOCAL_HEURISTIC, sender != null ? TextSignals.evidence(sender) : "same sender"));
        }

        return new DetectorResult(ID, VERSION, ResultSource.LOCAL_HEURISTIC, indicators, notes, sufficient);
    }
}
