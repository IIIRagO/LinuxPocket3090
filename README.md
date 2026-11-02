# LinuxPocket 3090

A phone-first Android project intended to provide a lightweight, rootless Linux userspace.

## Current stage

This repository contains the Android starter application and a GitHub Actions workflow that builds a debug APK. The app currently displays Android version, API level, and supported CPU ABIs.

**It does not yet install or run Linux.** No root access is requested.

## Planned stages

1. Verify device ABI, Android compatibility, and available storage.
2. Select and verify a compatible Alpine Linux root filesystem.
3. Add a compatible PRoot runtime and safe extraction/installation flow.
4. Implement a real terminal process backend with output and exit codes.
5. Add session management, diagnostics, and backup/restore.
6. Evaluate an optional lightweight graphical desktop only after the command-line environment works.

## Build

GitHub Actions can build a debug APK when code is pushed to `main`, or when the workflow is started manually from the Actions tab.

The workflow uses Java 17, Gradle 8.9, Android SDK 35, and Android Gradle Plugin 8.7.3.

## Important

Test on the target phone before treating any feature as supported. Keep sufficient free storage for downloads, temporary files, and the extracted Linux root filesystem.
