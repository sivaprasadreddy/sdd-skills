package dev.sivalabs.sdd.plugin.model;

import java.util.Collections;
import java.util.List;

public class SddState {
    private final FeatureSpec featureSpec;
    private final WorkflowStage currentStage;
    private final List<PlanStep> planSteps;

    public static SddState empty() {
        return new SddState(null, WorkflowStage.INIT, Collections.emptyList());
    }

    public SddState(FeatureSpec featureSpec, WorkflowStage currentStage, List<PlanStep> planSteps) {
        this.featureSpec = featureSpec;
        this.currentStage = currentStage;
        this.planSteps = planSteps;
    }

    public FeatureSpec getFeatureSpec() { return featureSpec; }
    public WorkflowStage getCurrentStage() { return currentStage; }
    public List<PlanStep> getPlanSteps() { return planSteps; }

    // Convenience delegates — keep the existing API intact for Pipeline/Plan panels
    public String getFeatureName() {
        return featureSpec != null ? featureSpec.getTitle() : null;
    }

    public List<AcItem> getAcceptanceCriteria() {
        return featureSpec != null ? featureSpec.getAcceptanceCriteria() : Collections.emptyList();
    }

    public long getCheckedAcCount() {
        return getAcceptanceCriteria().stream().filter(AcItem::isChecked).count();
    }

    public int getTotalAcCount() {
        return getAcceptanceCriteria().size();
    }
}
