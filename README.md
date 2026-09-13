# Project AEGIS

## Product Requirements Document — v3.0: Strict, Safe, and Recoverable

## 1. Executive summary

**Package:** `com.jarvis.aegis`

**Platform:** Native Android using Kotlin and Jetpack Compose

**Purpose:** AEGIS is a high-friction cognitive gatekeeper that interrupts compulsive context switching and doomscrolling. Access to user-selected applications is placed behind short, procedurally generated academic, logic, or programming challenges.

AEGIS follows **defense in depth**, but does not claim to be immutable on a personally owned Android device. The device owner retains ultimate control through Android recovery mechanisms. AEGIS maximizes resistance to casual and deliberate bypasses while remaining time-bounded, accessible, and fail-open.

### Product principles

1. **Strict by choice:** Enforcement begins only after informed, authenticated enrollment.
2. **Time-bounded:** Every restrictive session has an immutable expiration time.
3. **Fail-open:** Crashes, corrupt state, or incomplete configuration must disable enforcement.
4. **Emergency-safe:** Calls, System UI, authentication, recovery, and selected essential apps are never blocked.
5. **No physical harm:** No strobing, acoustic jamming, forced volume, or hostile sensory effects.
6. **No hidden backdoor:** Recovery credentials are unique to each installation and controlled by the device owner.
7. **Accessible by design:** AEGIS must not disable screen readers or other assistive technology.
8. **Honest enforcement:** Personal-device mode is best-effort. Strong OS policy belongs in a separately provisioned Device Owner mode.

---

## 2. Operating modes

### 2.1 Standard Mode

For ordinary, user-owned Android devices.

- Configurable focus sessions and target applications.
- Adaptive challenges and optional Sincerity Points.
- Immediate in-app session cancellation.
- Intended for general daily use.

### 2.2 Extreme Mode

The strictest safe mode available on an unmanaged personal device.

- Fixed-duration session established before activation.
- Target list cannot be weakened during the session.
- App switching and persistent focus loss invalidate the current challenge.
- Split-screen and picture-in-picture invalidate the current challenge.
- Screenshots and ordinary screen recording are blocked with `FLAG_SECURE`.
- Copy, paste, drag-and-drop, autofill, and content insertion are disabled in challenge inputs.
- Incorrect answers and abandoned challenges reset the active streak.
- Repeated target launches create progressively longer cooldowns.
- Session state survives process death and reboot.
- Amnesty can be limited or disabled at enrollment.
- Exit remains possible through a delayed emergency procedure or recovery code.

Extreme Mode does **not** block Android Settings, App Info, permission controls, Safe Mode, or uninstallation. If required permissions are revoked, AEGIS records the session as interrupted and stops enforcement safely.

### 2.3 Managed Mode — future product flavor

For dedicated devices deliberately provisioned with AEGIS as Device Owner.

- Uses supported `DevicePolicyManager` APIs for kiosk and package policy.
- Requires setup-time provisioning and an authorized administrator.
- Is built and distributed separately from the personal-device application.
- Must have documented administrator recovery and deprovisioning.

Accessibility must never be used to imitate unavailable Device Owner powers.

---

## 3. Extreme Mode enrollment

Extreme Mode requires a deliberate activation ceremony:

1. Display a plain-language behavior and recovery summary.
2. Select target apps, duration, subjects, difficulty, and essential-app exemptions.
3. Choose whether Amnesty Tokens are available.
4. Select a delayed-exit period between 1 and 10 minutes.
5. Generate a one-time owner recovery code and require confirmation that it was saved.
6. Authenticate with `BiometricPrompt` or the device credential.
7. Type a randomized confirmation phrase.
8. Complete a 30-second cancellation countdown.
9. Persist the signed session configuration and activate enforcement.

Initial releases limit Extreme sessions to **24 hours**. Longer sessions require later safety review.

The confirmation screen must state:

