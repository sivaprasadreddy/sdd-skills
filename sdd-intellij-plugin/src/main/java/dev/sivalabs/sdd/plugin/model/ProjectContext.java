package dev.sivalabs.sdd.plugin.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjectContext {
    private final Map<String, String> summaryPairs;
    private final List<String> apiEndpoints;
    private final List<String> architectureDecisions;
    private final List<String> approvedDependencies;
    private final String rawContent;

    public static ProjectContext empty() {
        return new ProjectContext(new LinkedHashMap<>(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), "");
    }

    public ProjectContext(Map<String, String> summaryPairs,
                          List<String> apiEndpoints,
                          List<String> architectureDecisions,
                          List<String> approvedDependencies,
                          String rawContent) {
        this.summaryPairs = summaryPairs;
        this.apiEndpoints = apiEndpoints;
        this.architectureDecisions = architectureDecisions;
        this.approvedDependencies = approvedDependencies;
        this.rawContent = rawContent;
    }

    public Map<String, String> getSummaryPairs()           { return summaryPairs; }
    public List<String>        getApiEndpoints()           { return apiEndpoints; }
    public List<String>        getArchitectureDecisions()  { return architectureDecisions; }
    public List<String>        getApprovedDependencies()   { return approvedDependencies; }
    public String              getRawContent()             { return rawContent; }
}
