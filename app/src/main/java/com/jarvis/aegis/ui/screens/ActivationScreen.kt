package com.jarvis.aegis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.aegis.session.SessionMode
import com.jarvis.aegis.session.SessionPolicy
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.components.AegisOutlineButton
import kotlinx.coroutines.delay
import java.security.SecureRandom

private val confirmationPhrases = listOf(
    "I CHOOSE FOCUS OVER IMPULSE",
    "MY COMMITMENT OUTLASTS DISTRACTION",
    "I UNDERSTAND AND ACT DELIBERATELY",
)

fun randomConfirmationPhrase(random: SecureRandom = SecureRandom()): String =
    confirmationPhrases[random.nextInt(confirmationPhrases.size)]

@Composable
fun ActivationScreen(
    policy: SessionPolicy,
    recoveryCode: String,
    onAuthenticate: ((Boolean, String?) -> Unit) -> Unit,
    onActivate: () -> Unit,
    onCancel: () -> Unit,
) {
    val phrase = remember { randomConfirmationPhrase() }
    var phraseEntry by remember { mutableStateOf("") }
    var recoveryEntry by remember { mutableStateOf("") }
    var authenticated by remember { mutableStateOf(policy.mode == SessionMode.STANDARD) }
    var authStatus by remember { mutableStateOf(if (authenticated) "STANDARD MODE DOES NOT REQUIRE DEVICE AUTH." else "DEVICE AUTHENTICATION REQUIRED.") }
    var countdown by remember { mutableIntStateOf(if (policy.mode == SessionMode.EXTREME) 30 else 5) }
    LaunchedEffect(Unit) {
        while (countdown > 0) { delay(1_000); countdown-- }
    }
    val phraseMatches = phraseEntry.trim().equals(phrase, ignoreCase = true)
    val recoveryMatches = recoveryEntry.trim() == recoveryCode
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("> FINAL_AUTHORIZATION // ${policy.mode.name}")
        Text("TARGETS: ${policy.targetPackages.size}")
        Text("DURATION: ${policy.duration.toMinutes()} MINUTES")
        Text("EXIT DELAY: ${policy.exitDelay.toMinutes()} MINUTES")
        Text("DIFFICULTY: ${policy.difficulty.name}")
        Text("SUBJECTS: ${policy.challengeSubjects.joinToString()}")
        Text("RECOVERY REMAINS AVAILABLE. ANDROID OWNER CONTROLS ARE NOT BLOCKED.")
        Text("SAVE AND RE-ENTER THIS ONE-TIME RECOVERY CODE:")
        Text(recoveryCode)
        OutlinedTextField(recoveryEntry, { recoveryEntry = it }, label = { Text("RECOVERY CODE") }, modifier = Modifier.fillMaxWidth())
        Text("TYPE: $phrase")
        OutlinedTextField(phraseEntry, { phraseEntry = it }, label = { Text("CONFIRMATION PHRASE") }, modifier = Modifier.fillMaxWidth())
        if (policy.mode == SessionMode.EXTREME) {
            AegisButton("[ AUTHENTICATE DEVICE OWNER ]", {
                onAuthenticate { success, error ->
                    authenticated = success
                    authStatus = if (success) "DEVICE OWNER AUTHENTICATED." else error ?: "AUTHENTICATION FAILED."
                }
            }, enabled = !authenticated)
        }
        Text(authStatus)
        Text(if (countdown > 0) "CANCELLATION WINDOW: ${countdown}s" else "ACTIVATION READY.")
        AegisButton(
            "[ ACTIVATE ENFORCEMENT ]", onActivate, accent = true,
            enabled = countdown == 0 && phraseMatches && recoveryMatches && authenticated,
        )
        AegisOutlineButton("[ CANCEL WITHOUT ACTIVATING ]", onCancel)
    }
}
