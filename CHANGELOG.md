# Changelog

## 0.13.0 — 2026-09-14

- Add mandatory enforcement preflight before session authorization.
- Block Strict and Hard activation when Accessibility interception or device authentication is unavailable.
- Validate target installation, essential-package separation, encrypted recovery storage, and watchdog health.
- Add notification readiness warnings and direct Accessibility settings guidance.
- Run a Keystore AES-GCM round-trip self-test before strict enforcement can arm.

## 0.12.0 — 2026-09-14

- Add monotonic and boot-aware time anchors backed by elapsed realtime and Android boot count.
- Detect elapsed-time rollback, wall-clock rollback, and excessive same-boot clock drift.
- Apply conservative deadline reconciliation after reboot and fail open on impossible time state.
- Anchor sessions, persisted challenges, access grants, launch cooldowns, delayed exits, and recovery backoff.
- Restore challenge timers from monotonic remaining time instead of adjustable wall time.

## 0.11.0 — 2026-09-13

- Persist active challenge parameters and encrypted expected answers across activity recreation.
- Restore timers from immutable challenge deadlines rather than restarting countdowns.
- Bind every challenge to its session and target package.
- Make challenge IDs single-use and reject stale or duplicate submissions.
- Clear active challenges on confirmed context switching and session decommissioning.
- Prevent duplicate streak rewards from replaying an already consumed challenge.

## 0.10.0 — 2026-09-13

- Add Standard, Strict, and Hard blocking levels with explicit enforcement rules.
- Make Standard sessions immediately cancellable and Strict sessions delay-or-key protected.
- Make Hard sessions key-only for in-app decommissioning while preserving automatic expiry and safety Fail-Open.
- Disable Amnesty in Hard mode and require device authentication for Strict and Hard enrollment.
- Scale repeated-launch cooldowns and context-switch streak consequences by blocking level.

## 0.9.0 — 2026-09-13

- Add fully on-device ML Kit OCR for imported study-note images.
- Preserve OCR output as page-aware notebook content without requesting storage access.
- Reject images with no readable text and warn when confidence is unavailable or low.
- Add encrypted notebook revision counters and display revision metadata in Learning Mode.
- Expand the document picker to accept images alongside PDF, text, and Markdown.

## 0.8.0 — 2026-09-13

- Add searchable PDF import with per-page text extraction and page markers.
- Enforce 10 MB and 200-page PDF safety limits and reject encrypted or image-only PDFs clearly.
- Add chapter and question-number parsing for grounded notebook requests.
- Resolve repeated question numbers through chapter context or request clarification.
- Include page, chapter, and question metadata in exact-match citations.

## 0.7.0 — 2026-09-13

- Add encrypted persistent notebook storage with create, edit, rename, and delete flows.
- Add Android text and Markdown document import with a bounded 2 MB reader.
- Add grounded local notebook search with section-level citations.
- Add deterministic revision-note, flashcard, and quiz generation.
- Add source metadata, timestamps, import errors, and explicit insufficient-source responses.

## 0.6.0 — 2026-09-13

- Add escalating per-target cooldowns for repeated launch attempts, capped at five minutes.
- Add cooldown status and enforcement to the cognitive gate.
- Reset challenge streaks on deliberate task exit, multi-window, and picture-in-picture entry.
- Validate sessions safely after boot and package replacement without launching an activity.
- Add active-session and Fail-Open notification channels.
- Restore active-session status notifications after reboot and request Android 13+ notification permission.

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