> Extreme Mode aggressively interrupts selected applications until the displayed expiration time. Leaving a challenge may destroy progress and reset your streak. Screenshots are disabled. A delayed emergency exit and owner recovery code remain available. On an unmanaged device, Android still permits permission revocation, force-stop, uninstall, Safe Mode, and factory reset.

Consent records include the policy version, session configuration hash, activation time, expiration time, and authentication result. Consent is not a waiver for unsafe behavior.

---

## 4. Architecture

```text
Target app enters foreground
          |
          v
AegisAccessibilityService ---- permission/state health check
          |
          v
FocusSessionManager ---------- expiry, target list, cooldown, recovery
          |
          v
AegisLockActivity ------------ FLAG_SECURE, lifecycle classification
          |
          v
ChallengeEngine -------------- generate, render, validate, rotate
          |
          +-------------------- success -> temporary access grant
          |
          +-------------------- abandon/fail -> reset and cooldown

WatchdogManager -------------- crash-loop and corrupt-state fail-open
RecoveryManager -------------- delayed exit and owner recovery code
```

### 4.1 `AegisAccessibilityService`

Permitted responsibilities:

- Observe foreground package transitions necessary to detect configured targets.
- Ignore all packages and text outside the minimum configured scope.
- Request the lock surface when a target enters the foreground.
- Classify known system interruptions where possible.

Prohibited responsibilities:

- Blocking Settings, App Info, permission controls, or uninstall flows.
- Reading or logging user-entered content.
- Interfering with accessibility services.
- Claiming that revocation can be prevented.

All event processing occurs locally. Package transition history is kept only as long as needed for active enforcement and is not included in analytics.

### 4.2 `FocusSessionManager`

Owns the authoritative session state:

- Session ID and policy version.
- Activation and expiration timestamps.
- Monotonic-time checkpoint and boot identity.
- Target and essential package sets.
- Challenge policy, streak, Amnesty policy, and cooldown state.
- Recovery and fail-open state.

A session may never be extended silently. Clock anomalies choose the result that avoids indefinite restriction. Once the stored expiration has passed, interception stops unconditionally.

### 4.3 `AegisLockActivity`

- Uses `FLAG_SECURE` during challenges.
- Uses an accessible, non-distorted Compose interface.
- Disables copy/paste, autofill, drag-and-drop, and external content insertion for challenge inputs in Extreme Mode.
- Handles multi-window, picture-in-picture, focus, and lifecycle callbacks.
- Does not treat every `onPause` as cheating.
- Allows short grace periods for keyboards, biometric prompts, calls, permission dialogs, and known system overlays.
- Invalidates the challenge after confirmed unrelated-app focus.

AEGIS does not claim to kill other applications. It interrupts access and returns the user to an appropriate safe surface using Android-supported behavior.

### 4.4 Optional usage-access radar

A `UsageStatsManager` fallback may be evaluated after the MVP.

- Polling must be adaptive rather than fixed at 500 ms.
- It must run only during an active session.
- It must use a visible foreground-service notification when required.
- Battery impact must be measured before release.
- Failure of the radar must not create a crash loop or indefinite lock.

---

## 5. Bypass resistance

| Attempt | Strict response | Expected strength |
|---|---|---|
| Open another app to find an answer | Confirm unrelated foreground transition, invalidate challenge, reset streak, generate a new challenge | High while Accessibility is enabled |
| Split-screen or picture-in-picture | Invalidate active challenge and deny progress | Medium–High |
| Screenshot or ordinary screen recording | `FLAG_SECURE` | High, subject to OEM behavior |
| Copy/paste or autofill | Disable content insertion in Extreme Mode | Medium–High |
| Memorize or farm questions | Secure procedural generation, challenge IDs, recent-history exclusion | High |
| Repeated rapid launches | Exponential cooldown with a documented maximum | High against impulsive retries |
| Reboot or change wall clock | Persist deadline with wall-clock, monotonic, and boot-state checks | Medium–High |
| Secondary camera | Short-lived, unique challenges make relay inconvenient | Low; cannot be prevented safely |
| External voice/AI relay | Short-lived, unique, information-dense challenges | Low–Medium |
| Revoke permissions, force-stop, clear data, uninstall | Cannot be prevented in personal-device modes | None by design |
| Root, ADB with sufficient authority, custom ROM, factory reset | Outside the personal-device threat boundary | None |

