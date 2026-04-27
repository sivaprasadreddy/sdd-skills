package dev.sivalabs.sdd.plugin.parser;

import dev.sivalabs.sdd.plugin.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses review.md produced by /sdd-review into a ReviewReport.
 * Handles both ### heading and **bold** finding formats.
 */
public class ReviewReportParser {

    // Matches: ### AuthService.java:84 — Title
    //      or: **AuthService.java:84** — Title
    //      or: - **`AuthService.java:84`** — Title
    private static final Pattern FINDING_HEADING = Pattern.compile(
            "(?:#{2,4}\\s+|[-*]\\s+)?\\*{0,2}`?([\\w./]+\\.\\w+):(\\d+)`?\\*{0,2}\\s*[—–\\-]+\\s*(.+)"
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

        // Pending finding state
        String pendingFile = null;
        int pendingLine = -1;
        String pendingTitle = null;
        List<String> pendingDesc = new ArrayList<>();

        for (String rawLine : content.split("\n")) {
            String t = rawLine.trim();

            // ── verdict ────────────────────────────────────────────────
            if (verdict == null && t.toLowerCase().contains("verdict")) {
                verdict = ReviewVerdict.detect(t);
            }

            // ── date ───────────────────────────────────────────────────
            if (lastRunTime == null) {
                Matcher dm = DATE_LINE.matcher(t);
                if (dm.find()) lastRunTime = dm.group(1).trim();
            }

            // ── AC coverage section header ─────────────────────────────
            if (t.toLowerCase().matches(".*acceptance criteria coverage.*")
                    || t.toLowerCase().matches(".*ac coverage.*")) {
                savePending(findings, currentSeverity, pendingFile, pendingLine, pendingTitle, pendingDesc);
                pendingFile = null; pendingTitle = null; pendingDesc = new ArrayList<>();
                inAcSection = true;
                continue;
            }

            // ── AC coverage entry ──────────────────────────────────────
            if (inAcSection) {
                Matcher acm = AC_LINE.matcher(t);
                if (acm.matches()) {
                    String symbol = acm.group(1);
                    boolean covered = symbol.equals("✓") || symbol.equals("[x]");
                    String id = acm.group(2);
                    String desc = acm.group(3).trim()
                            .replaceAll("\\*+", "")
                            .replaceAll("\\|.*$", "")
                            .trim();
                    acCoverage.add(new AcCoverage(id, desc, covered));
                }
                continue;
            }

            // ── severity section heading ───────────────────────────────
            if (t.startsWith("#") || t.startsWith("##")) {
                Severity s = Severity.detect(t);
                if (s != null) {
                    savePending(findings, currentSeverity, pendingFile, pendingLine, pendingTitle, pendingDesc);
                    pendingFile = null; pendingTitle = null; pendingDesc = new ArrayList<>();
                    currentSeverity = s;
                    inAcSection = false;
                    continue;
                }
            }

            // ── finding heading ────────────────────────────────────────
            if (currentSeverity != null) {
                Matcher fm = FINDING_HEADING.matcher(t);
                if (fm.matches()) {
                    savePending(findings, currentSeverity, pendingFile, pendingLine, pendingTitle, pendingDesc);
                    pendingFile = fm.group(1);
                    pendingLine = Integer.parseInt(fm.group(2));
                    pendingTitle = fm.group(3).trim().replaceAll("\\*+", "");
                    pendingDesc = new ArrayList<>();
                    continue;
                }
            }

            // ── description line ───────────────────────────────────────
            if (pendingTitle != null && !t.isEmpty()
                    && !t.matches("[*_=\\-]{3,}") && !t.startsWith("#")) {
                String clean = t.replaceAll("^[>|]\\s*", "").trim();
                if (!clean.isEmpty()) pendingDesc.add(clean);
            }
        }

        savePending(findings, currentSeverity, pendingFile, pendingLine, pendingTitle, pendingDesc);

        return new ReviewReport(
                verdict != null ? verdict : ReviewVerdict.MERGE_AFTER_MINOR,
                lastRunTime,
                findings,
                acCoverage
        );
    }

    private void savePending(List<ReviewFinding> out, Severity severity,
                              String file, int line, String title, List<String> desc) {
        if (title == null || severity == null) return;
        String description = String.join(" ", desc).trim();
        out.add(new ReviewFinding(severity, file, line, title, description));
    }
}
