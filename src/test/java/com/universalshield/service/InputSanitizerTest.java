package com.universalshield.service;

import com.universalshield.dto.ScanRequest;
import com.universalshield.engine.ScanCategory;
import com.universalshield.engine.input.MessageInput;
import com.universalshield.engine.input.TransactionInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InputSanitizerTest {

    private final InputSanitizer s = new InputSanitizer();

    @Test
    void masksLuhnValidCardNumbers() {
        assertThat(s.sanitize("card 4111 1111 1111 1111 declined")).isEqualTo("card [card number removed] declined");
    }

    @Test
    void masksCodesAfterKeywords() {
        assertThat(s.sanitize("your OTP is 4821 ok")).isEqualTo("your OTP is [code removed] ok");
        assertThat(s.sanitize("PIN: 1234")).isEqualTo("PIN: [code removed]");
        assertThat(s.sanitize("password = hunter2")).isEqualTo("password = [code removed]");
    }

    @Test
    void masksRemainingLongDigitRuns() {
        assertThat(s.sanitize("ref 98765432 thanks")).isEqualTo("ref •••••••• thanks");
        assertThat(s.sanitize("meet at 1030")).isEqualTo("meet at 1030");
    }

    @Test
    void excerptIsTruncatedAndSanitised() {
        var req = new ScanRequest(ScanCategory.MESSAGE, new MessageInput("x".repeat(600) + " otp 123456", "SMS", null), null, null, null, null);
        assertThat(s.excerpt(req)).hasSize(InputSanitizer.EXCERPT_MAX).endsWith("…");
        var tx = new ScanRequest(ScanCategory.TRANSACTION, null, null, new TransactionInput(new BigDecimal("12.50"), "INR", "bob",
                null, null, null, null, "card 4111111111111111"), null, null);
        assertThat(s.excerpt(tx)).isEqualTo("Amount 12.50 INR to bob | card [card number removed]");
    }

    @Test
    void luhn() {
        assertThat(InputSanitizer.luhnValid("4111111111111111")).isTrue();
        assertThat(InputSanitizer.luhnValid("4111111111111112")).isFalse();
    }
}