No feature may use strobing, unreadable distortion, acoustic attacks, forced volume, semantic removal, or obstruction of Android recovery controls.

---

## 6. Cognitive engine

### 6.1 Challenge categories

- Mental arithmetic and algebra.
- Logic and sequence reasoning.
- Java, Kotlin, and Python syntax or behavior.
- Kinematics and introductory physics.
- Chemistry and unit conversion.
- Matrix operations.

Users calibrate subjects and difficulty before starting strict enforcement. AEGIS must never require knowledge outside the selected profile.

### 6.2 Generation

- Use `SecureRandom`; do not seed solely from timestamps.
- Assign every challenge a random, non-reusable challenge ID.
- Exclude recently displayed parameter combinations.
- Validate solvability and the expected answer before presentation.
- Bound all generated values to avoid ambiguity, overflow, and unreasonable manual computation.
- Store only the minimum state required to validate the active challenge.

### 6.3 Dynamic time-to-kill

```text
TTK = base time + complexity adjustment + accessibility adjustment
```

| Type | Base | Default maximum |
|---|---:|---:|
| Arithmetic/logic | 15 s | 35 s |
| Java/Kotlin/Python | 20 s | 60 s |
| Physics/chemistry | 25 s | 60 s |
| Matrix operations | 30 s | 75 s |

Timing accommodations are configured before an Extreme session and cannot be reduced during it. Standard Mode may disable timers.

### 6.4 Validation

- Numeric answers use units where relevant and both absolute and relative tolerance.
- Mathematical expressions use a restricted expression parser.
- Code answers use language-aware tokenization, parsing, or isolated test cases.
- Python indentation and string content must be preserved.
- Regex whitespace collapsing is not a substitute for syntax validation.
- Ambiguous or generator-invalid challenges are discarded without penalizing the user.

### 6.5 Learning Mode — AI study workspace

Learning Mode is a separate, non-punitive tutoring workspace. Users can add notebooks, textbooks, class handouts, assignment sheets, and their own notes, then ask AEGIS to explain, summarize, quiz, and create revision material grounded in those sources.

#### Supported study sources

Initial supported inputs:

- Searchable PDF documents.
- Camera scans and images using on-device OCR where practical.
- Plain text and Markdown notes.
- User-created notebook pages inside AEGIS.

Later versions may add DOCX, EPUB, presentation, and cloud-drive import after security and compatibility review.

Every imported source records:

- User-defined title and subject.
- Class/grade and curriculum, inherited from the Learner Profile unless overridden.
- Chapter names and detected section hierarchy.
- Printed page number and document page index where available.
- Detected question numbers and exercise boundaries.
- OCR confidence and extraction warnings.

The user can correct chapter names, page numbers, question boundaries, and OCR mistakes. Low-confidence text must be visibly marked rather than silently treated as accurate.

#### Core requests

Learning Mode supports requests such as:

- `Give the solution to question 12 from Chapter 4.`
- `Explain Example 3 using simpler steps.`
- `Make concise notes for this chapter.`
- `Create a formula sheet from Chapters 2–5.`
- `Quiz me on this notebook without showing the answers first.`
- `Create flashcards from the highlighted sections.`
- `Compare my answer with the method in the notebook.`
- `Find the section that explains conservation of momentum.`

When a question number is ambiguous, AEGIS asks the user to choose the source, chapter, exercise, or page instead of guessing.

#### Grounded retrieval and citations

Learning Mode uses retrieval-augmented generation:

1. Extract and normalize source text while retaining page and section metadata.
2. Split text into structure-aware chunks rather than arbitrary fixed windows.
3. Create a local searchable index and, when enabled, an embedding index.
4. Retrieve only the passages relevant to the request.
5. Generate an answer grounded in retrieved passages.
6. Cite the notebook title, chapter or section, and page for each source-dependent claim.

