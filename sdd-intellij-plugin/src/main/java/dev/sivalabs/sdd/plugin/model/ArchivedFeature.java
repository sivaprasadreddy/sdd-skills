package dev.sivalabs.sdd.plugin.model;

public class ArchivedFeature {
    private final String directoryPath;
    private final String featureName;
    private final String date;
    private final int acCount;
    private final ReviewVerdict reviewVerdict;
    private final boolean hasSpec;
    private final boolean hasPlan;
    private final boolean hasReview;

    public ArchivedFeature(String directoryPath, String featureName, String date,
                           int acCount, ReviewVerdict reviewVerdict,
                           boolean hasSpec, boolean hasPlan, boolean hasReview) {
        this.directoryPath = directoryPath;
        this.featureName = featureName;
        this.date = date;
        this.acCount = acCount;
        this.reviewVerdict = reviewVerdict;
        this.hasSpec = hasSpec;
        this.hasPlan = hasPlan;
        this.hasReview = hasReview;
    }

    public String getDirectoryPath() { return directoryPath; }
    public String getFeatureName()   { return featureName; }
    public String getDate()          { return date; }
    public int    getAcCount()       { return acCount; }
    public ReviewVerdict getReviewVerdict() { return reviewVerdict; }
    public boolean isHasSpec()       { return hasSpec; }
    public boolean isHasPlan()       { return hasPlan; }
    public boolean isHasReview()     { return hasReview; }

    public String getSpecPath()   { return directoryPath + "/feature.md"; }
    public String getPlanPath()   { return directoryPath + "/plan.md"; }
    public String getReviewPath() { return directoryPath + "/review.md"; }
}
