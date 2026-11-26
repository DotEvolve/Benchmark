# Benchmark Engine v2.0 Enhancement & Positive-Skew Scoring Plan

**Repository:** `DotEvolve/benchmark` (`net.dotevolve.benchmark`)  
**Scope:** `BenchmarkEngine.java`, `PerformanceMetrics.java`, and unit tests  
**Engine Version Constraint:** Keep `BenchmarkEngine.ENGINE_VERSION = "2.0.0"` unchanged (preserve SQLite schema `v5` and Cloudflare D1 ingestion contract).

---

## 1. Problem Statement & Goals

### Observed Issues
1. **Score Compression Around ~50 Across All Devices:** Entry-level, mid-range, and flagship Android devices all receive overall scores clustered around `45–55`, reducing user engagement and failing to differentiate hardware tiers.
2. **Discouraging Linear/Clipped Baselines:** Users expect rewarding, motivating scores on capable phones (and clear upward movement when closing background apps or cooling the device) rather than seeing a flagship phone score `52/100`.
3. **Measurement Bugs in Current v2.0 Tests:** Several timed sections measure `java.util.Random` generation, heap allocation, per-operation `System.nanoTime()` overhead, or GC pauses instead of actual CPU/crypto/memory hardware throughput.

### Goals
- **Positive-Skewed Exponential Scoring Scale:** Replace clipped linear/uncalibrated curves with a smooth, concave exponential saturation curve so scores are motivating, clearly separate hardware tiers (`~55–70` entry, `~72–86` mid-range, `~88–98` flagship), and leave headroom up to `100`.
- **Higher Measurement Accuracy & Better Workloads:** Separate setup/allocation from timers, add deterministic JIT warm-up and anti-DCE (`blackhole`) sinks, use macro-batch timing instead of 1M+ boxed `Long` allocations, fix cache-thrashing and OOM risks, and score every test fairly.
- **Zero Breaking Changes in v2.0:** Keep `ENGINE_VERSION = "2.0.0"`, keep all existing SQLite columns (`PerformanceDatabaseHelper`) and API JSON keys (`BenchmarkApiClient`), including preserving `rsaTotalTime = 0` (`getRsaTotalTime()`) since it is actively serialized in `PerformanceDatabaseHelper.insertBenchmarkResult()` (line 246), `BenchmarkResult` constructor (line 89), and `BenchmarkApiClient.uploadBenchmark()` (line 140).

---

## 2. Root-Cause Audit of Current v2.0 Implementation

Inspection of `app/src/main/java/net/dotevolve/benchmark/core/PerformanceMetrics.java` (754 lines) and `BenchmarkEngine.java` (490 lines) reveals six compounding flaws plus dead RSA scaffolding:

