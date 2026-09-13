Product Requirements Document (PRD): Project AEGIS (v2.0 Hardened)
1. Executive Summary & Philosophy
 * Project Name: AEGIS
 * Package Name: com.jarvis.aegis
 * Target Platform: Android (Native Kotlin, Jetpack Compose)
 * Core Paradigm: High-friction cognitive gatekeeper. AEGIS prevents compulsive context-switching and doomscrolling by locking target packages and URLs behind mandatory, procedurally generated academic/coding challenges.
 * Architecture Philosophy: Zero-Trust Defense-in-Depth. The system assumes an adversarial user who will attempt app-switching, screen-splitting, optical recognition via secondary devices, voice relays, permission revocation, and data wipes.
2. System Architecture & OS-Level Hooks
+-------------------------------------------------------------+
|                      Target Action                          |
|         (App Launch, Settings Breach, Target URL)           |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|               AegisAccessibilityService                     |
|    - Intercepts Window State Changes                        |
|    - Inspects Browser Address Bars                          |
|    - Blocks Settings / App Info                             |
+-------------------------------------------------------------+
          | (Active)                         | (Service Killed)
          v                                  v
+-----------------------+          +--------------------------+
|   AegisLockActivity   |          |    AegisRadarService     |
|   - FLAG_SECURE       |          |  (UsageStatsManager      |
|   - Canvas Warping    |          |   Fallback Poller)       |
|   - Dynamic TTK       |          +--------------------------+
+-----------------------+                        |
                                                 v
                                    Re-engages Lock Activity

2.1 The Interceptor (AegisAccessibilityService)
 * Event Type: TYPE_WINDOW_STATE_CHANGED, TYPE_VIEW_TEXT_CHANGED.
 * Monitored Scope:
   * Configured target packages (e.g., social media apps).
   * System attack paths: com.android.settings, android.settings.APPLICATION_DETAILS_SETTINGS.
   * Mobile browsers: Real-time URL inspection targeting blacklisted domains (e.g., instagram.com, reddit.com, x.com).
 * Intervention: Spawns AegisLockActivity using FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NO_ANIMATION.
2.2 The Anchor (AegisDeviceAdminReceiver)
 * Role: Uninstallation resistance.
 * Mechanism: Binds to android.app.action.DEVICE_ADMIN_ENABLED.
 * Dead-Man's Switch: Overrides onDisableRequested() to return an interception prompt while alerting the accessibility monitor to deploy a Master-Tier gate.
2.3 The Redundant Radar (AegisRadarService)
 * Role: Failover sensor.
 * Mechanism: Persistent low-overhead Foreground Service monitoring UsageStatsManager every 500ms.
 * Trigger: Activates immediately if AegisAccessibilityService is killed or interrupted.
3. Anti-Cheat & Defense Subsystems
| Vector | Attack Description | AEGIS Defense Architecture |
|---|---|---|
| Context Switch | Exiting app to Google answers | onPause Annihilation: Focus loss destroys current state; resets problem variables and active answer streak to 0. |
| Split-Screen | Opening AI or browser side-by-side | Window Denial: android:resizeableActivity="false" in Manifest. onWindowFocusChanged(false) triggers instant kill-switch if touched. |
| AI Screen Readers | Gemini/Lens on-device parsing | FLAG_SECURE + Semantic Blindness: Screen captures render black frames. Text semantics stripped via clearAndSetSemantics {}. |
| Secondary Device OCR | Camera pointed at screen | Procedural Canvas Distortion: Text rendered directly onto Compose Canvas with geometric character-warping and dynamic Perlin noise grain. Breaks OCR without strobing. |
| Audio Relay | Dictating to Google Assistant / Siri | Syntax & Information Density: Challenges emphasize complex code syntax and multi-variable matrices unreadable within the time limit. |
| Question Farming | Memorizing finite question sets | Algorithmic Procedural Generation: Variables generated at runtime via timestamp-seeded PRNG. State space exceeds 10^6 permutations per template. |
| App Clearing | Wiping storage in App Info | Settings Firewall: Intercepts APPLICATION_DETAILS_SETTINGS before storage settings draw to screen. |
Protocol: Hubris (Acoustic Pressure & Auditory Feedback)
 * Audio Engine: Native offline TextToSpeech coupled with short cached sound triggers (res/raw).
 * Volume Lock: Uses AudioManager to lock stream volume at 75% for audio dispatch, releasing on completion.
 * Mockery Triggers:
   * Settings Breach: "Running to settings? You cannot uninstall your own lack of discipline. Solve the terminal."
   * App Switch / Focus Loss: "Context shift detected. Current problem destroyed. Streak reset to zero."
   * Time-to-Kill (TTK) Expiry: Low-frequency mechanical clock ticks accelerating into a brief mocking laugh before force-dropping to the home screen.
