package dev.sivalabs.sdd.plugin.gutter;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import dev.sivalabs.sdd.plugin.model.ReviewFinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SddGutterIconRenderer extends GutterIconRenderer {

    private final ReviewFinding finding;

    public SddGutterIconRenderer(@NotNull ReviewFinding finding) {
        this.finding = finding;
    }

    public ReviewFinding getFinding() {
        return finding;
    }

    @Override
    public @NotNull Icon getIcon() {
        return createDotIcon(finding.getSeverity().getColor());
    }

    @Override
    public @Nullable String getTooltipText() {
        String title = finding.getSeverity().getLabel() + ": " + finding.getTitle();
        String desc = finding.getDescription();
        if (desc != null && !desc.isBlank()) {
            return "<html><b>" + escapeHtml(title) + "</b><br>"
                    + "<span style='color:gray'>" + escapeHtml(desc) + "</span></html>";
        }
        return "<html><b>" + escapeHtml(title) + "</b></html>";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SddGutterIconRenderer other)) return false;
        return finding.equals(other.finding);
    }

    @Override
    public int hashCode() {
        return finding.hashCode();
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static Icon createDotIcon(Color color) {
        int size = 12;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillOval(1, 1, size - 2, size - 2);
        g.dispose();
        return new ImageIcon(img);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
