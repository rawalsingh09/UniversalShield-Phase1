package com.universalshield.api;

import com.universalshield.repository.AuditEventRepository;
import com.universalshield.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserPrivacyIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository users;
    @Autowired AuditEventRepository audit;

    @Test
    void profileNeverExposesPasswordHash() throws Exception {
        MockHttpSession s = newUserSession();
        String body = mvc.perform(get("/api/users/me").session(s)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferences.retentionDays").value(90))
                .andExpect(jsonPath("$.data.preferences.storeInputExcerpts").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("password").doesNotContain("bcrypt");
    }

    @Test
    void preferencesAreValidatedAndApplied() throws Exception {
        MockHttpSession s = newUserSession();
        mvc.perform(put("/api/users/me/preferences").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("retentionDays", 0, "storeInputExcerpts", true, "defaultCategory", "URL"))))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/users/me/preferences").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("retentionDays", 30, "storeInputExcerpts", false, "defaultCategory", "URL"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.retentionDays").value(30));
        String id = createScan(s, messageScan("secret personal message that should not be stored"));
        mvc.perform(get("/api/scans/" + id).session(s)).andExpect(jsonPath("$.data.inputExcerpt").doesNotExist());
    }

    @Test
    void deleteHistoryKeepsAccount() throws Exception {
        MockHttpSession s = newUserSession();
        createScan(s, messageScan("synthetic one for deletion test"));
        createScan(s, messageScan("synthetic two for deletion test"));
        mvc.perform(delete("/api/users/me/history").session(s).with(csrf())).andExpect(jsonPath("$.data.deleted").value(2));
        mvc.perform(get("/api/dashboard/summary").session(s)).andExpect(jsonPath("$.data.totalScans").value(0));
        mvc.perform(get("/api/users/me").session(s)).andExpect(status().isOk());
    }

    @Test
    void deleteAccountRequiresPasswordAndRemovesEverything() throws Exception {
        MockHttpSession s = newUserSession();
        String email = mvc.perform(get("/api/auth/me").session(s)).andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"email\":\"([^\"]+)\".*", "$1");
        createScan(s, messageScan("synthetic content before account deletion"));
        mvc.perform(post("/api/users/me/delete-account").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("password", "WrongPassword1")))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_MISMATCH"));
        mvc.perform(post("/api/users/me/delete-account").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("password", PASSWORD)))).andExpect(status().isOk());
        assertThat(users.findByEmailIgnoreCase(email)).isEmpty();
        assertThat(audit.findAll()).anyMatch(e -> e.getEventType().equals("ACCOUNT_DELETED") && e.getUserId() == null);
        mvc.perform(get("/api/auth/me").session(s)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("email", email, "password", PASSWORD)))).andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardSummaryReflectsOnlyOwnData() throws Exception {
        MockHttpSession s = newUserSession();
        createScan(s, messageScan("URGENT share the OTP now or the account will be blocked"));
        createScan(s, Map.of("category", "URL", "url", Map.of("url", "https://www.wikipedia.org/")));
        MockHttpSession other = newUserSession();
        mvc.perform(get("/api/dashboard/summary").session(s)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalScans").value(2))
                .andExpect(jsonPath("$.data.byCategory.MESSAGE").value(1))
                .andExpect(jsonPath("$.data.byCategory.URL").value(1))
                .andExpect(jsonPath("$.data.byCategory.CALL").value(0))
                .andExpect(jsonPath("$.data.byRiskLevel.LOW").value(1))
                .andExpect(jsonPath("$.data.recentScans.length()").value(2))
                .andExpect(jsonPath("$.data.recentHighRisk.length()").value(1))
                .andExpect(jsonPath("$.data.demoScans").value(0))
                .andExpect(jsonPath("$.data.statisticsNote").exists());
        mvc.perform(get("/api/dashboard/summary").session(other)).andExpect(jsonPath("$.data.totalScans").value(0));
    }
}
