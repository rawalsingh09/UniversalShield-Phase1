package com.universalshield.detectors.transaction;

import com.universalshield.detectors.common.TextSignals;
import com.universalshield.engine.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyses user-submitted transaction metadata for warning signs of fraud.
 * Never collects card numbers, CVV, PIN or OTP.
 */
@Component
public class TransactionFraudDetector implements DetectionModule {

    public static final String ID = "transaction-heuristic";
    static final String VERSION = "1.0.0";
    static final int MIN_HISTORY = 5;
    public static final BigDecimal DEFAULT_LARGE_AMOUNT = BigDecimal.valueOf(10_000);

    private final BigDecimal largeAmount;

    public TransactionFraudDetector() {
        this(DEFAULT_LARGE_AMOUNT);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TransactionFraudDetector(
            @org.springframework.beans.factory.annotation.Value("${universalshield.transaction.large-amount:10000}") BigDecimal largeAmount) {
        this.largeAmount = largeAmount;
    }

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Transaction fraud detector"; }
    @Override public String version() { return VERSION; }
    @Override public String description() {
        return "Checks transaction metadata for unusual amounts, new recipients, "
                + "repeated attempts, suspicious payment requests and risky payment methods.";
    }
    @Override public Set<ScanCategory> categories() { return Set.of(ScanCategory.TRANSACTION); }
    @Override public ResultSource sourceType() { return ResultSource.LOCAL_HEURISTIC; }

    @Override
    public boolean appliesTo(FraudEvent event) {
        return event.category() == ScanCategory.TRANSACTION && event.transaction() != null;
    }

    @Override
    public DetectorResult analyze(FraudEvent event) {
        var tx = event.transaction();
        var hist = event.history();
        List<Indicator> indicators = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        // Amount anomaly: compared with the user's own history when at least MIN_HISTORY amounts exist
        List<BigDecimal> previous = hist.previousTransactionAmounts();
        if (previous.size() >= MIN_HISTORY) {
            BigDecimal median = median(previous);
            if (median.signum() > 0) {
                BigDecimal ratio = tx.amount().divide(median, MathContext.DECIMAL64);
                if (ratio.compareTo(BigDecimal.TEN) >= 0) {
                    indicators.add(ind("TX_AMOUNT_EXTREME", "AMOUNT_ANOMALY", "Amount far above your usual payments",
                            "This amount is at least 10x the median of your previous " + previous.size() + " scanned payments.",
                            0.5, "≈" + ratio.setScale(0, java.math.RoundingMode.HALF_UP) + "x median"));
                } else if (ratio.compareTo(BigDecimal.valueOf(3)) >= 0) {
                    indicators.add(ind("TX_AMOUNT_UNUSUAL", "AMOUNT_ANOMALY", "Amount above your usual payments",
                            "This amount is at least 3x the median of your previous " + previous.size() + " scanned payments.",
                            0.35, "≈" + ratio.setScale(1, java.math.RoundingMode.HALF_UP) + "x median"));
                }
            }
        } else {
            notes.add("Not enough history to judge whether the amount is unusual (" + previous.size() + " of "
                    + MIN_HISTORY + " previous transaction scans).");
            if (tx.amount().compareTo(largeAmount) >= 0) {
                indicators.add(ind("TX_LARGE_AMOUNT", "AMOUNT_ANOMALY", "Large amount",
                        "The amount is at or above the configured large-payment threshold (" + largeAmount.toPlainString()
                                + "). Large payments deserve extra verification.", 0.2, tx.amount().toPlainString()));
            }
        }

        // New recipient
        if (Boolean.TRUE.equals(tx.recipientIsNew())
                || (tx.recipientIsNew() == null && tx.recipient() != null && !tx.recipient().isBlank()
                && hist.priorScansOfSubject() == 0 && !previous.isEmpty())) {
            indicators.add(ind("TX_NEW_RECIPIENT", "RECIPIENT", "New recipient",
                    "First-time payees are where most authorised-push-payment fraud happens.",
                    0.3, tx.recipient() != null ? TextSignals.evidence(tx.recipient()) : "new"));
        }

        // Rapid retries
        if (tx.attemptsLastHour() != null && tx.attemptsLastHour() >= 3) {
            indicators.add(ind("TX_VELOCITY", "VELOCITY", "Multiple payment attempts in one hour",
                    "Repeated attempts may indicate pressure from a scammer.",
                    tx.attemptsLastHour() >= 5 ? 0.5 : 0.3, tx.attemptsLastHour() + " attempts"));
        }

        // Requested by other party
        if (Boolean.TRUE.equals(tx.requestedByOtherParty())) {
            indicators.add(ind("TX_REQUESTED", "PAYMENT_REQUEST", "Payment requested by the other party",
                    "Being asked to send money is a warning sign. Verify the request through a channel you trust.",
                    0.3, "requested"));
        }

        // Risky payment method
        if (tx.paymentMethod() != null) {
            switch (tx.paymentMethod()) {
                case "GIFT_CARD" -> indicators.add(ind("TX_GIFT_CARD", "PAYMENT_METHOD", "Gift card payment",
                        "Gift cards are a favourite of scammers because payments cannot be reversed.", 0.55, "GIFT_CARD"));
                case "CRYPTO" -> indicators.add(ind("TX_CRYPTO", "PAYMENT_METHOD", "Cryptocurrency payment",
                        "Crypto payments are irreversible and hard to trace.", 0.4, "CRYPTO"));
                case "CASH" -> { /* cash is fine by itself */ }
                default -> { /* standard methods carry no standalone risk signal */ }
            }
        }

        // Prior history
        if (hist.priorHighRiskOfSubject() > 0) {
            indicators.add(ind("TX_KNOWN_RISKY", "KNOWN_RISKY", "Recipient flagged in your earlier scans",
                    "You scanned this recipient before and received a HIGH or CRITICAL result.",
                    0.45, tx.recipient() != null ? TextSignals.evidence(tx.recipient()) : "same recipient"));
        }

        // Scan free-text description for social-engineering language
        if (tx.description() != null && !tx.description().isBlank()) {
            indicators.addAll(TextSignals.match(tx.description(), TextSignals.ALL,
                    Map.of("URGENCY_LANGUAGE", 0.2, "ORG_IMPERSONATION", 0.15), ID, ""));
            if (tx.description().matches("(?s).*\\b\\d{13,19}\\b.*")) {
                notes.add("The description appears to contain a card or account number. It was masked before storage. Never enter card numbers, CVV, PIN or OTP here.");
            }
        }

        boolean sufficient = (tx.description() != null && !tx.description().isBlank())
                || tx.recipientIsNew() != null || tx.attemptsLastHour() != null || tx.requestedByOtherParty() != null;
        if (!sufficient) {
            notes.add("Only the amount was provided. Add context (recipient, who asked for the payment, description) for a meaningful assessment.");
        }
        notes.add("Transaction analysis is based only on the details you entered. UniversalShield has no access to your bank account.");
        return new DetectorResult(ID, VERSION, ResultSource.LOCAL_HEURISTIC, indicators, notes, sufficient);
    }

    static BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2)
                : sorted.get(n / 2 - 1).add(sorted.get(n / 2)).divide(BigDecimal.valueOf(2), MathContext.DECIMAL64);
    }

    private static Indicator ind(String code, String group, String title, String explanation, double weight, String evidence) {
        return new Indicator(code, group, title, explanation, weight, ID, ResultSource.LOCAL_HEURISTIC, evidence);
    }
}
