package com.universalshield.auth;

import com.universalshield.config.AppProperties;
import com.universalshield.dto.LoginRequest;
import com.universalshield.dto.RegisterRequest;
import com.universalshield.exception.ApiException;
import com.universalshield.exception.ConflictException;
import com.universalshield.security.AppUserPrincipal;
import com.universalshield.service.AuditService;
import com.universalshield.user.User;
import com.universalshield.user.UserPreference;
import com.universalshield.user.UserPreferenceRepository;
import com.universalshield.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Registration and session login. Passwords are hashed with BCrypt (via Spring's delegating
 * encoder); the session id is rotated on login to prevent session fixation.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final UserPreferenceRepository preferences;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final SecurityContextRepository contextRepository;
    private final AuditService audit;
    private final AppProperties props;

    public AuthService(UserRepository users, UserPreferenceRepository preferences, PasswordEncoder encoder,
                       AuthenticationManager authManager, SecurityContextRepository contextRepository,
                       AuditService audit, AppProperties props) {
        this.users = users;
        this.preferences = preferences;
        this.encoder = encoder;
        this.authManager = authManager;
        this.contextRepository = contextRepository;
        this.audit = audit;
        this.props = props;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public User register(RegisterRequest req, HttpServletRequest http) {
        String email = normalizeEmail(req.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("EMAIL_TAKEN", "An account with this email already exists.");
        }
        User user = users.save(new User(email, req.displayName().trim(), encoder.encode(req.password())));
        preferences.save(new UserPreference(user.getId(), props.retention().defaultDays()));
        audit.record(user.getId(), AuditService.REGISTERED, null, http);
        return user;
    }

    public AppUserPrincipal login(LoginRequest req, HttpServletRequest http, HttpServletResponse res) {
        Authentication auth;
        try {
            auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(normalizeEmail(req.email()), req.password()));
        } catch (AuthenticationException e) {
            audit.record(null, AuditService.LOGIN_FAILURE, null, http);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect.");
        }
        if (http.getSession(false) != null) {
            http.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, http, res);
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        audit.record(principal.getId(), AuditService.LOGIN_SUCCESS, null, http);
        return principal;
    }
}
