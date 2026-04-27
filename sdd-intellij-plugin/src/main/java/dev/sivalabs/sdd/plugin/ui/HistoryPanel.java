package dev.sivalabs.sdd.plugin.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import dev.sivalabs.sdd.plugin.model.ArchivedFeature;
import dev.sivalabs.sdd.plugin.model.ProjectContext;
import dev.sivalabs.sdd.plugin.model.SddState;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class HistoryPanel extends JBPanel<HistoryPanel> {

    private static final Color MUTED      = new JBColor(new Color(0x888888), new Color(0xAAAAAA));
    private static final Color DONE_COLOR = PipelinePanel.DONE_COLOR;

    private final Project project;
    private final JPanel archiveListContainer = new JPanel();
    private final JPanel projectContextPanel  = new JPanel();
    private final SearchTextField searchField = new SearchTextField();

    private List<ArchivedFeature> allArchives = List.of();

    public HistoryPanel(Project project, SddState state) {
        super(new BorderLayout());
        this.project = project;
        setBorder(JBUI.Borders.empty(8));

        // ── top: title + search ───────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(JBUI.Borders.emptyBottom(6));

        JBLabel title = new JBLabel("Feature History");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        header.add(title, BorderLayout.WEST);

        searchField.setPreferredSize(new Dimension(180, searchField.getPreferredSize().height));
        searchField.getTextEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filterArchives(); }
            @Override public void removeUpdate(DocumentEvent e)  { filterArchives(); }
            @Override public void changedUpdate(DocumentEvent e) { filterArchives(); }
        });
        header.add(searchField, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── scrollable body ───────────────────────────────────────────
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        archiveListContainer.setOpaque(false);
        archiveListContainer.setLayout(new BoxLayout(archiveListContainer, BoxLayout.Y_AXIS));
        body.add(archiveListContainer);

        body.add(Box.createVerticalStrut(10));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        body.add(sep);
        body.add(Box.createVerticalStrut(8));

        projectContextPanel.setOpaque(false);
        projectContextPanel.setLayout(new BoxLayout(projectContextPanel, BoxLayout.Y_AXIS));
        body.add(projectContextPanel);

        body.add(Box.createVerticalGlue());

        add(new JBScrollPane(body) {{ setBorder(JBUI.Borders.empty()); }}, BorderLayout.CENTER);

        updateState(state);
    }

    public void updateState(SddState state) {
        allArchives = state.getArchivedFeatures();
        rebuildArchiveList(allArchives);
        rebuildProjectContext(state.getProjectContext());
        revalidate();
        repaint();
    }

    // ── archive list ──────────────────────────────────────────────────

    private void filterArchives() {
        String query = searchField.getText().strip().toLowerCase();
        if (query.isEmpty()) {
            rebuildArchiveList(allArchives);
        } else {
            List<ArchivedFeature> filtered = allArchives.stream()
                    .filter(f -> f.getFeatureName().toLowerCase().contains(query)
                            || f.getDate().contains(query))
                    .toList();
            rebuildArchiveList(filtered);
        }
    }

    private void rebuildArchiveList(List<ArchivedFeature> archives) {
        archiveListContainer.removeAll();
        if (archives.isEmpty()) {
            JBLabel empty = new JBLabel("No archived features yet.");
            empty.setForeground(MUTED);
            empty.setAlignmentX(LEFT_ALIGNMENT);
            archiveListContainer.add(empty);
        } else {
            for (ArchivedFeature af : archives) {
                archiveListContainer.add(buildArchiveEntry(af));
                archiveListContainer.add(Box.createVerticalStrut(6));
            }
        }
        archiveListContainer.revalidate();
        archiveListContainer.repaint();
    }

    private JPanel buildArchiveEntry(ArchivedFeature af) {
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setBorder(JBUI.Borders.customLine(JBColor.border(), 1));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.setBorder(JBUI.Borders.empty(4, 6, 2, 6));

        JBLabel dateDot = new JBLabel("● " + af.getDate());
        dateDot.setForeground(DONE_COLOR);
        dateDot.setFont(dateDot.getFont().deriveFont(Font.BOLD, 11f));
        top.add(dateDot, BorderLayout.WEST);

        JBLabel nameLabel = new JBLabel(af.getFeatureName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        top.add(nameLabel, BorderLayout.CENTER);
        card.add(top);

        // Sub-row: AC count + verdict
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        meta.setOpaque(false);
        meta.setBorder(JBUI.Borders.emptyLeft(6));

        String acText = af.getAcCount() > 0 ? af.getAcCount() + " ACs" : "—";
        JBLabel acLabel = new JBLabel(acText);
        acLabel.setForeground(MUTED);
        meta.add(acLabel);

        if (af.getReviewVerdict() != null) {
            JBLabel verdict = new JBLabel("Review: " + af.getReviewVerdict().getEmoji()
                    + " " + af.getReviewVerdict().getLabel());
            verdict.setForeground(af.getReviewVerdict().getColor());
            meta.add(new JBLabel("•") {{ setForeground(MUTED); }});
            meta.add(verdict);
        }
        card.add(meta);

        // Action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        actions.setOpaque(false);
        actions.setBorder(JBUI.Borders.emptyLeft(2));

        if (af.isHasSpec()) {
            JButton specBtn = new JButton("View Spec");
            specBtn.setFocusable(false);
            specBtn.addActionListener(e -> openFile(af.getSpecPath()));
            actions.add(specBtn);
        }
        if (af.isHasPlan()) {
            JButton planBtn = new JButton("View Plan");
            planBtn.setFocusable(false);
            planBtn.addActionListener(e -> openFile(af.getPlanPath()));
            actions.add(planBtn);
        }
        if (af.isHasReview()) {
            JButton reviewBtn = new JButton("View Review");
            reviewBtn.setFocusable(false);
            reviewBtn.addActionListener(e -> openFile(af.getReviewPath()));
            actions.add(reviewBtn);
        }
        card.add(actions);

        return card;
    }

    // ── project context ───────────────────────────────────────────────

    private void rebuildProjectContext(ProjectContext ctx) {
        projectContextPanel.removeAll();

        JBLabel sectionTitle = new JBLabel("Project Context  (docs/project.md)");
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 12f));
        sectionTitle.setAlignmentX(LEFT_ALIGNMENT);
        sectionTitle.setBorder(JBUI.Borders.emptyBottom(6));
        projectContextPanel.add(sectionTitle);

        if (ctx == null) {
            JBLabel missing = new JBLabel("docs/project.md not found — run /sdd-init to create it.");
            missing.setForeground(MUTED);
            missing.setAlignmentX(LEFT_ALIGNMENT);
            projectContextPanel.add(missing);
        } else {
            // Key-value summary pairs
            for (Map.Entry<String, String> entry : ctx.getSummaryPairs().entrySet()) {
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.setAlignmentX(LEFT_ALIGNMENT);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

                JBLabel key = new JBLabel(entry.getKey() + ":");
                key.setFont(key.getFont().deriveFont(Font.BOLD, 11f));
                key.setPreferredSize(new Dimension(130, 18));

                JBLabel val = new JBLabel(entry.getValue());
                row.add(key, BorderLayout.WEST);
                row.add(val, BorderLayout.CENTER);
                projectContextPanel.add(row);
            }

            if (!ctx.getSummaryPairs().isEmpty()) {
                projectContextPanel.add(Box.createVerticalStrut(6));
            }

            // Collapsible lists
            if (!ctx.getApiEndpoints().isEmpty()) {
                projectContextPanel.add(collapsibleSection(
                        "API Surface", ctx.getApiEndpoints().size() + " endpoints",
                        ctx.getApiEndpoints()));
                projectContextPanel.add(Box.createVerticalStrut(4));
            }
            if (!ctx.getArchitectureDecisions().isEmpty()) {
                projectContextPanel.add(collapsibleSection(
                        "Architecture Decisions", String.valueOf(ctx.getArchitectureDecisions().size()),
                        ctx.getArchitectureDecisions()));
                projectContextPanel.add(Box.createVerticalStrut(4));
            }
            if (!ctx.getApprovedDependencies().isEmpty()) {
                projectContextPanel.add(collapsibleSection(
                        "Approved Dependencies", String.valueOf(ctx.getApprovedDependencies().size()),
                        ctx.getApprovedDependencies()));
                projectContextPanel.add(Box.createVerticalStrut(4));
            }
        }

        // Edit project.md button
        projectContextPanel.add(Box.createVerticalStrut(8));
        JButton editBtn = new JButton("Edit project.md");
        editBtn.setFocusable(false);
        editBtn.setAlignmentX(LEFT_ALIGNMENT);
        editBtn.addActionListener(e -> openProjectMd());
        projectContextPanel.add(editBtn);

        projectContextPanel.revalidate();
        projectContextPanel.repaint();
    }

    private JPanel collapsibleSection(String label, String countHint, List<String> items) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setAlignmentX(LEFT_ALIGNMENT);

        JPanel headerRow = new JPanel(new BorderLayout(6, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        headerRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerRow.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));

        JBLabel toggleIcon = new JBLabel("▶");
        toggleIcon.setFont(toggleIcon.getFont().deriveFont(9f));
        toggleIcon.setPreferredSize(new Dimension(12, 16));

        JBLabel headerLabel = new JBLabel(label + "  (" + countHint + ")");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 11f));

        headerRow.add(toggleIcon, BorderLayout.WEST);
        headerRow.add(headerLabel, BorderLayout.CENTER);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(JBUI.Borders.emptyLeft(16));
        content.setVisible(false);

        for (String item : items) {
            JBLabel itemLabel = new JBLabel("• " + item);
            itemLabel.setForeground(MUTED);
            itemLabel.setAlignmentX(LEFT_ALIGNMENT);
            content.add(itemLabel);
        }

        headerRow.addMouseListener(new java.awt.event.MouseAdapter() {
            private boolean expanded = false;
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                expanded = !expanded;
                toggleIcon.setText(expanded ? "▼" : "▶");
                content.setVisible(expanded);
                wrapper.revalidate();
                wrapper.repaint();
            }
        });

        wrapper.add(headerRow);
        wrapper.add(content);
        return wrapper;
    }

    // ── file navigation ───────────────────────────────────────────────

    private void openFile(String absolutePath) {
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(absolutePath);
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, true);
        } else {
            Notifications.Bus.notify(new Notification("SDD", "File not found",
                    absolutePath, NotificationType.WARNING), project);
        }
    }

    private void openProjectMd() {
        String basePath = project.getBasePath();
        if (basePath == null) return;
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(basePath + "/docs/project.md");
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, true);
        } else {
            Notifications.Bus.notify(new Notification("SDD", "docs/project.md not found",
                    "Run /sdd-init to create it.", NotificationType.WARNING), project);
        }
    }

    private static String escapeHtml(String t) {
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
