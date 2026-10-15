package com.universalshield.user;

import com.universalshield.config.AppProperties;
import com.universalshield.dto.PreferencesDto;
import com.universalshield.dto.UserProfileResponse;
import com.universalshield.exception.ApiException;
import com.universalshield.exception.BadRequestException;
import com.universalshield.exception.NotFoundException;
import com.universalshield.repository.AuditEventRepository;
import com.universalshield.repository.ScanRepository;
import com.universalshield.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Profile, preferences and the "delete my data" flows. */
@Service
public class UserService {

    private final UserRepository users;
    private final UserPreferenceRepository preferences;
    private final ScanRepository scans;
    private final AuditEventRepository auditEvents;
    private final AuditService audit;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public UserService(UserRepository users, UserPreferenceRepository preferences, ScanRepository scans,
                       AuditEventRepository auditEvents, AuditService audit, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.preferences = preferences;
        this.scans = scans;
        this.auditEvents = auditEvents;
        this.audit = audit;
        this.encoder = encoder;
        this.props = props;
    }

    @Transactional
    public UserProfileResponse profile(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        return new UserProfileResponse(u.getEmail(), u.getDisplayName(), u.getCreatedAt(), toDto(prefs(userId)));
    }

    @Transactional
    public PreferencesDto updatePreferences(Long userId, PreferencesDto dto) {
        if (dto.retentionDays() > props.retention().maxDays()) {
            throw new BadRequestException("RETENTION_TOO_LONG", "Retention can be at most " + props.retention().maxDays() + " days.");
        }
        UserPreference p = prefs(userId);
        p.update(dto.retentionDays(), dto.storeInputExcerpts(), dto.defaultCategory());
        return toDto(preferences.save(p));
    }

    @Transactional
    public int deleteHistory(Long userId, HttpServletRequest http) {
        int n = scans.deleteAllByUserId(userId);
        audit.record(userId, AuditService.HISTORY_DELETED, n + " scans", http);
        return n;
    }

    /** Deletes the account, all scans, preferences; audit rows are kept but detached from the user. */
    @Transactional
    public void deleteAccount(Long userId, String password, HttpServletRequest http) {
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!encoder.matches(password, u.getPasswordHash())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PASSWORD_MISMATCH", "The password is incorrect.");
        }
        int n = scans.deleteAllByUserId(userId);
        preferences.deleteById(userId);
        auditEvents.detachUser(userId);
        users.delete(u);
        audit.record(null, AuditService.ACCOUNT_DELETED, n + " scans removed", http);
    }

    UserPreference prefs(Long userId) {
        return preferences.findById(userId)
                .orElseGet(() -> preferences.save(new UserPreference(userId, props.retention().defaultDays())));
    }

    static PreferencesDto toDto(UserPreference p) {
        return new PreferencesDto(p.getRetentionDays(), p.isStoreInputExcerpts(), p.getDefaultCategory());
    }
}