4. Cognitive Engine & Dynamic Challenges
Rather than static 15-second cutoffs that incentivize panic, AEGIS calculates a Dynamic Time-to-Kill (TTK) based on question complexity:
+-----------------------------------------------------------------------+
|  CHALLENGE TYPE      | BASE TIME | VARIABLE METRIC      | MAX TTK     |
+----------------------+-----------+----------------------+-------------+
|  Java/Python Syntax  | 15s       | Line count / tokens  | 30s         |
|  Kinematics/Physics  | 20s       | Number of steps      | 45s         |
|  Matrix Operations   | 25s       | Dimension size       | 50s         |
|  Master Gate (Admin) | 60s       | Multi-step algorithm | 120s        |
+-----------------------------------------------------------------------+

4.1 Evaluation Logic (Tolerant Syntax Parsing)
 * Numeric Answers: Evaluated within an absolute floating-point tolerance (\epsilon = \pm 0.05).
 * Code Corrections: Strips leading/trailing whitespace, line breaks, and trailing semicolons using Regex tokenization to prevent syntax-valid submissions from failing due to formatting:
   fun normalize(input: String): String = 
    input.replace("\\s+".toRegex(), " ").trim()

5. Token Economy: Sincerity Points (Protocol: Amnesty)
[ Correct Answer ] ---> Streak Count +1
                            |
           (If Streak = 20) v
       +---------------------------------------------+
       | Sincerity Point Granted (Stored in DataStore)|
       | Streak resets to 0                          |
       +---------------------------------------------+
                            |
                 [ Spend Amnesty Token ]
                            |
                            v
       [ Bypass Gate: 15-minute Unrestricted Window ]

 * Streak Rules: Requires 20 strictly consecutive correct answers within the dynamic TTK.
 * Streak Resets: Occur automatically upon TTK expiry, incorrect submissions, or onPause focus loss.
 * Storage: Encrypted via EncryptedSharedPreferences backed by the Android Keystore.
 * UI Indicator: When tokens > 0, an Electric Purple button renders beneath the input box:
   [ 1 AMNESTY TOKEN AVAILABLE. INITIATE BYPASS? ]

