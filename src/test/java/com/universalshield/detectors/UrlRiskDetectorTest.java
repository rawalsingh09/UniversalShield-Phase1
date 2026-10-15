package com.universalshield.detectors;

import com.universalshield.detectors.url.DomainReputationProvider;
import com.universalshield.detectors.url.NotConfiguredDomainReputationProvider;
import com.universalshield.detectors.url.ParsedUrl;
import com.universalshield.detectors.url.ReputationVerdict;
import com.universalshield.detectors.url.UrlRiskDetector;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.ResultSource;
import com.universalshield.engine.ScanHistory;
import com.universalshield.engine.input.MessageInput;
import com.universalshield.engine.input.UrlInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static com.universalshield.detectors.DetectorTestSupport.codes;
import static com.universalshield.detectors.DetectorTestSupport.groups;
import static org.assertj.core.api.Assertions.assertThat;

class UrlRiskDetectorTest {

    private final UrlRiskDetector detector = new UrlRiskDetector(new NotConfiguredDomainReputationProvider());

    private com.universalshield.engine.DetectorResult scan(String url) {
        return detector.analyze(FraudEvent.ofUrl(new UrlInput(url), null));
    }

    @Test
    void brandOnUnrelatedDomainWithLureWordsAndNoHttps() {
        var r = scan("http://paypal.com.account-verify.top/signin");
        assertThat(codes(r)).contains("URL_BRAND_MISUSE", "URL_NO_HTTPS", "URL_RISKY_TLD", "URL_LURE_KEYWORDS");
    }

    @Test
    void legitimateBrandDomainIsNotFlaggedAsDeceptive() {
        var r = scan("https://www.paypal.com/signin");
        assertThat(groups(r)).doesNotContain("DECEPTIVE_DOMAIN");
    }

    @Test
    void httpsAloneNeverMeansSafeAndNoteSaysSo() {
        var r = scan("https://paypal-account-verify.top/login");
        assertThat(codes(r)).contains("URL_BRAND_MISUSE");
        assertThat(r.notes()).anyMatch(n -> n.contains("HTTPS only means the connection is encrypted"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://paypa1.com/login", "http://g00gle.com", "http://arnazon.com",
            "http://paypa1-secure-login.top/verify", "https://amaz0n-support.example/help"})
    void typosquatsAreDetected(String url) {
        assertThat(codes(scan(url))).contains("URL_TYPOSQUAT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://apply-now.example/form", "https://good-deals.example", "https://my-shop-2024.example"})
    void ordinaryHyphenatedDomainsAreNotTyposquats(String url) {
        assertThat(codes(scan(url))).doesNotContain("URL_TYPOSQUAT", "URL_BRAND_MISUSE");
    }

    @Test
    void ipHostAndPort() {
        assertThat(codes(scan("http://192.168.10.5:8080/bank/login.php"))).contains("URL_IP_HOST", "URL_NONSTANDARD_PORT");
    }

    @Test
    void userInfoTrickIsObfuscation() {
        var r = scan("http://www.trusted-bank.com@evil-login.xyz/");
        assertThat(codes(r)).contains("URL_USERINFO_TRICK");
    }

    @Test
    void scriptSchemeIsCritical() {
        var r = scan("javascript:alert(document.cookie)");
        assertThat(codes(r)).containsExactly("URL_SCRIPT_SCHEME");
        assertThat(r.indicators().get(0).weight()).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void punycodeShortenerAndDownload() {
        assertThat(codes(scan("http://xn--pypal-4ve.com/login"))).contains("URL_PUNYCODE");
        assertThat(codes(scan("https://bit.ly/abc"))).contains("URL_SHORTENER");
        assertThat(codes(scan("https://files.example.org/update.apk"))).contains("URL_EXECUTABLE_DOWNLOAD");
    }

    @Test
    void deepSubdomainsAndOpenRedirect() {
        assertThat(codes(scan("https://a.b.c.example.com/"))).contains("URL_DEEP_SUBDOMAINS");
        assertThat(codes(scan("https://example.com/r?url=https%3A%2F%2Fevil.example"))).contains("URL_OPEN_REDIRECT");
    }

    @Test
    void ordinarySiteHasNoIndicators() {
        assertThat(scan("https://www.wikipedia.org/").indicators()).isEmpty();
    }

    @Test
    void malformedUrlIsReportedAsInsufficient() {
        var r = scan("http://exa mple.com");
        assertThat(codes(r)).containsExactly("URL_MALFORMED");
        assertThat(r.inputSufficient()).isFalse();
    }

    @Test
    void embeddedLinksInMessagesAreAnalysedWithPrefixedGroups() {
        var e = FraudEvent.ofMessage(new MessageInput("Update now at http://sbi-kyc-update.xyz/login please", "SMS", null), null);
        assertThat(detector.appliesTo(e)).isTrue();
        var r = detector.analyze(e);
        assertThat(groups(r)).allMatch(g -> g.startsWith("url:"));
        assertThat(codes(r)).contains("URL_BRAND_MISUSE");
    }

    @Test
    void messagesWithoutLinksDoNotTriggerUrlDetector() {
        var e = FraudEvent.ofMessage(new MessageInput("no links in this one at all", "SMS", null), null);
        assertThat(detector.appliesTo(e)).isFalse();
    }

    @Test
    void verifiedReputationIsLabeledExternal() {
        DomainReputationProvider bad = d -> new ReputationVerdict(ReputationVerdict.Status.MALICIOUS, "TestFeed", "Listed for phishing");
        var r = new UrlRiskDetector(bad).analyze(FraudEvent.ofUrl(new UrlInput("https://example.com"), null));
        assertThat(r.indicators()).anySatisfy(i -> {
            assertThat(i.code()).isEqualTo("URL_REPUTATION_MALICIOUS");
            assertThat(i.source()).isEqualTo(ResultSource.VERIFIED_EXTERNAL);
        });
    }

    @Test
    void notConfiguredReputationIsStatedHonestly() {
        assertThat(scan("https://example.com").notes()).anyMatch(n -> n.contains("heuristic findings only"));
    }

    @Test
    void previouslyHighRiskDomain() {
        var r = detector.analyze(FraudEvent.ofUrl(new UrlInput("https://example.com"), new ScanHistory(List.of(), 1, 1)));
        assertThat(codes(r)).contains("URL_PREVIOUSLY_HIGH_RISK");
    }

    @Test
    void parsedUrlComputesRegistrableDomain() {
        ParsedUrl u = ParsedUrl.parse("https://login.secure.example.co.uk:8443/x?y=1").orElseThrow();
        assertThat(u.host()).isEqualTo("login.secure.example.co.uk");
        assertThat(u.port()).isEqualTo(8443);
        assertThat(u.registrableDomain()).isEqualTo("example.co.uk");
        assertThat(u.subdomainLabels()).containsExactly("login", "secure");
        assertThat(ParsedUrl.parse("example.com/path").orElseThrow().scheme()).isEmpty();
        assertThat(ParsedUrl.parse("")).isEmpty();
        assertThat(ParsedUrl.parse("http://host:99999/")).isEmpty();
    }

    @Test
    void extractsAtMostFiveUrls() {
        String text = "a.com b.com c.com d.com e.com f.com https://g.example/x.";
        assertThat(UrlRiskDetector.extractUrls(text)).hasSize(5);
        assertThat(UrlRiskDetector.extractUrls("visit https://ok.example/path.")).containsExactly("https://ok.example/path");
    }
}
