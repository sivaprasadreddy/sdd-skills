package dev.sivalabs.sdd.plugin.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class SddStatusBarWidgetFactory implements StatusBarWidgetFactory {

    static final String ID = "SddStatusBarWidget";

    @Override
    public @NotNull String getId() {
        return ID;
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return "SDD Status";
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new SddStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        widget.dispose();
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}
