# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is an IntelliJ IDEA plugin that supports Spec Driven Development.

## Build & Run Commands

```bash
./gradlew build          # Compile and build
./gradlew runIde         # Launch IntelliJ sandbox with plugin loaded
./gradlew buildPlugin    # Build distributable plugin ZIP
./gradlew verifyPlugin   # Verify IDE compatibility
./gradlew test           # Run tests (none exist yet)
./gradlew clean          # Clean build outputs
```

## Key Configuration Files

- `build.gradle.kts` — Uses `org.jetbrains.intellij.platform` v2.12.0, targets IntelliJ 2025.1.7 (build 251+), Java 21 toolchain.
- `src/main/resources/META-INF/plugin.xml` — Plugin descriptor with extension point registration.
