package com.jarvis.aegis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jarvis.aegis.profile.LearnerProfile
import com.jarvis.aegis.session.FocusSession
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.components.AegisOutlineButton
import com.jarvis.aegis.ui.theme.AegisWhite
import java.time.Duration
import java.time.Instant

@Composable
fun DashboardScreen(
    profile: LearnerProfile,
    activeSession: FocusSession?,
    onStartSession: () -> Unit,
    onOpenLearning: () -> Unit,
    onEditProfile: () -> Unit,
    onEndSession: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("> AEGIS_CORE", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Text("V0.1 // ${if (activeSession == null) "READY" else activeSession.policy.mode.name}")
            }
            HorizontalDivider(thickness = 2.dp, color = AegisWhite)
            Text("OPERATOR: ${profile.displayName.ifBlank { "UNCONFIGURED" }}")
            Text("LEVEL: ${profile.grade} // ${profile.curriculum}")
            Text("SUBJECTS: ${profile.subjects.joinToString().uppercase()}")
            if (activeSession == null) {
                AegisButton("[ START FOCUS SESSION ]", onStartSession)
                AegisButton("[ OPEN LEARNING MODE ]", onOpenLearning, accent = true)
            } else {
                val remaining = Duration.between(Instant.now(), activeSession.expiresAt).coerceAtLeast(Duration.ZERO)
                Text("SESSION ACTIVE // ${remaining.toMinutes()} MINUTES REMAIN")
                Text("TARGETS: ${activeSession.policy.targetPackages.size}")
                AegisOutlineButton("[ END SESSION ]", onEndSession)
            }
            AegisOutlineButton("[ CONFIGURE LEARNER PROFILE ]", onEditProfile)
            Text(if (activeSession == null) "STATUS: NO ACTIVE RESTRICTIONS" else "STATUS: ENFORCEMENT ARMED")
        }
    }
}
