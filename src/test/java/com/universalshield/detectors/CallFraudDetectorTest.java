package com.universalshield.detectors;

import com.universalshield.detectors.call.CallFraudDetector;
import com.universalshield.detectors.call.NotConfiguredPhoneReputationProvider;
import com.universalshield.detectors.url.ReputationVerdict;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.ResultSource;
import com.universalshield.engine.ScanHistory;
import com.universalshield.engine.input.CallInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.universalshield.detectors.DetectorTestSupport.codes;
import static org.assertj.core.api.Assertions.assertThat;

class CallFraudDetectorTest {

    private final CallFraudDetector detector = new CallFraudDetector(new NotConfiguredPhoneReputationProvider());

    private com.universalshield.engine.DetectorResult call(String who, String number, String desc) {
        return detector.analyze(FraudEvent.ofCall(new CallInput(who, number, desc, 10), null));
    }

    @Test
    void digitalArrestScriptIsDetected() {
        var r = call("Cyber crime police", "+91 90000 00000",
                "They said I am under digital arrest, must stay on the call and transfer money or face legal action");
        assertThat(codes(r)).contains("CALL_GOV_CLAIM", "THREAT_LANGUAGE", "SECRECY_REQUEST");
    }

    @Test
    void fakeBankSupportAskingForRemoteAccessAndOtp() {
        var r = call("Bank fraud department", null,
                "Asked me to install AnyDesk so they can secure the account and then read out the OTP");
        assertThat(codes(r)).contains("CALL_BANK_CLAIM", "REMOTE_ACCESS_REQUEST", "OTP_REQUEST");
        assertThat(r.indicators()).filteredOn(i -> i.code().equals("REMOTE_ACCESS_REQUEST")).first()
                .satisfies(i -> assertThat(i.weight()).isEqualTo(0.7));
    }

    @Test
    void ordinaryCallHasNoIndicatorsAndHonestNotes() {
        var r = call("Dentist reception", "+44 20 0000 0000", "Called to confirm my appointment for Tuesday morning at ten");
        assertThat(r.indicators()).isEmpty();
        assertThat(r.notes()).anyMatch(n -> n.contains("never records calls"));
        assertThat(r.notes()).anyMatch(n -> n.contains("Caller ID can be spoofed"));
        assertThat(r.notes()).anyMatch(n -> n.contains("no verified provider"));
    }

    @Test
    void shortDescriptionIsInsufficient() {
        assertThat(call(null, null, "weird call").inputSufficient()).isFalse();
    }

    @Test
    void shortNumberFormat() {
        assertThat(codes(call(null, "12345", "caller asked about my day today briefly"))).contains("CALL_SHORT_NUMBER");
    }

    @Test
    void verifiedPhoneReputationIsExternal() {
        var d = new CallFraudDetector(n -> new ReputationVerdict(ReputationVerdict.Status.MALICIOUS, "TestPhoneDB", "Reported 40 times"));
        var r = d.analyze(FraudEvent.ofCall(new CallInput(null, "+1 555 0100", "caller asked many odd questions today", null), null));
        assertThat(r.indicators()).anySatisfy(i -> assertThat(i.source()).isEqualTo(ResultSource.VERIFIED_EXTERNAL));
    }

    @Test
    void knownRiskyCaller() {
        var r = detector.analyze(FraudEvent.ofCall(new CallInput(null, "+1 555 0100", "they called again about the same thing",
                null), new ScanHistory(List.of(), 3, 2)));
        assertThat(codes(r)).contains("CALL_KNOWN_RISKY");
    }
}