| Area | Current Behavior (Code Reference) | Impact on Score |
| :--- | :--- | :--- |
| **`calculateEfficiencyScore()` (15% weight)** [`PerformanceMetrics.java` L297–311] | Uses **linear** `normalizeInverse` on `ns/op` with overly tight ceilings: SHA-512 `[10, 1000]` ns, MD5 `[8, 800]` ns, AES `[50, 5000]` ns. Real Java `MessageDigest.getInstance("SHA-512")` + per-op `System.nanoTime()` overhead typically produces `~500–5,000+ ns/op`. **Not** a hard clip to 0 — the linear interpolation compresses scores into the `0–50` range on most devices. | Compresses `efficiencyScore` linearly into low values (`0–50`) on nearly all hardware. |
| **`runMemoryBandwidthBenchmark()` & `calculateMemoryScore()` (20% weight)** [`BenchmarkEngine.java` L360–401, `PerformanceMetrics.java` L347–363] | `startMemoryTiming()` fires **before** allocating two `50 MB / 4` int arrays (~100 MB heap) and running `25,000,000` synchronized `random.nextInt()` calls. The `sum` result variable is never consumed (ART DCE risk). `calculateMemoryScore()` divides only `MEMORY_TEST_SIZE` (50 MB) by `memoryBandwidthTime` despite the benchmark actually transferring ~3× that across copy + reverse + sum passes. | Depresses `bandwidthScore` toward **0** (inaccurate MB/s denominator) and risks OOM on low-RAM / Android Go devices. |
| **`runMatrixMultiplicationBenchmark()` & `runSortingBenchmark()` (30% weight)** [`BenchmarkEngine.java` L237–297] | `startMatrixTiming()` fires before `524,288` `random.nextDouble()` calls and 3× `double[512][512]` allocation. `512×512` `ijk` loop order accesses `matrixB[k][j]` by column (cache-unfriendly stride), inflating wall time. `startSortTiming()` fires before `100,000` `random.nextInt()` calls. Recursive quicksort with last-element pivot risks `StackOverflowError` on pathological (sorted/reverse-sorted) inputs. | Crushes `computationalScore` toward `0–40`. |
| **`calculateStabilityScore()` (5% weight) & Display Consistency** [`PerformanceMetrics.java` L313–328, L489–497] | `BenchmarkEngine` calls `addSha1Sample()` / `addMd5Sample()` / `addAesSample()` per-iteration, boxing **~1,051,000 `Long` objects** into `ArrayList<Long>` per full run (500K SHA-512 + 500K MD5 + 50K AES + ~1K compression). Sample lists are **never cleared** between repeat runs — subsequent runs concatenate onto existing lists. `calculateStabilityScore()` correctly uses CV-based `normalizeInverse`, but operates on noisy per-op samples dominated by timer quantization and GC pauses. **Separately**, `getFormattedResults()` computes consistency display as `100 - (stdDev / 1000)` (comparing nanosecond stddev against an arbitrary 1000 divisor). | Stability score measures timer noise, not CPU jitter; memory leaks across repeat runs; display consistency metric is meaningless. |
| **`calculateMultiThreadingScore()` (5% weight)** [`PerformanceMetrics.java` L365–379] | Computes `singleThreadedTime = sha1TotalTime + md5TotalTime + matrixMultiplicationTime` and divides by `multiThreadedTime`, which only runs `SHA-512` across `N` threads. This is an apples-to-oranges ratio (summing three different single-threaded workloads vs. one multi-threaded workload). | Artificially inflates `multiThreadingScore` near **100** regardless of actual multi-core scaling. |
| **`runLoopOverheadTest()`** [`BenchmarkEngine.java` L218–235] | Accumulates into local variable `dummy` (never read → ART JIT dead-code elimination candidate). `loopOverheadTime` is stored but **never referenced** in `calculateComputationalScore()` (which only uses `matrixMultiplicationTime` and `sortingTime`). | Loop time is unreliable and entirely ignored in scoring. |
| **Dead RSA scaffolding** [`PerformanceMetrics.java` L29, L41, L52, L158–165, L234–236] | `RSA_ITERATIONS = 5000`, `rsaSamples` list, `startRsaTiming()`, `endRsaTiming()`, and `addRsaSample()` exist but are **never called** from `BenchmarkEngine`. However, `rsaTotalTime` field, `getRsaTotalTime()` getter are actively consumed by `PerformanceDatabaseHelper` (L246), `BenchmarkResult` (L89), and `BenchmarkApiClient` (L140). | Dead clutter in `PerformanceMetrics`; `rsaTotalTime` field + getter must remain (`0L`) for DB/API compatibility. |

**Net Effect:** Heavy weights (`efficiencyScore` 15%, `memoryScore` 20%, `computationalScore` 30%) are pulled toward the bottom, `multiThreadingScore` pegs near `100`, and `cryptoScore` sits around `45–65`, forcing `overallScore` into the **`45–55`** range across nearly all phones.

---

## 3. Scoring System Redesign (Exponential Positive-Skew Scale)

### 3.1 Two-Stage Normalization Pipeline

Every metric will be scored in two clean, testable stages in `PerformanceMetrics`:

