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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.aegis.data.AegisStore
import com.jarvis.aegis.recovery.RecoveryAttemptLimiter
import com.jarvis.aegis.recovery.RecoveryCodeManager
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.components.AegisOutlineButton
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
fun RecoveryCodeScreen(code: String, onConfirmed: () -> Unit) {
    var copiedConfirmation by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("> OWNER_RECOVERY_CODE")
        Text("SAVE THIS CODE OUTSIDE THE DEVICE. IT IS SHOWN ONCE.")
        Text(code)
        OutlinedTextField(copiedConfirmation, { copiedConfirmation = it }, label = { Text("TYPE SAVED") }, modifier = Modifier.fillMaxWidth())
        AegisButton("[ I SAVED THE CODE ]", onConfirmed, accent = true, enabled = copiedConfirmation.trim().equals("SAVED", ignoreCase = true))
    }
}

@Composable
fun SessionExitScreen(store: AegisStore, onEnded: () -> Unit, onBack: () -> Unit) {
    val session = remember { store.activeSession() }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("A DELAYED EXIT PRESERVES CONSCIOUS CHOICE. RECOVERY CODE EXITS IMMEDIATELY.") }
    var availableAt by remember { mutableStateOf(store.exitAvailableAt()) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(availableAt) {
        while (availableAt != null && nowMillis < availableAt!!.toEpochMilli()) {
            delay(1_000); nowMillis = System.currentTimeMillis()
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("> CONSCIOUS_OVERRIDE")
        Text(status)
        if (availableAt == null) {
            AegisButton("[ REQUEST DELAYED EXIT ]", {
                val now = Instant.now()
                availableAt = now.plus(session?.policy?.exitDelay ?: java.time.Duration.ofMinutes(5))
                store.saveExitRequest(now, availableAt!!)
            })
        } else {
            val remaining = ((availableAt!!.toEpochMilli() - nowMillis).coerceAtLeast(0) + 999) / 1000
            Text("EXIT AVAILABLE IN: ${remaining}s")
            AegisButton("[ END SESSION AND OPEN TARGETS ]", {
                store.clearSession(); onEnded()
            }, enabled = remaining == 0L)
            AegisOutlineButton("[ CANCEL EXIT REQUEST ]", { store.cancelExitRequest(); availableAt = null })
        }
        OutlinedTextField(code, { code = it }, label = { Text("OWNER RECOVERY CODE") }, modifier = Modifier.fillMaxWidth())
        AegisButton("[ VERIFY AND DECOMMISSION ]", {
            val now = Instant.now()
            val limiter = RecoveryAttemptLimiter()
            val attemptState = store.recoveryAttemptState()
            val decision = limiter.decision(attemptState, now)
            if (!decision.allowed) {
                status = "RECOVERY LOCKED FOR ${decision.remaining.seconds + 1} SECONDS."
            } else {
                val verifier = store.recoveryVerifier()
                if (verifier != null && RecoveryCodeManager().verify(code.toCharArray(), verifier)) {
                    store.saveRecoveryAttemptState(limiter.onSuccess())
                    store.clearSession()
                    onEnded()
                } else {
                    val failed = limiter.onFailure(attemptState, now)
                    store.saveRecoveryAttemptState(failed)
                    status = "RECOVERY CODE REJECTED. ATTEMPT ${failed.failures}."
                }
            }
            code = ""
        }, accent = true, enabled = code.isNotBlank())
        AegisOutlineButton("[ RETURN TO COMMITMENT ]", onBack)
    }
}
