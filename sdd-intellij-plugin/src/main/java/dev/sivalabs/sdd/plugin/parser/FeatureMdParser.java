package dev.sivalabs.sdd.plugin.parser;

import dev.sivalabs.sdd.plugin.model.AcItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureMdParser {

    // Matches: - [ ] **AC-01**: description  OR  - [x] AC-01 — description
    private static final Pattern AC_PATTERN = Pattern.compile(
            "^-\\s+\\[([ xX])\\]\\s+\\*{0,2}(AC-\\d+)\\*{0,2}[\\s:—\\-]*(.+)$"
    );
    private static final Pattern H1_PATTERN = Pattern.compile("^#\\s+(.+)$");

    public ParseResult parse(String content) {
        if (content == null || content.isBlank()) {
            return new ParseResult(null, Collections.emptyList());
        }

        String featureName = null;
        List<AcItem> acs = new ArrayList<>();

        for (String line : content.split("\n")) {
            String trimmed = line.trim();

            if (featureName == null) {
                Matcher h1 = H1_PATTERN.matcher(trimmed);
                if (h1.matches()) {
                    featureName = h1.group(1).trim();
                    continue;
                }
            }

            Matcher ac = AC_PATTERN.matcher(trimmed);
            if (ac.matches()) {
                boolean checked = !ac.group(1).equals(" ");
                String id = ac.group(2);
                String desc = ac.group(3).trim().replace("**", "");
                acs.add(new AcItem(id, desc, checked));
            }
        }

        return new ParseResult(featureName, acs);
    }

    public record ParseResult(String featureName, List<AcItem> acceptanceCriteria) {}
}