The UI must distinguish:

- **From your materials:** directly supported by imported sources.
- **General explanation:** model knowledge not found in the materials.
- **Uncertain:** incomplete, conflicting, or low-confidence source extraction.

AEGIS must not fabricate a quotation, page reference, theorem, worked answer, or source citation. If the source does not contain enough information, it says so and asks permission before using general model knowledge.

#### Tutoring behavior

Users choose an assistance style:

- **Hint first:** progressive hints before a full solution.
- **Socratic:** asks guiding questions one step at a time.
- **Worked solution:** shows assumptions, formulas, substitutions, units, and checks.
- **Revision notes:** creates structured summaries and key-point lists.
- **Exam practice:** generates questions at the selected curriculum and mastery level.

For assessed homework, AEGIS should encourage an attempt before revealing a complete answer. This is instructional friction, not an absolute restriction. Generated notes and solutions remain editable and can be exported with source references.

#### Isolation from gate challenges

Learning Mode must not become a built-in bypass mechanism:

- It cannot be opened while a gate challenge is active.
- Gate challenges are never inserted into the Learning Mode query context or history.
- Active challenge text, answer, random seed, and validation state are inaccessible to the learning assistant.
- AEGIS invalidates a gate challenge if the user deliberately leaves it for Learning Mode.
- Generated gate questions are not answered by calling the learning model internally.
- Shared components expose content through narrow interfaces and separate storage domains.

Learning Mode can help users study the same subject before or after a focus session, but cannot solve the currently displayed access challenge.

#### Notes generation

Chapter notes may include:

- Learning objectives.
- Definitions and key concepts.
- Formulae with symbol and unit definitions.
- Step-by-step methods.
- Worked examples clearly labeled as generated or source-derived.
- Common mistakes.
- Flashcards and recall questions.
- A short chapter quiz with a separate answer key.
- Citations back to imported materials.

Before saving, the user previews the notes and chooses whether to replace, append, or create a new version. Source documents are never modified in place.

#### AI provider and offline behavior

The architecture uses a provider-neutral `LearningAssistant` interface so the app can support:

- An on-device model when hardware and model licensing permit.
- A user-enabled hosted model for higher-quality explanations.
- Retrieval-only search when no model is available.

Cloud processing is opt-in and must show exactly which document excerpts will leave the device. Authentication data, Accessibility events, blocked-app history, recovery codes, and active gate state are never included in model requests. Provider retention and training controls must be disclosed before first use.

#### Privacy, ownership, and deletion

- Imported materials are private by default.
- Originals, extracted text, indexes, conversations, and generated notes can be deleted independently or together.
- Removing a notebook also removes its derived chunks and embeddings.
- School name, student ID, handwriting identity, and unrelated page metadata are not extracted unless required and explicitly approved.
- Users may import only material they are authorized to use; AEGIS does not publish or share uploaded textbooks.
- Cloud synchronization is excluded from the MVP until encryption, deletion, and minor-account requirements are defined.

#### Learning quality safeguards

- Mathematical outputs are checked with deterministic evaluators where possible.
- Unit consistency is verified for numerical science answers.
- Code examples are parsed or tested in a restricted sandbox where supported.
- Generated quizzes store their answer key separately from the initial question view.
- Users can report an incorrect explanation and retain the source passages for comparison.
- The assistant never changes mastery estimates solely because of an unverified AI judgment.

---

## 7. Sincerity Points — Protocol Amnesty

- A configurable correct-answer streak grants one Sincerity Point; default: 20.
- Incorrect or abandoned challenges reset the Extreme Mode streak.
- A token grants a configurable access window; default: 15 minutes.
- Extreme Mode enrollment may disable earning and spending tokens.
- Token and streak updates are transactional to prevent duplication after crashes.
- Recovery or session expiration never consumes a token.

