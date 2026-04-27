package dev.sivalabs.sdd.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import dev.sivalabs.sdd.plugin.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class PipelinePanel extends JBPanel<PipelinePanel> {

    // Visible pipeline stages — Init and Archive are intentionally excluded.
    private static final WorkflowStage[] PIPELINE_STAGES = {
        WorkflowStage.ANALYSE, WorkflowStage.PLAN, WorkflowStage.IMPLEMENT, WorkflowStage.REVIEW
    };

    private static final String CARD_NO_INIT = "noInit";
    private static final String CARD_PIPELINE = "pipeline";

    static final Color DONE_COLOR    = new JBColor(new Color(0x4CAF50), new Color(0x4CAF50));
    static final Color ACTIVE_COLOR  = new JBColor(new Color(0x2196F3), new Color(0x42A5F5));
    static final Color PENDING_COLOR = new JBColor(new Color(0x9E9E9E), new Color(0x757575));

    private final CardLayout cardLayout;
    private final JPanel cardContainer;
    private final Consumer<WorkflowStage> stageClickListener;

    // Pipeline card components (initialised in buildPipelineCard)
    private JBLabel featureNameLabel;
    private StageProgressPanel stageProgressPanel;

    public PipelinePanel(Project project, SddState state, Consumer<WorkflowStage> stageClickListener) {
        super(new BorderLayout());
        this.stageClickListener = stageClickListener;
        setBorder(JBUI.Borders.empty(8));

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);

        cardContainer.add(buildNoInitCard(project), CARD_NO_INIT);
        cardContainer.add(buildPipelineCard(), CARD_PIPELINE);

        add(cardContainer, BorderLayout.CENTER);

        updateState(state);
    }

    // ── no-init card ─────────────────────────────────────────────────────────

    private JPanel buildNoInitCard(Project project) {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JBLabel label = new JBLabel("docs/project.md not found. Initialise SDD for this project first.");
        label.setForeground(PENDING_COLOR);
        label.setAlignmentX(CENTER_ALIGNMENT);

        JButton button = new JButton("Run SDD Init");
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.addActionListener(e -> {
            StringSelection sel = new StringSelection("/sdd-init");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            button.setText("Copied!");
            Timer timer = new Timer(2000, ev -> button.setText("Run SDD Init"));
            timer.setRepeats(false);
            timer.start();
        });

        inner.add(label);
        inner.add(Box.createVerticalStrut(12));
        inner.add(button);

        outer.add(inner);
        return outer;
    }

    // ── pipeline card ─────────────────────────────────────────────────────────

    private JPanel buildPipelineCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        featureNameLabel = new JBLabel("No active feature");
        featureNameLabel.setFont(featureNameLabel.getFont().deriveFont(Font.BOLD, 13f));
        featureNameLabel.setAlignmentX(LEFT_ALIGNMENT);
        top.add(featureNameLabel);
        top.add(Box.createVerticalStrut(10));

        stageProgressPanel = new StageProgressPanel(stageClickListener);
        stageProgressPanel.setAlignmentX(LEFT_ALIGNMENT);
        top.add(stageProgressPanel);

        panel.add(top, BorderLayout.NORTH);
        return panel;
    }

    // ── state updates ─────────────────────────────────────────────────────────

    public void updateState(SddState state) {
        if (state.getCurrentStage() == WorkflowStage.INIT) {
            cardLayout.show(cardContainer, CARD_NO_INIT);
        } else {
            cardLayout.show(cardContainer, CARD_PIPELINE);
            updatePipelineCard(state);
        }
        revalidate();
        repaint();
    }

    private void updatePipelineCard(SddState state) {
        if (state.getFeatureName() != null) {
            featureNameLabel.setText("Current Feature:  \"" + state.getFeatureName() + "\"");
        } else {
            featureNameLabel.setText("No active feature — run /sdd-analyse to begin");
        }
        stageProgressPanel.setCurrentStage(state.getCurrentStage());
    }

    // ── Stage progress dots ───────────────────────────────────────────────────

    private static class StageProgressPanel extends JPanel {

        private WorkflowStage currentStage = WorkflowStage.ANALYSE;
        private final Consumer<WorkflowStage> clickListener;

        StageProgressPanel(Consumer<WorkflowStage> clickListener) {
            this.clickListener = clickListener;
            setOpaque(false);
            Dimension d = new Dimension(400, 65);
            setMinimumSize(d);
            setPreferredSize(d);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int n = PIPELINE_STAGES.length;
                    int seg = getWidth() / (n + 1);
                    for (int i = 0; i < n; i++) {
                        int dotX = seg * (i + 1);
                        if (Math.abs(e.getX() - dotX) <= seg / 2) {
                            clickListener.accept(PIPELINE_STAGES[i]);
                            return;
                        }
                    }
                }
            });
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

            int n = PIPELINE_STAGES.length;
            int w = getWidth();
            int dotY    = 12;
            int labelY  = 32;
            int statusY = 52;
            int r   = 6;
            int seg = w / (n + 1);

            int[] dotX = new int[n];
            for (int i = 0; i < n; i++) dotX[i] = seg * (i + 1);

            Font baseFont = g2.getFont().deriveFont(10f);
            Font boldFont = baseFont.deriveFont(Font.BOLD, 9f);

            // connecting lines
            g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i < n - 1; i++) {
                boolean done = PIPELINE_STAGES[i].ordinal() < currentStage.ordinal();
                g2.setColor(done ? DONE_COLOR : PENDING_COLOR);
                g2.drawLine(dotX[i] + r, dotY, dotX[i + 1] - r, dotY);
            }

            // dots + labels
            for (int i = 0; i < n; i++) {
                int ord = PIPELINE_STAGES[i].ordinal();
                int cur = currentStage.ordinal();
                Color c = ord < cur ? DONE_COLOR : ord == cur ? ACTIVE_COLOR : PENDING_COLOR;

                g2.setColor(c);
                g2.fillOval(dotX[i] - r, dotY - r, r * 2, r * 2);

                g2.setFont(baseFont);
                FontMetrics fm = g2.getFontMetrics();
                String label = PIPELINE_STAGES[i].getDisplayName();
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