1. **Stage 1 — Hardware Ratio Normalization (`x` in `[0.0, 1.0]`):**
   - **Throughput metrics (`ops/sec`, `MB/s`, `ops/ms/core`):** Smooth logarithmic ratio against calibrated floor/ceiling anchors:
     $$x = \operatorname{clamp}\!\left(\frac{\ln(\text{value} / v_{\min})}{\ln(v_{\max} / v_{\min})},\ 0,\ 1\right)$$
   - **Latency / Time metrics (`ms`):** Smooth inverse-log normalization so slow devices do not hit a harsh linear cliff while fast devices gain smooth separation:
     $$x = \operatorname{clamp}\!\left(\frac{\ln(t_{\text{worst}} / t)}{\ln(t_{\text{worst}} / t_{\text{best}})},\ 0,\ 1\right)$$

2. **Stage 2 — Concave Exponential Skew (`score` in `[0, 100]`):**
   Apply a normalized exponential saturation curve with curvature parameter $k > 0$ (exposed via `@VisibleForTesting static int applyExponentialSkew(double normalized, double k)`):
   $$\operatorname{skewExp}(x, k) = \text{round}\!\left(100 \cdot \frac{1 - e^{-k \cdot x}}{1 - e^{-k}}\right), \quad x \in [0, 1]$$

   Properties of $\operatorname{skewExp}(x, k)$:
   - Strictly monotonic: faster hardware **always** scores $\ge$ slower hardware.
   - Exact endpoints: $f(0) = 0$, $f(1) = 100$.
   - Smoothly rewards performance gains while shifting typical consumer devices into motivating ranges.

#### Calibration Curve Comparison ($k = 2.2$ recommended default)

| Normalized Hardware Tier (`x`) | Old Linear Score | Exponential ($k = 2.2$) | Exponential ($k = 2.5$) | Target Category |
| :--- | :---: | :---: | :---: | :--- |
| `0.20` (Low-end / Throttled) | 20 | **40** | 43 | `BELOW_AVERAGE` |
| `0.35` (Entry-level) | 35 | **60** | 64 | `AVERAGE` |
| `0.50` (Mid-range) | 50 | **75** | 78 | `GOOD` |
| `0.65` (Upper mid-range) | 65 | **86** | 88 | `GOOD` |
| `0.75` (Last-gen flagship) | 75 | **91** | 92 | `EXCELLENT` |
| `0.85` (Current flagship) | 85 | **95** | 96 | `EXCELLENT` |
| `0.95` (Top-tier / Cooled) | 95 | **99** | 99 | `EXCELLENT` |
| `1.00` (Reference ceiling) | 100 | **100** | 100 | `EXCELLENT` |

> **Recommendation:** Use **$k = 2.2$** (`SCORE_CURVE_K = 2.2`). It places mid-range hardware cleanly in `GOOD` (`70–85`) and flagships in `EXCELLENT` (`90–98`) while preserving `~10` points of separation across upper tiers (`0.70` → `88`, `0.85` → `95`, `0.95` → `99`).

### 3.2 Concrete Calibration Anchors (Stage 1 Reference Table)

These anchors will be defined as named constants in `PerformanceMetrics.java` and tuned during Phase 3 on-device verification (where $v_{\min}$ represents an entry-level / older ARMv7/A53 baseline and $v_{\max}$ represents a cooled flagship SoC ceiling):

