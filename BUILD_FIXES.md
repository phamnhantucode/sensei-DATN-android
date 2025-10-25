# Build Configuration Fixes

## KSP Plugin Configuration

Fixed the KSP (Kotlin Symbol Processing) plugin error by properly configuring it in the version catalog.

### Changes Made:

#### 1. `gradle/libs.versions.toml`
Added KSP version that matches Kotlin 2.2.0:
```toml
[versions]
ksp = "2.2.0-1.0.29"

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

#### 2. `build.gradle.kts` (root)
Applied KSP plugin at project level:
```kotlin
plugins {
    alias(libs.plugins.ksp) apply false
}
```

#### 3. `app/build.gradle.kts`
Applied KSP plugin to app module:
```kotlin
plugins {
    alias(libs.plugins.ksp)
}
```

### Why This Matters

KSP is required for Room database annotation processing. The version must match the Kotlin version for compatibility:
- Kotlin: 2.2.0
- KSP: 2.2.0-1.0.29

### Dependencies Using KSP

```kotlin
ksp("androidx.room:room-compiler:2.6.1")
```

This processes Room annotations (`@Entity`, `@Dao`, `@Database`) at compile time to generate database code.

## Build Command

To build the project:
```bash
./gradlew assembleDebug
```

or on Windows:
```cmd
gradlew.bat assembleDebug
```
