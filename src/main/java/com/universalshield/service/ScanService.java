package com.universalshield.service;

import com.universalshield.detectors.url.ParsedUrl;
import com.universalshield.domain.Scan;
import com.universalshield.domain.ScanIndicator;
import com.universalshield.dto.PageResponse;
import com.universalshield.dto.ScanRequest;
import com.universalshield.dto.ScanResponse;
import com.universalshield.dto.ScanSummary;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.FraudResult;
import com.universalshield.engine.Indicator;
import com.universalshield.engine.ResultSource;
import com.universalshield.engine.RiskEngine;
import com.universalshield.engine.RiskLevel;
import com.universalshield.engine.ScanCategory;
import com.universalshield.engine.ScanHistory;
import com.universalshield.exception.BadRequestException;
import com.universalshield.exception.NotFoundException;
import com.universalshield.repository.ScanRepository;
import com.universalshield.security.SubjectHasher;
import com.universalshield.user.User;
import com.universalshield.user.UserPreference;
import com.universalshield.user.UserPreferenceRepository;
import com.universalshield.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates a scan: validates the request shape, gathers the user's history, runs the
 * {@link RiskEngine}, stores a privacy-preserving record and returns the explainable result.
 * Every read and delete is scoped to the requesting user's id.
 */
@Service
public class ScanService {

    public static final int MAX_PAGE_SIZE = 50;

    private final RiskEngine engine;
    private final ScanRepository scans;
    private final UserRepository users;
    private final UserPreferenceRepository preferences;
    private final ScanHistoryService history;
    private final InputSanitizer sanitizer;
    private final SubjectHasher hasher;
    private final ScanMapper mapper;

    public ScanService(RiskEngine engine, ScanRepository scans, UserRepository users, UserPreferenceRepository preferences,
                       ScanHistoryService history, InputSanitizer sanitizer, SubjectHasher hasher, ScanMapper mapper) {
        this.engine = engine;
        this.scans = scans;
        this.users = users;
        this.preferences = preferences;
        this.history = history;
        this.sanitizer = sanitizer;
        this.hasher = hasher;
        this.mapper = mapper;
    }

    @Transactional
    public ScanResponse create(Long userId, ScanRequest req) {
        return mapper.toResponse(createEntity(userId, req, false));
    }

    /** Runs and stores a scan. Also used by the demo seeder (demo = true). */
    @Transactional
    public Scan createEntity(Long userId, ScanRequest req, boolean demo) {
        validateShape(req);
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        String subjectHash = hasher.hash(subject(req));
        ScanHistory h = history.forUser(userId, req.category(), subjectHash);
        FraudResult result = engine.evaluate(toEvent(req, h));

        Scan scan = new Scan(result.resultId(), user, result.category());
        scan.setResult(result.riskScore(), result.riskLevel(), result.evidenceStrength(),
                result.sources().stream().map(ResultSource::name).sorted().collect(Collectors.joining(",")),
                result.rulesetVersion(), ScanMapper.formatVersions(result.detectorVersions()));
        scan.setExplanations(result.reasons(), result.recommendations(), result.notes(), result.disclaimers());
        boolean storeExcerpt = preferences.findById(userId).map(UserPreference::isStoreInputExcerpts).orElse(true);
        scan.setInputExcerpt(storeExcerpt ? sanitizer.excerpt(req) : null);
        scan.setSubjectHash(subjectHash);
        if (req.category() == ScanCategory.TRANSACTION) {
            scan.setAmount(req.transaction().amount());
        }
        scan.setDemo(demo);
        for (Indicator i : result.indicators()) {
            scan.addIndicator(new ScanIndicator(i.code(), i.group(), i.title(), i.explanation(), i.weight(),
                    i.detectorId(), i.source(), i.evidence() == null ? null : sanitizer.sanitize(i.evidence())));
        }
        return scans.save(scan);
    }

    @Transactional(readOnly = true)
    public ScanResponse get(Long userId, UUID id) {
        return mapper.toResponse(find(userId, id));
    }

    @Transactional(readOnly = true)
    public Scan find(Long userId, UUID id) {
        return scans.findByPublicIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Scan not found"));
    }