| Metric | Type | Floor Anchor (`x = 0`) | Ceiling Anchor (`x = 1`) | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **SHA-512** (`sha1TotalTime`) | Throughput | `25,000 ops/sec` | `1,200,000 ops/sec` | Macro-batched mixed block (`40` batches) |
| **MD5** (`md5TotalTime`) | Throughput | `40,000 ops/sec` | `2,500,000 ops/sec` | Macro-batched (`40` batches, no per-op timer tax) |
| **AES-256** (`aesTotalTime`) | Throughput | `5,000 ops/sec` | `250,000 ops/sec` | `AES/CBC/PKCS5Padding` on 1 KB block (`25` batches) |
| **Matrix (`ikj`)** (`matrixMultiplicationTime`) | Inverse-Log Latency | `1,800 ms` | `25 ms` | Pre-allocated flattened/2D `ikj` multiply |
| **Sorting** (`sortingTime`) | Inverse-Log Latency | `120 ms` | `4 ms` | `Arrays.sort(int[])` on pre-filled array (`100K`) |
| **ALU/FP Loop** (`loopOverheadTime`) | Inverse-Log Latency | `250 ms` | `8 ms` | `blackhole`-protected ALU + FP loop |
| **Memory Bandwidth** (`memoryBytesTransferred / memoryBandwidthTime`) | Throughput | `1,000 MB/s` | `28,000 MB/s` | Measured across copy + strided + reduction passes |
| **Compression** (`compressionTime`) | Throughput | `60 ops/sec` | `1,500 ops/sec` | Right-sized 64 KB buffer (`200` iterations, `20` batches) |
| **Per-Core Efficiency** (Crypto + Compute composite / effective cores) | Throughput / Core | `1.0x` baseline | `18.0x` baseline | Effective cores capped at `min(cpuCores, 8)` |
| **Stability** (Mean macro-batch `CV`) | Inverse Linear/Log | `CV = 0.40` (40%) | `CV = 0.02` (2%) | Computed on primitive batch durations (`<= 40` per test) |
| **Multi-Threading** (`sha1TotalTime / multiThreadedTime`) | Speedup Ratio | `1.0x` (no scaling) | `max(1.5, min(cpuCores, 8) * 0.65)` | Compares identical `SHA-512` workload on 1 vs `N` threads |

### 3.3 Subscore Weights & Formulas (Contract Preserved)

Keep the existing overall weighting in `PerformanceMetrics.calculateScores()` (L250–276):
- `cryptoScore`: **25%** — Average of skewed `SHA-512`, `MD5`, and `AES` throughput scores.
- `computationalScore`: **30%** — Weighted blend of `Matrix Multiplication` (45%), `Sorting` (35%), and `ALU/FP Loop` (20%) using inverse-log latency + `skewExp` (falling back gracefully only if a legacy/partial run omits those tests).
- `memoryScore`: **20%** — Equal blend of true `Memory Bandwidth` (**`memoryBytesTransferred / memoryBandwidthTime`** in `MB/s` — using the new `memoryBytesTransferred` field that records exact bytes across all passes, **not** the static `MEMORY_TEST_SIZE` constant) and `Compression` throughput, both using log-throughput + `skewExp`.
- `efficiencyScore`: **15%** — Geometric/weighted normalized throughput per effective CPU core (`min(cpuCores, 8)`) across crypto + computational workloads + `skewExp`, replacing the broken `ns/op` linear `normalizeInverse` with tight ceilings.
- `stabilityScore`: **5%** — Mean macro-batch `CV` across collected primitive batch arrays (`sha1Batches`, `md5Batches`, `aesBatches`, `compressionBatches`), mapped via `[0.02, 0.40]` + `skewExp`.
- `multiThreadingScore`: **5%** — Parity-matched `sha1TotalTime / multiThreadedTime` speedup (same SHA-512 workload, single-thread vs. N-thread) normalized against core-count ceiling + `skewExp`.

---

## 4. Benchmark Engine Test Enhancements (`BenchmarkEngine.java`)

All improvements keep existing field names (`sha1TotalTime`, `md5TotalTime`, `aesTotalTime`, `loopOverheadTime`, `matrixMultiplicationTime`, `sortingTime`, `compressionTime`, `memoryBandwidthTime`, `multiThreadedTime`) and `totalTests = 9`:

### 4.1 Cross-Cutting Engine Fixes
1. **Reset Metrics & Batch Buffers Before Every Run:**
   - Add `metrics.resetForNewRun()` clearing all primitive batch arrays, timing fields, `memoryBytesTransferred`, all score fields, `lastClientRunId`, and `lastRemoteId`. Call it at the start of both `runComprehensiveBenchmark()` (before L70) and `runLegacyBenchmark()` (after L462).
2. **Deterministic Iteration-Based JIT Warm-up (Untimed):**
   - Run a fixed-iteration warm-up (e.g., `500` iterations of SHA-512/MD5, `100` AES ops, small `64×64` `ikj` matrix, `2,000`-element sort, `50,000` ALU ops) before Test 1 starts so run #1 does not pay cold ART interpreter penalties and warm-up is deterministic across all device speeds.
