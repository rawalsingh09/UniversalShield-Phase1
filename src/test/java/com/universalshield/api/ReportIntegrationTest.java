package com.universalshield.api;

import com.universalshield.reporting.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportIntegrationTest extends IntegrationTestBase {

    @Test
    void htmlReportIsDownloadableAndEscapesScannedContent() throws Exception {
        MockHttpSession s = newUserSession();
        String id = createScan(s, Map.of("category", "MESSAGE", "message", Map.of(
                "text", "<script>alert('x')</script> URGENT share the OTP now", "sender", "<img src=x onerror=alert(1)>")));
        String html = mvc.perform(get("/api/reports/" + id).session(s))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(id + ".html")))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("UniversalShield scan report", id, "Recommended actions", "Scoring method");
        assertThat(html).doesNotContain("<script>alert").doesNotContain("<img src=x");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    void jsonReportContainsFullResult() throws Exception {
        MockHttpSession s = newUserSession();
        String id = createScan(s, messageScan("Please share the OTP you received with our agent"));
        mvc.perform(get("/api/reports/" + id + "?format=json").session(s))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".json")))
                .andExpect(jsonPath("$.reportType").value("UniversalShield scan report"))
                .andExpect(jsonPath("$.result.id").value(id))
                .andExpect(jsonPath("$.result.indicators").isNotEmpty());
    }

    @Test
    void unsupportedFormatAndUnknownIdAreErrors() throws Exception {
        MockHttpSession s = newUserSession();
        String id = createScan(s, messageScan("plain synthetic text message here"));
        mvc.perform(get("/api/reports/" + id + "?format=pdf").session(s)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FORMAT"));
        mvc.perform(get("/api/reports/00000000-0000-0000-0000-000000000000").session(s)).andExpect(status().isNotFound());
        mvc.perform(get("/api/reports/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void escapeHandlesAllSpecialCharacters() {
        assertThat(ReportService.esc("<a href=\"x\">'&'</a>")).isEqualTo("&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;");
        assertThat(ReportService.esc(null)).isEmpty();
    }
}
