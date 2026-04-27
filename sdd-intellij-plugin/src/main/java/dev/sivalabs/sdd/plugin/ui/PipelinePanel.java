package dev.sivalabs.sdd.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import dev.sivalabs.sdd.plugin.model.*;

import javax.swing.*;
import java.awt.*;

public class PipelinePanel extends JBPanel<PipelinePanel> {

    private static final WorkflowStage[] STAGES = WorkflowStage.values();
    static final Color DONE_COLOR = new JBColor(new Color(0x4CAF50), new Color(0x4CAF50));
    static final Color ACTIVE_COLOR = new JBColor(new Color(0x2196F3), new Color(0x42A5F5));
    static final Color PENDING_COLOR = new JBColor(new Color(0x9E9E9E), new Color(0x757575));

    private final JBLabel featureNameLabel;
    private final StageProgressPanel stageProgressPanel;
    private final JProgressBar acProgressBar;
    private final JBLabel acProgressLabel;
    private final JPanel stepsPanel;

    public PipelinePanel(Project project, SddState state) {
        super(new BorderLayout());
        setBorder(JBUI.Borders.empty(8));

        // ── top section ──────────────────────────────────────────────
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        featureNameLabel = new JBLabel("No active feature");
        featureNameLabel.setFont(featureNameLabel.getFont().deriveFont(Font.BOLD, 13f));
        featureNameLabel.setAlignmentX(LEFT_ALIGNMENT);
        top.add(featureNameLabel);
        top.add(Box.createVerticalStrut(10));

        stageProgressPanel = new StageProgressPanel();
        stageProgressPanel.setAlignmentX(LEFT_ALIGNMENT);
        top.add(stageProgressPanel);
        top.add(Box.createVerticalStrut(10));

        // AC progress bar row
        JPanel acRow = new JPanel(new BorderLayout(6, 0));
        acRow.setOpaque(false);
        acRow.setAlignmentX(LEFT_ALIGNMENT);
        acRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JBLabel acHeader = new JBLabel("Progress  ");
        acProgressBar = new JProgressBar(0, 1);
        acProgressBar.setForeground(ACTIVE_COLOR);
        acProgressLabel = new JBLabel("0 / 0 ACs");
        acRow.add(acHeader, BorderLayout.WEST);
        acRow.add(acProgressBar, BorderLayout.CENTER);
        acRow.add(acProgressLabel, BorderLayout.EAST);
        top.add(acRow);
        top.add(Box.createVerticalStrut(10));

        JBLabel stepsHeader = new JBLabel("Plan Steps");
        stepsHeader.setFont(stepsHeader.getFont().deriveFont(Font.BOLD, 12f));
        stepsHeader.setAlignmentX(LEFT_ALIGNMENT);
        top.add(stepsHeader);
        top.add(Box.createVerticalStrut(4));

        add(top, BorderLayout.NORTH);

        // ── scrollable steps ─────────────────────────────────────────
        stepsPanel = new JPanel();
        stepsPanel.setOpaque(false);
        stepsPanel.setLayout(new BoxLayout(stepsPanel, BoxLayout.Y_AXIS));
        JBScrollPane scroll = new JBScrollPane(stepsPanel);
        scroll.setBorder(JBUI.Borders.empty());
        add(scroll, BorderLayout.CENTER);

        updateState(state);
    }

    public void updateState(SddState state) {
        if (state.getFeatureName() != null) {
            featureNameLabel.setText("Current Feature:  \"" + state.getFeatureName() + "\"");
        } else {
            featureNameLabel.setText("No active feature — run /sdd-init to begin");
        }

        stageProgressPanel.setCurrentStage(state.getCurrentStage());

        int total = state.getTotalAcCount();
        int checked = (int) state.getCheckedAcCount();
        if (total > 0) {
            acProgressBar.setMaximum(total);
            acProgressBar.setValue(checked);
            acProgressLabel.setText(checked + " / " + total + " ACs passing");
        } else {
            acProgressBar.setMaximum(1);
            acProgressBar.setValue(0);
            acProgressLabel.setText("—");
        }

        rebuildSteps(state);
        revalidate();
        repaint();
    }

