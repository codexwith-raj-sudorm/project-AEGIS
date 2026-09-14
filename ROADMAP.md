# AEGIS Release Roadmap

This roadmap separates AEGIS into two major deliverables:

- **v0.1–v1.0:** safe, strict, recoverable blocking system.
- **v1.1–v2.0:** complete grounded learning and tutoring system.

A feature is complete only after implementation, automated tests, a successful CI build, zero CI annotations, and relevant real-device validation notes.

## Track One — Blocking System

### Completed foundations

- **v0.1:** Android/Compose scaffold, learner profile, session domain, basic gate.
- **v0.2:** Android CI and APK artifacts.
- **v0.3:** encrypted state, recovery backoff, crash-loop Fail-Open.
- **v0.4:** authenticated enrollment, policy consent, essential-app selection.
- **v0.5:** academic challenge categories, streaks, Sincerity Points.
- **v0.6:** lifecycle enforcement, cooldowns, boot receiver, notifications.
- **v0.7–v0.9:** experimental Learning Mode foundations; not considered production-complete until Track Two.
- **v0.10:** Standard, Strict, and key-only in-app Hard Mode policies.
- **v0.11:** persisted single-use challenges and replay resistance.

### Remaining blocking releases

#### v0.12 — Time integrity

- Monotonic session, challenge, grant, cooldown, exit, and recovery deadlines.
- Boot-count tracking and reboot reconciliation.
- Clock rollback/jump detection and fail-open impossible-state handling.

#### v0.13 — Enforcement preflight

- Accessibility Service health detection and setup guidance.
- Notification and authentication readiness.
- Target availability checks.
- Recovery-key encrypted write/read self-test.
- Refuse Strict/Hard activation when mandatory enforcement is unavailable.

#### v0.14 — Essential access and interruption classification

- Default dialer, Telecom, System UI, permission controller, authentication, and emergency intent exemptions.
- Keyboard, call, alarm, biometric, permission-dialog, Sharesheet, and system-overlay grace handling.
- OEM-aware essential package catalog.

#### v0.15 — Atomic enforcement transactions

- Atomic challenge consumption, streak update, reward, token spend, and grant creation.
- Transaction IDs and crash-recovery journal.
- Concurrent gate/process protection and expired-grant cleanup.

#### v0.16 — Programming challenges

- Java, Kotlin, and Python challenge templates.
- Language-aware answer normalization and parsing.
- Multiple valid solutions and bounded test-case validation.
- Remove unselected-subject fallback in Strict/Hard modes.

#### v0.17 — Surface hardening

- Notification action, bubble, widget, custom-tab, deep-link, and exported-activity threat handling where platform-supported.
- Safe overlay protection without disabling assistive technology.
- Work-profile, secondary-user, and cloned-app warnings.

#### v0.18 — Blocking UX and accessibility

- Responsive, scroll-safe enrollment and gate layouts.
- TalkBack, Switch Access, large-text, reduced-motion, keyboard, phone, and tablet support.
- Accountability preview, user-authored commitments, rotation, and frequency limits.

#### v0.19 — Blocking release candidate

- Android lint, static analysis, instrumentation tests, emulator CI, R8, resource shrinking, signing configuration, and AAB output.
- Privacy and Accessibility disclosures.
- OEM/device test checklist and battery profiling.
- No unresolved critical/high blocking or recovery defects.

#### v1.0 — Blocking System complete

Acceptance criteria:

- Standard, Strict, and Hard policies behave as documented.
- Hard Mode is key-only through AEGIS's in-app decommission path while automatic expiry and safety Fail-Open remain mandatory.
- Emergency and essential access remains available.
- Sessions cannot become indefinite through crashes, corruption, reboot, or clock changes.
- Challenge submissions and economy updates are replay-resistant and atomic.
- Accessibility and recovery flows pass supported-device tests.
- Signed APK/AAB release artifacts build successfully with zero CI annotations.

Android owner controls such as permission revocation, force-stop, data clearing, Safe Mode, uninstall, and factory reset remain outside personal-mode enforcement.

## Track Two — Learning System

Existing notebook, PDF, OCR, retrieval, citation, note, flashcard, and quiz code is an experimental base for this track.

#### v1.1 — Scalable learning storage

- Migrate encrypted notebook JSON to structured database storage.
- Documents, pages, chapters, chunks, revisions, and deletion relationships.
- Full-text search and migration from prototype notebooks.

#### v1.2 — Document pipeline

- Robust PDF/text/Markdown/image ingestion.
- Background import jobs, progress, cancellation, and recovery.
- Printed-page versus document-page metadata.

#### v1.3 — Scanned PDF OCR

- Render and OCR image-only or mixed PDF pages.
- Per-page confidence, rotation correction, language selection, and correction UI.
- Strict memory, size, and page limits.

#### v1.4 — Structure and reference engine

- Chapter, section, exercise, example, theorem, table, and question boundaries.
- User correction tools and ambiguity resolution UI.
- Page-accurate citation validation.

#### v1.5 — Study workspace

- Notebook search, highlights, editable generated notes, formula sheets, flashcards, quizzes, and separate answer keys.
- Export to user-selected locations with citations.

#### v1.6 — Grounded AI provider layer

- Provider-neutral LearningAssistant implementation.
- On-device model option and optional hosted provider.
- Explicit cloud consent and exact excerpt preview.
- No Accessibility, blocking history, recovery, or challenge data in requests.

#### v1.7 — Tutoring modes

- Hint-first, Socratic, worked-solution, revision, and exam-practice workflows.
- Curriculum, class, subject, topic, and mastery adaptation.
- User-answer comparison.

#### v1.8 — Verification and gate isolation

- Deterministic mathematics, units, and bounded code checks.
- Citation/hallucination detection and uncertainty labels.
- Repository/service-level blocking-session checks.
- Tests proving active gate content cannot reach any learning provider.

#### v1.9 — Learning release candidate

- Learning accessibility, privacy, minor-account, deletion, export, performance, offline, and provider-failure tests.
- Production UI and localization readiness.
- No unresolved critical/high privacy, citation, or gate-isolation defects.

#### v2.0 — Complete AEGIS release

Acceptance criteria:

- v1.0 blocking acceptance remains green.
- Imported materials are encrypted, searchable, editable, versioned, and fully deletable.
- Searchable PDFs, scanned materials, images, text, and Markdown retain reliable source metadata.
- Answers distinguish source-grounded content, general explanation, and uncertainty.
- Citations resolve to the correct source location.
- Notes, formula sheets, flashcards, quizzes, hints, Socratic tutoring, and worked solutions operate at the learner's configured level.
- Active gate challenges cannot be sent to or solved by Learning Mode.
- Signed APK/AAB artifacts and all automated quality gates pass with zero CI annotations.

## Deferred beyond v2.0

- Dedicated-device/Device Owner product flavor.
- Cloud notebook synchronization.
- Trusted-contact accountability.
- Privacy-reviewed domain filtering.
- Usage Stats fallback radar, unless device testing proves it necessary and battery-safe.
