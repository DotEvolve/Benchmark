# Device Brand & Manufacturer Enhancement Plan

**Repository:** `DotEvolve/Benchmark` (`net.dotevolve.benchmark`)  
**Companion Web Repo:** `DotEvolve/benchmark.dotevolve.net` (`https://benchmark.dotevolve.net`)  
**Goal:** Capture and send `android.os.Build.MANUFACTURER` and `android.os.Build.BRAND` alongside `Build.MODEL` during benchmark ingestion so brand attribution is accurate for uncommon OEMs instead of relying solely on regex heuristics (`inferBrand(deviceModel)`).

---

## 1. Current State

- **Android (`PerformanceMetrics.java`)**: Captures `deviceModel = Build.MODEL`, `androidVersion`, `cpuCores`, `totalMemory`, and `architecture`.
- **Android (`BenchmarkApiClient.java`)**: Sends `deviceModel` in `POST /api/v1/benchmarks`.
- **Web API (`functions/api/v1/benchmarks/index.ts`)**: Derives `inferred_brand` via `inferBrand(data.deviceModel)` in `src/lib/brand.ts` (and resolves `marketing_name` via D1 `device_catalog`).
- **Gap**: Uncommon OEMs or generic model strings (e.g., `TB310XU`, `CPH*`, regional/carrier variants) fall through to `'Other'` when `Build.MODEL` does not match `BRAND_RULES`.

---

## 2. Android Implementation (`DotEvolve/Benchmark`)

> **Branching**: Create a dedicated feature branch off `dev` (or after PR #142 `feat/cloudflare-api-migration` lands on `dev`): `feat/device-brand-telemetry`.

### 2.1 Capture Hardware Brand & Manufacturer
- **`app/src/main/java/net/dotevolve/benchmark/core/PerformanceMetrics.java`**:
  - Add fields `deviceManufacturer` (`Build.MANUFACTURER`) and `deviceBrand` (`Build.BRAND`).
  - Add getters `getDeviceManufacturer()` and `getDeviceBrand()`.
- **`app/src/main/java/net/dotevolve/benchmark/data/model/BenchmarkResult.java`**:
  - Add nullable `deviceManufacturer` and `deviceBrand` fields (populated from `PerformanceMetrics` when present).

### 2.2 Local SQLite Storage (`PerformanceDatabaseHelper.java`)
- Add nullable `device_manufacturer TEXT` and `device_brand TEXT` columns via non-destructive `ALTER TABLE` migration (only when bumping `DATABASE_VERSION`, preserving existing user run history).
- Fall back to `Build.MANUFACTURER` / `Build.BRAND` if older local rows are null and uploaded from the current device.

### 2.3 API Payload (`BenchmarkApiClient.java`)
- In `buildPayload(PerformanceMetrics)` and `buildPayload(BenchmarkResult)`:
  - Include optional string fields:
    - `"deviceManufacturer": "<Build.MANUFACTURER>"` (trimmed, omitted if blank)
    - `"deviceBrand": "<Build.BRAND>"` (trimmed, omitted if blank)
- Keep `deviceModel` unchanged so backward compatibility and `clientRunId` idempotency are preserved.

### 2.4 Android Verification
- Unit test payload serialization in `BenchmarkApiClient` (non-empty `deviceManufacturer` and `deviceBrand`, graceful omission when null/blank).
- Run `./gradlew testDebugUnitTest` and verify `POST /api/v1/benchmarks` against `https://benchmark-dev.dotevolve.net`.

---

## 3. Companion Web API & Privacy Alignment (`DotEvolve/benchmark.dotevolve.net`)

Deploy these **before** releasing the Android update:

1. **Validation (`functions/lib/validation.ts`)**:
   - Add optional `deviceManufacturer: z.string().trim().max(64).optional()` and `deviceBrand: z.string().trim().max(64).optional()` to `benchmarkIngestSchema`.
2. **Brand Resolution Precedence (`functions/api/v1/benchmarks/index.ts`)**:
   1. Normalized `device_catalog.brand` (if `device_model` exists in Google Play `device_catalog`).
   2. Normalized `deviceManufacturer` / `deviceBrand` from the Android payload (e.g., `"samsung"` → `"Samsung"`, `"google"` → `"Google"`, `"xiaomi"` → `"Xiaomi"`, title-cased fallback for other OEMs).
   3. Existing `inferBrand(data.deviceModel)` regex fallback for older app versions.
3. **Privacy Policy (`src/pages/PrivacyPolicyPage.tsx`)**:
   - Update the "Device Information (Android App)" bullet to note that device manufacturer and brand (`Build.MANUFACTURER` / `Build.BRAND`) are collected alongside device model.

---

## 4. Rollout Sequence

1. **Web (`benchmark.dotevolve.net`)**: Add optional schema fields + brand precedence + privacy policy update → merge to `dev` → release to `master`.
2. **Android (`DotEvolve/Benchmark`)**: Implement `deviceManufacturer` / `deviceBrand` capture & upload on `feat/device-brand-telemetry` → test against `benchmark-dev.dotevolve.net` → merge to `dev` → release.
