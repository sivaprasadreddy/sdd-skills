package dev.sivalabs.sdd.plugin.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import dev.sivalabs.sdd.plugin.model.*;
import dev.sivalabs.sdd.plugin.parser.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT)
public final class SddStateService {

    private final Project project;
    private volatile SddState currentState;
    private final FeatureMdParser featureParser   = new FeatureMdParser();
    private final PlanMdParser planParser         = new PlanMdParser();
    private final ReviewReportParser reviewParser = new ReviewReportParser();
    private final ProjectMdParser projectParser   = new ProjectMdParser();

    public SddStateService(@NotNull Project project) {
        this.project = project;
        this.currentState = loadState();
        setupFileWatcher();
    }

    public static SddStateService getInstance(@NotNull Project project) {
        return project.getService(SddStateService.class);
    }

    public SddState getState() { return currentState; }

    public void refresh() {
        currentState = loadState();
        publishStateChange();
    }

    public void toggleAc(String acId, boolean checked) {
        String basePath = project.getBasePath();
        if (basePath == null) return;

        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(basePath + "/feature.md");
        if (vf == null) return;

        try {
            String content = new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
            String updated = toggleAcLine(content, acId, checked);
            if (updated.equals(content)) return;

            WriteCommandAction.runWriteCommandAction(project, "Toggle AC " + acId, null, () -> {
                try {
                    vf.setBinaryContent(updated.getBytes(StandardCharsets.UTF_8));
                } catch (IOException ex) {
                    // file write failure — state will stay stale until next refresh
                }
            });
        } catch (IOException e) {
            // ignore read failure
        }
    }

    // ── internals ──────────────────────────────────────────────────