---

## 8. Safety and recovery

### 8.1 Essential access

AEGIS never intercepts:

- Emergency dialing and Telecom flows.
- System UI and lock-screen flows.
- Biometric and device-credential prompts.
- Permission controllers and package-management recovery interfaces.
- Device setup and recovery components.
- User-selected medical, safety, authentication, and essential communication apps.

Detection must consider intents, roles, and OEM variations—not only hardcoded package names. AEGIS dependencies cannot be selected as targets.

### 8.2 Delayed emergency exit

The owner can initiate exit at any time:

1. Authenticate with device credentials where available.
2. Start the preselected delay.
3. Keep essential applications available during the delay.
4. Allow cancellation of the exit request.
5. Deactivate the session automatically when the delay ends.

The delay may add friction but cannot be restarted indefinitely or conditioned on solving a challenge.

### 8.3 Owner recovery code

- Generated uniquely during enrollment with cryptographically secure randomness.
- Displayed once and confirmed by the owner.
- Stored only as a versioned, salted, slow password verifier.
- Rate-limited with exponential backoff.
- Never shared across devices or known to the developer.
- Successful use immediately decommissions the active session.

Use Android Keystore for locally held encryption keys where supported. Hardware backing is detected rather than assumed. Salt is random and unique but is not treated as secret.

### 8.4 Crash-loop watchdog

If the lock activity or enforcement path fails three times within 120 seconds:

- Enter Fail-Open Mode for at least 15 minutes.
- Stop presenting the lock surface.
- Show a diagnostic notification.
- Preserve only non-sensitive failure metadata.
- Require a healthy self-check before enforcement resumes.

Missing, corrupt, incompatible, or partially migrated session state always fails open. Enforcement must not depend solely on an in-process crash handler.

### 8.5 Boot behavior

- Verify schema, integrity, expiration, permissions, and essential exemptions before rearming.
- Expired or unverifiable sessions remain disabled.
- Never start an activity repeatedly during boot.
- Provide clear Safe Mode and uninstall recovery documentation.

---

## 9. Accountability and motivational feedback

AEGIS may use deliberate psychological friction to reconnect the user with the commitment they voluntarily made. It challenges the **decision to bypass**, never the user's identity, intelligence, appearance, mental health, or personal worth.

### 9.1 Motivation profiles

Before an Extreme session, the user selects and previews one profile:

- **Neutral:** reports the event and consequence without persuasion.
- **Reflective:** asks the user to pause and reconsider the action.
- **Challenger:** uses competitive, ego-oriented prompts without insults.
- **Hard Truth:** states the behavioral pattern, opportunity cost, and consequence in deliberately uncomfortable language without attacking identity or predicting permanent failure.
- **Personal commitment:** displays phrases written by the user for their future self.

Example prompts:

**Neutral**

- `CONTEXT SHIFT DETECTED. CHALLENGE INVALIDATED.`
- `TIME LIMIT EXPIRED. STREAK RESET.`

**Reflective**

- `DO YOU REALLY WANT TO ABANDON THIS SESSION FOR THIS APP?`
- `YOU HAVE 18 MINUTES LEFT. IS THIS INTERRUPTION WORTH YOUR GOAL?`
- `PAUSE. YOU ACTIVATED AEGIS BECAUSE THIS MOMENT WAS PREDICTABLE.`

**Challenger**

- `IS THIS YOUR LIMIT, OR WILL YOU FINISH WHAT YOU STARTED?`
- `THE DISTRACTION CAN WAIT. PROVE THAT YOUR COMMITMENT CANNOT.`
- `ONE CHALLENGE STANDS BETWEEN IMPULSE AND INTENTION. COMPLETE IT.`
- `YOU ARE CLOSER TO THE FINISH THAN TO THE START. HOLD THE LINE.`

**Hard Truth**

