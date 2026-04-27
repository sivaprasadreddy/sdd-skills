package dev.sivalabs.sdd.plugin.ui;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import dev.sivalabs.sdd.plugin.model.*;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PlanPanel extends JBPanel<PlanPanel> {

    private static final Color DONE_COLOR = PipelinePanel.DONE_COLOR;
    private static final Color ACTIVE_COLOR = PipelinePanel.ACTIVE_COLOR;
    private static final Color PENDING_COLOR = PipelinePanel.PENDING_COLOR;
    private static final Color LINK_COLOR = new JBColor(new Color(0x1565C0), new Color(0x42A5F5));

    private final Project project;
    private final JPanel stepsContainer;

    public PlanPanel(Project project, SddState state) {
        super(new BorderLayout());
        this.project = project;
        setBorder(JBUI.Borders.empty(8));

        JBLabel header = new JBLabel("Implementation Plan");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        header.setBorder(JBUI.Borders.emptyBottom(8));
        add(header, BorderLayout.NORTH);

        stepsContainer = new JPanel();
        stepsContainer.setOpaque(false);
        stepsContainer.setLayout(new BoxLayout(stepsContainer, BoxLayout.Y_AXIS));

        JBScrollPane scroll = new JBScrollPane(stepsContainer);
        scroll.setBorder(JBUI.Borders.empty());
        add(scroll, BorderLayout.CENTER);

        updateState(state);
    }

    public void updateState(SddState state) {
        stepsContainer.removeAll();

        if (state.getPlanSteps().isEmpty()) {
            JBLabel msg = new JBLabel("No implementation plan found");
            msg.setForeground(PENDING_COLOR);
            msg.setAlignmentX(LEFT_ALIGNMENT);
            stepsContainer.add(msg);
            stepsContainer.add(Box.createVerticalStrut(8));

            JButton generateBtn = new JButton("Generate Plan");
            generateBtn.setAlignmentX(LEFT_ALIGNMENT);
            generateBtn.addActionListener(e -> {
                StringSelection sel = new StringSelection("/sdd-plan");
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
                generateBtn.setText("Copied!");
                Timer timer = new Timer(2000, ev -> generateBtn.setText("Generate Plan"));
                timer.setRepeats(false);
                timer.start();
            });
            stepsContainer.add(generateBtn);
        } else {
            for (PlanStep step : state.getPlanSteps()) {
                stepsContainer.add(buildStepPanel(step));
                stepsContainer.add(Box.createVerticalStrut(2));
            }
        }

        stepsContainer.add(Box.createVerticalGlue());
        stepsContainer.revalidate();
        stepsContainer.repaint();
    }

    private JPanel buildStepPanel(PlanStep step) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        Border bottom = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0);
        panel.setBorder(BorderFactory.createCompoundBorder(bottom, JBUI.Borders.empty(4, 0, 6, 0)));

        // Step header
        JPanel headerRow = new JPanel(new BorderLayout(6, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        StepStatus status = step.getStatus();
        Color color = colorFor(status);

        JBLabel iconLabel = new JBLabel(iconFor(status));
        iconLabel.setForeground(color);
        iconLabel.setPreferredSize(new Dimension(18, 18));

        JBLabel titleLabel = new JBLabel("Step " + step.getNumber() + " — " + step.getTitle());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        if (status == StepStatus.IN_PROGRESS) titleLabel.setForeground(ACTIVE_COLOR);

        headerRow.add(iconLabel, BorderLayout.WEST);
        headerRow.add(titleLabel, BorderLayout.CENTER);
        panel.add(headerRow);

        // Sub-tasks
        for (PlanSubTask subTask : step.getSubTasks()) {
            panel.add(buildSubTaskRow(subTask));
        }

        return panel;
    }

    private JPanel buildSubTaskRow(PlanSubTask subTask) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setBorder(JBUI.Borders.emptyLeft(22));

        String checkIcon = subTask.isDone() ? "☑" : "☐";
        JBLabel checkLabel = new JBLabel(checkIcon);
        checkLabel.setForeground(subTask.isDone() ? DONE_COLOR : PENDING_COLOR);
        checkLabel.setPreferredSize(new Dimension(18, 18));
        row.add(checkLabel, BorderLayout.WEST);

        String filePath = subTask.getFilePath();
        if (filePath != null) {
            JLabel link = new JLabel("<html><a href=''>" + escapeHtml(subTask.getDescription()) + "</a></html>");
            link.setForeground(LINK_COLOR);
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openFile(filePath);
                }
            });
            row.add(link, BorderLayout.CENTER);
        } else {
            row.add(new JBLabel(subTask.getDescription()), BorderLayout.CENTER);
        }

        return row;
    }

    private void openFile(String relativePath) {
        String basePath = project.getBasePath();
        if (basePath == null) return;
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath + "/" + relativePath);
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, true);
        }
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static Color colorFor(StepStatus status) {
        return switch (status) {
            case DONE -> DONE_COLOR;
            case IN_PROGRESS -> ACTIVE_COLOR;
            default -> PENDING_COLOR;
        };
    }

    private static String iconFor(StepStatus status) {
        return switch (status) {
            case DONE -> "✓";
            case IN_PROGRESS -> "▶";
            default -> "○";
        };
    }
}
