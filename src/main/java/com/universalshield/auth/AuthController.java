package com.universalshield.auth;

import com.universalshield.dto.ApiResponse;
import com.universalshield.dto.LoginRequest;
import com.universalshield.dto.RegisterRequest;
import com.universalshield.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authentication endpoints. Logout (POST /api/auth/logout) is handled by Spring Security's
 * logout filter configured in SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    public record SessionUser(String email, String displayName) {
    }

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Get a CSRF token (also set as the XSRF-TOKEN cookie)")
    public ApiResponse<Map<String, String>> csrf(CsrfToken token) {
        return ApiResponse.ok(Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an account and sign in")
    public ApiResponse<SessionUser> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http,
                                             HttpServletResponse res) {
        auth.register(req, http);
        AppUserPrincipal p = auth.login(new LoginRequest(req.email(), req.password()), http, res);
        return ApiResponse.ok(new SessionUser(p.getUsername(), p.getDisplayName()));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with email and password; starts a session cookie")
    public ApiResponse<SessionUser> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http,
                                          HttpServletResponse res) {
        AppUserPrincipal p = auth.login(req, http, res);
        return ApiResponse.ok(new SessionUser(p.getUsername(), p.getDisplayName()));
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in user (401 if not signed in)")
    public ApiResponse<SessionUser> me(@AuthenticationPrincipal AppUserPrincipal p) {
        return ApiResponse.ok(new SessionUser(p.getUsername(), p.getDisplayName()));
    }
}
