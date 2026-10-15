package com.universalshield.api;

import com.universalshield.dto.ScanResponse;
import com.universalshield.exception.BadRequestException;
import com.universalshield.reporting.ReportService;
import com.universalshield.security.AppUserPrincipal;
import com.universalshield.service.ScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Downloadable reports for a single scan (owner only). */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports")
public class ReportController {

    private final ScanService scans;
    private final ReportService reports;

    public ReportController(ScanService scans, ReportService reports) {
        this.scans = scans;
        this.reports = reports;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Download a report as html or json (format parameter)")
    public ResponseEntity<?> report(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable UUID id,
                                    @RequestParam(defaultValue = "html") String format) {
        ScanResponse r = scans.get(p.getId(), id);
        String name = "universalshield-report-" + id;
        return switch (format) {
            case "html" -> ResponseEntity.ok()
                    .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name + ".html").build().toString())
                    .body(reports.html(r));
            case "json" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name + ".json").build().toString())
                    .body(reports.json(r));
            default -> throw new BadRequestException("UNSUPPORTED_FORMAT", "Format must be 'html' or 'json'.");
        };
    }
}
