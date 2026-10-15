package com.universalshield.service;

import com.universalshield.domain.AuditEvent;
import com.universalshield.repository.AuditEventRepository;
import com.universalshield.security.SubjectHasher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records security-relevant events. Never stores passwords or scan content; IPs are hashed. */
@Service
public class AuditService {

    public static final String REGISTERED = "USER_REGISTERED";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String HISTORY_DELETED = "HISTORY_DELETED";
    public static final String ACCOUNT_DELETED = "ACCOUNT_DELETED";

    private final AuditEventRepository repo;
    private final SubjectHasher hasher;

    public AuditService(AuditEventRepository repo, SubjectHasher hasher) {
        this.repo = repo;
        this.hasher = hasher;
    }

    @Transactional
    public void record(Long userId, String type, String detail, HttpServletRequest request) {
        String ip = request == null ? null : hasher.hash("ip:" + request.getRemoteAddr());
        repo.save(new AuditEvent(userId, type, detail, ip));
    }
}
