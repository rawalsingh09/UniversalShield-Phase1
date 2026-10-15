package com.universalshield.api;

import com.universalshield.repository.AuditEventRepository;
import com.universalshield.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository users;
    @Autowired AuditEventRepository audit;

    @Test
    void registerHashesPasswordAndStartsSession() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/register").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email.toUpperCase(), "displayName", "Alice", "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(content().string(not(containsString(PASSWORD))))
                .andExpect(content().string(not(containsString("passwordHash"))));
        var user = users.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(user.getPasswordHash()).startsWith("{bcrypt}").doesNotContain(PASSWORD);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.data.displayName").value("Alice"));
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        String email = uniqueEmail();
        var body = toJson(Map.of("email", email, "displayName", "Bob", "password", PASSWORD));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("EMAIL_TAKEN"));
    }

    @Test
    void weakPasswordAndBadEmailAreRejectedWithFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "not-an-email", "displayName", "C", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fields[?(@.field=='email')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field=='password')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field=='displayName')]").exists())
                .andExpect(content().string(not(containsString("short\""))));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", uniqueEmail(), "displayName", "Dee", "password", "onlyletterslong"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginSucceedsWithCorrectPasswordAndFailsGenerically() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("email", email, "displayName", "Eve", "password", PASSWORD)))).andExpect(status().isCreated());
        MockHttpSession s = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("email", email, "password", PASSWORD)))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(s)).andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email, "password", "WrongPass999"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "nobody@example.test", "password", "WrongPass999"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.message").value("Email or password is incorrect."));
        assertThat(audit.findAll()).anyMatch(e -> e.getEventType().equals("LOGIN_FAILURE"));
    }

    @Test
    void logoutEndsSession() throws Exception {
        MockHttpSession s = newUserSession();
        mvc.perform(post("/api/auth/logout").session(s).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(s)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "a@example.test", "password", PASSWORD))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("CSRF_INVALID"));
    }

    @Test
    void csrfEndpointIssuesToken() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
        // The XSRF-TOKEN cookie itself is verified over real HTTP in CsrfCookieHttpTest
        // (MockMvc's Spring Security support swaps in a test token repository).
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }
}
