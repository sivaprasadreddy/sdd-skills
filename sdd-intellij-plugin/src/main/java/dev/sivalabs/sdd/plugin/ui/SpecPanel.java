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
import dev.sivalabs.sdd.plugin.model.AcItem;
import dev.sivalabs.sdd.plugin.model.FeatureSpec;
import dev.sivalabs.sdd.plugin.model.RevisionEntry;
import dev.sivalabs.sdd.plugin.model.SddState;
import dev.sivalabs.sdd.plugin.service.SddStateService;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class SpecPanel extends JBPanel<SpecPanel> {

    private static final Color DONE_COLOR = PipelinePanel.DONE_COLOR;
    private static final Color PENDING_COLOR = PipelinePanel.PENDING_COLOR;
    private static final Color MUTED = new JBColor(new Color(0x888888), new Color(0xAAAAAA));
    private static final Color WARN_COLOR = new JBColor(new Color(0xE65100), new Color(0xFFB74D));

    private final Project project;

    // top area (rebuilt on state change)
    private final JBLabel titleLabel = new JBLabel();
    private final JBLabel versionLabel = new JBLabel();

    // scrollable body (rebuilt on state change)
    private final JPanel body = new JPanel();

    // toolbar buttons (visibility toggled by updateState)
    private JButton generateBtn;
    private JButton editRawBtn;
    private JButton refineBtn;

    public SpecPanel(Project project, SddState state) {
        super(new BorderLayout());
        this.project = project;
        setBorder(JBUI.Borders.empty(8));

        // ── toolbar ──────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(JBUI.Borders.emptyBottom(6));

        JPanel toolbarLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        toolbarLeft.setOpaque(false);
        generateBtn = new JButton("Generate…");
        generateBtn.setFocusable(false);
        generateBtn.addActionListener(e -> openAnalyseDialog());
        editRawBtn = new JButton("Edit Raw");
        editRawBtn.setFocusable(false);
        editRawBtn.addActionListener(e -> openRaw());
        refineBtn = new JButton("Refine…");
        refineBtn.setFocusable(false);
        refineBtn.addActionListener(e -> openRefineDialog());
        toolbarLeft.add(generateBtn);
        toolbarLeft.add(editRawBtn);
        toolbarLeft.add(refineBtn);
        toolbar.add(toolbarLeft, BorderLayout.WEST);

        // ── title row ─────────────────────────────────────────────────
        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        titleRow.setBorder(JBUI.Borders.emptyBottom(8));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        versionLabel.setForeground(MUTED);
        titleRow.add(titleLabel, BorderLayout.CENTER);
        titleRow.add(versionLabel, BorderLayout.EAST);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        toolbar.setAlignmentX(LEFT_ALIGNMENT);
        titleRow.setAlignmentX(LEFT_ALIGNMENT);
        header.add(toolbar);
        header.add(titleRow);

        add(header, BorderLayout.NORTH);

        // ── scrollable body ───────────────────────────────────────────
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JBScrollPane scroll = new JBScrollPane(body);
        scroll.setBorder(JBUI.Borders.empty());
        add(scroll, BorderLayout.CENTER);

        updateState(state);
    }

    public void updateState(SddState state) {
        FeatureSpec spec = state.getFeatureSpec();
        boolean hasSpec = spec != null && spec.getTitle() != null;

        generateBtn.setVisible(!hasSpec);
        editRawBtn.setVisible(hasSpec);
        refineBtn.setVisible(hasSpec);

        if (!hasSpec) {
            titleLabel.setText("No active feature");
            versionLabel.setText("");
            rebuildBody(null);
            return;
        }

        titleLabel.setText(spec.getTitle());
        String version = spec.getLatestVersion();
        String date = spec.getLatestDate();
        if (version != null) {
            versionLabel.setText(version + (date != null ? "  •  " + date : ""));
        } else {
            versionLabel.setText("");
        }

        rebuildBody(spec);
    }

    // ── body sections ─────────────────────────────────────────────────

    private void rebuildBody(FeatureSpec spec) {
        body.removeAll();

        if (spec == null) {
            JBLabel hint = new JBLabel(
                    "No feature spec yet — click 'Generate…' to create one.");
            hint.setForeground(PENDING_COLOR);
            hint.setAlignmentX(LEFT_ALIGNMENT);
            body.add(hint);
            body.add(Box.createVerticalGlue());
            body.revalidate();
            body.repaint();
            return;
        }

        Runnable refresh = () -> { body.revalidate(); body.repaint(); };

        // SUMMARY
        if (spec.getSummary() != null && !spec.getSummary().isBlank()) {
            body.add(new CollapsibleSection("Summary",
                    textContent(spec.getSummary()), false, refresh));
            body.add(spacer());
        }

        // ACCEPTANCE CRITERIA (always visible by default, with inline progress)
        body.add(acSection(spec, refresh));
        body.add(spacer());

        // LIST SECTIONS (collapsed by default)
        addListSection(body, "User Stories", spec.getUserStories(), refresh);
        addListSection(body, "Functional Requirements", spec.getFunctionalRequirements(), refresh);
        addListSection(body, "Out of Scope", spec.getOutOfScope(), refresh);
        addQuestionsSection(body, spec, refresh);

        // REVISION HISTORY (always visible, not collapsible)
        if (!spec.getRevisionHistory().isEmpty()) {
            body.add(separator());
            body.add(spacer());
            body.add(revisionHistoryPanel(spec.getRevisionHistory()));
        }

        body.add(Box.createVerticalGlue());
        body.revalidate();
        body.repaint();
    }

    // ── section builders ──────────────────────────────────────────────

    private CollapsibleSection acSection(FeatureSpec spec, Runnable refresh) {
        List<AcItem> acs = spec.getAcceptanceCriteria();

        // Progress bar shown in the section header
        JProgressBar bar = new JProgressBar(0, Math.max(acs.size(), 1));
        bar.setForeground(PipelinePanel.ACTIVE_COLOR);
        bar.setPreferredSize(new Dimension(80, 10));
        bar.setMaximumSize(new Dimension(80, 10));

        int checked = (int) acs.stream().filter(AcItem::isChecked).count();
        bar.setValue(checked);
        JBLabel countLabel = new JBLabel(checked + " / " + acs.size() + " ✓");
        countLabel.setForeground(MUTED);

        JPanel acContent = new JPanel();
        acContent.setOpaque(false);
        acContent.setLayout(new BoxLayout(acContent, BoxLayout.Y_AXIS));

        if (acs.isEmpty()) {
            JBLabel empty = new JBLabel("No acceptance criteria found in feature.md.");
            empty.setForeground(PENDING_COLOR);
            empty.setAlignmentX(LEFT_ALIGNMENT);
            acContent.add(empty);
        } else {
            for (AcItem ac : acs) {
                acContent.add(acRow(ac, bar, countLabel, acs.size()));
            }
        }

        return new CollapsibleSection("Acceptance Criteria", acContent, false, refresh,
                countLabel, bar);
    }

    private JPanel acRow(AcItem ac, JProgressBar bar, JBLabel countLabel, int total) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JCheckBox check = new JCheckBox();
        check.setSelected(ac.isChecked());
        check.setOpaque(false);
        check.addActionListener(e -> {
            SddStateService.getInstance(project).toggleAc(ac.getId(), check.isSelected());
            // Optimistically update the progress indicator
            int newChecked = check.isSelected()
                    ? bar.getValue() + 1
                    : Math.max(0, bar.getValue() - 1);
            bar.setValue(newChecked);
            countLabel.setText(newChecked + " / " + total + " ✓");
        });

        JBLabel label = new JBLabel(ac.getId() + "  " + ac.getDescription());
        label.setForeground(ac.isChecked() ? DONE_COLOR : null);

        row.add(check, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    private void addListSection(JPanel container, String title,
                                List<String> items, Runnable refresh) {
        if (items.isEmpty()) return;
        container.add(new CollapsibleSection(title, listContent(items), true, refresh));
        container.add(spacer());
    }

    private void addQuestionsSection(JPanel container, FeatureSpec spec, Runnable refresh) {
        List<String> questions = spec.getOpenQuestions();
        if (questions.isEmpty()) return;

        int count = spec.getUnresolvedQuestionCount();
        JBLabel badge = count > 0
                ? badgeLabel("⚠ " + count + " unresolved", WARN_COLOR)
                : null;

        CollapsibleSection section = badge != null
                ? new CollapsibleSection("Open Questions", listContent(questions), true, refresh, badge)
                : new CollapsibleSection("Open Questions", listContent(questions), true, refresh);
        container.add(section);
        container.add(spacer());
    }

    private JPanel revisionHistoryPanel(List<RevisionEntry> entries) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JBLabel header = new JBLabel("Revision History");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setBorder(JBUI.Borders.emptyBottom(4));
        panel.add(header);

        for (RevisionEntry entry : entries) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

            JBLabel ver = new JBLabel(entry.version());
            ver.setFont(ver.getFont().deriveFont(Font.BOLD));
            ver.setPreferredSize(new Dimension(30, 16));

            JBLabel date = new JBLabel(entry.date());
            date.setForeground(MUTED);
            date.setPreferredSize(new Dimension(80, 16));

            JBLabel desc = new JBLabel(entry.description());

            row.add(ver, BorderLayout.WEST);
            JPanel middle = new JPanel(new BorderLayout(6, 0));
            middle.setOpaque(false);
            middle.add(date, BorderLayout.WEST);
            middle.add(desc, BorderLayout.CENTER);
            row.add(middle, BorderLayout.CENTER);

            panel.add(row);
        }

        return panel;
    }

    // ── helpers ───────────────────────────────────────────────────────

    private JPanel textContent(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JBLabel label = new JBLabel("<html><body style='width:400px'>" +
                text.replace("\n", "<br>") + "</body></html>");
        label.setVerticalAlignment(SwingConstants.TOP);
        p.add(label, BorderLayout.NORTH);
        return p;
    }

    private JPanel listContent(List<String> items) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (String item : items) {
            JPanel row = new JPanel(new BorderLayout(4, 0));
            row.setOpaque(false);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            JBLabel bullet = new JBLabel("•");
            bullet.setForeground(MUTED);
            bullet.setPreferredSize(new Dimension(12, 16));
            row.add(bullet, BorderLayout.WEST);
            row.add(new JBLabel(item), BorderLayout.CENTER);
            p.add(row);
        }
        return p;
    }

    private JBLabel badgeLabel(String text, Color color) {
        JBLabel label = new JBLabel(text);
        label.setForeground(color);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        return label;
    }

    private Component spacer() {
        return Box.createVerticalStrut(6);
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private void openRaw() {
        String basePath = project.getBasePath();
        if (basePath == null) return;
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(basePath + "/feature.md");
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, true);
        }
    }

    private void openRefineDialog() {
        SddState state = SddStateService.getInstance(project).getState();
        new RefineDialog(project, state.getFeatureName()).show();
    }

    private void openAnalyseDialog() {
        new AnalyseDialog(project).show();
    }

    // ── CollapsibleSection (inner class) ──────────────────────────────

    static class CollapsibleSection extends JPanel {

        private boolean collapsed;
        private final JLabel toggleIcon;
        private final JPanel contentWrapper;

        CollapsibleSection(String title, JPanel content, boolean collapsed,
                           Runnable onToggle, JComponent... headerExtras) {
            setLayout(new BorderLayout());
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            this.collapsed = collapsed;

            // Header row
            JPanel header = new JPanel(new BorderLayout(6, 0));
            header.setOpaque(false);
            header.setBorder(JBUI.Borders.empty(2, 0));
            header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            toggleIcon = new JBLabel(collapsed ? "▶" : "▼");
            toggleIcon.setFont(toggleIcon.getFont().deriveFont(10f));
            toggleIcon.setPreferredSize(new Dimension(14, 16));

            JBLabel titleLabel = new JBLabel(title.toUpperCase());
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            left.setOpaque(false);
            left.add(toggleIcon);
            left.add(titleLabel);
            header.add(left, BorderLayout.WEST);

            if (headerExtras.length > 0) {
                JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
                right.setOpaque(false);
                for (JComponent extra : headerExtras) right.add(extra);
                header.add(right, BorderLayout.EAST);
            }

            // Content wrapper with left indent
            contentWrapper = new JPanel(new BorderLayout());
            contentWrapper.setOpaque(false);
            contentWrapper.setBorder(JBUI.Borders.emptyLeft(18));
            contentWrapper.add(content);
            contentWrapper.setVisible(!collapsed);

            add(header, BorderLayout.NORTH);
            add(contentWrapper, BorderLayout.CENTER);

            header.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    CollapsibleSection.this.collapsed = !CollapsibleSection.this.collapsed;
                    toggleIcon.setText(CollapsibleSection.this.collapsed ? "▶" : "▼");
                    contentWrapper.setVisible(!CollapsibleSection.this.collapsed);
                    if (onToggle != null) onToggle.run();
                }
            });
        }
    }
}
