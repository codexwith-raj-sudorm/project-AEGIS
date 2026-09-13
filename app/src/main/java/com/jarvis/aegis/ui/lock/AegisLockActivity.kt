package com.jarvis.aegis.ui.lock

import android.content.Intent
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
import com.jarvis.aegis.challenge.ChallengeGenerator
import com.jarvis.aegis.challenge.ValidationResult
import com.jarvis.aegis.data.AegisStore
import com.jarvis.aegis.recovery.WatchdogManager
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
                val generator = remember { ChallengeGenerator() }
                fun nextChallenge() = generator.generate(session.policy.challengeSubjects, session.policy.difficulty)
                var challenge by remember { mutableStateOf(nextChallenge()) }
                var answer by remember { mutableStateOf("") }
                var seconds by remember(challenge.id) { mutableIntStateOf(challenge.timeLimitSeconds) }
                val store = remember { AegisStore(this@AegisLockActivity) }
                val profile = remember { store.profile() }
                val accountability = remember { AccountabilityMessages() }
                var tokenBalance by remember { mutableIntStateOf(store.tokens()) }
                var status by remember { mutableStateOf("ACCESS DENIED. COGNITIVE VERIFICATION REQUIRED.") }

                LaunchedEffect(challenge.id) {
                    while (seconds > 0) { delay(1_000); seconds-- }
                    store.updateProgress(0)
                    status = accountability.message(profile.motivation, profile.ageBand, AccountabilityEvent.TIMEOUT)
                    challenge = nextChallenge()
                    answer = ""
                }

                Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("> AEGIS_GATE // ${challenge.category.name} // TTK: ${seconds}s")
                    Text("TARGET: $target")
                    Text("STREAK: ${store.activeSession()?.streak ?: 0}/20 // TOKENS: $tokenBalance")
                    Text(status)
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
                                val nextStreak = (store.activeSession()?.streak ?: 0) + 1
                                if (nextStreak >= 20) {
                                    tokenBalance++
                                    store.updateProgress(0, tokenBalance)
                                } else store.updateProgress(nextStreak)
                                grantAndOpen(store, target)
                            }
                            is ValidationResult.IncorrectUnit -> status = "UNIT REJECTED. EXPECTED ${result.expectedUnit}."
                            else -> {
                                store.updateProgress(0)
                                status = "INCORRECT. STREAK RESET. NEW CHALLENGE."
                                challenge = nextChallenge()
                                answer = ""
                            }
                        }
                    })
                    if (session.policy.amnestyEnabled && tokenBalance > 0) {
                        AegisButton("[ SPEND 1 SINCERITY POINT // 15 MINUTES ]", {
                            if (store.spendToken()) {
                                tokenBalance--
                                grantAndOpen(store, target)
                            }
                        }, accent = true)
                    }
                    Text("LEAVING THIS GATE INVALIDATES THE CURRENT CHALLENGE.")
                }
            }
        }
        window.decorView.post { watchdog.markGateHealthy() }
    }

    private fun grantAndOpen(store: AegisStore, target: String) {
        completed = true
        store.grantTarget(target, Instant.now().plusSeconds(15 * 60))
        packageManager.getLaunchIntentForPackage(target)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
        finish()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!completed) finish()
    }

    companion object { const val EXTRA_TARGET = "target_package" }
}
