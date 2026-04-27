package dev.sivalabs.sdd.plugin.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class AnalyseDialog extends DialogWrapper {

    private final Project project;
    private JBTextArea featureArea;

    public AnalyseDialog(Project project) {
        super(project, true);
        this.project = project;
        setTitle("Generate Feature Spec");
        setOKButtonText("Copy Command");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(JBUI.Borders.empty(8));
        panel.setPreferredSize(new Dimension(480, 220));

        JBLabel descLabel = new JBLabel("Describe the feature you want to build:");
        panel.add(descLabel, BorderLayout.NORTH);

        featureArea = new JBTextArea(8, 40);
        featureArea.setLineWrap(true);
        featureArea.setWrapStyleWord(true);
        featureArea.setBorder(JBUI.Borders.empty(4));
        panel.add(new JBScrollPane(featureArea), BorderLayout.CENTER);

        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return featureArea;
    }

    @Override
    protected void doOKAction() {
        String description = featureArea.getText().trim();
        if (description.isEmpty()) {
            featureArea.requestFocus();
            return;
        }

        String command = "/sdd-analyse " + description;
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(command), null);

        String shortCmd = command.length() > 60 ? command.substring(0, 57) + "…" : command;
        Notifications.Bus.notify(
                new Notification("SDD",
                        "Analyse command copied",
                        "Paste into your AI agent:  <tt>" + shortCmd + "</tt>",
                        NotificationType.INFORMATION),
                project
        );

        super.doOKAction();
    }
}
