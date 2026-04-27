package dev.sivalabs.sdd.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;
import dev.sivalabs.sdd.plugin.model.SddState;
import dev.sivalabs.sdd.plugin.model.WorkflowStage;
import dev.sivalabs.sdd.plugin.service.SddStateService;
import dev.sivalabs.sdd.plugin.service.SddStateTopic;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class SddToolWindowPanel extends JPanel {

    private final JBTabbedPane tabbedPane;
    private final PipelinePanel   pipelinePanel;
    private final SpecPanel       specPanel;
    private final PlanPanel       planPanel;
    private final ImplementPanel  implementPanel;
    private final ReviewPanel     reviewPanel;
    private final HistoryPanel    historyPanel;
    private boolean extraTabsShown = false;

    public SddToolWindowPanel(Project project) {
        super(new BorderLayout());

        SddStateService stateService = SddStateService.getInstance(project);
        SddState initialState = stateService.getState();

        tabbedPane = new JBTabbedPane();

        specPanel      = new SpecPanel(project, initialState);
        planPanel      = new PlanPanel(project, initialState);
        implementPanel = new ImplementPanel(project, initialState);
        reviewPanel    = new ReviewPanel(project, initialState);
        historyPanel   = new HistoryPanel(project, initialState);

        Consumer<WorkflowStage> stageNav = stage -> {
            JComponent target = switch (stage) {
                case ANALYSE  -> specPanel;
                case PLAN     -> planPanel;
                case IMPLEMENT -> implementPanel;
                case REVIEW   -> reviewPanel;
                default -> null;
            };
            if (target != null && tabbedPane.indexOfComponent(target) >= 0) {
                tabbedPane.setSelectedComponent(target);
            }
        };
        pipelinePanel = new PipelinePanel(project, initialState, stageNav);

        tabbedPane.addTab("Pipeline", pipelinePanel);
        add(tabbedPane, BorderLayout.CENTER);

        syncTabs(initialState);

        project.getMessageBus().connect().subscribe(SddStateTopic.TOPIC, state ->
                SwingUtilities.invokeLater(() -> {
                    pipelinePanel.updateState(state);
                    specPanel.updateState(state);
                    planPanel.updateState(state);
                    implementPanel.updateState(state);
                    reviewPanel.updateState(state);
                    historyPanel.updateState(state);
                    syncTabs(state);
                })
        );
    }

    private void syncTabs(SddState state) {
        boolean hasProject = state.getCurrentStage() != WorkflowStage.INIT;
        if (hasProject == extraTabsShown) return;

        if (hasProject) {
            tabbedPane.addTab("Spec",      specPanel);
            tabbedPane.addTab("Plan",      planPanel);
            tabbedPane.addTab("Implement", implementPanel);
            tabbedPane.addTab("Review",    reviewPanel);
            tabbedPane.addTab("History",   historyPanel);
        } else {
            while (tabbedPane.getTabCount() > 1) {
                tabbedPane.removeTabAt(tabbedPane.getTabCount() - 1);
            }
        }
        extraTabsShown = hasProject;
    }
}
