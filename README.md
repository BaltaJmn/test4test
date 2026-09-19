# Test4Test

Google Play asks a personal developer account for 12 testers over 14 continuous
days before it lets the app go to production. Test4Test is where Android
developers find each other to cover that: you list your app, other developers
join your closed test, you join theirs.

Kotlin Multiplatform and Compose Multiplatform, one codebase for Android and
Web. The web build is functionally identical to Android; the in-app purchase is
the only thing that exists on Android alone. Live at
[testers.baltajmn.dev](https://testers.baltajmn.dev).

## Setup

1. Copy `local.properties.example` to `local.properties` and fill it in. That
   file documents every key it takes: the Supabase project, the RevenueCat
   public Android key (optional, without it the purchase button is not drawn)
   and the upload keystore (optional, without it the release build comes out
   unsigned).
2. `local.properties` is gitignored and stays that way. So does
   `keystore/upload.jks`.

Everything that lives outside this repo (Google Cloud, Play Console, RevenueCat,
Supabase and Cloudflare) is written down in
[docs/servicios-externos.md](./docs/servicios-externos.md), together with the
mistakes each of those dashboards already cost us. Read it before touching any
of them.

The Supabase `service_role` key is not in this repo and must never be: it only
lives in the Supabase Edge Function secrets. The `anon` key is safe to hold
locally, it is protected by RLS and designed to ship inside clients.

Gradle generates `SupabaseConfig` from those values into `shared/build/generated`,
so every target reads them without a platform-specific mechanism. The build
fails with an explicit message if they are missing. CI has no
`local.properties`, so it passes `SUPABASE_URL`, `SUPABASE_ANON_KEY` and
`REVENUECAT_ANDROID_KEY` as environment variables instead: the env vars win when
both are present.

## Database

The schema lives in [`supabase/migrations`](./supabase/migrations) and is already
applied to the hosted project. Authorization is enforced entirely by RLS,
because clients talk to PostgREST directly with the public anon key: see the
migration comments for what each policy guards.

`is_premium` (paid tier) and `is_admin` (moderation) sit behind a column-level
grant on `profiles`, so users cannot set them on themselves. Flip `is_admin`
from the Supabase dashboard.

The feed reads `apps_with_followers`, a view that adds the tester count, the
days at 12 testers and whether the owner reciprocates. It carries
`security_invoker=true` so RLS still applies to whoever queries it: keep that
option on any migration that replaces the view.

## Project layout

* [/shared](./shared/src) holds the code shared between Android and Web.
  - [commonMain](./shared/src/commonMain/kotlin) is common to both targets.
  - Other folders compile only for the platform named in the folder.
* [/androidApp](./androidApp) is the Android entry point and the only module
  that knows about billing.
* [/webApp](./webApp) is the Web entry point. It depends on `compose.ui` and
  `:shared`, nothing else: anything that needs `foundation` or `material3` has
  to live in `:shared`.
* [/branding](./branding) holds the Play Store listing copy.
* [/docs](./docs) holds the runbook for the external services.

### Translations

`shared/src/commonMain/composeResources` carries six locale folders: `values`
(English, the default), `values-es`, `values-pt-rBR`, `values-hi`, `values-id`
and `values-in`. The last two are byte-identical on purpose: `in` is the old
ISO code for Indonesian and Android still resolves it on older devices. Any
string added to one has to be added to all six or the build fails.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You
can also use these commands and options:

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

Before pushing, run exactly what CI runs:

```
./gradlew :androidApp:assembleDebug :androidApp:lintDebug :shared:testAndroidHostTest
```

Note there is no `:shared:compileDebugKotlinAndroid` task in this project. To
check that a change compiles for every target without running the whole CI line:

```
./gradlew :shared:testAndroidHostTest :shared:compileKotlinWasmJs :shared:compileKotlinJs
```

## Releasing

Three workflows live in [`.github/workflows`](./.github/workflows):

- `android-ci.yml` runs on every push and pull request to `main`.
- `deploy-web.yml` builds the wasm distribution and deploys it to the Cloudflare
  Worker on every push to `main`. It strips the source maps first, because they
  carry the full Kotlin source and browsers only fetch them with devtools open.
- `release.yml` ships the Android bundle to Play. It delegates to
  `BaltaJmn/ci/.github/workflows/android-play-release.yml`, which is shared with
  another app, so anything that changes the signing or upload sequence has to be
  changed there.

To publish a build:

1. Bump `versionCode` and `versionName` in
   [`androidApp/build.gradle.kts`](./androidApp/build.gradle.kts). Play rejects a
   bundle whose `versionCode` it has already seen, so every upload burns one.
2. Push to `main`.
3. `gh workflow run release.yml --ref main -f track=alpha`, or push a `v*` tag,
   which defaults to the same track.
4. Write the release notes by hand in Play Console. The workflow does not pass a
   `whatsnew-dir`, so a release uploaded without this step reaches testers with
   no notes at all.

The service account the upload runs as, the permissions it needs and the rest of
the Play Console setup are in
[docs/servicios-externos.md](./docs/servicios-externos.md).

Only the **closed** testing track counts toward the 12 testers and 14 continuous
days that Play requires. Internal testing does not, whatever the tester count
says there.
