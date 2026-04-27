package dev.sivalabs.sdd.plugin.model;

import java.util.Collections;
import java.util.List;

public class FeatureSpec {
    private final String title;
    private final String summary;
    private final List<AcItem> acceptanceCriteria;
    private final List<String> userStories;
    private final List<String> functionalRequirements;
    private final List<String> outOfScope;
    private final List<String> openQuestions;
    private final List<RevisionEntry> revisionHistory;
    private final String rawContent;

    public static FeatureSpec empty() {
        return new FeatureSpec(null, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "");
    }

    public FeatureSpec(String title, String summary,
                       List<AcItem> acceptanceCriteria,
                       List<String> userStories,
                       List<String> functionalRequirements,
                       List<String> outOfScope,
                       List<String> openQuestions,
                       List<RevisionEntry> revisionHistory,
                       String rawContent) {
        this.title = title;
        this.summary = summary;
        this.acceptanceCriteria = acceptanceCriteria;
        this.userStories = userStories;
        this.functionalRequirements = functionalRequirements;
        this.outOfScope = outOfScope;
        this.openQuestions = openQuestions;
        this.revisionHistory = revisionHistory;
        this.rawContent = rawContent;
    }

    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public List<AcItem> getAcceptanceCriteria() { return acceptanceCriteria; }
    public List<String> getUserStories() { return userStories; }
    public List<String> getFunctionalRequirements() { return functionalRequirements; }
    public List<String> getOutOfScope() { return outOfScope; }
    public List<String> getOpenQuestions() { return openQuestions; }
    public List<RevisionEntry> getRevisionHistory() { return revisionHistory; }
    public String getRawContent() { return rawContent; }

    public int getUnresolvedQuestionCount() {
        return openQuestions.size();
    }

    public String getLatestVersion() {
        if (revisionHistory.isEmpty()) return null;
        return revisionHistory.get(0).version();
    }

    public String getLatestDate() {
        if (revisionHistory.isEmpty()) return null;
        return revisionHistory.get(0).date();
    }
}
