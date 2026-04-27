package dev.sivalabs.sdd.plugin.model;

import java.util.List;

public class PlanStep {
    private final int number;
    private final String title;
    private final List<PlanSubTask> subTasks;

    public PlanStep(int number, String title, List<PlanSubTask> subTasks) {
        this.number = number;
        this.title = title;
        this.subTasks = subTasks;
    }

    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public List<PlanSubTask> getSubTasks() { return subTasks; }

    public StepStatus getStatus() {
        if (subTasks.isEmpty()) return StepStatus.PENDING;
        long doneCount = subTasks.stream().filter(PlanSubTask::isDone).count();
        if (doneCount == subTasks.size()) return StepStatus.DONE;
        if (doneCount > 0) return StepStatus.IN_PROGRESS;
        return StepStatus.PENDING;
    }
}
