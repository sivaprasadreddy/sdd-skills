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
import dev.sivalabs.sdd.plugin.parser.FeatureMdParser;
import dev.sivalabs.sdd.plugin.parser.PlanMdParser;
import dev.sivalabs.sdd.plugin.parser.ReviewReportParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT)
public final class SddStateService {

    private final Project project;
    private volatile SddState currentState;
    private final FeatureMdParser featureParser = new FeatureMdParser();
    private final PlanMdParser planParser = new PlanMdParser();
    private final ReviewReportParser reviewParser = new ReviewReportParser();

    public SddStateService(@NotNull Project project) {
        this.project = project;
        this.currentState = loadState();
        setupFileWatcher();
    }

    public static SddStateService getInstance(@NotNull Project project) {
        return project.getService(SddStateService.class);
    }

    public SddState getState() {
        return currentState;
    }

    public void refresh() {
        currentState = loadState();
        publishStateChange();
    }

    /** Toggle one AC checkbox in feature.md and refresh state. */
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
                            return path.endsWith("/feature.md") || path.endsWith("/plan.md")
                                    || path.endsWith("/review.md");
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

        VirtualFile featureMd = LocalFileSystem.getInstance().findFileByPath(basePath + "/feature.md");
        VirtualFile planMd    = LocalFileSystem.getInstance().findFileByPath(basePath + "/plan.md");
        VirtualFile reviewMd  = LocalFileSystem.getInstance().findFileByPath(basePath + "/review.md");

        if (featureMd == null || !featureMd.exists()) {
            return SddState.empty();
        }

        FeatureSpec featureSpec = featureParser.parse(readFile(featureMd));

        ReviewReport reviewReport = (reviewMd != null && reviewMd.exists())
                ? reviewParser.parse(readFile(reviewMd))
                : null;

        if (planMd == null || !planMd.exists()) {
            return new SddState(featureSpec, WorkflowStage.ANALYSE, Collections.emptyList(), reviewReport);
        }

        List<PlanStep> planSteps = planParser.parse(readFile(planMd));
        WorkflowStage stage = determineStage(featureSpec.getAcceptanceCriteria(), planSteps);
        return new SddState(featureSpec, stage, planSteps, reviewReport);
    }

    private WorkflowStage determineStage(List<AcItem> acs, List<PlanStep> planSteps) {
        if (!acs.isEmpty()) {
            long checked = acs.stream().filter(AcItem::isChecked).count();
            if (checked == acs.size()) return WorkflowStage.REVIEW;
            if (checked > 0) return WorkflowStage.IMPLEMENT;
        }
        boolean anyProgress = planSteps.stream()
                .anyMatch(s -> s.getStatus() == StepStatus.DONE || s.getStatus() == StepStatus.IN_PROGRESS);
        return anyProgress ? WorkflowStage.IMPLEMENT : WorkflowStage.PLAN;
    }

    private String readFile(VirtualFile vf) {
        try {
            return new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private String toggleAcLine(String content, String acId, boolean checked) {
        // Matches lines like:  - [ ] **AC-01**:  or  - [x] AC-01
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
