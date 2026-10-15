package com.universalshield.api;

import com.universalshield.domain.Scan;
import com.universalshield.repository.ScanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScanApiIntegrationTest extends IntegrationTestBase {

    @Autowired ScanRepository scans;
    @Autowired TransactionTemplate tx;

    static Stream<Arguments> fiveDetectorFlows() {
        return Stream.of(
                Arguments.of(messageScan("URGENT your account will be blocked. Share the OTP to verify your account now."), "MESSAGE", "HIGH"),
                Arguments.of(Map.of("category", "URL", "url", Map.of("url", "http://paypal.com.secure-login.top/signin")), "URL", "HIGH"),
                Arguments.of(Map.of("category", "TRANSACTION", "transaction", Map.of("amount", 25000, "currency", "INR",
                        "recipientIsNew", true, "paymentMethod", "UPI", "attemptsLastHour", 3, "requestedByOtherParty", true,
                        "description", "Customer care said enter your UPI PIN to receive the refund, it is urgent")), "TRANSACTION", "CRITICAL"),
                Arguments.of(Map.of("category", "CALL", "call", Map.of("callerClaimedIdentity", "Police officer",
                        "description", "Said I am under digital arrest, install AnyDesk, do not hang up and transfer money")), "CALL", "CRITICAL"),
                Arguments.of(Map.of("category", "APP_PERMISSION", "app", Map.of("appName", "Torch", "appCategory", "FLASHLIGHT",
                        "installSource", "UNKNOWN_SOURCE", "permissions", List.of("READ_SMS", "BIND_ACCESSIBILITY_SERVICE", "CAMERA",
                                "READ_CONTACTS", "SYSTEM_ALERT_WINDOW"))), "APP_PERMISSION", "CRITICAL"));
    }

    @ParameterizedTest(name = "{1} flow")
    @MethodSource("fiveDetectorFlows")
    void everyDetectorFlowReturnsStructuredExplainableResult(Map<String, Object> payload, String category, String minLevel) throws Exception {
        MockHttpSession s = newUserSession();
        var r = body(mvc.perform(post("/api/scans").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(toJson(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value(category))
                .andExpect(jsonPath("$.data.indicators").isNotEmpty())
                .andExpect(jsonPath("$.data.recommendations").isNotEmpty())
                .andExpect(jsonPath("$.data.disclaimers").isNotEmpty())
                .andExpect(jsonPath("$.data.rulesetVersion").exists())
                .andExpect(jsonPath("$.data.sources[0]").value("LOCAL_HEURISTIC"))
                .andReturn()).path("data");
        int score = r.path("riskScore").asInt();
        assertThat(score).isBetween(0, 95);
        List<String> order = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
        assertThat(order.indexOf(r.path("riskLevel").asText())).isGreaterThanOrEqualTo(order.indexOf(minLevel));
        assertThat(r.path("evidenceStrength").asText()).isIn("LIMITED", "MODERATE", "STRONG");

        // persisted and retrievable with identical content
        mvc.perform(get("/api/scans/" + r.path("id").asText()).session(s)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskScore").value(score))
                .andExpect(jsonPath("$.data.indicators", hasSize(r.path("indicators").size())));
    }

    @Test
    void benignInputsScoreLow() throws Exception {
        MockHttpSession s = newUserSession();
        mvc.perform(post("/api/scans").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(messageScan("Hi, are we still meeting at the library at four today?"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.data.disclaimers[?(@ =~ /.*does not guarantee safety.*/)]").exists());
    }

    @Test
    void validationErrors() throws Exception {
        MockHttpSession s = newUserSession();
        // missing category
        mvc.perform(post("/api/scans").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("category"));
        // category without matching input
        mvc.perform(post("/api/scans").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"URL\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("MISSING_INPUT"));
        // unknown category
        mvc.perform(post("/api/scans").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"FAX\"}"))
                .andExpect(status().isBadRequest());
        // too long
        mvc.perform(post("/api/scans").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(messageScan("a".repeat(5001)))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("message.text"))
                .andExpect(content().string(not(containsString("aaaaaaaaaa"))));
        // blank text, negative amount, bad permission name
        mvc.perform(post("/api/scans").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(toJson(messageScan("  "))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/scans").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("category", "TRANSACTION", "transaction", Map.of("amount", -5)))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/scans").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("category", "APP_PERMISSION", "app", Map.of("appName", "x", "appCategory", "GAME",
                                "permissions", List.of("READ_SMS; DROP TABLE users"))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestsGet401AndMissingCsrfGets403() throws Exception {
        mvc.perform(get("/api/scans")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(post("/api/scans").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(toJson(messageScan("hello there"))))
                .andExpect(status().isUnauthorized());
        MockHttpSession s = newUserSession();
        mvc.perform(post("/api/scans").session(s).contentType(MediaType.APPLICATION_JSON).content(toJson(messageScan("hello there"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void usersCannotSeeOrDeleteEachOthersScans() throws Exception {
        MockHttpSession alice = newUserSession();
        MockHttpSession bob = newUserSession();
        String id = createScan(alice, messageScan("Please share the OTP you received to verify your account"));
        mvc.perform(get("/api/scans/" + id).session(bob)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/scans/" + id).session(bob).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/reports/" + id).session(bob)).andExpect(status().isNotFound());
        mvc.perform(get("/api/scans").session(bob)).andExpect(jsonPath("$.data.totalItems").value(0));
        mvc.perform(delete("/api/scans").session(bob).with(csrf())).andExpect(jsonPath("$.data.deleted").value(0));
        mvc.perform(get("/api/scans/" + id).session(alice)).andExpect(status().isOk());
    }

    @Test
    void historyDeletionSingleAndAll() throws Exception {
        MockHttpSession s = newUserSession();
        String a = createScan(s, messageScan("first synthetic message for history"));
        createScan(s, messageScan("second synthetic message for history"));
        mvc.perform(delete("/api/scans/" + a).session(s).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/scans/" + a).session(s)).andExpect(status().isNotFound());
        mvc.perform(get("/api/scans").session(s)).andExpect(jsonPath("$.data.totalItems").value(1));
        mvc.perform(delete("/api/scans").session(s).with(csrf())).andExpect(jsonPath("$.data.deleted").value(1));
        mvc.perform(get("/api/scans").session(s)).andExpect(jsonPath("$.data.totalItems").value(0));
        assertThat(scans.findByPublicIdAndUserId(UUID.fromString(a), 0L)).isEmpty();
    }

    @Test
    void listSupportsFiltersSearchAndPagination() throws Exception {
        MockHttpSession s = newUserSession();
        for (int i = 0; i < 3; i++) {
            createScan(s, messageScan("Hello friend number " + i + ", see you at the cafe later"));
        }
        createScan(s, messageScan("URGENT share the OTP now or your account will be blocked today"));
        createScan(s, Map.of("category", "URL", "url", Map.of("url", "https://www.wikipedia.org/")));

        mvc.perform(get("/api/scans?size=2&page=0").session(s))
                .andExpect(jsonPath("$.data.items", hasSize(2))).andExpect(jsonPath("$.data.totalItems").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3));
        mvc.perform(get("/api/scans?size=2&page=2").session(s)).andExpect(jsonPath("$.data.items", hasSize(1)));
        mvc.perform(get("/api/scans?category=URL").session(s)).andExpect(jsonPath("$.data.totalItems").value(1));
        mvc.perform(get("/api/scans?riskLevel=LOW&category=MESSAGE").session(s)).andExpect(jsonPath("$.data.totalItems").value(3));
        mvc.perform(get("/api/scans?q=cafe").session(s)).andExpect(jsonPath("$.data.totalItems").value(3));
        mvc.perform(get("/api/scans?q=OTP or verification").session(s)).andExpect(jsonPath("$.data.totalItems").value(1));
        mvc.perform(get("/api/scans?q=100%25_").session(s)).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalItems").value(0));
        mvc.perform(get("/api/scans?q=' OR 1=1 --").session(s)).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalItems").value(0));
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        mvc.perform(get("/api/scans?from=" + today + "&to=" + today).session(s)).andExpect(jsonPath("$.data.totalItems").value(5));
        mvc.perform(get("/api/scans?from=2000-01-01&to=2000-01-02").session(s)).andExpect(jsonPath("$.data.totalItems").value(0));
        mvc.perform(get("/api/scans?size=500").session(s)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/scans?from=not-a-date").session(s)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/scans/not-a-uuid").session(s)).andExpect(status().isBadRequest());
    }

    @Test
    void sensitiveValuesAreMaskedBeforeStorage() throws Exception {
        MockHttpSession s = newUserSession();
        String id = createScan(s, Map.of("category", "TRANSACTION", "transaction", Map.of("amount", 10,
                "description", "my card 4111 1111 1111 1111 and otp is 839201")));
        Scan stored = tx.execute(st -> {
            Scan sc = scans.findAll().stream().filter(x -> x.getPublicId().toString().equals(id)).findFirst().orElseThrow();
            sc.getIndicators().size();
            return sc;
        });
        assertThat(stored.getInputExcerpt()).contains("[card number removed]").doesNotContain("4111").doesNotContain("839201");
        assertThat(stored.getAmount()).isEqualByComparingTo("10");
    }

    @Test
    void repeatedHighRiskSubjectIsRecognisedFromHistory() throws Exception {
        MockHttpSession s = newUserSession();
        Map<String, Object> payload = Map.of("category", "URL", "url", Map.of("url", "http://paypal.com.secure-login.top/signin"));
        createScan(s, payload);
        mvc.perform(post("/api/scans").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(toJson(payload)))
                .andExpect(jsonPath("$.data.indicators[?(@.code=='URL_PREVIOUSLY_HIGH_RISK')]").exists());
    }

    @Test
    void permissionReportUploadIsParsedAndValidated() throws Exception {
        MockHttpSession s = newUserSession();
        var file = new org.springframework.mock.web.MockMultipartFile("file", "report.txt", "text/plain",
                "uses-permission: name='android.permission.READ_SMS'\nuses-permission: name='android.permission.CAMERA'".getBytes());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/scans/app-report")
                        .file(file).session(s).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.permissions", hasSize(2)));
        var exe = new org.springframework.mock.web.MockMultipartFile("file", "evil.exe", "application/octet-stream", new byte[] {0x4d, 0x5a});
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/scans/app-report")
                        .file(exe).session(s).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));
        var binary = new org.springframework.mock.web.MockMultipartFile("file", "x.txt", "text/plain", new byte[] {(byte) 0xff, (byte) 0xfe, 0});
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/scans/app-report")
                        .file(binary).session(s).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("NOT_TEXT"));
        var big = new org.springframework.mock.web.MockMultipartFile("file", "big.txt", "text/plain", new byte[70 * 1024]);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/scans/app-report")
                        .file(big).session(s).with(csrf()))
                .andExpect(status().is4xxClientError());
        var none = new org.springframework.mock.web.MockMultipartFile("file", "n.txt", "text/plain", "nothing here".getBytes());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/scans/app-report")
                        .file(none).session(s).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("NO_PERMISSIONS_FOUND"));
    }
}
