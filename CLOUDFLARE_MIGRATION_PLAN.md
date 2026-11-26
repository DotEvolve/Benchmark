# Android Migration Plan — Cloud Firestore to Cloudflare Worker/D1 API

**Repository:** `DotEvolve/Benchmark` (`net.dotevolve.benchmark`)  
**Companion Web Repo:** `DotEvolve/benchmark.dotevolve.net` (`https://benchmark.dotevolve.net`)  
**Goal:** Migrate benchmark result telemetry persistence from **Cloud Firestore** to a zero-billing **Cloudflare Pages Functions / Worker + D1** HTTPS ingestion endpoint (`POST /api/v1/benchmarks`), while keeping free Firebase Spark services (Analytics, Crashlytics, Performance Monitoring, FCM, and Play Integrity App Check) intact.

---

## 1. Current Android Codebase Audit

### Services to KEEP on Firebase Spark (`$0`)
Found in [`app/build.gradle.kts`](app/build.gradle.kts) and [`gradle/libs.versions.toml`](gradle/libs.versions.toml):
- `firebase-analytics`
- `firebase-crashlytics` + `firebase-crashlytics-ndk`
- `firebase-perf`
- `firebase-messaging`
- `firebase-appcheck` + `firebase-appcheck-playintegrity` (used to attest requests to the Cloudflare API via `X-Firebase-AppCheck` header without paying for Firebase Blaze)
- `google-services-mock` plugin (`buildSrc/`)

