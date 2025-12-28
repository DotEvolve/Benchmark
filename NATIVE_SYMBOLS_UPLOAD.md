# Native Debug Symbols Upload Guide

## Overview

This app includes native libraries (from Firebase Crashlytics NDK and other dependencies), so Google Play Console requires native debug symbols to be uploaded for better crash and ANR analysis.

## Automatic Solution (Recommended)

The build is now configured to generate native debug symbols automatically. When you build a release bundle, the symbols will be generated in:

```
app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip
```

## Upload Methods

### Method 1: Upload via Play Console UI (Easiest)

1. Build your release bundle:
   ```bash
   ./gradlew bundleRelease
   ```

2. Locate the symbols file:
   ```
   app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip
   ```

3. Go to Google Play Console → Your App → Release → Production (or your track)

4. Click on the version you just uploaded

5. Scroll down to "Native debug symbols" section

6. Click "Upload" and select the `native-debug-symbols.zip` file

### Method 2: Upload via Play Console API (Automated)

You can automate the upload using the Google Play Developer API. Here's a script example:

```bash
#!/bin/bash
# upload-symbols.sh

BUNDLE_PATH="app/build/outputs/bundle/release/app-release.aab"
SYMBOLS_PATH="app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip"
PACKAGE_NAME="net.dotevolve.benchmark"
VERSION_CODE="17"  # Update this to match your version

# Upload symbols using Play Console API
# Requires: pip install google-api-python-client google-auth-httplib2 google-auth-oauthlib
python3 upload_symbols.py \
  --package-name "$PACKAGE_NAME" \
  --version-code "$VERSION_CODE" \
  --symbols-file "$SYMBOLS_PATH"
```

### Method 3: Include in Bundle (Alternative)

You can also configure the bundle to automatically include symbols, but this increases bundle size. The current configuration generates symbols separately, which is the recommended approach.

## Verification

After uploading, verify in Play Console:
1. Go to Release → Your version
2. Check "Native debug symbols" section
3. Should show "Uploaded" with timestamp

## Troubleshooting

### Symbols not generated?

1. Ensure you're building a release variant:
   ```bash
   ./gradlew bundleRelease
   ```

2. Check that `ndk.debugSymbolLevel = "FULL"` is set in `build.gradle.kts`

3. Verify the output directory exists:
   ```bash
   ls -la app/build/outputs/native-debug-symbols/release/
   ```

### Upload fails?

1. Ensure the symbols file matches the bundle version code
2. Check file size (should be a few MB, not empty)
3. Verify you have the correct permissions in Play Console

## CI/CD Integration

For automated builds, add this to your CI pipeline:

```yaml
# Example GitHub Actions
- name: Build Release Bundle
  run: ./gradlew bundleRelease

- name: Upload Native Symbols
  uses: r0adkll/upload-google-play@v1
  with:
    serviceAccountJsonPlainText: ${{ secrets.GOOGLE_PLAY_SERVICE_ACCOUNT }}
    packageName: net.dotevolve.benchmark
    releaseFiles: app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip
    track: production
```

## Notes

- Symbols are generated automatically when building release bundles
- Symbols should be uploaded for each new release
- Old symbols remain available for historical crash analysis
- Symbol files are typically 1-5 MB in size



