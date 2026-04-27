package dev.sivalabs.sdd.plugin.model;

import java.util.Collections;
import java.util.List;

public class SddState {
    private final FeatureSpec featureSpec;
    private final WorkflowStage currentStage;
    private final List<PlanStep> planSteps;
    private final ReviewReport reviewReport;
    private final String implSummary;
    private final ProjectContext projectContext;
    private final List<ArchivedFeature> archivedFeatures;

    public static SddState empty() {
        return new SddState(null, WorkflowStage.INIT, Collections.emptyList(), null, null, null, Collections.emptyList());
    }

    public SddState(FeatureSpec featureSpec, WorkflowStage currentStage, List<PlanStep> planSteps) {
        this(featureSpec, currentStage, planSteps, null, null, null, Collections.emptyList());
    }

    public SddState(FeatureSpec featureSpec, WorkflowStage currentStage, List<PlanStep> planSteps,
                    ReviewReport reviewReport) {
        this(featureSpec, currentStage, planSteps, reviewReport, null, null, Collections.emptyList());
    }

    public SddState(FeatureSpec featureSpec, WorkflowStage currentStage, List<PlanStep> planSteps,
                    ReviewReport reviewReport, ProjectContext projectContext,
                    List<ArchivedFeature> archivedFeatures) {
        this(featureSpec, currentStage, planSteps, reviewReport, projectContext, null, archivedFeatures);
    }

    public SddState(FeatureSpec featureSpec, WorkflowStage currentStage, List<PlanStep> planSteps,
                    ReviewReport reviewReport, ProjectContext projectContext,
                    String implSummary, List<ArchivedFeature> archivedFeatures) {
        this.featureSpec = featureSpec;
        this.currentStage = currentStage;
        this.planSteps = planSteps;
        this.reviewReport = reviewReport;
        this.implSummary = implSummary;
        this.projectContext = projectContext;
        this.archivedFeatures = archivedFeatures != null ? archivedFeatures : Collections.emptyList();
    }

    public FeatureSpec getFeatureSpec()               { return featureSpec; }
    public WorkflowStage getCurrentStage()            { return currentStage; }
    public List<PlanStep> getPlanSteps()              { return planSteps; }
    public ReviewReport getReviewReport()             { return reviewReport; }
    public String getImplSummary()                    { return implSummary; }
    public ProjectContext getProjectContext()          { return projectContext; }
    public List<ArchivedFeature> getArchivedFeatures(){ return archivedFeatures; }

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
