package com.jarvis.aegis.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarvis.aegis.security.CheckSeverity
import com.jarvis.aegis.security.EnforcementPreflight
import com.jarvis.aegis.session.SessionPolicy
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.components.AegisOutlineButton

@Composable
fun PreflightScreen(policy: SessionPolicy, onContinue: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var report by remember(policy) { mutableStateOf(EnforcementPreflight(context).run(policy)) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("> ENFORCEMENT_PREFLIGHT")
        Text("STRICT AND HARD MODES ARM ONLY AFTER ALL MANDATORY CHECKS PASS.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(report.checks, key = { it.id }) { check ->
                Column(Modifier.fillMaxWidth()) {
                    Text("${when (check.severity) { CheckSeverity.PASS -> "[PASS]"; CheckSeverity.WARNING -> "[WARN]"; CheckSeverity.BLOCKER -> "[BLOCK]" }} ${check.label.uppercase()}")
                    Text(check.detail)
                }
            }
        }
        if (report.checks.any { it.id == "accessibility" && it.severity != CheckSeverity.PASS }) {
            AegisButton("[ OPEN ACCESSIBILITY SETTINGS ]", {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.TextButton(onClick = { report = EnforcementPreflight(context).run(policy) }) { Text("RUN AGAIN") }
        }
        AegisButton("[ CONTINUE TO AUTHORIZATION ]", onContinue, accent = true, enabled = report.canActivate)
        AegisOutlineButton("[ CANCEL ]", onCancel)
    }
}