    private void rebuildSteps(SddState state) {
        stepsPanel.removeAll();

        if (state.getPlanSteps().isEmpty()) {
            JBLabel hint = new JBLabel("No plan steps yet — run /sdd-plan to generate the plan");
            hint.setForeground(PENDING_COLOR);
            hint.setBorder(JBUI.Borders.empty(4, 0));
            hint.setAlignmentX(LEFT_ALIGNMENT);
            stepsPanel.add(hint);
        } else {
            for (PlanStep step : state.getPlanSteps()) {
                stepsPanel.add(stepRow(step));
            }
        }
        stepsPanel.add(Box.createVerticalGlue());
        stepsPanel.revalidate();
        stepsPanel.repaint();
    }

    private JPanel stepRow(PlanStep step) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setBorder(JBUI.Borders.empty(2, 0));

        StepStatus status = step.getStatus();
        Color color = colorFor(status);
        String icon = iconFor(status);

        JBLabel iconLabel = new JBLabel(icon);
        iconLabel.setForeground(color);
        iconLabel.setPreferredSize(new Dimension(18, 18));

        JBLabel title = new JBLabel(step.getNumber() + ".  " + step.getTitle());
        if (status == StepStatus.IN_PROGRESS) {
            title.setFont(title.getFont().deriveFont(Font.BOLD));
            title.setForeground(ACTIVE_COLOR);
        }

        String statusText = switch (status) {
            case DONE -> "Done";
            case IN_PROGRESS -> "In Progress";
            default -> "Pending";
        };
        JBLabel statusLabel = new JBLabel(statusText);
        statusLabel.setForeground(color);
        statusLabel.setPreferredSize(new Dimension(90, 18));

        row.add(iconLabel, BorderLayout.WEST);
        row.add(title, BorderLayout.CENTER);
        row.add(statusLabel, BorderLayout.EAST);
        return row;
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

    // ── Stage progress dots ───────────────────────────────────────────

    private static class StageProgressPanel extends JPanel {

        private WorkflowStage currentStage = WorkflowStage.INIT;

        StageProgressPanel() {
            setOpaque(false);
            Dimension d = new Dimension(400, 65);
            setMinimumSize(d);
            setPreferredSize(d);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        }

        void setCurrentStage(WorkflowStage stage) {
            this.currentStage = stage;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int n = STAGES.length;
            int w = getWidth();
            int dotY = 12;
            int labelY = 32;
            int statusY = 52;
            int r = 6;
            int seg = w / (n + 1);

            int[] dotX = new int[n];
            for (int i = 0; i < n; i++) dotX[i] = seg * (i + 1);

            Font baseFont = g2.getFont().deriveFont(10f);
            Font boldFont = baseFont.deriveFont(Font.BOLD, 9f);

            // connecting lines
            g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i < n - 1; i++) {
                boolean done = STAGES[i].ordinal() < currentStage.ordinal();
                g2.setColor(done ? DONE_COLOR : PENDING_COLOR);
                g2.drawLine(dotX[i] + r, dotY, dotX[i + 1] - r, dotY);
            }

            // dots + labels
            for (int i = 0; i < n; i++) {
                int ord = STAGES[i].ordinal();
                int cur = currentStage.ordinal();
                Color c = ord < cur ? DONE_COLOR : ord == cur ? ACTIVE_COLOR : PENDING_COLOR;

                g2.setColor(c);
                g2.fillOval(dotX[i] - r, dotY - r, r * 2, r * 2);

                g2.setFont(baseFont);
                FontMetrics fm = g2.getFontMetrics();
                String label = STAGES[i].getDisplayName();
                g2.drawString(label, dotX[i] - fm.stringWidth(label) / 2, labelY);

                if (ord < cur) {
                    g2.setFont(baseFont);
                    fm = g2.getFontMetrics();
                    g2.setColor(DONE_COLOR);
                    g2.drawString("✓", dotX[i] - fm.stringWidth("✓") / 2, statusY);
                } else if (ord == cur) {
                    g2.setFont(boldFont);
                    fm = g2.getFontMetrics();
                    g2.setColor(ACTIVE_COLOR);
                    String active = "IN PROGRESS";
                    g2.drawString(active, dotX[i] - fm.stringWidth(active) / 2, statusY);
                }
            }

            g2.dispose();
        }
    }
}