- `YOU ACTIVATED AEGIS BECAUSE THIS IMPULSE HAS DEFEATED YOUR PLANS BEFORE. IT IS HAPPENING AGAIN.`
- `ENDING THE SESSION WILL GIVE YOU IMMEDIATE COMFORT, NOT THE CHANGE YOU SAID YOU WANTED.`
- `REPEATING THE SAME CHOICE REINFORCES THE SAME HABIT. A DIFFERENT RESULT REQUIRES A DIFFERENT ACTION.`
- `YOU CAN OPEN THE APP, BUT CALL THE DECISION WHAT IT IS: ABANDONING THIS SESSION EARLY.`
- `AEGIS CANNOT MAKE THE FINAL CHOICE FOR YOU. IT CAN MAKE SURE YOU DO NOT MAKE IT MINDLESSLY.`

Hard Truth may be uncomfortable, but it must remain accurate and behavior-focused. It may say that the current action does not advance the stated goal; it may not say the user will never improve, will always fail, or is inherently weak.

Prompts may include the user's stated goal, remaining time, completed time, and current streak. They must not invent personal facts or infer emotional or mental-health conditions.

### 9.2 Delivery rules

- Every built-in phrase is visible during enrollment before consent.
- The selected profile is fixed for the active Extreme session.
- Prompts are event-triggered and frequency-limited; they must not create notification spam.
- Repeated events rotate messages and then fall back to concise factual status.
- Hard Truth is never the default, requires a separate content preview and confirmation, and is unavailable to accounts identified as minors.
- A Conscious Override presents the commitment, consequence, and two unambiguous choices: `[ RETURN TO MY COMMITMENT ]` and `[ END SESSION AND OPEN TARGET ]`.
- Ending the session requires the user to hold to confirm or enter `I UNDERSTAND THAT I AM ENDING THIS SESSION EARLY`, followed by the configured exit delay.
- After a confirmed override, AEGIS grants access and stops persuasive messaging. It records only a local `voluntary_override` event and applies only the streak/token consequences agreed to at enrollment.
- Audio is optional and off by default.
- Audio follows current device volume and audio-focus rules.
- AEGIS never raises or locks volume.
- Haptic and silent alternatives are available.
- User-authored phrases are stored locally and can be deleted outside an active session.

### 9.3 Prohibited feedback

AEGIS must never:

- Call the user weak, stupid, worthless, lazy, broken, or a failure.
- Shame appearance, identity, disability, religion, relationships, or socioeconomic status.
- Threaten exposure, punishment, abandonment, or consequences outside AEGIS.
- Claim that another person is disappointed in the user.
- Encourage self-harm, deprivation, dangerous overwork, or ignoring health needs.
- Use private activity history for unexpected emotional targeting.

The governing rule is: **confront the bypass, reinforce the goal, and preserve the person's dignity.**

---

## 10. Visual identity

- Matte black background: `#000000`.
- White foreground and 2 dp square borders: `#FFFFFF`.
- Electric purple for earned tokens.
- Crimson for destructive or recovery actions.
- Zero-radius geometry and no gradients.
- Monospaced typography with scalable text.
- Reduced-motion support and WCAG-conscious contrast.
- No flicker, strobe, rapidly flashing content, or readability-reducing distortion.

```text
+-----------------------------------------------------------+
| > AEGIS_CORE // EXTREME                    [TTK: 00:24]   |
+-----------------------------------------------------------+
| TARGET: com.instagram.android                             |
| STATUS: COGNITIVE VERIFICATION REQUIRED                  |
| SESSION ENDS: 18:30                    STREAK: 14/20      |
+-----------------------------------------------------------+
| Calculate final velocity given:                          |
| u = 12.4 m/s, a = 9.8 m/s², t = 3.6 s                   |
+-----------------------------------------------------------+
| > ENTER VALUE: [ 47.68                              ]     |
| [ EXECUTE_SUBMIT ]                                       |
+-----------------------------------------------------------+
```

---

## 11. Privacy and data handling

