package com.universalshield.api;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicEndpointsIntegrationTest extends IntegrationTestBase {

    @Test
    void healthReportsDatabaseUp() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP")).andExpect(jsonPath("$.data.database").value("UP"));
    }

    @Test
    void detectorsListsAllFiveHeuristicsAndUnavailableMl() throws Exception {
        mvc.perform(get("/api/detectors")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem("message-heuristic")))
                .andExpect(jsonPath("$.data[*].id", hasItem("url-heuristic")))
                .andExpect(jsonPath("$.data[*].id", hasItem("transaction-heuristic")))
                .andExpect(jsonPath("$.data[*].id", hasItem("call-heuristic")))
                .andExpect(jsonPath("$.data[*].id", hasItem("app-permission-heuristic")))
                .andExpect(jsonPath("$.data[?(@.id=='ml-text-classifier')].available").value(false));
    }

    @Test
    void methodologyExposesThresholdsAndMlStatus() throws Exception {
        mvc.perform(get("/api/methodology")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.thresholds.medium").value(30))
                .andExpect(jsonPath("$.data.thresholds.high").value(60))
                .andExpect(jsonPath("$.data.thresholds.critical").value(85))
                .andExpect(jsonPath("$.data.mlModels.MESSAGE.status", containsString("UNAVAILABLE")));
    }

    @Test
    void openApiSpecIsServed() throws Exception {
        mvc.perform(get("/api/openapi")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("UniversalShield API"))
                .andExpect(jsonPath("$.paths['/api/scans']").exists());
    }

    @Test
    void frontendIsServedWithSecurityHeaders() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("UniversalShield")))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
        mvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mvc.perform(get("/css/main.css")).andExpect(status().isOk());
    }

    @Test
    void unknownApiPathRequiresAuthAndThen404s() throws Exception {
        mvc.perform(get("/api/does-not-exist")).andExpect(status().isUnauthorized());
        MockHttpSession s = newUserSession();
        mvc.perform(get("/api/does-not-exist").session(s)).andExpect(status().isNotFound());
    }

    @Test
    void wrongMethodIs405() throws Exception {
        MockHttpSession s = newUserSession();
        mvc.perform(put("/api/scans").session(s).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json").content(toJson(Map.of())))
                .andExpect(status().isMethodNotAllowed());
    }
}