    @Transactional(readOnly = true)
    public PageResponse<ScanSummary> list(Long userId, ScanCategory category, RiskLevel level, LocalDate from,
                                          LocalDate to, String q, int page, int size) {
        if (page < 0) {
            throw new BadRequestException("INVALID_PAGE", "Page must be 0 or greater.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("INVALID_PAGE_SIZE", "Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("INVALID_DATE_RANGE", "'From' date must be on or before 'to' date.");
        }
        if (q != null && q.length() > 100) {
            throw new BadRequestException("SEARCH_TOO_LONG", "Search text must be at most 100 characters.");
        }
        Specification<Scan> spec = filter(userId, category, level, from, to, q);
        var result = scans.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, mapper::toSummary);
    }

    @Transactional
    public void delete(Long userId, UUID id) {
        scans.delete(find(userId, id));
    }

    @Transactional
    public int deleteAll(Long userId) {
        return scans.deleteAllByUserId(userId);
    }

    static Specification<Scan> filter(Long userId, ScanCategory category, RiskLevel level, LocalDate from, LocalDate to,
                                      String q) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("user").get("id"), userId));
            if (category != null) {
                p.add(cb.equal(root.get("category"), category));
            }
            if (level != null) {
                p.add(cb.equal(root.get("riskLevel"), level));
            }
            if (from != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (to != null) {
                p.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%")
                        .replace("_", "\\_") + "%";
                Subquery<Long> sub = query.subquery(Long.class);
                var ind = sub.from(ScanIndicator.class);
                sub.select(ind.get("id")).where(cb.equal(ind.get("scan"), root),
                        cb.like(cb.lower(ind.get("title")), like, '\\'));
                p.add(cb.or(cb.like(cb.lower(root.get("inputExcerpt")), like, '\\'), cb.exists(sub)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /** Ensures the input object matching the category is present. */
    static void validateShape(ScanRequest r) {
        Object input = switch (r.category()) {
            case MESSAGE -> r.message();
            case URL -> r.url();
            case TRANSACTION -> r.transaction();
            case CALL -> r.call();
            case APP_PERMISSION -> r.app();
        };
        if (input == null) {
            String field = switch (r.category()) {
                case MESSAGE -> "message";
                case URL -> "url";
                case TRANSACTION -> "transaction";
                case CALL -> "call";
                case APP_PERMISSION -> "app";
            };
            throw new BadRequestException("MISSING_INPUT",
                    "Category " + r.category() + " requires the '" + field + "' object in the request body.");
        }
    }

    static FraudEvent toEvent(ScanRequest r, ScanHistory h) {
        return switch (r.category()) {
            case MESSAGE -> FraudEvent.ofMessage(r.message(), h);
            case URL -> FraudEvent.ofUrl(r.url(), h);
            case TRANSACTION -> FraudEvent.ofTransaction(r.transaction(), h);
            case CALL -> FraudEvent.ofCall(r.call(), h);
            case APP_PERMISSION -> FraudEvent.ofApp(r.app(), h);
        };
    }

    /** The identifier used to recognise repeat subjects; hashed before storage. */
    static String subject(ScanRequest r) {
        return switch (r.category()) {
            case MESSAGE -> r.message().sender() == null || r.message().sender().isBlank() ? null : "sender:" + r.message().sender();
            case URL -> ParsedUrl.parse(r.url().url()).map(u -> "domain:" + u.registrableDomain()).orElse(null);
            case TRANSACTION -> r.transaction().recipient() == null || r.transaction().recipient().isBlank() ? null
                    : "recipient:" + r.transaction().recipient();
            case CALL -> r.call().callerNumber() != null && !r.call().callerNumber().isBlank()
                    ? "number:" + r.call().callerNumber().replaceAll("[^0-9+]", "")
                    : r.call().callerClaimedIdentity() == null || r.call().callerClaimedIdentity().isBlank() ? null
                    : "caller:" + r.call().callerClaimedIdentity();
            case APP_PERMISSION -> "app:" + r.app().appName();
        };
    }
}
