package com.universalshield.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check over a real HTTP connection (embedded server on a random port) of the
 * double-submit CSRF cookie and session-cookie flags the JavaScript frontend relies on.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CsrfCookieHttpTest {

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

    private HttpResponse<String> send(HttpRequest.Builder b) throws Exception {
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    @Test
    void csrfCookieIsIssuedAndRequiredForStateChangingRequests() throws Exception {
        HttpResponse<String> csrf = send(HttpRequest.newBuilder(uri("/api/auth/csrf")).GET());
        List<String> cookies = csrf.headers().allValues("set-cookie");
        String xsrfCookie = cookies.stream().filter(c -> c.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow();
        assertThat(xsrfCookie).doesNotContainIgnoringCase("HttpOnly").contains("SameSite=Lax");
        String token = xsrfCookie.substring("XSRF-TOKEN=".length(), xsrfCookie.indexOf(';'));
        assertThat(csrf.body()).contains("X-XSRF-TOKEN").contains(token);

        String body = "{\"email\":\"u" + UUID.randomUUID() + "@example.test\",\"displayName\":\"Http\",\"password\":\"SyntheticPass123\"}";
        HttpResponse<String> noToken = send(HttpRequest.newBuilder(uri("/api/auth/register"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)));
        assertThat(noToken.statusCode()).isEqualTo(403);
        assertThat(noToken.body()).contains("CSRF_INVALID");

        HttpResponse<String> withToken = send(HttpRequest.newBuilder(uri("/api/auth/register"))
                .header("Content-Type", "application/json").header("Cookie", "XSRF-TOKEN=" + token).header("X-XSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.ofString(body)));
        assertThat(withToken.statusCode()).isEqualTo(201);
        String session = withToken.headers().allValues("set-cookie").stream().filter(c -> c.startsWith("JSESSIONID=")).findFirst().orElseThrow();
        assertThat(session).containsIgnoringCase("HttpOnly").contains("SameSite=Lax");
    }
}
