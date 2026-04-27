package dev.sivalabs.sdd.plugin.model;

public class AcItem {
    private final String id;
    private final String description;
    private final boolean checked;

    public AcItem(String id, String description, boolean checked) {
        this.id = id;
        this.description = description;
        this.checked = checked;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public boolean isChecked() { return checked; }
}