3. **Zero-Overhead Anti-DCE Blackhole (`private volatile long blackholeSink`):**
   - Accumulate checksums/results in a **local `long` variable** inside tight loops and write `blackholeSink ^= localAcc` **once at the end of each test method** (preventing ART dead-code elimination without paying `volatile` memory-barrier overhead per iteration inside hot loops).
4. **Separate Allocation & PRNG Setup from Timing:**
   - Replace `java.util.Random` (which uses a synchronized `AtomicLong` CAS under the hood, adding contention overhead) with a fast local **XorShift64** generator to initialize buffers/arrays **before** calling `metrics.start*Timing()`.
5. **Macro-Batch Primitive Sampling (Zero GC Pressure):**
   - Collect **25–40 macro-batch durations** per iterative test (`SHA-512`, `MD5`, `AES`, `Compression`) using fixed-size primitive `long[]` buffers instead of boxing `~1,051,000` individual `Long` objects into `ArrayList<Long>`. Eliminates GC interference, autoboxing overhead, and the memory leak caused by never-cleared sample lists.

### 4.2 Per-Test Enhancements

1. **Test 1 — SHA-512 (`runSha1Benchmark` → `sha1TotalTime`):**
   - Keep `MessageDigest.getInstance("SHA-512")` (preserve `sha1TotalTime` key for D1/SQLite compatibility).
   - Hash a deterministic `256 B` / `testString`-derived buffer across `40` timed macro-batches (`SHA1_ITERATIONS / 40` ops per batch), folding `digest[0]` into a local `long` accumulator; write `blackholeSink ^= localAcc` once at method end.
2. **Test 2 — MD5 (`runMd5Benchmark` → `md5TotalTime`):**
   - Same `40`-batch macro-timed pattern (`MD5_ITERATIONS / 40` ops per batch) + local accumulator → single `blackholeSink` write at end.
3. **Test 3 — AES-256 (`runAesBenchmark` → `aesTotalTime`):**
   - Encrypt a `1 KB` payload using pre-initialized `AES/CBC/PKCS5Padding` key + `IvParameterSpec` reused per batch across `25` timed batches (`AES_ITERATIONS / 25` ops per batch), folding output byte into local accumulator → single `blackholeSink` write.
4. **Test 4 — CPU ALU & FP Loop (`runLoopOverheadTest` → `loopOverheadTime`):**
   - Mixed 64-bit integer arithmetic, bit shifts/rotations, conditional branching, and floating-point multiply-add operations over `LOOP_ITERATIONS`, storing final `long intAcc ^ Double.doubleToRawLongBits(fpAcc)` into `blackholeSink`. **Now included in `calculateComputationalScore()`** at 20% weight (fixing the current bug where `loopOverheadTime` is collected but ignored).
5. **Test 5 — Matrix Multiplication (`runMatrixMultiplicationBenchmark` → `matrixMultiplicationTime`):**
   - Pre-allocate and XorShift64-fill `double[][]` (or flattened `double[]`) matrices **before** `startMatrixTiming()`.
   - Use cache-friendly **`ikj` loop order** (`double a = matrixA[i][k]; for (j) result[i][j] += a * matrixB[k][j];`), keeping matrix size calibrated (`384×384` or `512×512` `ikj`) so execution takes `~25–1800 ms` across devices. Fold `result[0][0]` + diagonal trace into `blackholeSink`.
6. **Test 6 — Sorting (`runSortingBenchmark` → `sortingTime`):**
   - Pre-fill `int[SORT_ARRAY_SIZE]` with XorShift64 values **before** `startSortTiming()`.
   - Replace the recursive quicksort (L299–325, vulnerable to `StackOverflowError` on pathological inputs) with **`Arrays.sort(array)`** (JDK dual-pivot quicksort — deterministic, no stack overflow risk, measures real ART sort throughput). Fold `array[0] ^ array[size - 1]` into `blackholeSink`.