    private void setupFileWatcher() {
        project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                boolean relevant = events.stream()
                        .anyMatch(e -> {
                            String path = e.getPath();
                            return path.endsWith("/feature.md")
                                    || path.endsWith("/plan.md")
                                    || path.endsWith("/impl-summary.md")
                                    || path.endsWith("/review.md")
                                    || path.endsWith("/project.md")
                                    || path.contains("/specs-archive/");
                        });
                if (relevant) {
                    ApplicationManager.getApplication().executeOnPooledThread(SddStateService.this::refresh);
                }
            }
        });
    }

    private SddState loadState() {
        String basePath = project.getBasePath();
        if (basePath == null) return SddState.empty();

        VirtualFile projectMd = LocalFileSystem.getInstance().findFileByPath(basePath + "/docs/project.md");
        if (projectMd == null || !projectMd.exists()) {
            return SddState.empty();
        }
        String projectMdContent = readFile(projectMd);
        if (projectMdContent.isBlank()) {
            return SddState.empty();
        }

        ProjectContext projectContext = projectParser.parse(projectMdContent);
        List<ArchivedFeature> archives = loadArchivedFeatures(basePath);

        VirtualFile featureMd     = LocalFileSystem.getInstance().findFileByPath(basePath + "/feature.md");
        VirtualFile planMd        = LocalFileSystem.getInstance().findFileByPath(basePath + "/plan.md");
        VirtualFile implSummaryMd = LocalFileSystem.getInstance().findFileByPath(basePath + "/impl-summary.md");
        VirtualFile reviewMd      = LocalFileSystem.getInstance().findFileByPath(basePath + "/review.md");

        boolean hasFeature     = featureMd     != null && featureMd.exists();
        boolean hasPlan        = planMd        != null && planMd.exists();
        boolean hasImplSummary = implSummaryMd != null && implSummaryMd.exists();
        boolean hasReview      = reviewMd      != null && reviewMd.exists();

        WorkflowStage stage = determineStage(hasFeature, hasPlan, hasImplSummary, hasReview);

        FeatureSpec featureSpec   = hasFeature     ? featureParser.parse(readFile(featureMd))  : null;
        List<PlanStep> planSteps  = hasPlan        ? planParser.parse(readFile(planMd))        : Collections.emptyList();
        String implSummary        = hasImplSummary ? readFile(implSummaryMd)                   : null;
        ReviewReport reviewReport = hasReview      ? reviewParser.parse(readFile(reviewMd))    : null;

        return new SddState(featureSpec, stage, planSteps, reviewReport, projectContext, implSummary, archives);
    }

    // Stage is the first step whose output file does not yet exist.
    // ARCHIVE is used as a sentinel when all four output files exist (all stages done).
    private WorkflowStage determineStage(boolean hasFeature, boolean hasPlan,
                                         boolean hasImplSummary, boolean hasReview) {
        if (!hasFeature)     return WorkflowStage.ANALYSE;
        if (!hasPlan)        return WorkflowStage.PLAN;
        if (!hasImplSummary) return WorkflowStage.IMPLEMENT;
        if (!hasReview)      return WorkflowStage.REVIEW;
        return WorkflowStage.ARCHIVE; // all output files present — every pipeline stage is done
    }

    private List<ArchivedFeature> loadArchivedFeatures(String basePath) {
        VirtualFile archiveRoot = LocalFileSystem.getInstance()
                .findFileByPath(basePath + "/docs/specs-archive");
        if (archiveRoot == null || !archiveRoot.isDirectory()) return Collections.emptyList();

        List<ArchivedFeature> result = new ArrayList<>();
        for (VirtualFile dir : archiveRoot.getChildren()) {
            if (!dir.isDirectory()) continue;
            ArchivedFeature af = parseArchiveEntry(dir);
            if (af != null) result.add(af);
        }
        // newest first: sort by date string descending (yyyymmddHHMM prefix)
        result.sort(Comparator.comparing(f -> f.getDirectoryPath(), Comparator.reverseOrder()));
        return Collections.unmodifiableList(result);
    }

    private static final Pattern ARCHIVE_DIR = Pattern.compile("^(\\d{12})-(.+)$");

    private ArchivedFeature parseArchiveEntry(VirtualFile dir) {
        String name = dir.getName();
        Matcher m = ARCHIVE_DIR.matcher(name);
        String date;
        String featureName;
        if (m.matches()) {
            date = formatArchiveDate(m.group(1));
            featureName = slugToTitle(m.group(2));
        } else {
            date = "";
            featureName = name;
        }

        VirtualFile specFile   = dir.findChild("feature.md");
        VirtualFile planFile   = dir.findChild("plan.md");
        VirtualFile reviewFile = dir.findChild("review.md");

        boolean hasSpec   = specFile   != null && specFile.exists();
        boolean hasPlan   = planFile   != null && planFile.exists();
        boolean hasReview = reviewFile != null && reviewFile.exists();

        // Use title from feature.md if available
        if (hasSpec) {
            String specContent = readVirtualFile(specFile);
            for (String line : specContent.split("\n")) {
                String stripped = line.strip();
                if (stripped.startsWith("# ")) {
                    featureName = stripped.substring(2).strip();
                    break;
                }
            }
        }

        int acCount = hasSpec ? countAcs(readVirtualFile(specFile)) : 0;

        ReviewVerdict verdict = null;
        if (hasReview) {
            ReviewReportParser rp = new ReviewReportParser();
            ReviewReport report = rp.parse(readVirtualFile(reviewFile));
            verdict = report != null ? report.getVerdict() : null;
        }

        return new ArchivedFeature(dir.getPath(), featureName, date,
                acCount, verdict, hasSpec, hasPlan, hasReview);
    }

    private static String formatArchiveDate(String stamp) {
        // stamp = yyyymmddHHMM, e.g. "202604271432"
        if (stamp.length() < 8) return stamp;
        return stamp.substring(0, 4) + "-" + stamp.substring(4, 6) + "-" + stamp.substring(6, 8);
    }

    private static String slugToTitle(String slug) {
        String[] words = slug.split("[-_]");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1));
        }
        return sb.toString();
    }

    private static int countAcs(String content) {
        int count = 0;
        for (String line : content.split("\n")) {
            if (line.strip().matches("^-\\s+\\[[ xX]]\\s+\\*{0,2}AC-\\d+.*")) count++;
        }
        return count;
    }

    private String readFile(VirtualFile vf) {
        try {
            return new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private String readVirtualFile(VirtualFile vf) {
        return readFile(vf);
    }

    private String toggleAcLine(String content, String acId, boolean checked) {
        Pattern p = Pattern.compile(
                "(?m)^(- \\[)[ xX](\\] \\*{0,2}" + Pattern.quote(acId) + "\\*{0,2})"
        );
        Matcher m = p.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1) + (checked ? 'x' : ' ') + m.group(2));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void publishStateChange() {
        if (!project.isDisposed()) {
            project.getMessageBus().syncPublisher(SddStateTopic.TOPIC).stateChanged(currentState);
        }
    }
}
