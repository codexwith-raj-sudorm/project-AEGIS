Product Requirements Document (PRD): Project AEGIS
1. Product Overview
Project Name: AEGIS (The Gate)
Core Concept: A "Cognitive Friction" productivity application designed to physically and psychologically block access to distracting applications by enforcing mandatory academic and coding challenges.
Philosophy: AEGIS is not a digital assistant; it is a strict, immutable gatekeeper. It operates on a Zero-Trust architecture, assuming the user will attempt to cheat, bypass, or forcefully dismantle the system to access restricted content.
2. Visual Identity & UI (Neo-Brutalism)
The user interface is designed to evoke the feeling of a harsh, unyielding developer terminal.
 * Color Palette: Absolute matte black (#000000), stark white (#FFFFFF), with a single high-contrast accent (e.g., Electric Purple or Crimson Red) reserved strictly for critical states or earned tokens.
 * Geometry: Razor-sharp edges (0dp corner radius). Solid blocks of color with thick 2dp borders. No gradients, no soft shadows, no modern rounded aesthetics.
 * Typography: Monospace exclusively (e.g., JetBrains Mono). Heavy, stark, and highly legible.
3. Core Architecture & System Hooks
AEGIS utilizes deep Android OS-level permissions to ensure absolute lockdown of targeted packages.
| Component | Android API | Function |
|---|---|---|
| The Interceptor | AccessibilityService | Monitors TYPE_WINDOW_STATE_CHANGED. Drops the lock screen the millisecond a blacklisted package or URL is detected. |
| The Anchor | DeviceAdminReceiver | Prevents standard uninstallation by disabling the OS-level trash/uninstall actions. |
| Settings Firewall | Hardcoded Blacklist | Prevents access to com.android.settings to stop users from clearing data or revoking accessibility permissions. |
| Redundant Radar | UsageStatsManager | A fallback foreground service that monitors app usage if the Accessibility Service is somehow killed. |
4. Anti-Cheat Protocols
Every conventional and unconventional bypass method is actively neutralized by the application lifecycle and security flags.
| Exploit Attempt | AEGIS Defense Mechanism | Technical Implementation |
|---|---|---|
| App Switching/Googling | The Lifecycle Kill-Switch | onPause instantly destroys the current problem and resets the streak. |
| Split-Screen Multitasking | Multi-Window Denial & Focus Trap | resizeableActivity="false" and onWindowFocusChanged(false) kill-switch. |
| AI Screen Readers (Gemini) | Visual Blackout & Ghosting | WindowManager.LayoutParams.FLAG_SECURE and Compose semantic clearing. |
| Secondary Camera / OCR | Shutter-Speed Trap | High-frequency strobe typography and background ASCII noise to break camera sensors and OCR models. |
| Voice AI / Smart Speakers | Unspeakable Syntax & Acoustic Jamming | Code-heavy questions impossible to dictate, paired with max-volume rhythmic ticking. |
| Question Farming | Procedural Generation & Anti-Spam | Timestamp-seeded RNG ensures questions never repeat. High-frequency app launches trigger a 1-hour hard lockout. |
| Clear Data / App Info | App Info Interceptor | AccessibilityService blocks android.settings.APPLICATION_DETAILS_SETTINGS. |
5. The Cognitive Engine (Challenge Generation)
Questions are not pulled from a static database; they are mathematically and logically compiled at runtime to guarantee infinite state space.
 * The Syntax Gate: Dynamically flawed code blocks (Java/Python) requiring exact character-for-character correction.
 * The Formula Gate: Algorithmic physics and chemistry numericals (e.g., kinematics, stoichiometry) with randomized variables on every launch.
 * The 15-Second Death Clock: A shrinking white progress bar. If time expires before the correct answer is entered, the target app is killed, and the user is dropped to the home screen.
 * Type-Cadence Trap: Input fields measure keystroke speed to detect and reject pasted text or AI-injected answers.
6. Token Economy (Sincerity Points)
To prevent total user burnout and reward cognitive endurance, AEGIS incorporates a strictly balanced amnesty system.
 * The Streak: Users must correctly answer 20 consecutive challenges within the time limit.
 * The Reward: Accumulating a 20-streak grants 1 "Amnesty Token" (Sincerity Point), stored securely in encrypted Android DataStore.
 * The Activation: Tokens can be spent to bypass the gate without answering a question. The UI displays an electric-purple button: [ 1 AMNESTY TOKEN AVAILABLE. INITIATE BYPASS? ].
7. Protocol: Hubris (Audio Feedback)
AEGIS utilizes the device's native TextToSpeech (TTS) engine and forced volume control to audibly mock the user upon detected bypass attempts.
 * Split-Screen Attempt: "Focus. Stop pushing this device to its thermal limits to avoid a basic equation."
 * Flee Attempt (onPause): "The problem is gone, but I am still here. Try the next one without cheating."
 * Uninstall Attempt: "You cannot uninstall your own lack of discipline. Solve the calculus."
 * Death Clock Failure: Plays a pre-rendered, arrogant .mp3 audio laugh.
8. Master Key Protocol (Architect Override)
A cryptographic backdoor accessible only to the developer for graceful system teardown.
 * Activation: The user types a specific terminal command (e.g., sudo aegis.teardown()) into the standard challenge input box.
 * Authentication: The UI inverts to white/red. The user inputs the master cipher, which is verified against a hardcoded SHA-256 hash.
 * Teardown Sequence: Upon verification, AEGIS calls removeActiveAdmin(), unbinds the Accessibility Service, clears the SharedPreferences vault, and restores standard uninstallation rights.
# project-AEGIS
