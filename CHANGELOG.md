# Changelog

## 0.5.0 — 2026-09-13

- Add arithmetic, algebra, logic, kinematics, chemistry, unit-conversion, and matrix challenge categories.
- Select challenges from the session's configured subjects and difficulty.
- Add dynamic category-specific timers and recent-question exclusion.
- Validate supplied units and normalize common equivalent unit names.
- Display streak and Sincerity Point balance in the gate.
- Add the complete Sincerity Point spending flow with a 15-minute access grant.

## 0.4.0 — 2026-09-13

- Add multi-step Standard and Extreme session configuration.
- Add target and essential-app selection with overlap prevention.
- Add challenge subject, difficulty, Amnesty, duration, and exit-delay policies.
- Add one-time recovery-code re-entry and randomized activation phrases.
- Add a 30-second Extreme Mode cancellation window.
- Add biometric or device-credential authorization for Extreme Mode.
- Store a versioned SHA-256 policy consent record with each activated session.

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
