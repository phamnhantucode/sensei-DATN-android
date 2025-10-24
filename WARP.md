# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Project overview
- Android app built with Kotlin, Jetpack Compose, and Navigation Compose. Single-Activity (`MainActivity`) with a composable navigation graph (`ui/navigation`).
- Authentication via an embedded Clerk SDK (`com/clerk/api`). `MyApp` initializes Clerk using a publishable key from BuildConfig.
- Data for industry insights comes from Neon (Postgres REST API). If missing/stale, it uses Google Gemini to generate insights and writes them back to Neon (`data/industry/IndustryInsightsRepository.kt`).
- User profile syncing and updates are handled via `data/neon/NeonUserService.kt` with OkHttp.
- Feature screens live under `ui/*` (login, onboarding, industry insights, resume builder, interview prep, cover letter, account settings). Each has an accompanying ViewModel where applicable.
- Prisma schema (`prisma/schema.prisma`) documents the Neon/Postgres data model but is not used at runtime by the Android app.

Common commands (Windows PowerShell)
- Build debug APK
```bash path=null start=null
./gradlew.bat assembleDebug
```
- Install debug on a connected device/emulator
```bash path=null start=null
./gradlew.bat installDebug
```
- Run unit tests (local JVM)
```bash path=null start=null
./gradlew.bat testDebugUnitTest
```
- Run a single unit test (class or method)
```bash path=null start=null
# Class
./gradlew.bat testDebugUnitTest --tests "com.phamnhantucode.aicareercoach.ExampleUnitTest"
# Specific method
./gradlew.bat testDebugUnitTest --tests "com.phamnhantucode.aicareercoach.ExampleUnitTest.someMethod"
```
- Run Android instrumented tests (on device/emulator)
```bash path=null start=null
./gradlew.bat connectedDebugAndroidTest
```
- Run a single instrumented test
```bash path=null start=null
# By class
./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.phamnhantucode.aicareercoach.ExampleInstrumentedTest
# By package
./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.phamnhantucode.aicareercoach
```
- Lint (Android Lint)
```bash path=null start=null
./gradlew.bat lint
# Or variant-specific
./gradlew.bat lintDebug
```
- Release build
```bash path=null start=null
./gradlew.bat assembleRelease
```

Required local configuration
These BuildConfig values are sourced from `local.properties`. Create or update it at the project root with the following keys as needed.
```properties path=null start=null
# Clerk (publishable key is embedded via build.gradle by default)
# Optional overrides for data integrations
NEON_API_URL=https://<your-neon-endpoint>/neondb/rest/v1
NEON_API_KEY=<optional-bearer-token>
NEON_DB_ROLE=<optional-role-for-basic-auth>
NEON_DB_PASSWORD=<optional-password-for-basic-auth>
GEMINI_API_KEY=<your-gemini-api-key>
```
Notes on auth and data flow
- Clerk initialization: `MyApp` calls `Clerk.initialize(this, BuildConfig.CLERK_PUBLISHABLE_KEY)`.
- Deep links for OAuth are set in `AndroidManifest.xml` with scheme `aicareercoach://` for `oauth-callback` and `home`.
- Neon auth resolution order for repository/services:
  - Prefer Clerk session JWT (`Clerk.session.fetchToken()`), else
  - Fallback to `NEON_API_KEY` (Bearer), else
  - Fallback to Basic auth using `NEON_DB_ROLE`/`NEON_DB_PASSWORD`.
- Industry insights loading (`IndustryInsightsRepository`):
  - Requires a signed-in user and an industry set during onboarding; loads existing Neon record if fresh.
  - If missing/stale (based on `nextUpdate` or 7-day window), calls Gemini (`gemini-2.5-flash`) to generate JSON and upserts to Neon.

Architecture map (big picture)
- Entry: `MainActivity` -> `AppNavigation()` sets NavHost and routes (`ui/navigation`).
- Auth: `ui/login/*` + Clerk SDK (`com/clerk/api`), with `LoginViewModel` orchestrating sign-in/up, verification, and Neon sync.
- Onboarding: `ui/onboarding/*` collects user’s industry and profile; persisted to Neon via `NeonUserService.updateUserProfile`.
- Industry Insights: `ui/industryinsights/*` + `IndustryInsightsViewModel` uses `IndustryInsightsRepository` to read Neon and optionally regenerate via Gemini; maps records to UI models.
- Resume Builder: `ui/resumebuilder/*` + `ResumeBuilderViewModel` manages local state and exports via `ResumeExporter`.
- Interview Prep: `ui/interviewprep/*` provides quiz and essay flows with in-memory scoring/state.
- Cover Letter: `ui/coverletter/*` (and `editor/`) manages entries and navigation with query-encoded args.

Android/Gradle specifics
- Kotlin JVM target 17; Compose enabled via `org.jetbrains.kotlin.plugin.compose`.
- Compose BOM is used; Navigation Compose `2.7.6`.
- Testing deps: JUnit 4 for unit tests; AndroidX JUnit/Espresso for instrumented tests.
- Packaging excludes common META-INF artifacts in `android { packaging { resources { ... } } }`.

Operational tips specific to this repo
- If industry insights fail with “Gemini API key is not configured” or “Neon API URL is not configured”, ensure `local.properties` contains `GEMINI_API_KEY` and `NEON_API_URL`.
- If the insights screen shows onboarding-required errors, complete the onboarding flow to set `industry` before retrying.
- When adding new Neon fields, note the repository’s flexible JSON parsing and column-naming strategies; update Prisma schema accordingly for long-term consistency.
