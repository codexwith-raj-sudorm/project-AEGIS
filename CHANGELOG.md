# Changelog

## 0.3.0 — 2026-09-13

- Encrypt profiles, sessions, recovery verifiers, exit state, and token balances with an Android Keystore AES-GCM key.
- Migrate legacy plaintext values to encrypted envelopes after successful reads.
- Add persistent exponential backoff for owner recovery attempts.
- Add an independent crash-loop watchdog and 15-minute Fail-Open Mode.
- Integrate watchdog checks into app interception and gate startup.

## 0.2.0 — 2026-09-13

- Add per-session PBKDF2 owner recovery codes.
- Add persisted delayed Conscious Override flow.
- Add age-gated accountability profiles and Hard Truth messaging.
- Persist challenge streaks and Sincerity Point balances.
- Add GitHub Actions unit-test and debug-APK build pipeline.

## 0.1.0 — 2026-09-13

- Establish the native Kotlin and Jetpack Compose Android project.
- Add learner profiles, focus-session enrollment, target-app detection, cognitive gate, and local Learning Mode retrieval.
