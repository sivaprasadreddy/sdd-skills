package dev.sivalabs.sdd.plugin.gutter;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.DocumentUtil;
import dev.sivalabs.sdd.plugin.model.ReviewFinding;
import dev.sivalabs.sdd.plugin.model.ReviewReport;
import dev.sivalabs.sdd.plugin.model.SddState;
import dev.sivalabs.sdd.plugin.service.SddStateTopic;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.PROJECT)
public final class SddGutterAnnotationService {

    private final Project project;
    /** key: absolute file path → list of active highlighters */
    private final Map<String, List<RangeHighlighter>> highlighters = new ConcurrentHashMap<>();
    /** key: relative file path → findings */
    private volatile Map<String, List<ReviewFinding>> findingsByRelPath = Collections.emptyMap();

    public SddGutterAnnotationService(@NotNull Project project) {
        this.project = project;

        // Re-annotate on every state change
        project.getMessageBus().connect().subscribe(SddStateTopic.TOPIC, state ->
                ApplicationManager.getApplication().invokeLater(() -> onStateChanged(state))
        );

        // Annotate a file the moment it opens
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        ApplicationManager.getApplication().invokeLater(() -> annotateFile(file));
                    }
                }
        );
    }

    public static SddGutterAnnotationService getInstance(@NotNull Project project) {
        return project.getService(SddGutterAnnotationService.class);
    }

    // ── internals ──────────────────────────────────────────────────────

    private void onStateChanged(SddState state) {
        clearAllAnnotations();

        ReviewReport report = state.getReviewReport();
        if (report == null || report.getFindings().isEmpty()) {
            findingsByRelPath = Collections.emptyMap();
            return;
        }

        Map<String, List<ReviewFinding>> map = new HashMap<>();
        for (ReviewFinding f : report.getFindings()) {
            if (f.getFilePath() != null && !f.getFilePath().isBlank()) {
                map.computeIfAbsent(f.getFilePath(), k -> new ArrayList<>()).add(f);
            }
        }
        findingsByRelPath = map;

        // Apply to all currently open files
        for (VirtualFile vf : FileEditorManager.getInstance(project).getOpenFiles()) {
            annotateFile(vf);
        }
    }

    private void annotateFile(VirtualFile vf) {
        if (project.isDisposed()) return;

        String relPath = toRelativePath(vf);
        List<ReviewFinding> findings = findingsByRelPath.getOrDefault(relPath, Collections.emptyList());

        Document doc = FileDocumentManager.getInstance().getDocument(vf);
        if (doc == null) return;

        MarkupModel markup = com.intellij.openapi.editor.impl.DocumentMarkupModel
                .forDocument(doc, project, true);

        // Clear old SDD annotations for this file
        List<RangeHighlighter> old = highlighters.remove(vf.getPath());
        if (old != null) {
            for (RangeHighlighter h : old) {
                if (h.isValid()) markup.removeHighlighter(h);
            }
        }

        if (findings.isEmpty()) return;

        List<RangeHighlighter> newOnes = new ArrayList<>();
        for (ReviewFinding finding : findings) {
            int lineIndex = finding.getLine() - 1; // 0-based
            if (lineIndex < 0 || lineIndex >= doc.getLineCount()) continue;
            RangeHighlighter h = markup.addLineHighlighter(lineIndex, HighlighterLayer.LAST, null);
            h.setGutterIconRenderer(new SddGutterIconRenderer(finding));
            newOnes.add(h);
        }
        if (!newOnes.isEmpty()) {
            highlighters.put(vf.getPath(), newOnes);
        }
    }

    private void clearAllAnnotations() {
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(this::clearAllAnnotations);
            return;
        }
        for (VirtualFile vf : FileEditorManager.getInstance(project).getOpenFiles()) {
            Document doc = FileDocumentManager.getInstance().getDocument(vf);
            if (doc == null) continue;
            MarkupModel markup = com.intellij.openapi.editor.impl.DocumentMarkupModel
                    .forDocument(doc, project, false);
            if (markup == null) continue;
            List<RangeHighlighter> old = highlighters.remove(vf.getPath());
            if (old != null) {
                for (RangeHighlighter h : old) {
                    if (h.isValid()) markup.removeHighlighter(h);
                }
            }
        }
    }

    private String toRelativePath(VirtualFile vf) {
        String base = project.getBasePath();
        String path = vf.getPath();
        if (base != null && path.startsWith(base + "/")) {
            return path.substring(base.length() + 1);
        }
        return path;
    }
}
