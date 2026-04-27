package dev.sivalabs.sdd.plugin.model;

import java.util.Collections;
import java.util.List;

public class SddState {
    private final String featureName;
    private final WorkflowStage currentStage;
    private final List<AcItem> acceptanceCriteria;
    private final List<PlanStep> planSteps;

    public static SddState empty() {
        return new SddState(null, WorkflowStage.INIT, Collections.emptyList(), Collections.emptyList());
    }

    public SddState(String featureName, WorkflowStage currentStage,
                    List<AcItem> acceptanceCriteria, List<PlanStep> planSteps) {
        this.featureName = featureName;
        this.currentStage = currentStage;
        this.acceptanceCriteria = acceptanceCriteria;
        this.planSteps = planSteps;
    }

    public String getFeatureName() { return featureName; }
    public WorkflowStage getCurrentStage() { return currentStage; }
    public List<AcItem> getAcceptanceCriteria() { return acceptanceCriteria; }
    public List<PlanStep> getPlanSteps() { return planSteps; }

    public long getCheckedAcCount() {
        return acceptanceCriteria.stream().filter(AcItem::isChecked).count();
    }

    public int getTotalAcCount() {
        return acceptanceCriteria.size();
    }
}
