# Test4Test

Google Play asks a personal developer account for 12 testers over 14 continuous
days before it lets the app go to production. Test4Test is where Android
developers find each other to cover that: you list your app, other developers
join your closed test, you join theirs.

Android only, written with Kotlin Multiplatform and Compose Multiplatform so the
UI and data layer live in `:shared`. The web version was dropped in 1.6 and
everything happens in the app. The only thing left on the web is the privacy
policy at [testers.baltajmn.dev/privacy](https://testers.baltajmn.dev/privacy),
because Play and Google sign-in require a public URL for it.

## Setup

1. Copy `local.properties.example` to `local.properties` and fill it in. That
   file documents every key it takes: the Supabase project, the RevenueCat
   public Android key (optional, without it the subscribe button is not drawn)
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

The Supabase project is `tneuaccyzausvszpplms` since 26-10-2026. The first one
was paused for inactivity on the free plan and then lost, together with every
account and app in it; `keep-alive.yml` pings it daily so it does not happen
again.

## Database

The schema lives in [`supabase/migrations`](./supabase/migrations) and is already
applied to the hosted project. Everything applied to the database goes through
that directory: the first project lost the streak, reports and reciprocity
because they were applied by hand and never committed. Authorization is enforced
entirely by RLS, because clients talk to PostgREST directly with the public anon
key: see the migration comments for what each policy guards.

Roles:

| Who | Can |
|---|---|
| anon | Nothing. Every read and write needs a session. |
| Signed-in user | Read everything; list 1 app at a time, edit its name and links, delete it; join and leave tests as themselves; comment as themselves; report; delete their own account. |
| Subscriber (`is_premium`) | The same, with no limit on apps. Only the RevenueCat webhook writes the flag. |
| Admin (`is_admin`) | Also delete any app or comment, and read and clear reports. Set from the dashboard. |

`is_premium` and `is_admin` sit behind a column-level grant on `profiles`, and
`apps` only lets owners update name and links, so nobody can move their own
streak or hand an app to someone else.

Proof of install: `app_testers.verified_at` is only written by
`confirm_installed()`, with the server clock, and the Android client calls it
on every resume for the apps it sees installed from Google Play. Owners see the
result per tester through `app_tester_status`.

The feed reads `apps_with_followers`, a view that adds the tester count, how
many of them were seen with the app installed, the streak day and whether the
owner reciprocates. It carries `security_invoker=true` so RLS still applies to
whoever queries it: keep that option on any migration that replaces the view.

[`supabase/tests/rls_test.sql`](./supabase/tests/rls_test.sql) covers every
rule above inside a transaction that is rolled back. Run it after any change to
a policy, grant or view.

## Project layout

* [/shared](./shared/src) holds the UI and data layer.
  - [commonMain](./shared/src/commonMain/kotlin) is platform-agnostic.
  - [androidMain](./shared/src/androidMain/kotlin) has billing, the install
    check and the system back handler.
* [/androidApp](./androidApp) is the Android entry point: manifest, the
  `test4test://login` deep link and signing.
* [/web](./web) holds only the privacy policy pages.
* [/branding](./branding) holds the Play Store listing copy.
* [/docs](./docs) holds the runbook for the external services.

### Translations

`shared/src/commonMain/composeResources` carries six locale folders: `values`
(English, the default), `values-es`, `values-pt-rBR`, `values-hi`, `values-id`
and `values-in`. The last two are byte-identical on purpose: `in` is the old
ISO code for Indonesian and Android still resolves it on older devices. Any
string added to one has to be added to all six or the build fails.

### Running the apps

- Android app: `./gradlew :androidApp:assembleDebug`
- Tests: `./gradlew :shared:testAndroidHostTest`

Before pushing, run exactly what CI runs:

```
./gradlew :androidApp:assembleDebug :androidApp:lintDebug :shared:testAndroidHostTest
```

## Releasing

Four workflows live in [`.github/workflows`](./.github/workflows):

- `android-ci.yml` runs on every push and pull request to `main`.
- `deploy-web.yml` deploys [`web/`](./web) to the Cloudflare Worker when it
  changes on `main`. Nothing is built.
- `keep-alive.yml` pings Supabase once a day so the free plan never pauses it.
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
