package dev.sivalabs.sdd.plugin.service;

import com.intellij.util.messages.Topic;

public final class SddStateTopic {
    public static final Topic<SddStateListener> TOPIC =
            Topic.create("SDD State Changed", SddStateListener.class);

    private SddStateTopic() {}
}
