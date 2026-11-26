# Agent Development & Operations Guide

## Tier: Android Application (`android-app`)

---

## 1. Architectural Precedence & Steering Files

Before planning or writing any code, inspect the repository's configuration and architectural structure:

1. `app/build.gradle.kts` & `settings.gradle.kts` — Dependencies, SDK versions (`minSdk`, `targetSdk`, `compileSdk`), and build configurations.
2. `app/src/main/AndroidManifest.xml` — Components, permissions, process declarations, and app metadata.
3. Android Architecture Conventions — Jetpack Compose UI, MVVM pattern, Kotlin Coroutines, StateFlow, Room DB / SQLite, and Firebase integration.

> **Precedence Rule**: Local project build scripts and explicit architectural rules supersede workspace or global defaults.

---

## 2. Pre-Commit Verification Gates

Before staging or committing any changes, run and pass all the following:

1. **Compilation Check**: `./gradlew assembleDebug` with zero compilation errors.
2. **Automated Unit Tests**: `./gradlew test` (or `./gradlew testDebugUnitTest`) with 100% passing tests.
3. **Android Lint & Code Quality**: `./gradlew lintDebug` with zero errors.

Commits follow **Conventional Commits**: `feat(<scope>): ...`, `fix(<scope>): ...`, `chore(<scope>): ...`, `docs(<scope>): ...`, `test(<scope>): ...`.

---

## 3. Branching Strategy & Release Lifecycle (CalVer)

```text
feature/*  ──(PR)──>  dev  ──(cut)──>  release/<CalVer>  ──(PR)──>  master
```

- `feature/*` — Working branches off `dev` (`feat/*`, `fix/*`, `chore/*`, `docs/*`).
- `dev` — Integration branch; verified via debug builds and internal testing.
- `release/<calver>` — Staged release candidate cut from `dev`. Format: `YYYY.0M.0D` (append `.1`, `.2` for multiple same-day releases). Generates release bundle (`bundleRelease`) and native debug symbols.
- `master` — Production branch; deployed to Google Play Console or production distribution channels.

> **Rule**: Never commit directly to `dev` or `master`. All changes require a PR.

### 3.1 Focused Feature PR Rule (Individual & Discrete PRs)

> **Rule**: Features, enhancements, bug fixes, and chores should be developed on dedicated branches and opened as individual, focused PRs targeting `dev`. Opening separate PRs per feature or fix is encouraged to ensure isolated risk, granular automated testing, and concise code reviews.
>
> **Batching Disallowed**: Monolithic PRs bundling multiple unrelated features, fixes, or chores together are discouraged. Each distinct change should be delivered through its own discrete PR.

### Promotion Steps:

1. **Feature → dev**: `git checkout dev && git pull origin dev && git checkout -b feat/<name>` → implement & test → run `./gradlew assembleDebug test` → `git push -u origin feat/<name>` → `gh pr create --base dev` (merge using **Squash Merge**).
2. **dev → release**: `git checkout dev && git pull origin dev && git checkout -b release/YYYY.0M.0D` → verify full test suite (`./gradlew bundleRelease`) → `git push -u origin release/YYYY.0M.0D` → `gh pr create --base master --title "release: YYYY.0M.0D [dev -> master]"` (merge using **Merge Commit**).

---

## 4. PR Quality Gates & Remediation Loop

1. **Never merge immediately** — wait for CI checks to complete.
2. **Poll**: `gh pr checks <pr-number>` and `gh pr view <pr-number> --json reviews,comments`.
3. **Zero open comments** — address every comment from humans, bots, and automated code reviews. Fix the code, run tests, push updates, and reply on the thread before resolving.
4. Verify all PR Quality checks are green (`SUCCESS`) before proceeding.

---

## 5. HARD GATE: Explicit Merge Authorization

Even when all checks pass with zero open comments, **agents must never merge autonomously**.

Ask the user:

> _"PR #\<id\> has passed all CI checks with zero open comments. Do you approve merging into `<target-branch>`?"_

Only after explicit user confirmation:

### Merge Strategy by Phase:

- **[Phase 1: Feature -> dev]** Use **SQUASH MERGE** (`--squash`):
  ```bash
  gh pr merge <pr-number> --squash --delete-branch
  ```
  - Keeps `dev` clean with 1 commit per consolidated feature/fix.

- **[Phase 2: release -> master]** Use **MERGE COMMIT** (`--merge` / `--no-ff`):
  ```bash
  gh pr merge <pr-number> --merge --delete-branch
  ```
  - Keeps exact `dev` commit SHAs in `master`'s git tree.
  - Ensures `dev` and `master` never diverge.

> **Rule**: Rebase merges (`--rebase`) are strictly prohibited.

---

## 6. Post-Merge Build & Distribution Verification

After merging to `dev` or `master`:

1. Verify build artifacts are generated correctly:
   - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
   - Release AAB: `app/build/outputs/bundle/release/app-release.aab`
   - Native Debug Symbols (if enabled): `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip`
2. Run smoke tests on an Android Virtual Device (AVD) or physical test device if required.

---

## 7. Agent Decision & Confirmation Protocol

### 7.1 Confirm Before Acting

When the next action is unclear, the agent **must stop and ask** before proceeding. Never guess.

Situations requiring explicit confirmation:
- Which environment or release track to target when not stated.
- Which branch to merge into when the PR target is ambiguous.
- Whether to proceed with destructive or irreversible operations.
- Modifying core Gradle build scripts (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`).
- Modifying `AndroidManifest.xml` permissions or component exported states.

> **Rule**: A short clarifying question is always preferable to an incorrect autonomous action.

### 7.2 No Guessing Root Causes

When investigating a bug, crash, or build failure:
- Read the actual exception stack trace, Logcat log, or Gradle error output first.
- Only propose a fix after the root cause is confirmed by observable log output or code analysis.

### 7.3 Security & Credential Protection Rule

- **DO NOT HARDCODE SECRETS OR API KEYS IN SOURCE FILES**: API keys (AdMob, Firebase, Aptoide, etc.), tokens, and environment-dependent values must strictly be resolved via `BuildConfig`, Gradle properties, or system environment variables. Never commit hardcoded fallback secrets into source code or XML resources.

---

## 8. Artifact & Documentation Persistence Protocol

All plans, analysis reports, architecture designs, and audit summaries generated by agents must be preserved in version-controlled Markdown documentation:

1. **Mandatory Documentation**: Any artifact generated by an agent (e.g., migration plans, architectural decisions, audit logs) must be saved into Markdown documentation in the project.
2. **Persistence**: Store architectural decisions and guides in repository Markdown files (e.g., `NATIVE_SYMBOLS_UPLOAD.md`, `AGENTS.md`, or `docs/`).
