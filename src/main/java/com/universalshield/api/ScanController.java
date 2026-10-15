package com.universalshield.api;

import com.universalshield.detectors.app.PermissionReportParser;
import com.universalshield.dto.ApiResponse;
import com.universalshield.dto.PageResponse;
import com.universalshield.dto.PermissionReportResponse;
import com.universalshield.dto.ScanRequest;
import com.universalshield.dto.ScanResponse;
import com.universalshield.dto.ScanSummary;
import com.universalshield.engine.RiskLevel;
import com.universalshield.engine.ScanCategory;
import com.universalshield.exception.BadRequestException;
import com.universalshield.security.AppUserPrincipal;
import com.universalshield.service.ScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Create, list, view and delete the signed-in user's scans. */
@RestController
@RequestMapping("/api/scans")
@Tag(name = "Scans")
public class ScanController {

    static final long MAX_REPORT_BYTES = 64 * 1024;
    static final Set<String> REPORT_EXTENSIONS = Set.of("txt", "json", "csv");

    private final ScanService scans;
    private final PermissionReportParser parser;

    public ScanController(ScanService scans, PermissionReportParser parser) {
        this.scans = scans;
        this.parser = parser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Run a scan and store the result")
    public ApiResponse<ScanResponse> create(@AuthenticationPrincipal AppUserPrincipal p, @Valid @RequestBody ScanRequest req) {
        return ApiResponse.ok(scans.create(p.getId(), req));
    }

    @GetMapping
    @Operation(summary = "List my scans with filters and pagination (page starts at 0)")
    public ApiResponse<PageResponse<ScanSummary>> list(@AuthenticationPrincipal AppUserPrincipal p,
                                                       @RequestParam(required = false) ScanCategory category,
                                                       @RequestParam(required = false) RiskLevel riskLevel,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                       @RequestParam(required = false) String q,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(scans.list(p.getId(), category, riskLevel, from, to, q, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my scans")
    public ApiResponse<ScanResponse> get(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable UUID id) {
        return ApiResponse.ok(scans.get(p.getId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one of my scans")
    public ApiResponse<Map<String, String>> delete(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable UUID id) {
        scans.delete(p.getId(), id);
        return ApiResponse.ok(Map.of("deleted", id.toString()));
    }

    @DeleteMapping
    @Operation(summary = "Delete all my scans")
    public ApiResponse<Map<String, Integer>> deleteAll(@AuthenticationPrincipal AppUserPrincipal p) {
        return ApiResponse.ok(Map.of("deleted", scans.deleteAll(p.getId())));
    }

    @PostMapping(path = "/app-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Parse an uploaded permission report (.txt/.json/.csv, max 64 KB). Nothing is stored.")
    public ApiResponse<PermissionReportResponse> parseReport(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("EMPTY_FILE", "The uploaded file is empty.");
        }
        if (file.getSize() > MAX_REPORT_BYTES) {
            throw new BadRequestException("FILE_TOO_LARGE", "Permission reports must be at most 64 KB.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        if (!REPORT_EXTENSIONS.contains(ext)) {
            throw new BadRequestException("UNSUPPORTED_FILE_TYPE", "Only .txt, .json or .csv permission reports are supported.");
        }
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(file.getBytes())).toString();
        } catch (CharacterCodingException e) {
            throw new BadRequestException("NOT_TEXT", "The file must be UTF-8 text.");
        }
        if (content.indexOf('\0') >= 0) {
            throw new BadRequestException("NOT_TEXT", "The file must be plain text.");
        }
        PermissionReportParser.ParsedReport parsed;
        try {
            parsed = parser.parse(content);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("UNPARSEABLE_REPORT", e.getMessage());
        }
        if (parsed.permissions().isEmpty()) {
            throw new BadRequestException("NO_PERMISSIONS_FOUND", "No permission names were found in the file.");
        }
        return ApiResponse.ok(new PermissionReportResponse(parsed.appName(), parsed.permissions()));
    }
}
