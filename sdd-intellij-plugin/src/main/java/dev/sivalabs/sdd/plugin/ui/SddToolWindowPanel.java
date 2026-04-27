package dev.sivalabs.sdd.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;
import dev.sivalabs.sdd.plugin.service.SddStateService;
import dev.sivalabs.sdd.plugin.service.SddStateTopic;

import javax.swing.*;
import java.awt.*;

public class SddToolWindowPanel extends JPanel {

    public SddToolWindowPanel(Project project) {
        super(new BorderLayout());

        SddStateService stateService = SddStateService.getInstance(project);

        PipelinePanel pipelinePanel = new PipelinePanel(project, stateService.getState());
        PlanPanel planPanel = new PlanPanel(project, stateService.getState());

        JBTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.addTab("Pipeline", pipelinePanel);
        tabbedPane.addTab("Plan", planPanel);
        add(tabbedPane, BorderLayout.CENTER);

        project.getMessageBus().connect().subscribe(SddStateTopic.TOPIC, state ->
                SwingUtilities.invokeLater(() -> {
                    pipelinePanel.updateState(state);
                    planPanel.updateState(state);
                })
        );
    }
}
