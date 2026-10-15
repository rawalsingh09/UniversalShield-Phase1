package com.universalshield.detectors;

import com.universalshield.detectors.common.TextSignals;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TextSignalsTest {

    private List<String> codes(String text) {
        return TextSignals.match(text, TextSignals.ALL, Map.of(), "t", "").stream().map(i -> i.code()).toList();
    }

    @Test
    void detectsOtpRequest() {
        assertThat(codes("Please share the OTP you received to confirm")).contains("OTP_REQUEST");
    }

    @Test
    void negatedOtpWarningFromBankIsNotARequest() {
        assertThat(codes("Your OTP is 123456. Never share this OTP with anyone.")).doesNotContain("OTP_REQUEST");
        assertThat(codes("Do not share your verification code with anyone")).doesNotContain("OTP_REQUEST");
    }

    @Test
    void overridesChangeWeight() {
        var inds = TextSignals.match("act now", List.of(TextSignals.URGENCY), Map.of("URGENCY_LANGUAGE", 0.9), "t", "x:");
        assertThat(inds).singleElement().satisfies(i -> {
            assertThat(i.weight()).isEqualTo(0.9);
            assertThat(i.group()).isEqualTo("x:URGENCY");
        });
    }

    @Test
    void evidenceMasksLongDigitRunsAndIsShortened() {
        assertThat(TextSignals.evidence("code 987654 here")).isEqualTo("code •••• here");
        assertThat(TextSignals.evidence("a".repeat(200))).hasSize(60).endsWith("…");
    }

    @Test
    void digitalArrestCallSignals() {
        var c = codes("I am under digital arrest, must stay on video call, not tell anyone and transfer money to verify.");
        assertThat(c).contains("THREAT_LANGUAGE", "SECRECY_REQUEST", "PAYMENT_DEMAND");
        assertThat(codes("Send 20000 urgently, I will explain later. Don't tell dad.")).contains("SECRECY_REQUEST");
    }

    @Test
    void bankWarningNotToTellAnyoneTheOtpIsNotSecrecy() {
        assertThat(codes("Never tell anyone your OTP or PIN. Bank staff will never ask for it.")).doesNotContain("SECRECY_REQUEST");
        assertThat(codes("Do not share with anyone your card details.")).doesNotContain("SECRECY_REQUEST");
    }

    @Test
    void pinToReceiveMoneyWithPossessive() {
        assertThat(codes("Buyer says I must enter my UPI PIN to receive the payment.")).contains("PIN_TO_RECEIVE");
    }

    @Test
    void blankTextYieldsNothing() {
        assertThat(codes("   ")).isEmpty();
        assertThat(TextSignals.wordCount(null)).isZero();
        assertThat(TextSignals.wordCount(" two words ")).isEqualTo(2);
    }
}
