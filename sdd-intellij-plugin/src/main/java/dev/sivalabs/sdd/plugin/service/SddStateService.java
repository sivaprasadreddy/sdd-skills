package dev.sivalabs.sdd.plugin.service;

import com.intellij.openapi.application.ApplicationManager;
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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class SddStateService {

    private final Project project;
    private volatile SddState currentState;
    private final FeatureMdParser featureParser = new FeatureMdParser();
    private final PlanMdParser planParser = new PlanMdParser();

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

    private void setupFileWatcher() {
        project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                boolean relevant = events.stream()
                        .anyMatch(e -> {
                            String path = e.getPath();
                            return path.endsWith("/feature.md") || path.endsWith("/plan.md");
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
        VirtualFile planMd = LocalFileSystem.getInstance().findFileByPath(basePath + "/plan.md");

        if (featureMd == null || !featureMd.exists()) {
            return SddState.empty();
        }

        String featureContent = readFile(featureMd);
        FeatureMdParser.ParseResult featureResult = featureParser.parse(featureContent);
        String featureName = featureResult.featureName();
        List<AcItem> acs = featureResult.acceptanceCriteria();

        if (planMd == null || !planMd.exists()) {
            return new SddState(featureName, WorkflowStage.ANALYSE, acs, Collections.emptyList());
        }

        String planContent = readFile(planMd);
        List<PlanStep> planSteps = planParser.parse(planContent);
        WorkflowStage stage = determineStage(acs, planSteps);

        return new SddState(featureName, stage, acs, planSteps);
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

    private void publishStateChange() {
        if (!project.isDisposed()) {
            project.getMessageBus().syncPublisher(SddStateTopic.TOPIC).stateChanged(currentState);
        }
    }
}
