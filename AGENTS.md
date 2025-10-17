## Android SDK Path Instructions

- When running Gradle from WSL, temporarily set `sdk.dir=/mnt/d/Users/ASUS/AppData/Local/Android/Sdk` in `local.properties`.
- After the build completes, restore the Windows path `sdk.dir=D:\\Users\\ASUS\\AppData\\Local\\Android\\Sdk`.

## Neon Configuration

- Add `NEON_API_KEY=<jwt-token>` to `local.properties`. This token needs SQL query access for the project.
- (Optional) Override `NEON_API_URL` in `local.properties` if you use a different Neon endpoint; otherwise the default endpoint in `app/build.gradle.kts` is used.
- The app first tries to fetch a Clerk session JWT and reuse Neon’s Clerk auth provider; the `NEON_API_KEY` acts as a fallback when no session token is available.
