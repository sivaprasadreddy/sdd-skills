package dev.sivalabs.sdd.plugin.parser;

import dev.sivalabs.sdd.plugin.model.PlanStep;
import dev.sivalabs.sdd.plugin.model.PlanSubTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlanMdParser {

    // Matches: ## Step 1 — Title  or  ### Step 1: Title  or  ## Step 1 - Title
    private static final Pattern STEP_PATTERN = Pattern.compile(
            "^#{2,3}\\s+[Ss]tep\\s+(\\d+)\\s*[—:\\-]?\\s*(.*)$"
    );
    // Matches: - [ ] description  or  - [x] description
    private static final Pattern SUBTASK_PATTERN = Pattern.compile(
            "^-\\s+\\[([ xX])\\]\\s+(.+)$"
    );
    // Matches backtick-enclosed paths that contain a slash (i.e. file paths)
    private static final Pattern BACKTICK_PATH = Pattern.compile("`([^`]+/[^`]+)`");

    public List<PlanStep> parse(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        List<PlanStep> steps = new ArrayList<>();
        int currentNumber = -1;
        String currentTitle = null;
        List<PlanSubTask> currentSubTasks = null;

        for (String line : content.split("\n")) {
            String trimmed = line.trim();

            Matcher stepMatcher = STEP_PATTERN.matcher(trimmed);
            if (stepMatcher.matches()) {
                if (currentNumber > 0 && currentTitle != null) {
                    steps.add(new PlanStep(currentNumber, currentTitle,
                            currentSubTasks != null ? currentSubTasks : Collections.emptyList()));
                }
                currentNumber = Integer.parseInt(stepMatcher.group(1));
                currentTitle = stepMatcher.group(2).trim();
                currentSubTasks = new ArrayList<>();
                continue;
            }

            if (currentNumber > 0 && currentSubTasks != null) {
                Matcher subMatcher = SUBTASK_PATTERN.matcher(trimmed);
                if (subMatcher.matches()) {
                    boolean done = !subMatcher.group(1).equals(" ");
                    String desc = subMatcher.group(2).trim();
                    String filePath = extractFilePath(desc);
                    currentSubTasks.add(new PlanSubTask(desc, done, filePath));
                }
            }
        }

        if (currentNumber > 0 && currentTitle != null) {
            steps.add(new PlanStep(currentNumber, currentTitle,
                    currentSubTasks != null ? currentSubTasks : Collections.emptyList()));
        }

        return steps;
    }

    private String extractFilePath(String text) {
        Matcher m = BACKTICK_PATH.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        // Also detect bare paths like src/main/... that look like file paths
        if (text.startsWith("src/") || text.startsWith("test/") || text.startsWith("resources/")) {
            return text.split("\\s")[0];
        }
        return null;
    }
}