7. **Test 7 — Compression (`runCompressionBenchmark` → `compressionTime`):**
   - Pre-build a `64 KB` mixed text/pseudo-random payload **before** timing.
   - Reuse a pre-sized `ByteArrayOutputStream(65536)` across iterations. Create `Deflater` once, call `deflater.reset()` per iteration, and **`deflater.end()` in a `finally` block** to prevent native zlib memory leaks (current code at L327–358 creates a new `GZIPOutputStream` per iteration without explicit native cleanup). Run `200` iterations (`20` batches × `10` ops), recording compressed output byte count into `blackholeSink`.
8. **Test 8 — Memory Bandwidth (`runMemoryBandwidthBenchmark` → `memoryBandwidthTime`):**
   - Compute OOM-safe working set from heap headroom: `min(24 * 1024 * 1024, (int) (Runtime.getRuntime().maxMemory() / 10))` rounded down to 4-byte `int` elements (current code hardcodes `MEMORY_TEST_SIZE / 4 = 12,500,000` ints = ~100 MB total for two arrays, risking OOM on Android Go / 1 GB heap devices).
   - Pre-allocate and XorShift64-fill `int[] src` and `int[] dst` **before** `startMemoryTiming()`.
   - Time only the memory passes: `System.arraycopy(src, 0, dst, 0, size)`, strided read/write pass, and sequential reduction sum. Write `blackholeSink += sum` at end (fixing current DCE risk where `sum` is computed but never read). Store exact `memoryBytesTransferred` via `metrics.setMemoryBytesTransferred(totalBytes)` covering all passes so `calculateMemoryScore()` computes true `MB/s`.
9. **Test 9 — Multi-Threaded (`runMultiThreadedBenchmark` → `multiThreadedTime`):**
   - Execute the **exact same total `SHA-512` work** (`SHA1_ITERATIONS` on the same `testString`-derived buffer) split evenly across `numThreads = Math.min(metrics.getCpuCores(), 8)` threads using a `CountDownLatch` start barrier (excluding pool startup from timed work) and a completion latch, then compare directly against `sha1TotalTime` (same algorithm, same iteration count — fixing the current apples-to-oranges comparison at L375 where `singleThreadedTime = sha1TotalTime + md5TotalTime + matrixMultiplicationTime`).

### 4.3 Formatting & Reporting Updates
- Fix [`PerformanceMetrics.getFormattedResults()`](app/src/main/java/net/dotevolve/benchmark/core/PerformanceMetrics.java) and `getDetailedTimingInfo()`:
  - Label `"SHA-512"` accurately instead of `"SHA-1"` (currently mislabeled at L443, L510).
  - Include `AES` and `ALU / Loop` timing lines and accurate `MB/s` (using `memoryBytesTransferred`) for Memory Bandwidth in `getFormattedResults()`.
  - Fix consistency display (L489–497) to use `Math.max(0.0, 100.0 - cv * 100.0)` (clamped `[0, 100]`) instead of the current `100 - (calculateStandardDeviation(samples) / 1000)` which compares nanosecond standard deviation against an arbitrary `1000` divisor.

### 4.4 Dead Code Cleanup
- **Remove** from `PerformanceMetrics.java`:
  - `RSA_ITERATIONS` constant (L41)
  - `rsaSamples` list field (L52)
  - `startRsaTiming()` method (L158–160)
  - `endRsaTiming()` method (L162–165)
  - `addRsaSample()` method (L234–236)
- **Preserve** (required by `PerformanceDatabaseHelper`, `BenchmarkResult`, `BenchmarkApiClient`):
  - `rsaTotalTime` field (L29) — remains `0L` (never assigned since no RSA test runs)
  - `getRsaTotalTime()` getter (L731) — returns `0L`

---

## 5. Implementation Roadmap & Verification Plan

> **Branching (`AGENTS.md §3.1`):** Implement in a dedicated branch `feat/benchmark-engine-v2-enhancements` branched off `origin/dev`, targeting `dev` via PR.

