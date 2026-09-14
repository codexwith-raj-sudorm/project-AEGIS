package com.jarvis.aegis.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.aegis.session.ChallengeDifficulty
import com.jarvis.aegis.session.SessionMode
import com.jarvis.aegis.session.SessionPolicy
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.components.AegisOutlineButton
import java.time.Duration

data class LaunchableApp(val label: String, val packageName: String)
private enum class PackageSelection { TARGETS, ESSENTIALS }

fun launchableApps(packageManager: PackageManager): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(intent, 0)
        .map { LaunchableApp(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
        .distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

@Composable
fun SessionSetupScreen(
    apps: List<LaunchableApp>,
    ownPackage: String,
    requiredEssentialPackages: Set<String>,
    availableSubjects: Set<String>,
    onContinue: (SessionPolicy) -> Unit,
    onBack: () -> Unit,
) {
    var mode by remember { mutableStateOf(SessionMode.STANDARD) }
    var minutes by remember { mutableFloatStateOf(45f) }
    var exitDelay by remember { mutableFloatStateOf(5f) }
    var targets by remember { mutableStateOf(emptySet<String>()) }
    var essential by remember { mutableStateOf(requiredEssentialPackages + ownPackage) }
    var selectedSubjects by remember { mutableStateOf(availableSubjects.ifEmpty { setOf("Mathematics") }) }
    var difficulty by remember { mutableStateOf(ChallengeDifficulty.INTERMEDIATE) }
    var amnesty by remember { mutableStateOf(true) }
    var packageSelection by remember { mutableStateOf(PackageSelection.TARGETS) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("> SESSION_ENROLLMENT // ${if (mode == SessionMode.EXTREME) "HARD" else mode.name}")
        Text(when (mode) {
            SessionMode.STANDARD -> "LEVEL 1: CHALLENGE GATE // IMMEDIATE IN-APP EXIT"
            SessionMode.STRICT -> "LEVEL 2: STREAK RESET + COOLDOWNS // DELAYED EXIT OR KEY"
            SessionMode.EXTREME -> "LEVEL 3: MAXIMUM PERSONAL MODE // RECOVERY KEY ONLY"
        })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == SessionMode.STANDARD, { mode = SessionMode.STANDARD }, { Text("STANDARD") })
            FilterChip(mode == SessionMode.STRICT, { mode = SessionMode.STRICT }, { Text("STRICT") })
            FilterChip(mode == SessionMode.EXTREME, { mode = SessionMode.EXTREME; amnesty = false }, { Text("HARD") })
        }
        Text("DURATION: ${minutes.toInt()} MINUTES")
        Slider(minutes, { minutes = it }, valueRange = 15f..if (mode == SessionMode.EXTREME) 1440f else 240f)
        Text("EXIT DELAY: ${exitDelay.toInt()} MINUTES")
        Slider(exitDelay, { exitDelay = it }, valueRange = 1f..10f, steps = 8)
        Text("CHALLENGE DIFFICULTY")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChallengeDifficulty.entries.forEach { FilterChip(difficulty == it, { difficulty = it }, { Text(it.name) }) }
        }
        Text("CHALLENGE SUBJECTS")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            availableSubjects.forEach { subject ->
                FilterChip(subject in selectedSubjects, {
                    selectedSubjects = if (subject in selectedSubjects && selectedSubjects.size > 1) selectedSubjects - subject else selectedSubjects + subject
                }, { Text(subject.uppercase()) })
            }
        }
        Row {
            Checkbox(amnesty, { amnesty = it }, enabled = mode != SessionMode.EXTREME)
            Text(if (mode == SessionMode.EXTREME) "AMNESTY DISABLED IN HARD MODE" else "ALLOW SINCERITY POINTS", Modifier.padding(top = 12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(packageSelection == PackageSelection.TARGETS, { packageSelection = PackageSelection.TARGETS }, { Text("TARGETS (${targets.size})") })
            FilterChip(packageSelection == PackageSelection.ESSENTIALS, { packageSelection = PackageSelection.ESSENTIALS }, { Text("ESSENTIAL (${essential.size})") })
        }
        LazyColumn(Modifier.weight(1f)) {
            items(apps.filter { it.packageName != ownPackage }, key = { it.packageName }) { app ->
                val checked = if (packageSelection == PackageSelection.TARGETS) app.packageName in targets else app.packageName in essential
                Row(Modifier.fillMaxWidth()) {
                    Checkbox(checked, { selected ->
                        if (packageSelection == PackageSelection.TARGETS) {
                            targets = if (selected) targets + app.packageName else targets - app.packageName
                            if (selected) essential = essential - app.packageName
                        } else {
                            essential = if (selected) essential + app.packageName else essential - app.packageName
                            if (selected) targets = targets - app.packageName
                        }
                    }, enabled = app.packageName !in requiredEssentialPackages)
                    Text("${app.label}\n${app.packageName}", Modifier.padding(top = 8.dp))
                }
            }
        }
        AegisButton("[ REVIEW AND AUTHORIZE ]", {
            onContinue(
                SessionPolicy(
                    mode = mode,
                    duration = Duration.ofMinutes(minutes.toLong()),
                    targetPackages = targets,
                    essentialPackages = essential,
                    exitDelay = Duration.ofMinutes(exitDelay.toLong()),
                    amnestyEnabled = amnesty && mode != SessionMode.EXTREME,
                    challengeSubjects = selectedSubjects,
                    difficulty = difficulty,
                ),
            )
        }, accent = mode == SessionMode.EXTREME, enabled = targets.isNotEmpty() && selectedSubjects.isNotEmpty())
        AegisOutlineButton("[ CANCEL ]", onBack)
    }
}
