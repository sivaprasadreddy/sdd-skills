package dev.sivalabs.sdd.plugin.model;

import com.intellij.ui.JBColor;

import java.awt.*;

public enum Severity {
    CRITICAL(4, "🔴", "CRITICAL", new JBColor(new Color(0xD32F2F), new Color(0xEF5350))),
    MAJOR   (3, "🟠", "MAJOR",    new JBColor(new Color(0xE65100), new Color(0xFFA726))),
    MINOR   (2, "🟡", "MINOR",    new JBColor(new Color(0xF9A825), new Color(0xFFD54F))),
    INFO    (1, "🔵", "INFO",     new JBColor(new Color(0x1565C0), new Color(0x42A5F5)));

    private final int level;
    private final String emoji;
    private final String label;
    private final Color color;

    Severity(int level, String emoji, String label, Color color) {
        this.level = level;
        this.emoji = emoji;
        this.label = label;
        this.color = color;
    }

    public int getLevel()    { return level; }
    public String getEmoji() { return emoji; }
    public String getLabel() { return label; }
    public Color getColor()  { return color; }

    /** Try to detect severity from a heading line; null if not recognized. */
    public static Severity detect(String line) {
        String upper = line.toUpperCase();
        if (upper.contains("CRITICAL") || line.contains("🔴")) return CRITICAL;
        if (upper.contains("MAJOR")    || line.contains("🟠")) return MAJOR;
        if (upper.contains("MINOR")    || line.contains("🟡")) return MINOR;
        if (upper.contains(" INFO")    || line.contains("🔵")) return INFO;
        return null;
    }
}