- Process package transitions and challenges on-device.
- Do not collect screen contents, address-bar contents, answers, or app history for analytics.
- Do not log sensitive Accessibility node data.
- Encrypt session and token state at rest using a Keystore-backed local key when available.
- Provide deletion controls and a concise data-use disclosure.
- Domain blocking is deferred until a separate privacy and browser-compatibility review.
- No network permission is required for the initial MVP unless a documented feature needs it.

---

## 12. Initial file structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/jarvis/aegis/
│   ├── accessibility/
│   │   └── AegisAccessibilityService.kt
│   ├── challenge/
│   │   ├── Challenge.kt
│   │   ├── ChallengeGenerator.kt
│   │   └── AnswerValidator.kt
│   ├── session/
│   │   ├── FocusSession.kt
│   │   ├── FocusSessionManager.kt
│   │   └── SessionPolicy.kt
│   ├── recovery/
│   │   ├── RecoveryManager.kt
│   │   └── WatchdogManager.kt
│   ├── learning/
│   │   ├── LearningAssistant.kt
│   │   ├── NotebookRepository.kt
│   │   ├── DocumentExtractor.kt
│   │   ├── StudyRetriever.kt
│   │   └── CitationValidator.kt
│   ├── security/
│   │   ├── KeystoreManager.kt
│   │   └── RecoveryCodeVerifier.kt
│   └── ui/
│       ├── enrollment/
│       ├── lock/AegisLockActivity.kt
│       └── theme/NeoBrutalistTheme.kt
└── res/xml/
    └── aegis_accessibility_config.xml
```

Device Admin, usage radar, browser inspection, and managed-device code are excluded from the initial MVP until their necessity, policy compliance, and safety are validated.

---

## 13. Implementation order

### Phase 1 — Safe MVP

1. Compose project and neo-brutalist accessible design system.
2. App selection and immutable, time-bounded session model.
3. Arithmetic and logic challenge generation with property-based tests.
4. Accessible lock activity with `FLAG_SECURE`.
5. Minimal Accessibility-based target-app detection.
6. Session expiry, delayed exit, recovery code, and corrupt-state fail-open.
7. Extreme Mode enrollment and authenticated consent.

### Phase 2 — Hardening

1. Lifecycle and system-interruption classification.
2. Multi-window and picture-in-picture handling.
3. Secure input restrictions.
4. Cooldowns and transactional streak/token storage.
5. Boot recovery and external crash-loop watchdog behavior.
6. Kotlin, Java, Python, physics, chemistry, and matrix challenges.
7. OEM, battery, accessibility, and adversarial bypass testing.

### Phase 3 — Learning Mode

1. Local notebook creation and searchable PDF/text import.
2. Structure-aware extraction with page, chapter, and question metadata.
3. Retrieval with visible source citations.
4. Hint-first, Socratic, worked-solution, notes, quiz, and flashcard workflows.
5. Gate/learning isolation tests and model-output quality checks.
6. Optional, consented hosted-model provider after privacy review.

### Phase 4 — Optional capabilities

1. Privacy-reviewed domain filtering.
2. Battery-tested Usage Stats fallback.
3. Opt-in TTS.
4. Separately provisioned Managed Mode flavor.

---

## 14. Release gates

No release may ship until all of the following pass:

- Session expiration works across reboot and clock changes.
- Corrupt state reliably fails open.
- Emergency calls and essential apps remain reachable.
- Delayed exit and recovery code work without a challenge.
- TalkBack, font scaling, switch access, and reduced motion are tested.
- No code changes or locks system volume.
- No UI flashes at unsafe frequencies.
- Accessibility data is neither logged nor transmitted.
- Battery tests show acceptable active-session consumption.
- The product description makes no claim of immutable enforcement.
- Current Google Play Accessibility, foreground-service, privacy, and device-policy requirements have been reviewed.

## 15. Success criteria

AEGIS succeeds when it makes impulsive access sufficiently inconvenient that answering the challenge or abandoning the distraction is easier than bypassing the gate—without risking physical harm, permanent lockout, loss of emergency access, or covert loss of device ownership.
