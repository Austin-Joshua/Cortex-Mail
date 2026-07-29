# Velocity — native mobile client

Flutter client for the same Spring Boot backend the web app uses. Shares the
design system, the category palette, and the Velocity Score formula with
`../frontend`, so the two clients cannot drift apart.

## Status

| | |
|---|---|
| `flutter analyze` | ✅ clean |
| `flutter test` | ✅ 17 passing |
| `flutter build web` | ✅ builds |
| `flutter build apk` | ⚠️ **not verified here** — see below |

The APK was never built in the environment this was written in: `dl.google.com`
is blocked by network policy there, so the Android SDK could not be installed.
Everything that does not require the Android toolchain was verified. Build the
APK on a machine with Android Studio before trusting it on a device.

## Running it

The backend URL is injected at build time — never hardcoded, never committed.

```bash
flutter pub get

# Android emulator (10.0.2.2 is the host's localhost from inside the emulator)
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080

# Physical device on the same network
flutter run --dart-define=API_BASE_URL=http://192.168.1.x:8080

# Release build
flutter build apk --release --dart-define=API_BASE_URL=https://your-backend.onrender.com
```

Default if unset: `http://10.0.2.2:8080`.

## Signing in

The backend's Google OAuth is a browser flow that returns a JWT. Native deep
linking is not wired up yet, so the sign-in screen accepts a pasted token:
sign in on the website, copy the token from Settings, paste it here. It is
stored with `shared_preferences` and sent as a bearer token.

This is deliberately explicit rather than a fake "Sign in with Google" button
that cannot complete.

## What is shared with the web client

- **Palette** — `lib/theme/tokens.dart` ports `styles/bento.css` value for
  value, including gold inverting between themes so text contrast holds.
- **Category hues** — mirrors `utils/catColors.ts`.
- **Velocity Score** — `DashboardSummary.velocityScore` reproduces the web
  formula exactly, and a test pins them to the same shared case. A score that
  differed between clients would be worse than no score.
- **Endpoints** — paths mirror `frontend/src/api/*`.

## Responsive behaviour

One layout serves every size rather than separate phone and tablet builds:

| Width | Bento columns | Navigation |
|---|---|---|
| < 380 | 1 | bottom bar |
| 380–719 | 2 | bottom bar |
| >= 720 | 2 | **side rail** |
| >= 900 | 4 | side rail |

Covered by tests in `test/velocity_test.dart`.

## Security

- No API keys in this client. Gemini and Claude keys stay server-side; the
  only credential here is the user's own JWT.
- `API_BASE_URL` is a build-time define, so no environment file ships in the
  repo.
- `INTERNET` is declared in the **main** manifest. Flutter's template only
  puts it in the debug manifest, which lets a release APK install and launch
  while every network call fails.

## Not done yet

- Native Google OAuth via deep link (token paste stands in)
- Brain Q&A screen — `api.askBrain()` exists and is wired, no UI yet
- Push notifications
- Offline cache; the app is online-only today
