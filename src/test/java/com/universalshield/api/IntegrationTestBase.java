package com.universalshield.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots the full application on an in-memory H2 database (test profile) with MockMvc. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    public static final String PASSWORD = "SyntheticPass123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.test";
    }

    /** Registers a new synthetic user and returns the authenticated session. */
    protected MockHttpSession newUserSession() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/register").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "displayName", "Test User", "password", PASSWORD))))
                .andExpect(status().isCreated());
        return session;
    }

    protected JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    protected String toJson(Object o) throws Exception {
        return json.writeValueAsString(o);
    }

    protected static Map<String, Object> messageScan(String text) {
        return Map.of("category", "MESSAGE", "message", Map.of("text", text, "channel", "SMS"));
    }

    /** Creates a scan and returns its id. */
    protected String createScan(MockHttpSession s, Object payload) throws Exception {
        MvcResult r = mvc.perform(post("/api/scans").session(s).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(toJson(payload))).andExpect(status().isCreated()).andReturn();
        return body(r).path("data").path("id").asText();
    }
}