6. Master Key Protocol (Architect Override)
An encrypted backdoor accessible only to the administrator to safely de-escalate the application without manual OS wipes.
6.1 Trigger Sequence
 * The user inputs sudo aegis.teardown() into the standard challenge terminal.
 * The UI pauses the TTK timer, inverting the screen: pure white background (#FFFFFF) with heavy crimson monospace text (#D32F2F):
   ARCHITECT AUTHORIZATION REQUIRED
ENTER MASTER CIPHER: [___________]

6.2 Cryptographic Validation
 * Storage: No plaintext keys or static hashes in the APK source.
 * Algorithm: The cipher is verified using PBKDF2WithHmacSHA256 (100,000 iterations, 256-bit key length) against a device-generated salt stored in the hardware-backed Android Keystore.
 * Execution (System Decommissioning):
   * Calls DevicePolicyManager.removeActiveAdmin(adminComponent).
   * Disables AegisAccessibilityService and stops AegisRadarService.
   * Writes decommissioned = true to encrypted storage.
   * Finishes AegisLockActivity and releases all package blocks.
   * TTS Dispatch: "Master cipher verified. AEGIS standing down."
7. Safety, Recovery & Anti-Bricking Failsafes
To ensure AEGIS does not brick the host device or block life-critical functions:
7.1 Absolute Whitelist (Non-Interceptable Packages)
The AegisAccessibilityService and AegisRadarService will instantly bypass and ignore the following packages:
 * Emergency Calls / Phone: com.android.dialer, com.android.phone, com.google.android.dialer.
 * System UI & Essential Overlays: com.android.systemui.
 * Authentication Prompts: Biometric/fingerprint dialogs (com.android.biometrics).
7.2 The Crash-Loop Watchdog
 * Watchdog State: Stored in a fast, unencrypted shared file (watchdog.json).
 * Rule: If AegisLockActivity crashes or throws an unhandled exception 3 times within 120 seconds, the watchdog triggers Fail-Open Mode.
 * Fail-Open Mode: All interception hooks disarm for 15 minutes, allowing user-space recovery without requiring a hardware Safe Mode reboot.
8. UI/UX Specifications (Neo-Brutalist Layout)
+-------------------------------------------------------------+
| > AEGIS_CORE // V.2.0-STABLE                    [TTK: 00:24]|
+-------------------------------------------------------------+
| TARGET: com.instagram.android                               |
| STATUS: ACCESS DENIED. COGNITIVE VERIFICATION REQUIRED.    |
| STREAK: 14/20 [||||||||||||||.......]                       |
+-------------------------------------------------------------+
|                                                             |
|   [ CANVAS NOISE / DISTORTED TEXT LAYER ]                   |
|   Calculate final velocity v given:                         |
|   u = 12.4 m/s, a = 9.8 m/s^2, t = 3.6s                     |
|                                                             |
+-------------------------------------------------------------+
| > ENTER VALUE: [ 47.68        ]                             |
|                                                             |
| [ EXECUTE_SUBMIT ]                                          |
|                                                             |
| [ 1 AMNESTY TOKEN AVAILABLE. INITIATE BYPASS? ]             |
+-------------------------------------------------------------+
| [PROGRESS BAR: ===========================>                ]|
+-------------------------------------------------------------+

Visual Guidelines
 * Background: Solid Matte Black (#000000).
 * Borders: 2dp Solid White (#FFFFFF), 0dp Corner Radius (Strict Sharp Edges).
 * Typography: Monospaced, Bold, Uppercase labels.
 * Terminal Input: Custom single-line text field with a blinking block cursor (▋).
9. File Tree & Implementation Order
app/src/main/
├── AndroidManifest.xml
├── res/
│   ├── raw/
│   │   └── laugh.mp3
│   └── xml/
│       ├── aegis_accessibility_config.xml
│       └── aegis_device_admin.xml
└── java/com/jarvis/aegis/
    ├── core/
    │   ├── AegisAccessibilityService.kt   # System window and URL interceptor
    │   ├── AegisDeviceAdminReceiver.kt    # Anti-uninstall receiver
    │   ├── AegisRadarService.kt           # UsageStatsManager fallback
    │   └── WatchdogManager.kt             # Anti-brick crash counter
    ├── crypto/
    │   ├── KeystoreManager.kt             # Hardware-backed token storage
    │   └── CipherValidator.kt             # PBKDF2 teardown verifier
    ├── engine/
    │   ├── ChallengeGenerator.kt          # Procedural math & syntax compiler
    │   └── AnswerValidator.kt             # Regex & floating-point evaluator
    └── ui/
        ├── AegisLockActivity.kt           # Main lock surface with FLAG_SECURE
        ├── NeoBrutalistComponents.kt      # Monospace buttons, borders, and layouts
        └── CanvasNoiseRenderer.kt         # OCR-resistant text rendering

