package dev.sivalabs.sdd.plugin.model;

public class AcCoverage {
    private final String acId;
    private final String description;
    private final boolean covered;

    public AcCoverage(String acId, String description, boolean covered) {
        this.acId = acId;
        this.description = description;
        this.covered = covered;
    }

    public String  getAcId()       { return acId; }
    public String  getDescription(){ return description; }
    public boolean isCovered()     { return covered; }
}
