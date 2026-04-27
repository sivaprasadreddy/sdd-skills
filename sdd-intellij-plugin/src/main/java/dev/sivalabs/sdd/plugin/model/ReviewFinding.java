package dev.sivalabs.sdd.plugin.model;

import java.util.Objects;

public class ReviewFinding {
    private final Severity severity;
    /** Relative path from project root, e.g. "src/main/.../AuthService.java". May be null. */
    private final String filePath;
    /** 1-based line number, or -1 if unknown. */
    private final int line;
    private final String title;
    private final String description;

    public ReviewFinding(Severity severity, String filePath, int line,
                         String title, String description) {
        this.severity = severity;
        this.filePath = filePath;
        this.line = line;
        this.title = title;
        this.description = description;
    }

    public Severity getSeverity()     { return severity; }
    public String   getFilePath()     { return filePath; }
    public int      getLine()         { return line; }
    public String   getTitle()        { return title; }
    public String   getDescription()  { return description; }

    /** Short label used in gutter tooltips: "FileName.java:84" or just the title. */
    public String getLocationLabel() {
        if (filePath == null) return title;
        String fileName = filePath.contains("/")
                ? filePath.substring(filePath.lastIndexOf('/') + 1)
                : filePath;
        return line > 0 ? fileName + ":" + line : fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReviewFinding r)) return false;
        return line == r.line
                && severity == r.severity
                && Objects.equals(filePath, r.filePath)
                && Objects.equals(title, r.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(severity, filePath, line, title);
    }
}
