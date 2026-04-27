package dev.sivalabs.sdd.plugin.startup;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import dev.sivalabs.sdd.plugin.gutter.SddGutterAnnotationService;
import dev.sivalabs.sdd.plugin.service.SddStateService;
import org.jetbrains.annotations.NotNull;

/** Ensures both project services are initialized at project open time. */
public class SddStartupActivity implements StartupActivity.DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {
        SddStateService.getInstance(project);
        SddGutterAnnotationService.getInstance(project);
    }
}
