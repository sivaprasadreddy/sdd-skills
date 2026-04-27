package dev.sivalabs.sdd.plugin.parser;

import dev.sivalabs.sdd.plugin.model.ProjectContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectMdParser {

    private static final Pattern HEADING = Pattern.compile("^#{1,4}\\s+(.+)$");
    private static final Pattern BULLET  = Pattern.compile("^[-*]\\s+(.+)$");
    private static final Pattern KEY_VAL = Pattern.compile("^\\*{0,2}([^:*]+?)\\*{0,2}:\\s*(.+)$");

    public ProjectContext parse(String content) {
        if (content == null || content.isBlank()) return ProjectContext.empty();

        Map<String, String> summaryPairs = new LinkedHashMap<>();
        List<String> apiEndpoints = new ArrayList<>();
        List<String> architectureDecisions = new ArrayList<>();
        List<String> approvedDependencies = new ArrayList<>();

        String currentSection = null;
        for (String raw : content.split("\n")) {
            String line = raw.stripTrailing();
            Matcher h = HEADING.matcher(line.strip());
            if (h.matches()) {
                currentSection = h.group(1).toLowerCase();
                continue;
            }
            if (currentSection == null || line.isBlank()) continue;

            String stripped = line.strip();
            if (stripped.startsWith("#")) continue;

            boolean isApi        = currentSection.contains("api");
            boolean isDecision   = currentSection.contains("decision");
            boolean isDependency = currentSection.contains("depend") || currentSection.contains("librar");

            Matcher b = BULLET.matcher(stripped);
            String text = b.matches() ? b.group(1).strip() : stripped;

            if (isApi) {
                apiEndpoints.add(text);
            } else if (isDecision) {
                architectureDecisions.add(text);
            } else if (isDependency) {
                approvedDependencies.add(text);
            } else {
                Matcher kv = KEY_VAL.matcher(text);
                if (kv.matches()) {
                    summaryPairs.put(kv.group(1).strip(), kv.group(2).strip());
                } else if (!text.isBlank()) {
                    // Plain text line in a summary section; use section title as key only once
                    String key = capitalize(currentSection);
                    summaryPairs.merge(key, text, (a, x) -> a + "; " + x);
                }
            }
        }

        return new ProjectContext(summaryPairs, apiEndpoints, architectureDecisions, approvedDependencies, content);
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
