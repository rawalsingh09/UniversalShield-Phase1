package com.universalshield.detectors;

import com.universalshield.detectors.message.MessageFraudDetector;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.ScanCategory;
import com.universalshield.engine.ScanHistory;
import com.universalshield.engine.input.MessageInput;
import com.universalshield.engine.input.UrlInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.universalshield.detectors.DetectorTestSupport.codes;
import static org.assertj.core.api.Assertions.assertThat;

class MessageFraudDetectorTest {

    private final MessageFraudDetector detector = new MessageFraudDetector();

    private FraudEvent msg(String text, String sender) {
        return FraudEvent.ofMessage(new MessageInput(text, "SMS", sender), null);
    }

    @Test
    void appliesOnlyToMessages() {
        assertThat(detector.appliesTo(msg("hello there friend", null))).isTrue();
        assertThat(detector.appliesTo(FraudEvent.ofUrl(new UrlInput("https://a.example"), null))).isFalse();
        assertThat(detector.categories()).containsExactly(ScanCategory.MESSAGE);
    }

    @Test
    void phishingMessageMatchesMultipleTactics() {
        var r = detector.analyze(msg("URGENT: your account will be suspended. Verify your account now and share the OTP to confirm.", null));
        assertThat(codes(r)).contains("URGENCY_LANGUAGE", "THREAT_LANGUAGE", "CREDENTIAL_REQUEST", "OTP_REQUEST");
        assertThat(r.inputSufficient()).isTrue();
    }

    @Test
    void prizeAndFeeLure() {
        var r = detector.analyze(msg("Congratulations you have won a lucky draw! Pay a processing fee of Rs 99 to claim your prize", null));
        assertThat(codes(r)).contains("PRIZE_LURE", "PAYMENT_DEMAND");
    }

    @Test
    void giftCardSecrecyAndRemoteAccess() {
        var r = detector.analyze(msg("Buy gift cards and send the codes. Keep this confidential. Install the AnyDesk app first.", null));
        assertThat(codes(r)).contains("UNUSUAL_PAYMENT_METHOD", "REMOTE_ACCESS_REQUEST");
    }

    @Test
    void ordinaryMessageHasNoIndicators() {
        var r = detector.analyze(msg("Hi, are we still meeting at the library at four to finish the assignment?", null));
        assertThat(r.indicators()).isEmpty();
    }

    @Test
    void bankOtpNotificationIsNotFlaggedAsOtpRequest() {
        var r = detector.analyze(msg("Your OTP for login is 482910. Do not share it with anyone. Bank staff will never ask for it.", null));
        assertThat(codes(r)).doesNotContain("OTP_REQUEST");
    }

    @Test
    void veryShortMessageIsMarkedInsufficient() {
        var r = detector.analyze(msg("call me", null));
        assertThat(r.inputSufficient()).isFalse();
        assertThat(r.notes()).anyMatch(n -> n.contains("very short"));
    }

    @Test
    void freeMailSenderClaimingToBeBankIsFlagged() {
        var r = detector.analyze(msg("Dear customer this is your bank, please update your details today", "security.team@gmail.com"));
        assertThat(codes(r)).contains("ORG_IMPERSONATION", "MSG_FREEMAIL_ORG_CLAIM");
    }

    @Test
    void senderBrandNotMentionedInBodyIsFlagged() {
        var r = detector.analyze(msg("Your package is waiting, confirm delivery details soon", "HDFC-ALERTS"));
        assertThat(codes(r)).contains("MSG_SENDER_BODY_MISMATCH");
    }

    @Test
    void priorHighRiskSenderAddsKnownRiskyIndicator() {
        var e = FraudEvent.ofMessage(new MessageInput("hello again from us, quick question", "SMS", "X"), new ScanHistory(List.of(), 2, 1));
        assertThat(codes(detector.analyze(e))).contains("MSG_KNOWN_RISKY_SENDER");
    }
}
