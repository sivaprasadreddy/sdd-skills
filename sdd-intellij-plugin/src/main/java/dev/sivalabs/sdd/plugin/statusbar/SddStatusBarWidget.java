package dev.sivalabs.sdd.plugin.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.util.messages.MessageBusConnection;
import dev.sivalabs.sdd.plugin.model.SddState;
import dev.sivalabs.sdd.plugin.service.SddStateService;
import dev.sivalabs.sdd.plugin.service.SddStateTopic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import com.intellij.util.Consumer;

import java.awt.event.MouseEvent;

public class SddStatusBarWidget implements StatusBarWidget, StatusBarWidget.TextPresentation {

    private final Project project;
    private StatusBar statusBar;
    private MessageBusConnection busConnection;
    private volatile String displayText = "SDD";

    public SddStatusBarWidget(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NotNull String ID() {
        return SddStatusBarWidgetFactory.ID;
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
        updateText(SddStateService.getInstance(project).getState());

        busConnection = project.getMessageBus().connect();
        busConnection.subscribe(SddStateTopic.TOPIC, state -> {
            updateText(state);
            StatusBar bar = this.statusBar;
            if (bar != null) {
                SwingUtilities.invokeLater(() -> bar.updateWidget(ID()));
            }
        });
    }

    @Override
    public void dispose() {
        if (busConnection != null) {
            busConnection.disconnect();
            busConnection = null;
        }
        statusBar = null;
    }

    // ── TextPresentation ─────────────────────────────────────────────

    @Override
    public @NotNull String getText() {
        return displayText;
    }

    @Override
    public float getAlignment() {
        return 0f;
    }

    @Override
    public @Nullable String getTooltipText() {
        SddState state = SddStateService.getInstance(project).getState();
        if (state.getFeatureName() == null) return "No active SDD feature";
        return "<html>SDD Feature: <b>" + state.getFeatureName() + "</b><br>"
                + "Stage: " + state.getCurrentStage().getDisplayName() + "<br>"
                + "ACs: " + state.getCheckedAcCount() + " / " + state.getTotalAcCount() + "</html>";
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return event -> {
            ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow("SDD");
            if (tw != null) tw.activate(null);
        };
    }

    private void updateText(SddState state) {
        if (state.getFeatureName() == null) {
            displayText = "SDD";
            return;
        }
        String name = state.getFeatureName();
        if (name.length() > 28) name = name.substring(0, 25) + "…";

        int total = state.getTotalAcCount();
        int checked = (int) state.getCheckedAcCount();
        if (total > 0) {
            displayText = "SDD: " + name + "  •  AC " + checked + "/" + total;
        } else {
            displayText = "SDD: " + name + "  •  " + state.getCurrentStage().getDisplayName();
        }
    }
}
