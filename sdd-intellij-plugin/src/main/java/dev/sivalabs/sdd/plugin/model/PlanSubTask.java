package dev.sivalabs.sdd.plugin.model;

public class PlanSubTask {
    private final String description;
    private final boolean done;
    private final String filePath;

    public PlanSubTask(String description, boolean done, String filePath) {
        this.description = description;
        this.done = done;
        this.filePath = filePath;
    }

    public String getDescription() { return description; }
    public boolean isDone() { return done; }
    /** Relative project path extracted from the description, or null. */
    public String getFilePath() { return filePath; }
}
