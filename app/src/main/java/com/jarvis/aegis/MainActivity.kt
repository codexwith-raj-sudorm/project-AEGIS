package com.jarvis.aegis

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.jarvis.aegis.data.AegisStore
import com.jarvis.aegis.notification.AegisNotifications
import com.jarvis.aegis.recovery.RecoveryCodeManager
import com.jarvis.aegis.security.DeviceAuthenticator
import com.jarvis.aegis.session.ConsentHasher
import com.jarvis.aegis.session.ConsentRecord
import com.jarvis.aegis.session.FocusSession
import com.jarvis.aegis.session.SessionPolicy
import com.jarvis.aegis.session.rules
import com.jarvis.aegis.ui.screens.ActivationScreen
import com.jarvis.aegis.ui.screens.DashboardScreen
import com.jarvis.aegis.ui.screens.LearningScreen
import com.jarvis.aegis.ui.screens.ProfileScreen
import com.jarvis.aegis.ui.screens.SessionExitScreen
import com.jarvis.aegis.ui.screens.SessionSetupScreen
import com.jarvis.aegis.ui.screens.launchableApps
import com.jarvis.aegis.ui.theme.AegisTheme
import java.time.Instant

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = AegisStore(this)
        val notifications = AegisNotifications(this).also { it.createChannels() }
        setContent {
            AegisTheme {
                val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) store.activeSession()?.let(notifications::showActiveSession)
                }
                var destination by rememberSaveable { mutableStateOf("dashboard") }
                var profile by remember { mutableStateOf(store.profile()) }
                var session by remember { mutableStateOf(store.activeSession()) }
                var recoveryCode by rememberSaveable { mutableStateOf("") }
                var recoveryVerifier by rememberSaveable { mutableStateOf("") }
                var pendingPolicy by remember { mutableStateOf<SessionPolicy?>(null) }
                when (destination) {
                    "dashboard" -> DashboardScreen(
                        profile = profile,
                        activeSession = session,
                        onStartSession = { destination = "session" },
                        onOpenLearning = { if (session == null) destination = "learning" },
                        onEditProfile = { destination = "profile" },
                        onEndSession = { destination = "exit" },
                    )
                    "profile" -> ProfileScreen(profile, onSave = {
                        store.saveProfile(it); profile = it; destination = "dashboard"
                    }, onBack = { destination = "dashboard" })
                    "session" -> SessionSetupScreen(
                        apps = remember { launchableApps(packageManager) },
                        ownPackage = packageName,
                        availableSubjects = profile.subjects,
                        onContinue = { policy ->
                            val enrollment = RecoveryCodeManager().enroll()
                            pendingPolicy = policy
                            recoveryCode = enrollment.displayCode
                            recoveryVerifier = enrollment.verifier
                            destination = "activation"
                        },
                        onBack = { destination = "dashboard" },
                    )
                    "activation" -> pendingPolicy?.let { policy ->
                        ActivationScreen(
                            policy = policy,
                            recoveryCode = recoveryCode,
                            onAuthenticate = { callback -> DeviceAuthenticator(this@MainActivity).authenticate(callback) },
                            onActivate = {
                                val activated = FocusSession(policy = policy, activatedAt = Instant.now())
                                store.saveRecoveryVerifier(recoveryVerifier)
                                store.saveConsent(
                                    ConsentRecord(
                                        sessionId = activated.id,
                                        policyVersion = ConsentHasher.POLICY_VERSION,
                                        policyHash = ConsentHasher.hash(policy),
                                        confirmedAt = Instant.now(),
                                        deviceAuthenticated = policy.mode.rules.requireDeviceAuthentication,
                                    ),
                                )
                                store.saveSession(activated)
                                notifications.showActiveSession(activated)
                                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                session = activated
                                recoveryCode = ""
                                recoveryVerifier = ""
                                pendingPolicy = null
                                destination = "dashboard"
                            },
                            onCancel = {
                                recoveryCode = ""; recoveryVerifier = ""; pendingPolicy = null
                                destination = "dashboard"
                            },
                        )
                    } ?: run { destination = "dashboard" }
                    "exit" -> SessionExitScreen(store, onEnded = {
                        notifications.cancelSession()
                        session = null; destination = "dashboard"
                    }, onBack = { destination = "dashboard" })
                    "learning" -> LearningScreen { destination = "dashboard" }
                }
            }
        }
    }
}
