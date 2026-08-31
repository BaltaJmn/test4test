This is a Kotlin Multiplatform project targeting Android and Web.

## Setup

1. Copy `local.properties.example` to `local.properties`.
2. Fill in `supabase.url` and `supabase.anonKey` from the [Supabase dashboard](https://supabase.com/dashboard) → Settings → API.
3. Never commit `local.properties` (already gitignored) or the `service_role` key anywhere in this repo — that one only lives in Supabase Edge Function secrets.

## Database

The schema lives in [`supabase/migrations`](./supabase/migrations) and is already applied to the
hosted project. Authorization is enforced entirely by RLS, because clients talk to PostgREST
directly with the public anon key — see the migration comments for what each policy guards.

`is_premium` (paid tier) and `is_admin` (moderation) sit behind a column-level grant on `profiles`,
so users cannot set them on themselves. Flip `is_admin` from the Supabase dashboard.

## Project layout

* [/shared](./shared/src) holds the code shared between Android and Web.
  - [commonMain](./shared/src/commonMain/kotlin) is common to both targets.
  - Other folders compile only for the platform named in the folder.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Web app:
  - Wasm target (faster, modern browsers): `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
  - JS target (slower, supports older browsers): `./gradlew :webApp:jsBrowserDevelopmentRun`

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- Web tests:
  - Wasm target: `./gradlew :shared:wasmJsTest`
  - JS target: `./gradlew :shared:jsTest`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).