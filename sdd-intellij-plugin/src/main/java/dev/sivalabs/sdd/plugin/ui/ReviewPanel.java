package dev.sivalabs.sdd.plugin.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import dev.sivalabs.sdd.plugin.model.*;
import dev.sivalabs.sdd.plugin.service.SddStateService;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ReviewPanel extends JBPanel<ReviewPanel> {

    private static final Color MUTED = new JBColor(new Color(0x888888), new Color(0xAAAAAA));
    private static final Color DONE_COLOR = PipelinePanel.DONE_COLOR;
    private static final Color PENDING_COLOR = PipelinePanel.PENDING_COLOR;

    private final Project project;
    private final JBLabel verdictLabel = new JBLabel();
    private final JBLabel lastRunLabel = new JBLabel();
    private final JPanel findingsContainer = new JPanel();
    private final JPanel acContainer = new JPanel();
    private JButton runBtn;

    public ReviewPanel(Project project, SddState state) {
        super(new BorderLayout());
        this.project = project;
        setBorder(JBUI.Borders.empty(8));

        // ── toolbar ──────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(JBUI.Borders.emptyBottom(6));

        JBLabel title = new JBLabel("Code Review");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        toolbar.add(title, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        runBtn = new JButton("Run Review");
        runBtn.setFocusable(false);
        runBtn.addActionListener(e -> triggerReview());
        JButton openReportBtn = new JButton("Open Report");
        openReportBtn.setFocusable(false);
        openReportBtn.addActionListener(e -> openReviewMd());
        JButton archiveBtn = new JButton("Archive");
        archiveBtn.setFocusable(false);
        archiveBtn.addActionListener(e -> {
            StringSelection sel = new StringSelection("/sdd-archive");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            archiveBtn.setText("Copied!");
            Timer timer = new Timer(2000, ev -> archiveBtn.setText("Archive"));
            timer.setRepeats(false);
            timer.start();
        });
        buttons.add(runBtn);
        buttons.add(openReportBtn);
        buttons.add(archiveBtn);
        toolbar.add(buttons, BorderLayout.EAST);

        // ── verdict + last-run rows ──────────────────────────────────
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(JBUI.Borders.emptyBottom(8));
        verdictLabel.setFont(verdictLabel.getFont().deriveFont(Font.BOLD, 12f));
        verdictLabel.setAlignmentX(LEFT_ALIGNMENT);
        lastRunLabel.setForeground(MUTED);
        lastRunLabel.setAlignmentX(LEFT_ALIGNMENT);
        infoPanel.add(verdictLabel);
        infoPanel.add(lastRunLabel);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        toolbar.setAlignmentX(LEFT_ALIGNMENT);
        infoPanel.setAlignmentX(LEFT_ALIGNMENT);
        header.add(toolbar);
        header.add(infoPanel);
        add(header, BorderLayout.NORTH);

        // ── scrollable body ──────────────────────────────────────────
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        findingsContainer.setOpaque(false);
        findingsContainer.setLayout(new BoxLayout(findingsContainer, BoxLayout.Y_AXIS));
        body.add(findingsContainer);

        body.add(Box.createVerticalStrut(8));
        body.add(new JSeparator() {{ setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); }});
        body.add(Box.createVerticalStrut(8));

        acContainer.setOpaque(false);
        acContainer.setLayout(new BoxLayout(acContainer, BoxLayout.Y_AXIS));
        body.add(acContainer);

        body.add(Box.createVerticalGlue());

        add(new JBScrollPane(body) {{ setBorder(JBUI.Borders.empty()); }}, BorderLayout.CENTER);

        updateState(state);
    }

    public void updateState(SddState state) {
        ReviewReport report = state.getReviewReport();
        if (report == null) {
            verdictLabel.setText("No review yet");
            verdictLabel.setForeground(MUTED);
            lastRunLabel.setText("Run /sdd-review to generate a report");
            rebuildFindings(null);
            rebuildAcCoverage(null);
        } else {
            verdictLabel.setText(report.getVerdict().getEmoji() + "  " + report.getVerdict().getLabel());
            verdictLabel.setForeground(report.getVerdict().getColor());
            lastRunLabel.setText(report.getLastRunTime() != null
                    ? "Last run: " + report.getLastRunTime()
                    : "");
            rebuildFindings(report);
            rebuildAcCoverage(report);
        }
        revalidate();
        repaint();
    }

    // ── findings section ──────────────────────────────────────────────

    private void rebuildFindings(ReviewReport report) {
        findingsContainer.removeAll();
        Runnable refresh = () -> { findingsContainer.revalidate(); findingsContainer.repaint(); };

        for (Severity severity : Severity.values()) {
            List<ReviewFinding> group = report != null
                    ? report.findingsBySeverity(severity)
                    : List.of();
            findingsContainer.add(severityGroup(severity, group, refresh));
            findingsContainer.add(Box.createVerticalStrut(4));
        }
        findingsContainer.revalidate();
        findingsContainer.repaint();
    }

    private JPanel severityGroup(Severity severity, List<ReviewFinding> findings, Runnable refresh) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setAlignmentX(LEFT_ALIGNMENT);

        // Group header line
        JPanel headerRow = new JPanel(new BorderLayout(6, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        headerRow.setBorder(JBUI.Borders.emptyBottom(2));

        JBLabel label = new JBLabel(severity.getEmoji() + "  " + severity.getLabel()
                + "  (" + findings.size() + ")");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setForeground(findings.isEmpty() ? MUTED : severity.getColor());
        headerRow.add(label, BorderLayout.WEST);

        Border bottom = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0);
        headerRow.setBorder(bottom);
        group.add(headerRow);
        group.add(Box.createVerticalStrut(3));

        if (findings.isEmpty()) {
            JBLabel none = new JBLabel("  (none)");
            none.setForeground(MUTED);
            none.setAlignmentX(LEFT_ALIGNMENT);
            group.add(none);
        } else {
            for (ReviewFinding finding : findings) {
                group.add(new FindingRow(finding, refresh));
            }
        }
        return group;
    }

    // ── AC coverage section ───────────────────────────────────────────

    private void rebuildAcCoverage(ReviewReport report) {
        acContainer.removeAll();
        if (report == null || report.getAcCoverage().isEmpty()) {
            acContainer.revalidate();
            acContainer.repaint();
            return;
        }

        JBLabel header = new JBLabel("Acceptance Criteria Coverage");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setBorder(JBUI.Borders.emptyBottom(4));
        acContainer.add(header);

        for (AcCoverage ac : report.getAcCoverage()) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

            String icon = ac.isCovered() ? "✓" : "✗";
            Color color = ac.isCovered() ? DONE_COLOR : PENDING_COLOR;
            JBLabel iconLabel = new JBLabel(icon);
            iconLabel.setForeground(color);
            iconLabel.setPreferredSize(new Dimension(16, 16));

            JBLabel textLabel = new JBLabel(ac.getAcId()
                    + (ac.getDescription().isBlank() ? "" : "  —  " + ac.getDescription()));
            if (!ac.isCovered()) textLabel.setForeground(MUTED);

            row.add(iconLabel, BorderLayout.WEST);
            row.add(textLabel, BorderLayout.CENTER);
            acContainer.add(row);
        }
        acContainer.revalidate();
        acContainer.repaint();
    }

    // ── actions ───────────────────────────────────────────────────────

    private void triggerReview() {
        StringSelection sel = new StringSelection("/sdd-review");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
        runBtn.setText("Copied!");
        Timer timer = new Timer(2000, ev -> runBtn.setText("Run Review"));
        timer.setRepeats(false);
        timer.start();
    }

    private void openReviewMd() {
        String base = project.getBasePath();
        if (base == null) return;
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(base + "/review.md");
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, true);
        } else {
            Notifications.Bus.notify(new Notification("SDD", "No review.md",
                    "Run /sdd-review to generate a review report.", NotificationType.WARNING), project);
        }
    }

    // ── FindingRow (expandable) ───────────────────────────────────────

    private class FindingRow extends JPanel {

        private boolean expanded = false;
        private final JLabel toggleIcon;
        private final JPanel detailPanel;

        FindingRow(ReviewFinding finding, Runnable onToggle) {
            setLayout(new BorderLayout());
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);

            // Summary row (always visible)
            JPanel summary = new JPanel(new BorderLayout(6, 0));
            summary.setOpaque(false);
            summary.setBorder(JBUI.Borders.empty(2, 2));
            summary.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            toggleIcon = new JBLabel("▶");
            toggleIcon.setFont(toggleIcon.getFont().deriveFont(9f));
            toggleIcon.setPreferredSize(new Dimension(12, 16));

            String location = finding.getLocationLabel();
            JBLabel titleLabel = new JBLabel(
                    "<html><b>" + escapeHtml(location) + "</b>  —  " + escapeHtml(finding.getTitle()) + "</html>");

            summary.add(toggleIcon, BorderLayout.WEST);
            summary.add(titleLabel, BorderLayout.CENTER);
            add(summary, BorderLayout.NORTH);

            // Detail panel (hidden by default)
            detailPanel = new JPanel();
            detailPanel.setOpaque(false);
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBorder(JBUI.Borders.emptyLeft(18));
            detailPanel.setVisible(false);

            if (!finding.getDescription().isBlank()) {
                JBLabel desc = new JBLabel("<html><body style='width:380px'>"
                        + escapeHtml(finding.getDescription()) + "</body></html>");
                desc.setForeground(MUTED);
                desc.setBorder(JBUI.Borders.emptyBottom(4));
                desc.setAlignmentX(LEFT_ALIGNMENT);
                detailPanel.add(desc);
            }

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            actions.setOpaque(false);
            actions.setAlignmentX(LEFT_ALIGNMENT);

            if (finding.getFilePath() != null) {
                JButton jumpBtn = new JButton("Jump to file");
                jumpBtn.setFocusable(false);
                jumpBtn.addActionListener(e -> jumpToFile(finding));
                actions.add(jumpBtn);
            }

            JButton fixBtn = new JButton("Ask AI to fix");
            fixBtn.setFocusable(false);
            fixBtn.addActionListener(e -> askAiToFix(finding));
            actions.add(fixBtn);
            detailPanel.add(actions);

            add(detailPanel, BorderLayout.CENTER);

            summary.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    expanded = !expanded;
                    toggleIcon.setText(expanded ? "▼" : "▶");
                    detailPanel.setVisible(expanded);
                    if (onToggle != null) onToggle.run();
                }
            });
        }

        private void jumpToFile(ReviewFinding finding) {
            String base = project.getBasePath();
            if (base == null) return;
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                    base + "/" + finding.getFilePath());
            if (vf != null) {
                int line = Math.max(0, finding.getLine() - 1);
                new OpenFileDescriptor(project, vf, line, 0).navigate(true);
            }
        }

        private void askAiToFix(ReviewFinding finding) {
            String cmd = "/sdd-review " + finding.getFilePath() + "\n\n"
                    + "Fix the following " + finding.getSeverity().getLabel() + " finding on line "
                    + finding.getLine() + ":\n" + finding.getTitle()
                    + (finding.getDescription().isBlank() ? "" : "\n\n" + finding.getDescription());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(cmd), null);
            Notifications.Bus.notify(new Notification("SDD", "Fix command copied",
                    "Paste into your AI agent to fix: <b>"
                            + escapeHtml(finding.getTitle()) + "</b>",
                    NotificationType.INFORMATION), project);
        }

        private String escapeHtml(String t) {
            return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
