package dev.sivalabs.sdd.plugin.parser;

import dev.sivalabs.sdd.plugin.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses review.md produced by /sdd-review into a ReviewReport.
 */
public class ReviewReportParser {

    // | [ ] | `File.java:42` | Category | Problem | Suggestion |
    // | [x] | `File.java:42-50` | ...  (resolved finding)
    private static final Pattern FINDING_TABLE_ROW = Pattern.compile(
            "^\\|\\s*(\\[[x ]\\])\\s*\\|\\s*`([\\w./]+\\.\\w+):(\\d+)(?:-\\d+)?`\\s*\\|\\s*([^|]*)\\|\\s*([^|]*)\\|\\s*([^|]*)\\|?\\s*$"
    );

    // Matches: ✓ AC-01 — description  or  ✗ AC-01 — description
    // Also table rows: | AC-01 | desc | ✓ |
    private static final Pattern AC_LINE = Pattern.compile(
            "^(?:\\|\\s*)?(✓|✗|\\[x]|\\[ ])\\s*\\*{0,2}(AC-\\d+)\\*{0,2}[\\s|—\\-:]*(.*)$"
    );

    private static final Pattern DATE_LINE = Pattern.compile(
            "(?i)(?:date|last run|run time|generated)[:\\s]+(.+)"
    );

    public ReviewReport parse(String content) {
        if (content == null || content.isBlank()) return null;

        ReviewVerdict verdict = null;
        String lastRunTime = null;
        List<ReviewFinding> findings = new ArrayList<>();
        List<AcCoverage> acCoverage = new ArrayList<>();

        Severity currentSeverity = null;
        boolean inAcSection = false;

        for (String rawLine : content.split("\n")) {
            String t = rawLine.trim();

            // ── date ───────────────────────────────────────────────────
            if (lastRunTime == null) {
                Matcher dm = DATE_LINE.matcher(t);
                if (dm.find()) lastRunTime = dm.group(1).trim();
            }

            // ── section headings ───────────────────────────────────────
            if (t.startsWith("#")) {
                String lower = t.toLowerCase();
                if (lower.contains("acceptance criteria") || lower.contains("ac coverage")) {
                    inAcSection = true;
                    currentSeverity = null;
                    continue;
                }
                if (lower.matches(".*\\bverdict\\b.*")) {
                    inAcSection = false;
                    currentSeverity = null;
                    continue;
                }
                Severity s = Severity.detect(t);
                if (s != null) {
                    currentSeverity = s;
                    inAcSection = false;
                }
                continue;
            }

            // ── verdict (checked item in Verdict section) ──────────────
            if (verdict == null && t.contains("[x]")) {
                ReviewVerdict v = ReviewVerdict.detect(t);
                if (v != null) verdict = v;
            }

            // ── AC coverage table row ──────────────────────────────────
            if (inAcSection) {
                Matcher acm = AC_LINE.matcher(t);
                if (acm.matches()) {
                    String symbol = acm.group(1);
                    boolean covered = symbol.equals("✓") || symbol.equals("[x]") || symbol.equals("✅");
                    String id = acm.group(2);
                    String desc = acm.group(3).trim().replaceAll("\\*+", "").replaceAll("\\|.*$", "").trim();
                    acCoverage.add(new AcCoverage(id, desc, covered));
                }
                continue;
            }

            // ── finding table row ──────────────────────────────────────
            if (currentSeverity != null) {
                Matcher ft = FINDING_TABLE_ROW.matcher(t);
                if (ft.matches()) {
                    String file  = ft.group(2);
                    int    line  = Integer.parseInt(ft.group(3));
                    String title = ft.group(5).trim().replaceAll("`", "");
                    String desc  = ft.group(6).trim().replaceAll("`", "");
                    findings.add(new ReviewFinding(currentSeverity, file, line, title, desc));
                }
            }
        }

        return new ReviewReport(
                verdict != null ? verdict : ReviewVerdict.MERGE_AFTER_MINOR,
                lastRunTime,
                findings,
                acCoverage
        );
    }
}
