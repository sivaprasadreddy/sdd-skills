package dev.sivalabs.sdd.plugin.model;

import java.util.Collections;
import java.util.List;

public class SddState {
    private final FeatureSpec featureSpec;
    private final WorkflowStage currentStage;
    private final List<PlanStep> planSteps;
    private final ReviewReport reviewReport;

    public static SddState empty() {
        return new SddState(null, WorkflowStage.INIT, Collections.emptyList(), null);
    }

    public SddState(FeatureSpec featureSpec, WorkflowStage currentStage, List<PlanStep> planSteps) {
        this(featureSpec, currentStage, planSteps, null);
    }

    public SddState(FeatureSpec featureSpec, WorkflowStage currentStage, List<PlanStep> planSteps,
                    ReviewReport reviewReport) {
        this.featureSpec = featureSpec;
        this.currentStage = currentStage;
        this.planSteps = planSteps;
        this.reviewReport = reviewReport;
    }

    public FeatureSpec getFeatureSpec() { return featureSpec; }
    public WorkflowStage getCurrentStage() { return currentStage; }
    public List<PlanStep> getPlanSteps() { return planSteps; }
    public ReviewReport getReviewReport() { return reviewReport; }

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
