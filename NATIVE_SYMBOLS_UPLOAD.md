# Native Debug Symbols Upload Guide

## Overview

This app uses native dependencies (such as Firebase NDK and native components). Google Play Console requires native debug symbols for symbolicated stack traces in crash and ANR reports.

---

## How Native Debug Symbols Work in AGP 8+ / 9+

In `app/build.gradle.kts`, native debug symbol generation is enabled via:

```kotlin
android {
    defaultConfig {
        ndk {
            debugSymbolLevel = "FULL"
        }
    }
}
```

### 1. Embedded in App Bundle (`.aab`) — Recommended & Automatic

When you build a release bundle (`./gradlew bundleRelease`), Android Gradle Plugin (AGP) automatically packages the full unstripped native debug symbol tables directly inside the `.aab` file under:

```text
BUNDLE-METADATA/com.android.tools.build.debugsymbols/
```

**Result**: When you upload `app-release.aab` to Google Play Console, Play Console automatically extracts and processes the native debug symbols. **No separate `.zip` file upload is required.**

---

### 2. Standalone Zip Archive (`native-debug-symbols.zip`)

If you are uploading APKs manually or require a standalone symbols archive, `app/build.gradle.kts` includes a custom Gradle task (`zipReleaseNativeDebugSymbols`) that packages extracted symbols into:

```text
app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip
```

> **Note on File Creation**: If the project or its current dependencies do not include unstripped C/C++ (`.so`) native shared objects, AGP extracts 0 symbol files during `extractReleaseNativeDebugMetadata`, and the `.zip` file will not be generated. This is expected behavior when no unstripped native binaries exist in the build.

---

## Verification Steps

### Play Console Verification
1. Upload your release bundle (`app-release.aab`) to Google Play Console.
2. Go to **Release** → **App Bundle Explorer** (or select your release track).
3. Select the uploaded version and navigate to **Downloads** / **Assets**.
4. Confirm that **Native debug symbols** shows as processed/uploaded automatically.

---

## Summary

- **Primary Delivery**: Symbols are automatically bundled inside `app-release.aab` via `ndk.debugSymbolLevel = "FULL"`.
- **Manual Delivery**: If native libraries are present, `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip` is generated during `./gradlew bundleRelease` / `./gradlew assembleRelease`.
