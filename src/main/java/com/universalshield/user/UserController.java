package com.universalshield.user;

import com.universalshield.dto.ApiResponse;
import com.universalshield.dto.DeleteAccountRequest;
import com.universalshield.dto.PreferencesDto;
import com.universalshield.dto.UserProfileResponse;
import com.universalshield.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Signed-in user's profile, preferences and data deletion. */
@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Account & privacy")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "Profile and preferences")
    public ApiResponse<UserProfileResponse> profile(@AuthenticationPrincipal AppUserPrincipal p) {
        return ApiResponse.ok(users.profile(p.getId()));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update privacy and scanner preferences")
    public ApiResponse<PreferencesDto> preferences(@AuthenticationPrincipal AppUserPrincipal p,
                                                   @Valid @RequestBody PreferencesDto dto) {
        return ApiResponse.ok(users.updatePreferences(p.getId(), dto));
    }

    @PostMapping("/delete-account")
    @Operation(summary = "Delete my account and all my data (requires password)")
    public ApiResponse<Map<String, String>> deleteAccount(@AuthenticationPrincipal AppUserPrincipal p,
                                                          @Valid @RequestBody DeleteAccountRequest req,
                                                          HttpServletRequest http, HttpServletResponse res) {
        users.deleteAccount(p.getId(), req.password(), http);
        var session = http.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ApiResponse.ok(Map.of("status", "Your account and all associated data were deleted."));
    }

    @DeleteMapping("/history")
    @Operation(summary = "Delete all my scan history but keep the account")
    public ApiResponse<Map<String, Integer>> deleteHistory(@AuthenticationPrincipal AppUserPrincipal p, HttpServletRequest http) {
        return ApiResponse.ok(Map.of("deleted", users.deleteHistory(p.getId(), http)));
    }
}
