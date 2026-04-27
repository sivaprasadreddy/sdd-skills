package dev.sivalabs.sdd.plugin.service;

import dev.sivalabs.sdd.plugin.model.SddState;

@FunctionalInterface
public interface SddStateListener {
    void stateChanged(SddState newState);
}
