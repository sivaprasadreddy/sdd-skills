package dev.sivalabs.sdd.plugin.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class RefineDialog extends DialogWrapper {

    private final Project project;
    private final String featureName;
    private JBTextArea descriptionArea;
    private JBCheckBox assessImpactBox;
    private JBCheckBox replanBox;

    public RefineDialog(Project project, @Nullable String featureName) {
        super(project, true);
        this.project = project;
        this.featureName = featureName != null ? featureName : "current feature";
        setTitle("Refine Spec");
        setOKButtonText("Copy Command");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(JBUI.Borders.empty(8));
        panel.setPreferredSize(new Dimension(480, 280));

        // Current spec label
        JBLabel currentLabel = new JBLabel("Current spec:  " + featureName);
        currentLabel.setFont(currentLabel.getFont().deriveFont(Font.BOLD));
        panel.add(currentLabel, BorderLayout.NORTH);

        // Center: description textarea
        JPanel centerPanel = new JPanel(new BorderLayout(0, 6));

        JBLabel descLabel = new JBLabel("What needs to change?");
        centerPanel.add(descLabel, BorderLayout.NORTH);

        descriptionArea = new JBTextArea(6, 40);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(JBUI.Borders.empty(4));
        JBScrollPane scroll = new JBScrollPane(descriptionArea);
        centerPanel.add(scroll, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // Bottom: impact assessment checkboxes
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(JBUI.Borders.emptyTop(4));

        JBLabel impactLabel = new JBLabel("Impact assessment");
        impactLabel.setFont(impactLabel.getFont().deriveFont(Font.BOLD));
        impactLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPanel.add(impactLabel);
        bottomPanel.add(Box.createVerticalStrut(4));

        assessImpactBox = new JBCheckBox("Assess impact on plan.md", true);
        assessImpactBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPanel.add(assessImpactBox);

        replanBox = new JBCheckBox("Automatically re-plan if steps are invalidated", false);
        replanBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPanel.add(replanBox);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    @Override
    protected void doOKAction() {
        String description = descriptionArea.getText().trim();
        if (description.isEmpty()) {
            descriptionArea.requestFocus();
            return;
        }

        String command = buildCommand(description);
        copyToClipboard(command);
        showNotification(command);
        super.doOKAction();
    }

    private String buildCommand(String description) {
        StringBuilder cmd = new StringBuilder("/sdd-refine ");
        cmd.append(description);
        if (assessImpactBox.isSelected()) {
            cmd.append("\n\nAlso assess the impact on plan.md.");
        }
        if (replanBox.isSelected()) {
            cmd.append(" Re-run /sdd-plan if any steps are invalidated.");
        }
        return cmd.toString();
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    private void showNotification(String command) {
        String shortCmd = command.length() > 60 ? command.substring(0, 57) + "…" : command;
        Notifications.Bus.notify(
                new Notification("SDD",
                        "Refinement command copied",
                        "Paste into your AI agent:  <tt>" + shortCmd + "</tt>",
                        NotificationType.INFORMATION),
                project
        );
    }
}