### Services & Files to REMOVE / REPLACE
1. **`com.google.firebase:firebase-firestore`** in:
   - [`gradle/libs.versions.toml`](gradle/libs.versions.toml#L42) (`firebase-firestore`)
   - [`app/build.gradle.kts`](app/build.gradle.kts#L136) (`implementation(libs.firebase.firestore)`)
   - [`BenchmarkApplication.kt`](app/src/main/java/net/dotevolve/benchmark/BenchmarkApplication.kt#L8-L38) (`PersistentCacheSettings`, `MemoryCacheSettings`, `FirebaseFirestore.getInstance().firestoreSettings`)
   - [`PerformanceMetrics.java`](app/src/main/java/net/dotevolve/benchmark/core/PerformanceMetrics.java#L8-L654) (`FirebaseFirestore.getInstance().collection("benchmarks").add(...)` inside `saveToHistory(Context)`)
2. **Legacy Firebase Hosting files** (once `benchmark.dotevolve.net` is live on Cloudflare Pages):
   - `.github/workflows/firebase-hosting-merge.yml`
   - `.github/workflows/firebase-hosting-pull-request.yml`
   - `firebase.json`, `.firebaserc`, `firestore.rules`, and `public/`

### Critical Bugs / Gaps Discovered During Audit
1. **`ScheduledBenchmarkWorker` Uploads Partial Legacy Runs as `"2.0.0"`**:
   - [`ScheduledBenchmarkWorker.java`](app/src/main/java/net/dotevolve/benchmark/work/ScheduledBenchmarkWorker.java#L37-L41) constructs `BenchmarkEngine` (which sets `benchmarkVersion = "2.0.0"`), runs `engine.runLegacyBenchmark()` (only SHA-512 & MD5; matrix/sort/compression/memory/multi-thread are `0`), and calls `metrics.saveToHistory(...)`.
   - `PerformanceMetrics` assigns a default score of `50` when `matrixMultiplicationTime == 0` or `memoryBandwidthTime == 0`, so scheduled background runs would skew the public leaderboard unless separated via `runType` (`"full"` vs `"legacy"`).
2. **Destructive SQLite `onUpgrade` in `PerformanceDatabaseHelper`**:
   - [`PerformanceDatabaseHelper.java`](app/src/main/java/net/dotevolve/benchmark/data/db/PerformanceDatabaseHelper.java#L112-L120) drops all local tables (`DROP TABLE IF EXISTS`) on `DATABASE_VERSION` bump. Migrating `v3 -> v4` to add sync tracking columns must use `ALTER TABLE` instead so users keep their historical runs.
3. **Missing Explicit Network Permissions in `AndroidManifest.xml`**:
   - [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) currently inherits `INTERNET` transitively from AdMob/Firebase libraries. Declare `<uses-permission android:name="android.permission.INTERNET" />` and `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` explicitly.
4. **RSA Test Skipped / SHA-512 Naming**:
   - [`BenchmarkEngine.java`](app/src/main/java/net/dotevolve/benchmark/core/BenchmarkEngine.java#L119-L148) runs `"SHA-512"` inside `runSha1Benchmark()` (`sha1TotalTime`), and `rsaTotalTime` remains `0`. Keep JSON field names backward-compatible (`sha1TotalTime`, `rsaTotalTime`) so client and server schemas stay aligned.

---

## 2. Canonical Shared API Contract (`POST /api/v1/benchmarks`)

Both Android (`DotEvolve/Benchmark`) and Web (`DotEvolve/benchmark.dotevolve.net`) must conform to this exact contract.

### Endpoints (by Build Type)
- **`release` buildType**: `https://benchmark.dotevolve.net/api/v1/benchmarks`
- **`debug` buildType**: `https://dev.benchmark.dotevolve.net/api/v1/benchmarks` (configurable via `local.properties` `benchmark.apiBaseUrl` / `BENCHMARK_API_BASE_URL` env var per `AGENTS.md` §7.3)

### Request Headers
- `Content-Type: application/json; charset=utf-8`
- `Accept: application/json`
- `X-Firebase-AppCheck: <token>` *(optional/recommended: retrieved from `FirebaseAppCheck.getInstance().getAppCheckToken(false)` — verified statelessly on Cloudflare against Firebase App Check JWKS; omit gracefully or allow debug bypass when mock `google-services.json` is active)*

### Request JSON Body
```json
{
  "clientRunId": "550e8400-e29b-41d4-a716-446655440000",
  "runType": "full",
  "deviceModel": "Pixel 8 Pro",
  "androidVersion": "Android 14",
  "cpuCores": 8,
  "totalMemory": 536870912,
  "architecture": "arm64-v8a",
  "benchmarkVersion": "2.0.0",
  "overallScore": 84,
  "cryptoScore": 81,
  "computationalScore": 88,
  "memoryScore": 79,
  "efficiencyScore": 85,
  "stabilityScore": 90,
  "multiThreadingScore": 82,
  "sha1TotalTime": 142500000,
  "md5TotalTime": 98200000,
  "aesTotalTime": 65400000,
  "rsaTotalTime": 0,
  "loopOverheadTime": 12400000,
  "matrixMultiplicationTime": 215000000,
  "sortingTime": 42100000,
  "compressionTime": 188000000,
  "memoryBandwidthTime": 74300000,
  "multiThreadedTime": 195000000,
  "clientTimestampMs": 1795700000000
}
```

### Field Validation Rules
- `clientRunId`: UUID v4 string (used for server-side idempotency so WorkManager retries never create duplicate leaderboard rows).
- `runType`: `"full"` (from `runComprehensiveBenchmark()`) or `"legacy"` (from `runLegacyBenchmark()` / `ScheduledBenchmarkWorker`). Only `"full"` runs appear on the public leaderboard.
- All scores (`overallScore` ... `multiThreadingScore`): integers `0..100`.
- `cpuCores`: integer `1..256`.
- `totalMemory`: integer `> 0` (bytes from `Runtime.getRuntime().maxMemory()`).
- All 10 timings (`sha1TotalTime` ... `multiThreadedTime`): integers `>= 0` (nanoseconds from `System.nanoTime()`).

### Expected HTTP Responses
- `201 Created`: `{ "id": "<uuid>", "clientRunId": "<uuid>", "status": "created" }`
- `200 OK`: `{ "id": "<uuid>", "clientRunId": "<uuid>", "status": "duplicate" }` *(idempotent replay — mark local row synced)*
- `400 Bad Request`: Validation error (permanent failure — mark row non-retryable or log to Crashlytics, do not infinite-retry)
- `401` / `403`: App Check failed (treat as non-retryable or log)
- `429` / `5xx`: Rate limited or transient server error (retry with WorkManager exponential backoff)

---

## 3. Step-by-Step Android Implementation Checklist

### Step 1: Gradle, Manifest & BuildConfig Wiring
1. **Update [`gradle/libs.versions.toml`](gradle/libs.versions.toml)**:
   - Remove `firebase-firestore` library entry.
   - *(No new HTTP/JSON library required — built-in `java.net.HttpURLConnection` + `org.json.JSONObject` keep APK size small and zero third-party network bloat. Optional: add OkHttp only if already desired later.)*
2. **Update [`app/build.gradle.kts`](app/build.gradle.kts)**:
   - Remove `implementation(libs.firebase.firestore)`.
   - In `defaultConfig` / `buildTypes`:
     - Inject `buildConfigField("String", "BENCHMARK_API_BASE_URL", ...)` reading `System.getenv("BENCHMARK_API_BASE_URL")` or `local.properties` `benchmark.apiBaseUrl` (`"https://benchmark.dotevolve.net"` for `release`, `"https://dev.benchmark.dotevolve.net"` for `debug`).
     - *(Rule check per `AGENTS.md` §7.3: resolve URL via `BuildConfig` / Gradle properties / env vars cleanly.)*
3. **Update [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml)**:
   - Explicitly add:
     ```xml
     <uses-permission android:name="android.permission.INTERNET" />
     <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
     ```

### Step 2: Clean Up `BenchmarkApplication.kt` & `LauncherActivity.kt`
1. **Edit [`BenchmarkApplication.kt`](app/src/main/java/net/dotevolve/benchmark/BenchmarkApplication.kt)**:
   - Remove all `com.google.firebase.firestore.*` imports and `FirebaseFirestore.getInstance().firestoreSettings = settings`.
   - Keep `FirebaseApp.initializeApp(this)` and `PlayIntegrityAppCheckProviderFactory.getInstance()` initialization (and remove the duplicate App Check setup in `LauncherActivity.kt` if redundant, or keep initialization cleanly in `BenchmarkApplication`).
2. **Review ProGuard (`app/proguard-rules.pro`)**:
   - No Gson/Moshi reflection models are needed if serializing via `org.json.JSONObject` manually. If you later adopt Kotlinx Serialization or Gson, add explicit `@Keep` / `-keep` rules for the upload DTO.

### Step 3: Track `clientRunId`, `runType`, and Sync State in Local SQLite
1. **Edit [`PerformanceDatabaseHelper.java`](app/src/main/java/net/dotevolve/benchmark/data/db/PerformanceDatabaseHelper.java)**:
   - Increment `DATABASE_VERSION` from `3` to `4`.
   - Add columns to `benchmarks` table:
     - `client_run_id TEXT` (UUID v4 generated once per run)
     - `run_type TEXT NOT NULL DEFAULT 'full'`
     - `synced INTEGER NOT NULL DEFAULT 0` (`0` = pending, `1` = synced, `-1` = permanent validation error)
     - `remote_id TEXT`
   - **Important:** Update `onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)` so `oldVersion < 4` runs non-destructive `ALTER TABLE benchmarks ADD COLUMN ...` statements rather than `DROP TABLE IF EXISTS`.
   - Add helper methods:
     - `insertBenchmarkResult(..., String clientRunId, String runType)` returning local row ID.
     - `getPendingSyncRuns(int limit)` returning unsynced `"full"` runs (`synced = 0`, `run_type = 'full'`).
     - `markRunSynced(long localId, String remoteId)` and `markRunPermanentFailure(long localId)`.

### Step 4: Replace Firestore Upload with Cloudflare API Client + Offline WorkManager Sync
1. **Create `net.dotevolve.benchmark.data.remote.BenchmarkApiClient`** (Java or Kotlin):
   - Builds the JSON payload using `org.json.JSONObject` matching Section 2.
   - Optionally fetches Firebase App Check token synchronously with a short timeout (`Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(false), 3, TimeUnit.SECONDS)` — catch failures safely so debug/offline/non-Play builds still submit or defer cleanly).
   - Opens `HttpsURLConnection` to `BuildConfig.BENCHMARK_API_BASE_URL + "/api/v1/benchmarks"` (`connectTimeout = 10_000`, `readTimeout = 10_000`, `POST`, UTF-8 body).
   - Classifies outcomes into sealed/enum result:
     - `SUCCESS(remoteId)` (`200` / `201`)
     - `PERMANENT_ERROR(httpCode)` (`400` / `401` / `403` / `422`)
     - `TRANSIENT_ERROR(httpCode or IOException)` (`429` / `5xx` / timeout)
2. **Create `net.dotevolve.benchmark.work.SyncBenchmarksWorker`** (`Worker` / `CoroutineWorker`):
   - Scheduled with `NetworkType.CONNECTED` and `BackoffPolicy.EXPONENTIAL`.
   - Reads pending rows from `PerformanceDatabaseHelper.getPendingSyncRuns(10)`, uploads via `BenchmarkApiClient`, marks `synced = 1` on `SUCCESS` or `synced = -1` on `PERMANENT_ERROR`, and returns `Result.retry()` on `TRANSIENT_ERROR`.
3. **Update [`BenchmarkEngine.java`](app/src/main/java/net/dotevolve/benchmark/core/BenchmarkEngine.java) & [`PerformanceMetrics.java`](app/src/main/java/net/dotevolve/benchmark/core/PerformanceMetrics.java)**:
   - Add `runType` (`"full"` vs `"legacy"`) on `PerformanceMetrics`, set `"full"` in `runComprehensiveBenchmark()` and `"legacy"` in `runLegacyBenchmark()`.
   - In `PerformanceMetrics.saveToHistory(Context context)`:
     - Generate `clientRunId = UUID.randomUUID().toString()`.
     - Save locally to `PerformanceDatabaseHelper` first (preserving offline-first guarantee).
     - If `runType` is `"full"`, attempt immediate upload via `BenchmarkApiClient` (already called from background threads in `MainActivity` & `ScheduledBenchmarkWorker`) OR enqueue `SyncBenchmarksWorker` on failure / offline so pending runs reliably sync when connectivity returns.
     - Do **not** upload `"legacy"` scheduled runs to the public leaderboard (or pass `runType = "legacy"` so server skips public leaderboard indexing).
   - Delete `import com.google.firebase.firestore.FirebaseFirestore;` and all Firestore write code.

### Step 5: Unit & Integration Tests
Add tests under `app/src/test/java/net/dotevolve/benchmark/`:
1. **Payload Serialization Test**: Verify `BenchmarkApiClient` produces all 27 JSON keys with exact field names, integer scores `0..100`, nanosecond timings, `clientRunId` UUID, and `runType`.
2. **HTTP Status Classification Test**: Verify `201`/`200` -> `SUCCESS`, `400`/`403` -> `PERMANENT_ERROR`, `429`/`503` -> `TRANSIENT_ERROR` (using local `MockWebServer` or unit-testable response parser).
3. **SQLite Migration Test** (Robolectric or instrumented `androidTest`): Verify `v3 -> v4` upgrade retains existing benchmark rows and initializes `synced = 0`, `run_type = 'full'`.

### Step 6: Verification Gates (Before Opening PR in `DotEvolve/Benchmark`)
Run inside `/Users/sadiq/GitHub/Benchmark` per `Benchmark/AGENTS.md`:
```bash
./gradlew assembleDebug
./gradlew test
./gradlew lintDebug
```
Confirm zero references to `FirebaseFirestore` / `firebase-firestore` remain in `app/src/` and `gradle/`, and verify `Benchmark` debug build posts to the dev Cloudflare endpoint (`dev.benchmark.dotevolve.net/api/v1/benchmarks`) and handles `400`/`429`/`500` gracefully.

### Step 7: Legacy Firebase Hosting Cleanup (After `benchmark.dotevolve.net` Production Cutover)
In a separate discrete PR on `DotEvolve/Benchmark`:
- Remove `.github/workflows/firebase-hosting-merge.yml`, `.github/workflows/firebase-hosting-pull-request.yml`, `firebase.json`, `.firebaserc`, `firestore.rules`, and `public/`.
