package dev.sivalabs.sdd.plugin.model;

public enum WorkflowStage {
    INIT("Init"),
    ANALYSE("Analyse"),
    PLAN("Plan"),
    IMPLEMENT("Implement"),
    REVIEW("Review"),
    ARCHIVE("Archive");

    private final String displayName;

    WorkflowStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