### Phase 1: Scoring Math & Reset Hygiene (`PerformanceMetrics.java`)
1. Implement `normalizeLogThroughput(double value, double vMin, double vMax)`, `normalizeLogLatency(double timeMs, double tWorst, double tBest)`, calibration anchor constants (§3.2 table), and `@VisibleForTesting static int applyExponentialSkew(double normalized, double k)` with `SCORE_CURVE_K = 2.2`.
2. Add `resetForNewRun()` clearing primitive batch sample buffers, all timing fields, `memoryBytesTransferred`, all 7 score fields, `lastClientRunId`, and `lastRemoteId`.
3. Add `memoryBytesTransferred` field with getter/setter for accurate MB/s calculation.
4. Clean up dead RSA scaffolding (`RSA_ITERATIONS`, `rsaSamples`, `startRsaTiming()`, `endRsaTiming()`, `addRsaSample()`) while **preserving `rsaTotalTime` and `getRsaTotalTime()`** per §4.4.
5. Update all 6 subscore calculations (`crypto`, `computational`, `memory`, `efficiency`, `stability`, `multiThreading`) with calibrated anchors and two-stage normalization.
6. Fix `getFormattedResults()` / `getDetailedTimingInfo()` labels (SHA-512, AES, ALU) and consistency formula.

### Phase 2: Benchmark Kernel & Harness Upgrades (`BenchmarkEngine.java`)
1. Add `private volatile long blackholeSink`, deterministic iteration-based JIT warm-up helper, and XorShift64 pre-fill helper method.
2. Move all allocations and PRNG fills outside `start*Timing()` calls for every test.
3. Convert per-op `nanoTime()` / `addSha1Sample()` loops to macro-batch primitive `long[]` sampling (`25–40` batches per test).
4. Upgrade:
   - `ikj` Matrix Multiplication (cache-friendly loop order)
   - `Arrays.sort(int[])` replacement for recursive quicksort
   - `Deflater` with `reset()` per iteration + `end()` in `finally`
   - OOM-safe Memory Bandwidth with `memoryBytesTransferred`
   - Parity-matched Multi-Threaded SHA-512 (identical workload comparison)

### Phase 3: Automated Testing & Hardware Calibration
1. **Net-New Unit Tests (create directory `app/src/test/java/net/dotevolve/benchmark/core/`):**
   - `PerformanceMetricsTest.java`:
     - Verify `applyExponentialSkew` bounds (`0` → `0`, `1` → `100`, `0.5` → `75`), strict monotonicity across 100 samples, and concave positive skew ($f''(x) < 0$).
     - Verify hardware-tier separation across simulated Entry (`low`), Mid-range (`mid`), and Flagship (`high`) timing profiles — confirming `mid` lands `~72–82` and `high` lands `~90–97` (not `~50`).
     - Verify `resetForNewRun()` zeroes all timing fields, clears batch sample buffers, and nulls `lastClientRunId` / `lastRemoteId`.
     - Verify `calculateMemoryScore()` uses `memoryBytesTransferred` (not `MEMORY_TEST_SIZE`).
     - Verify `calculateMultiThreadingScore()` rewards real speedup (`1x` → low, `~2.5x` → mid, `~4x+` → high) and does not inflate when `multiThreadedTime >= sha1TotalTime`.
   - `BenchmarkEngineTest.java`:
     - Verify `runComprehensiveBenchmark()` and `runLegacyBenchmark()` populate non-zero timings, bounded `0..100` scores, and `ENGINE_VERSION == "2.0.0"`.
     - Verify `rsaTotalTime` remains `0L` after both run types.
2. **Pre-Commit Gradle Gates (`AGENTS.md §2`):**
   - `./gradlew assembleDebug test lintDebug` — 0 errors, 100% passing unit tests.
3. **On-Device Regression Comparison & Calibration:**
   - Run the **current** engine and the **enhanced** engine side-by-side on 2–3 device tiers (entry-level, mid-range, flagship) and document the before/after score distributions in the PR description.
   - Verify no GC stalls, `StackOverflowError`, or OOM in Logcat.
   - Confirm `POST /api/v1/benchmarks` upload to `https://benchmark-dev.dotevolve.net` preserves `benchmarkVersion: "2.0.0"` and `rsaTotalTime: 0`.
   - Tune calibration anchors (§3.2) if on-device distributions deviate from target tier ranges.
