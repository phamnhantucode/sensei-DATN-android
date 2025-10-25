# KSP Version Compatibility Guide

## Current Configuration

The project is using Kotlin 2.2.0, which is very new. KSP versions need to match Kotlin versions closely.

## Tested KSP Versions (in order of preference)

Try these versions in `gradle/libs.versions.toml` until one works:

### Option 1: KSP 2.0.20 (Currently Set)
```toml
ksp = "2.0.20-1.0.25"
```

### Option 2: KSP 2.0.21
```toml
ksp = "2.0.21-1.0.28"
```

### Option 3: Downgrade Kotlin to 2.0.20 (Most Stable)
If KSP versions don't work, downgrade Kotlin:

```toml
[versions]
kotlin = "2.0.20"
ksp = "2.0.20-1.0.25"
```

This is the most stable combination and should work perfectly.

## Build Commands

After changing versions, sync and rebuild:

### Windows:
```cmd
gradlew.bat clean
gradlew.bat assembleDebug
```

### Linux/Mac:
```bash
./gradlew clean
./gradlew assembleDebug
```

## If Build Still Fails

If you continue to get KSP errors:

### Solution 1: Use Kapt Instead of KSP (Temporary)

Edit `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")  // Replace KSP with Kapt
}

dependencies {
    // Room for local database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)  // Change ksp to kapt
}
```

Kapt is older but more stable. KSP is faster but requires exact version matching.

### Solution 2: Recommended - Downgrade to Kotlin 2.0.20

This is the most reliable solution:

**`gradle/libs.versions.toml`:**
```toml
[versions]
kotlin = "2.0.20"
ksp = "2.0.20-1.0.25"
```

Kotlin 2.0.20 is stable and has full KSP support.

## Why This Matters

- **Room** requires annotation processing (KSP or Kapt)
- **KSP** is 2x faster than Kapt but requires version matching
- **Kapt** is slower but more compatible

## My Recommendation

**Downgrade to Kotlin 2.0.20** for maximum stability:

1. Edit `gradle/libs.versions.toml`:
   ```toml
   kotlin = "2.0.20"
   ksp = "2.0.20-1.0.25"
   ```

2. Sync Gradle

3. Clean and rebuild:
   ```
   gradlew.bat clean assembleDebug
   ```

This combination is tested and stable with Room 2.6.1.
