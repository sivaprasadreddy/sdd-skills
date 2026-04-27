package dev.sivalabs.sdd.plugin.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ReviewReport {
    private final ReviewVerdict verdict;
    private final String lastRunTime;
    private final List<ReviewFinding> findings;
    private final List<AcCoverage> acCoverage;

    public ReviewReport(ReviewVerdict verdict, String lastRunTime,
                        List<ReviewFinding> findings, List<AcCoverage> acCoverage) {
        this.verdict = verdict;
        this.lastRunTime = lastRunTime;
        this.findings = Collections.unmodifiableList(findings);
        this.acCoverage = Collections.unmodifiableList(acCoverage);
    }

    public ReviewVerdict      getVerdict()     { return verdict; }
    public String             getLastRunTime() { return lastRunTime; }
    public List<ReviewFinding> getFindings()   { return findings; }
    public List<AcCoverage>   getAcCoverage()  { return acCoverage; }

    public List<ReviewFinding> findingsBySeverity(Severity severity) {
        return findings.stream()
                .filter(f -> f.getSeverity() == severity)
                .collect(Collectors.toList());
    }
}
