package com.universalshield.reporting;

import com.universalshield.dto.IndicatorDto;
import com.universalshield.dto.ScanResponse;
import com.universalshield.engine.Recommendation;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates downloadable reports. The HTML report is a self-contained file with every value
 * HTML-escaped, so malicious text from a scanned message cannot execute when the report is opened.
 */
@Service
public class ReportService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    public Map<String, Object> json(ScanResponse r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reportType", "UniversalShield scan report");
        m.put("generatedBy", "UniversalShield 1.0.0");
        m.put("result", r);
        return m;
    }

    public String html(ScanResponse r) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>UniversalShield report ").append(esc(r.id().toString())).append("</title><style>")
                .append("body{font-family:system-ui,Segoe UI,Arial,sans-serif;max-width:860px;margin:2rem auto;padding:0 1rem;color:#0b1220;line-height:1.5}")
                .append("h1{margin:0}.muted{color:#4a5568}.badge{display:inline-block;padding:.2rem .6rem;border-radius:999px;font-weight:700;color:#fff}")
                .append(".LOW{background:#2f855a}.MEDIUM{background:#b7791f}.HIGH{background:#c05621}.CRITICAL{background:#c53030}")
                .append("table{border-collapse:collapse;width:100%}td,th{border:1px solid #cbd5e0;padding:.4rem;text-align:left;vertical-align:top}")
                .append(".box{border:1px solid #cbd5e0;border-radius:8px;padding:1rem;margin:1rem 0}</style></head><body>");
        sb.append("<h1>UniversalShield scan report</h1><p class=\"muted\">One shield for your digital life</p>");
        sb.append("<div class=\"box\"><p><strong>Result ID:</strong> ").append(esc(r.id().toString())).append("<br>")
                .append("<strong>Category:</strong> ").append(esc(r.category())).append("<br>")
                .append("<strong>Scanned:</strong> ").append(esc(TS.format(r.createdAt()))).append("<br>")
                .append("<strong>Risk score:</strong> ").append(r.riskScore()).append(" / 100 ")
                .append("<span class=\"badge ").append(esc(r.riskLevel())).append("\">").append(esc(r.riskLevel())).append("</span><br>")
                .append("<strong>Evidence strength:</strong> ").append(esc(r.evidenceStrength())).append("<br>")
                .append("<strong>Sources:</strong> ").append(esc(String.join(", ", r.sources()))).append("<br>")
                .append("<strong>Ruleset:</strong> ").append(esc(r.rulesetVersion())).append(" &middot; <strong>Detectors:</strong> ")
                .append(esc(r.detectorVersions().toString())).append("</p>");
        if (r.demo()) {
            sb.append("<p><strong>DEMO:</strong> this scan was generated from synthetic demonstration data.</p>");
        }
        if (r.inputExcerpt() != null) {
            sb.append("<p><strong>Input excerpt (sanitised):</strong><br>").append(esc(r.inputExcerpt())).append("</p>");
        }
        sb.append("</div><h2>Why this result</h2><ul>");
        r.reasons().forEach(x -> sb.append("<li>").append(esc(x)).append("</li>"));
        sb.append("</ul><h2>Matched indicators</h2>");
        if (r.indicators().isEmpty()) {
            sb.append("<p>No indicators matched.</p>");
        } else {
            sb.append("<table><tr><th>Indicator</th><th>Explanation</th><th>Weight</th><th>Source</th><th>Evidence</th></tr>");
            for (IndicatorDto i : r.indicators()) {
                sb.append("<tr><td>").append(esc(i.title())).append("<br><small>").append(esc(i.code())).append("</small></td><td>")
                        .append(esc(i.explanation())).append("</td><td>").append(String.format(java.util.Locale.ROOT, "%.2f", i.weight()))
                        .append("</td><td>").append(esc(i.source())).append("</td><td>").append(esc(i.evidence() == null ? "" : i.evidence()))
                        .append("</td></tr>");
            }
            sb.append("</table>");
        }
        sb.append("<h2>Recommended actions</h2><ol>");
        for (Recommendation rec : r.recommendations()) {
            sb.append("<li><strong>").append(esc(rec.action())).append("</strong><br><span class=\"muted\">")
                    .append(esc(rec.reason())).append("</span></li>");
        }
        sb.append("</ol>");
        if (!r.notes().isEmpty()) {
            sb.append("<h2>Notes</h2><ul>");
            r.notes().forEach(x -> sb.append("<li>").append(esc(x)).append("</li>"));
            sb.append("</ul>");
        }
        sb.append("<h2>Scoring method</h2><p>").append(esc(r.scoringMethod())).append("</p>");
        sb.append("<h2>Important</h2><ul>");
        r.disclaimers().forEach(x -> sb.append("<li>").append(esc(x)).append("</li>"));
        sb.append("</ul></body></html>");
        return sb.toString();
    }

    /** Minimal, complete HTML escaping for text and attribute contexts. */
    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
