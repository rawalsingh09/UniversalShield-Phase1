package com.universalshield.detectors;

import com.universalshield.detectors.transaction.TransactionFraudDetector;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.ScanHistory;
import com.universalshield.engine.input.TransactionInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.universalshield.detectors.DetectorTestSupport.codes;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionFraudDetectorTest {

    private final TransactionFraudDetector detector = new TransactionFraudDetector(new BigDecimal("10000"));

    private static TransactionInput tx(String amount, Boolean isNew, String method, Integer attempts, Boolean requested, String desc) {
        return new TransactionInput(new BigDecimal(amount), "INR", "someone@upi", isNew, method, attempts, requested, desc);
    }

    private static ScanHistory history(String... amounts) {
        return new ScanHistory(java.util.Arrays.stream(amounts).map(BigDecimal::new).toList(), 0, 0);
    }

    @Test
    void receiveMoneyPinTrickIsDetected() {
        var r = detector.analyze(FraudEvent.ofTransaction(tx("2000", true, "UPI", 1, true,
                "Buyer says enter your UPI PIN to receive the payment"), null));
        assertThat(codes(r)).contains("PIN_TO_RECEIVE", "TX_NEW_RECIPIENT", "TX_REQUESTED");
    }

    @Test
    void amountFarAboveMedianHistory() {
        var r = detector.analyze(FraudEvent.ofTransaction(tx("50000", false, "UPI", 0, false, null),
                history("500", "700", "400", "600", "550")));
        assertThat(codes(r)).contains("TX_AMOUNT_EXTREME");
    }

    @Test
    void amountModeratelyAboveMedianHistory() {
        var r = detector.analyze(FraudEvent.ofTransaction(tx("2000", false, "UPI", 0, false, null),
                history("500", "700", "400", "600", "550")));
        assertThat(codes(r)).contains("TX_AMOUNT_UNUSUAL").doesNotContain("TX_AMOUNT_EXTREME");
    }

    @Test
    void withoutEnoughHistoryUsesConfiguredLargeAmountAndSaysSo() {
        var r = detector.analyze(FraudEvent.ofTransaction(tx("15000", false, "UPI", 0, false, null), history("100")));
        assertThat(codes(r)).contains("TX_LARGE_AMOUNT");
        assertThat(r.notes()).anyMatch(n -> n.contains("Not enough history"));
        var small = detector.analyze(FraudEvent.ofTransaction(tx("500", false, "UPI", 0, false, null), null));
        assertThat(codes(small)).doesNotContain("TX_LARGE_AMOUNT");
    }

    @Test
    void repeatedAttemptsScaleWeight() {
        var three = detector.analyze(FraudEvent.ofTransaction(tx("100", false, "UPI", 3, false, null), null));
        var six = detector.analyze(FraudEvent.ofTransaction(tx("100", false, "UPI", 6, false, null), null));
        assertThat(three.indicators()).filteredOn(i -> i.code().equals("TX_VELOCITY")).first()
                .satisfies(i -> assertThat(i.weight()).isEqualTo(0.3));
        assertThat(six.indicators()).filteredOn(i -> i.code().equals("TX_VELOCITY")).first()
                .satisfies(i -> assertThat(i.weight()).isEqualTo(0.5));
    }

    @Test
    void giftCardAndCryptoMethods() {
        assertThat(codes(detector.analyze(FraudEvent.ofTransaction(tx("100", null, "GIFT_CARD", null, null, null), null)))).contains("TX_GIFT_CARD");
        assertThat(codes(detector.analyze(FraudEvent.ofTransaction(tx("100", null, "CRYPTO", null, null, null), null)))).contains("TX_CRYPTO");
    }

    @Test
    void knownRiskyRecipient() {
        var r = detector.analyze(FraudEvent.ofTransaction(tx("100", false, "UPI", 0, false, null), new ScanHistory(List.of(), 2, 1)));
        assertThat(codes(r)).contains("TX_KNOWN_RISKY");
    }

    @Test
    void ordinaryPaymentHasNoIndicators() {
        var r = detector.analyze(FraudEvent.ofTransaction(tx("450", false, "UPI", 1, false, "Monthly rent share to my roommate"), null));
        assertThat(r.indicators()).isEmpty();
        assertThat(r.inputSufficient()).isTrue();
    }

    @Test
    void amountOnlyIsInsufficient() {
        var r = detector.analyze(FraudEvent.ofTransaction(new TransactionInput(new BigDecimal("10"), null, null, null, null, null, null, null), null));
        assertThat(r.inputSufficient()).isFalse();
    }

    @Test
    void cardNumberInDescriptionTriggersWarningNote() {
        var r = detector.analyze(FraudEvent.ofTransaction(tx("10", false, "CARD", 0, false, "my card 4111111111111111 failed"), null));
        assertThat(r.notes()).anyMatch(n -> n.contains("card or account number"));
    }
}
