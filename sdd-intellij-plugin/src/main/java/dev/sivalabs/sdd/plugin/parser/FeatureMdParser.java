package dev.sivalabs.sdd.plugin.parser;

import dev.sivalabs.sdd.plugin.model.AcItem;
import dev.sivalabs.sdd.plugin.model.FeatureSpec;
import dev.sivalabs.sdd.plugin.model.RevisionEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureMdParser {

    private static final Pattern H1 = Pattern.compile("^#\\s+(.+)$");
    private static final Pattern H2_OR_H3 = Pattern.compile("^#{2,3}\\s+(.+)$");

    // Accepts: - [ ] **AC-01**: desc  or  - [x] AC-01 — desc
    private static final Pattern AC = Pattern.compile(
            "^-\\s+\\[([ xX])\\]\\s+\\*{0,2}(AC-\\d+)\\*{0,2}[\\s:—\\-]*(.+)$"
    );

    // Accepts: - **v3** (2026-04-27): desc  or  - v2 2026-04-25 desc
    private static final Pattern REVISION = Pattern.compile(
            "^-\\s+\\*{0,2}(v\\d+[^\\s*]*)\\*{0,2}[\\s(]*(\\d{4}-\\d{2}-\\d{2})?[)\\s:]*(.+)$"
    );

    private enum Section {
        NONE, SUMMARY, USER_STORIES, FUNCTIONAL_REQUIREMENTS,
        ACCEPTANCE_CRITERIA, OUT_OF_SCOPE, OPEN_QUESTIONS, REVISION_HISTORY, OTHER
    }

    public FeatureSpec parse(String content) {
        if (content == null || content.isBlank()) return FeatureSpec.empty();

        String title = null;
        List<String> summaryLines = new ArrayList<>();
        List<AcItem> acs = new ArrayList<>();
        List<String> userStories = new ArrayList<>();
        List<String> functionalReqs = new ArrayList<>();
        List<String> outOfScope = new ArrayList<>();
        List<String> openQuestions = new ArrayList<>();
        List<RevisionEntry> revisions = new ArrayList<>();

        Section current = Section.NONE;

        for (String line : content.split("\n")) {
            String t = line.trim();

            // H1 → title
            if (title == null) {
                Matcher m = H1.matcher(t);
                if (m.matches()) {
                    title = m.group(1)
                            .replaceFirst("(?i)^feature[:\\-–]\\s*", "")
                            .trim();
                    continue;
                }
            }

            // H2/H3 → new section
            Matcher h = H2_OR_H3.matcher(t);
            if (h.matches()) {
                current = detectSection(h.group(1).toLowerCase());
                continue;
            }

            if (t.isEmpty()) continue;

            switch (current) {
                case SUMMARY -> summaryLines.add(t);

                case ACCEPTANCE_CRITERIA -> {
                    Matcher ac = AC.matcher(t);
                    if (ac.matches()) {
                        boolean checked = !ac.group(1).equals(" ");
                        String id = ac.group(2);
                        String desc = ac.group(3).trim().replace("**", "");
                        acs.add(new AcItem(id, desc, checked));
                    }
                }

                case USER_STORIES -> {
                    if (t.startsWith("- ") || t.startsWith("* "))
                        userStories.add(t.substring(2).trim().replace("**", ""));
                }

                case FUNCTIONAL_REQUIREMENTS -> {
                    if (t.startsWith("- ") || t.startsWith("* "))
                        functionalReqs.add(t.substring(2).trim().replace("**", ""));
                }

                case OUT_OF_SCOPE -> {
                    if (t.startsWith("- ") || t.startsWith("* "))
                        outOfScope.add(t.substring(2).trim());
                }

                case OPEN_QUESTIONS -> {
                    if (t.startsWith("- ") || t.startsWith("* "))
                        openQuestions.add(t.substring(2).trim());
                }

                case REVISION_HISTORY -> {
                    Matcher r = REVISION.matcher(t);
                    if (r.matches()) {
                        String ver = r.group(1);
                        String date = r.group(2) != null ? r.group(2) : "";
                        String desc = r.group(3).trim();
                        revisions.add(new RevisionEntry(ver, date, desc));
                    }
                }

                default -> { /* ignore */ }
            }
        }

        String summary = summaryLines.isEmpty() ? null : String.join(" ", summaryLines);

        return new FeatureSpec(title, summary, acs, userStories, functionalReqs,
                outOfScope, openQuestions, revisions, content);
    }

    private Section detectSection(String heading) {
        if (heading.contains("summary") || heading.contains("overview") || heading.contains("description"))
            return Section.SUMMARY;
        if (heading.contains("user stor") || (heading.contains("stories") && !heading.contains("revision")))
            return Section.USER_STORIES;
        if (heading.contains("functional req") || (heading.contains("requirement") && !heading.contains("non-func")))
            return Section.FUNCTIONAL_REQUIREMENTS;
        if (heading.contains("acceptance") || heading.contains("criteria"))
            return Section.ACCEPTANCE_CRITERIA;
        if (heading.contains("out of scope") || (heading.contains("scope") && heading.contains("out")))
            return Section.OUT_OF_SCOPE;
        if (heading.contains("open question") || heading.contains("questions"))
            return Section.OPEN_QUESTIONS;
        if (heading.contains("revision") || heading.contains("changelog") || heading.contains("change history"))
            return Section.REVISION_HISTORY;
        return Section.OTHER;
    }
}
