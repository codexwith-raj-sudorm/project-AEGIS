package com.jarvis.aegis.ui.lock

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jarvis.aegis.accountability.AccountabilityEvent
import com.jarvis.aegis.accountability.AccountabilityMessages
import com.jarvis.aegis.challenge.AnswerValidator
import com.jarvis.aegis.challenge.ChallengeCoordinator
import com.jarvis.aegis.challenge.ValidationResult
import com.jarvis.aegis.data.AegisStore
import com.jarvis.aegis.recovery.WatchdogManager
import com.jarvis.aegis.session.rules
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.theme.AegisTheme
import kotlinx.coroutines.delay
import java.time.Instant

class AegisLockActivity : ComponentActivity() {
    private var completed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val watchdog = WatchdogManager(this)
        if (!watchdog.beginGateLaunch()) return finish()
        val target = intent.getStringExtra(EXTRA_TARGET) ?: return finish()
        val initialStore = AegisStore(this)
        val session = initialStore.activeSession() ?: return finish()

        setContent {
            AegisTheme {
                val store = remember { AegisStore(this@AegisLockActivity) }
                val coordinator = remember { ChallengeCoordinator(store) }
                var active by remember { mutableStateOf(coordinator.currentOrCreate(session, target)) }
                val challenge = active.challenge
                var answer by remember(active.challenge.id) { mutableStateOf("") }
                var seconds by remember(active.challenge.id) { mutableIntStateOf(active.remainingSeconds(Instant.now())) }
                val profile = remember { store.profile() }
                val accountability = remember { AccountabilityMessages() }
                var tokenBalance by remember { mutableIntStateOf(store.tokens()) }
                val cooldownDeadline = remember { intent.getLongExtra(EXTRA_COOLDOWN_UNTIL, 0L) }
                var cooldownSeconds by remember {
                    mutableIntStateOf(((cooldownDeadline - System.currentTimeMillis()).coerceAtLeast(0L) / 1000L).toInt())
                }
                var status by remember { mutableStateOf("ACCESS DENIED. COGNITIVE VERIFICATION REQUIRED.") }

                LaunchedEffect(cooldownDeadline) {
                    while (cooldownSeconds > 0) { delay(1_000); cooldownSeconds-- }
                }
                LaunchedEffect(active.challenge.id) {
                    while (cooldownSeconds > 0) delay(250)
                    while (seconds > 0) {
                        delay(250)
                        seconds = active.remainingSeconds(Instant.now())
                    }
                    if (coordinator.consume(active)) {
                        store.updateProgress(0)
                        status = accountability.message(profile.motivation, profile.ageBand, AccountabilityEvent.TIMEOUT)
                        active = coordinator.replace(session, target)
                    } else {
                        active = coordinator.currentOrCreate(session, target)
                        status = "STALE CHALLENGE REPLACED."
                    }
                }

                Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("> AEGIS_GATE // ${challenge.category.name} // TTK: ${seconds}s")
                    Text("TARGET: $target")
                    Text("CHALLENGE: ${challenge.id}")
                    Text("STREAK: ${store.activeSession()?.streak ?: 0}/20 // TOKENS: $tokenBalance")
                    Text(if (cooldownSeconds > 0) "RAPID RELAUNCH DETECTED. COOLDOWN: ${cooldownSeconds}s" else status)
                    LinearProgressIndicator(
                        progress = { seconds.toFloat() / challenge.timeLimitSeconds },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(challenge.prompt)
                    OutlinedTextField(
                        value = answer, onValueChange = { answer = it.take(32) },
                        label = { Text("ENTER VALUE${challenge.unit?.let { " ($it)" } ?: ""}") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    AegisButton("[ EXECUTE_SUBMIT ]", {
                        when (val result = AnswerValidator().validate(challenge, answer)) {
                            ValidationResult.Correct -> {
                                if (!coordinator.consume(active)) {
                                    status = "SUBMISSION REJECTED: CHALLENGE ALREADY CONSUMED."
                                    active = coordinator.currentOrCreate(session, target)
                                } else {
                                    val nextStreak = (store.activeSession()?.streak ?: 0) + 1
                                    if (nextStreak >= 20) {
                                        tokenBalance++
                                        store.updateProgress(0, tokenBalance)
                                    } else store.updateProgress(nextStreak)
                                    grantAndOpen(store, target)
                                }
                            }
                            is ValidationResult.IncorrectUnit -> status = "UNIT REJECTED. EXPECTED ${result.expectedUnit}."
                            else -> {
                                if (coordinator.consume(active)) {
                                    store.updateProgress(0)
                                    status = "INCORRECT. STREAK RESET. NEW CHALLENGE."
                                    active = coordinator.replace(session, target)
                                } else {
                                    status = "SUBMISSION REJECTED: STALE CHALLENGE."
                                    active = coordinator.currentOrCreate(session, target)
                                }
                            }
                        }
                    }, enabled = cooldownSeconds == 0)
                    if (session.policy.amnestyEnabled && session.policy.mode.rules.allowAmnesty && tokenBalance > 0) {
                        AegisButton("[ SPEND 1 SINCERITY POINT // 15 MINUTES ]", {
                            if (store.spendToken()) {
                                coordinator.consume(active)
                                tokenBalance--
                                grantAndOpen(store, target)
                            }
                        }, accent = true, enabled = cooldownSeconds == 0)
                    }
                    Text("LEAVING THIS GATE INVALIDATES THE CURRENT CHALLENGE.")
                }
            }
        }
        window.decorView.post { watchdog.markGateHealthy() }
    }

    private fun grantAndOpen(store: AegisStore, target: String) {
        completed = true
        store.resetLaunchAttempts(target)
        store.grantTarget(target, Instant.now().plusSeconds(15 * 60))
        packageManager.getLaunchIntentForPackage(target)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
        finish()
    }

    private fun invalidateForContextSwitch() {
        val store = AegisStore(this)
        store.clearActiveChallenge()
        if (store.activeSession()?.policy?.mode?.rules?.resetStreakOnContextSwitch == true) store.updateProgress(0)
        finish()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!completed) invalidateForContextSwitch()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        if (isInMultiWindowMode && !completed) invalidateForContextSwitch()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode && !completed) invalidateForContextSwitch()
    }

    companion object {
        const val EXTRA_TARGET = "target_package"
        const val EXTRA_COOLDOWN_UNTIL = "cooldown_until"
    }
}
