package dev.sivalabs.sdd.plugin.model;

import com.intellij.ui.JBColor;

import java.awt.*;

public enum ReviewVerdict {
    READY             ("✅", "Ready to merge",                 new JBColor(new Color(0x4CAF50), new Color(0x66BB6A))),
    MERGE_AFTER_MINOR ("🟡", "Merge after minor fixes",        new JBColor(new Color(0xF9A825), new Color(0xFFD54F))),
    REQUIRES_FIXES    ("🟠", "Requires fixes and re-review",   new JBColor(new Color(0xE65100), new Color(0xFFA726))),
    DO_NOT_MERGE      ("🔴", "Do not merge",                   new JBColor(new Color(0xD32F2F), new Color(0xEF5350)));

    private final String emoji;
    private final String label;
    private final Color color;

    ReviewVerdict(String emoji, String label, Color color) {
        this.emoji = emoji;
        this.label = label;
        this.color = color;
    }

    public String getEmoji() { return emoji; }
    public String getLabel() { return label; }
    public Color getColor()  { return color; }

    /** Try to detect verdict from a line containing "verdict:". */
    public static ReviewVerdict detect(String line) {
        String lower = line.toLowerCase();
        if (lower.contains("ready")              || line.contains("✅")) return READY;
        if (lower.contains("minor")              || line.contains("🟡")) return MERGE_AFTER_MINOR;
        if (lower.contains("requires")           || line.contains("🟠")) return REQUIRES_FIXES;
        if (lower.contains("do not merge")       || line.contains("🔴")) return DO_NOT_MERGE;
        return null;
    }
}
