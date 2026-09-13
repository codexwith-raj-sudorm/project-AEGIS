package com.jarvis.aegis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.jarvis.aegis.data.AegisStore
import com.jarvis.aegis.profile.LearnerProfile
import com.jarvis.aegis.session.FocusSession
import com.jarvis.aegis.ui.screens.DashboardScreen
import com.jarvis.aegis.ui.screens.LearningScreen
import com.jarvis.aegis.ui.screens.ProfileScreen
import com.jarvis.aegis.ui.screens.SessionSetupScreen
import com.jarvis.aegis.ui.screens.launchableApps
import com.jarvis.aegis.ui.theme.AegisTheme
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = AegisStore(this)
        setContent {
            AegisTheme {
                var destination by rememberSaveable { mutableStateOf("dashboard") }
                var profile by remember { mutableStateOf(store.profile()) }
                var session by remember { mutableStateOf(store.activeSession()) }
                when (destination) {
                    "dashboard" -> DashboardScreen(
                        profile = profile,
                        activeSession = session,
                        onStartSession = { destination = "session" },
                        onOpenLearning = { if (session == null) destination = "learning" },
                        onEditProfile = { destination = "profile" },
                        onEndSession = { store.clearSession(); session = null },
                    )
                    "profile" -> ProfileScreen(profile, onSave = {
                        store.saveProfile(it); profile = it; destination = "dashboard"
                    }, onBack = { destination = "dashboard" })
                    "session" -> SessionSetupScreen(
                        apps = remember { launchableApps(packageManager) },
                        ownPackage = packageName,
                        onStart = { policy ->
                            session = FocusSession(policy = policy, activatedAt = Instant.now())
                            store.saveSession(session!!)
                            destination = "dashboard"
                        },
                        onBack = { destination = "dashboard" },
                    )
                    "learning" -> LearningScreen { destination = "dashboard" }
                }
            }
        }
    }
}
