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
import com.jarvis.aegis.session.SessionMode
import com.jarvis.aegis.session.SessionPolicy
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.components.AegisOutlineButton
import java.time.Duration

data class LaunchableApp(val label: String, val packageName: String)

fun launchableApps(packageManager: PackageManager): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(intent, 0)
        .map { LaunchableApp(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
        .distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

@Composable
fun SessionSetupScreen(apps: List<LaunchableApp>, ownPackage: String, onStart: (SessionPolicy) -> Unit, onBack: () -> Unit) {
    var mode by remember { mutableStateOf(SessionMode.STANDARD) }
    var minutes by remember { mutableFloatStateOf(45f) }
    var targets by remember { mutableStateOf(emptySet<String>()) }
    var confirmed by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("> SESSION_ENROLLMENT // ${mode.name}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AegisButton("STANDARD", { mode = SessionMode.STANDARD; confirmed = false }, enabled = mode != SessionMode.STANDARD)
            AegisButton("EXTREME", { mode = SessionMode.EXTREME; confirmed = false }, accent = true, enabled = mode != SessionMode.EXTREME)
        }
        Text("DURATION: ${minutes.toInt()} MINUTES")
        Slider(minutes, { minutes = it }, valueRange = 15f..if (mode == SessionMode.EXTREME) 1440f else 240f)
        Text("SELECT TARGETS: ${targets.size}")
        LazyColumn(Modifier.weight(1f)) {
            items(apps.filter { it.packageName != ownPackage }, key = { it.packageName }) { app ->
                Row(Modifier.fillMaxWidth()) {
                    Checkbox(app.packageName in targets, { checked -> targets = if (checked) targets + app.packageName else targets - app.packageName })
                    Text("${app.label}\n${app.packageName}", Modifier.padding(top = 8.dp))
                }
            }
        }
        if (mode == SessionMode.EXTREME) {
            Row { Checkbox(confirmed, { confirmed = it }); Text("I understand challenges reset on deliberate context switches and recovery remains available.") }
        }
        AegisButton("[ ACTIVATE SESSION ]", {
            onStart(SessionPolicy(mode, Duration.ofMinutes(minutes.toLong()), targets, setOf(ownPackage), Duration.ofMinutes(5)))
        }, accent = mode == SessionMode.EXTREME, enabled = targets.isNotEmpty() && (mode != SessionMode.EXTREME || confirmed))
        AegisOutlineButton("[ CANCEL ]", onBack)
    }
}
