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
import dev.sivalabs.sdd.plugin.model.SddState;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class ImplementPanel extends JBPanel<ImplementPanel> {

    private static final Color DONE_COLOR    = PipelinePanel.DONE_COLOR;
    private static final Color PENDING_COLOR = PipelinePanel.PENDING_COLOR;
    private static final Color MUTED = new JBColor(new Color(0x888888), new Color(0xAAAAAA));

    private static final String CARD_EMPTY   = "empty";
    private static final String CARD_SUMMARY = "summary";

    private final Project project;
    private final CardLayout cardLayout;
    private final JPanel cardContainer;
    private final JPanel summaryBody = new JPanel();

    public ImplementPanel(Project project, SddState state) {
        super(new BorderLayout());
        this.project = project;
        setBorder(JBUI.Borders.empty(8));

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);

        cardContainer.add(buildEmptyCard(), CARD_EMPTY);
        cardContainer.add(buildSummaryCard(), CARD_SUMMARY);

        add(cardContainer, BorderLayout.CENTER);
        updateState(state);
    }

    // ── empty state ───────────────────────────────────────────────────

    private JPanel buildEmptyCard() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JBLabel msg = new JBLabel("No implementation summary found");
        msg.setForeground(PENDING_COLOR);
        msg.setAlignmentX(CENTER_ALIGNMENT);

        JButton implementBtn = new JButton("Run SDD Implement");
        implementBtn.setAlignmentX(CENTER_ALIGNMENT);
        implementBtn.addActionListener(e ->
                copyCommand(implementBtn, "/sdd-implement", "Run SDD Implement"));

        JButton tddBtn = new JButton("Run SDD TDD Implement");
        tddBtn.setAlignmentX(CENTER_ALIGNMENT);
        tddBtn.addActionListener(e ->
                copyCommand(tddBtn, "/sdd-tdd-implement", "Run SDD TDD Implement"));

        inner.add(msg);
        inner.add(Box.createVerticalStrut(12));
        inner.add(implementBtn);
        inner.add(Box.createVerticalStrut(6));
        inner.add(tddBtn);

        outer.add(inner);
        return outer;
    }

    // ── summary card ──────────────────────────────────────────────────

    private JPanel buildSummaryCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(JBUI.Borders.emptyBottom(8));

        JBLabel title = new JBLabel("Implementation Summary");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        toolbar.add(title, BorderLayout.WEST);

        JButton openBtn = new JButton("Open File");
        openBtn.setFocusable(false);
        openBtn.addActionListener(e -> openImplSummaryMd());
        toolbar.add(openBtn, BorderLayout.EAST);

        card.add(toolbar, BorderLayout.NORTH);

        summaryBody.setOpaque(false);
        summaryBody.setLayout(new BoxLayout(summaryBody, BoxLayout.Y_AXIS));

        JBScrollPane scroll = new JBScrollPane(summaryBody);
        scroll.setBorder(JBUI.Borders.empty());
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ── state updates ─────────────────────────────────────────────────

    public void updateState(SddState state) {
        String summary = state.getImplSummary();
        if (summary == null || summary.isBlank()) {
            cardLayout.show(cardContainer, CARD_EMPTY);
        } else {
            rebuildSummaryBody(summary);
            cardLayout.show(cardContainer, CARD_SUMMARY);
        }
        revalidate();
        repaint();
    }

    private void rebuildSummaryBody(String content) {
        summaryBody.removeAll();

        for (ImplSection section : parseSections(content)) {
            summaryBody.add(buildSectionPanel(section));
            summaryBody.add(Box.createVerticalStrut(2));
        }

        summaryBody.add(Box.createVerticalGlue());
        summaryBody.revalidate();
        summaryBody.repaint();
    }

    // ── section / item renderers (Plan-tab style) ─────────────────────

    private JPanel buildSectionPanel(ImplSection section) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        Border bottom = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0);
        panel.setBorder(BorderFactory.createCompoundBorder(bottom, JBUI.Borders.empty(4, 0, 6, 0)));

        // Determine section status from checkbox items
        long total   = section.items.stream().filter(i -> i.kind == ImplItem.Kind.CHECKED || i.kind == ImplItem.Kind.UNCHECKED).count();
        long checked = section.items.stream().filter(i -> i.kind == ImplItem.Kind.CHECKED).count();
        boolean allDone = (total == 0) || (checked == total);
        Color iconColor  = allDone ? DONE_COLOR : PENDING_COLOR;
        String iconText  = allDone ? "✓" : "○";

        JPanel headerRow = new JPanel(new BorderLayout(6, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JBLabel iconLabel = new JBLabel(iconText);
        iconLabel.setForeground(iconColor);
        iconLabel.setPreferredSize(new Dimension(18, 18));

        JBLabel titleLabel = new JBLabel(section.title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        headerRow.add(iconLabel, BorderLayout.WEST);
        headerRow.add(titleLabel, BorderLayout.CENTER);
        panel.add(headerRow);

        for (ImplItem item : section.items) {
            panel.add(buildItemRow(item));
        }

        return panel;
    }

    private JPanel buildItemRow(ImplItem item) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(JBUI.Borders.emptyLeft(22));

        if (item.kind == ImplItem.Kind.TEXT) {
            JBLabel label = new JBLabel(
                    "<html><body style='width:380px'>" + escapeHtml(item.text) + "</body></html>");
            label.setForeground(MUTED);
            row.add(label, BorderLayout.CENTER);
            return row;
        }

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        String iconText;
        Color  iconColor;
        Color  textColor = null;
        switch (item.kind) {
            case CHECKED   -> { iconText = "☑"; iconColor = DONE_COLOR;    textColor = DONE_COLOR; }
            case UNCHECKED -> { iconText = "☐"; iconColor = PENDING_COLOR; textColor = PENDING_COLOR; }
            default        -> { iconText = "•"; iconColor = MUTED; }
        }

        JBLabel iconLabel = new JBLabel(iconText);
        iconLabel.setForeground(iconColor);
        iconLabel.setPreferredSize(new Dimension(18, 18));

        JBLabel textLabel = new JBLabel(item.text);
        if (textColor != null) textLabel.setForeground(textColor);

        row.add(iconLabel, BorderLayout.WEST);
        row.add(textLabel, BorderLayout.CENTER);
        return row;
    }

    // ── parsing ───────────────────────────────────────────────────────

    private static List<ImplSection> parseSections(String content) {
        List<ImplSection> sections = new ArrayList<>();
        ImplSection current = null;

        for (String rawLine : content.split("\n")) {
            String t = rawLine.trim();
            if (t.startsWith("## ") || t.startsWith("### ")) {
                current = new ImplSection(t.replaceFirst("^#+\\s+", "").trim());
                sections.add(current);
            } else if (current != null) {
                if (t.startsWith("- [x]") || t.startsWith("- [X]")) {
                    current.items.add(new ImplItem(ImplItem.Kind.CHECKED,
                            t.replaceFirst("^-\\s+\\[[xX]\\]\\s*", "").trim()));
                } else if (t.startsWith("- [ ]")) {
                    current.items.add(new ImplItem(ImplItem.Kind.UNCHECKED,
                            t.replaceFirst("^-\\s+\\[\\s\\]\\s*", "").trim()));
                } else if (t.startsWith("- ")) {
                    current.items.add(new ImplItem(ImplItem.Kind.BULLET,
                            t.replaceFirst("^-\\s+", "").trim()));
                } else if (!t.isBlank()) {
                    current.items.add(new ImplItem(ImplItem.Kind.TEXT, t));
                }
            }
        }
        return sections;
    }

    // ── helpers ───────────────────────────────────────────────────────

    private static String escapeHtml(String t) {
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void copyCommand(JButton btn, String command, String originalLabel) {
        StringSelection sel = new StringSelection(command);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
        btn.setText("Copied!");
        Timer timer = new Timer(2000, e -> btn.setText(originalLabel));
        timer.setRepeats(false);
        timer.start();
    }

    private void openImplSummaryMd() {
        String basePath = project.getBasePath();
        if (basePath == null) return;
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(basePath + "/impl-summary.md");
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, true);
        }
    }

    // ── model ─────────────────────────────────────────────────────────

    private static class ImplSection {
        final String title;
        final List<ImplItem> items = new ArrayList<>();
        ImplSection(String title) { this.title = title; }
    }

    private static class ImplItem {
        enum Kind { CHECKED, UNCHECKED, BULLET, TEXT }
        final Kind   kind;
        final String text;
        ImplItem(Kind kind, String text) { this.kind = kind; this.text = text; }
    }
}
