package com.universalshield.api;

import com.universalshield.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Liveness and database connectivity check, used by Docker and hosting health checks. */
@RestController
@Tag(name = "Health")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/health")
    @Operation(summary = "Application and database status")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("application", "UniversalShield");
        m.put("version", "1.0.0");
        boolean db;
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            db = true;
        } catch (Exception e) {
            db = false;
        }
        m.put("status", db ? "UP" : "DEGRADED");
        m.put("database", db ? "UP" : "DOWN");
        return ResponseEntity.status(db ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.ok(m));
    }
}
