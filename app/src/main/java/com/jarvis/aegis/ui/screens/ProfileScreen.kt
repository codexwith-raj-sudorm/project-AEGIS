package com.jarvis.aegis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jarvis.aegis.accountability.AccountabilityEvent
import com.jarvis.aegis.accountability.AccountabilityMessages
import com.jarvis.aegis.profile.AgeBand
import com.jarvis.aegis.profile.LearnerProfile
import com.jarvis.aegis.profile.MotivationProfile
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.components.AegisOutlineButton

private val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "Computer Science")

@Composable
fun ProfileScreen(initial: LearnerProfile, onSave: (LearnerProfile) -> Unit, onBack: () -> Unit) {
    var profile by remember { mutableStateOf(initial) }
    val preview = AccountabilityMessages().message(profile.motivation, profile.ageBand, AccountabilityEvent.OVERRIDE_REQUEST)
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("> LEARNER_PROFILE", modifier = Modifier.semantics { heading() })
        OutlinedTextField(profile.displayName, { profile = profile.copy(displayName = it.take(60)) }, label = { Text("DISPLAY NAME") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(profile.grade, { profile = profile.copy(grade = it.take(40)) }, label = { Text("CLASS / GRADE") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(profile.curriculum, { profile = profile.copy(curriculum = it.take(60)) }, label = { Text("CURRICULUM") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("AGE BAND")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AgeBand.entries.forEach { band ->
                FilterChip(profile.ageBand == band, {
                    val motivation = profile.motivation.takeIf { it in profile.copy(ageBand = band).allowedMotivations } ?: MotivationProfile.REFLECTIVE
                    profile = profile.copy(ageBand = band, motivation = motivation)
                }, { Text(band.label) })
            }
        }
        Text("SUBJECTS")
        subjects.forEach { subject ->
            Row {
                Checkbox(subject in profile.subjects, { selected -> profile = profile.copy(subjects = if (selected) profile.subjects + subject else profile.subjects - subject) })
                Text(subject, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Text("ACCOUNTABILITY PROFILE")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            profile.allowedMotivations.forEach { mode ->
                FilterChip(profile.motivation == mode, { profile = profile.copy(motivation = mode) }, { Text(mode.label.uppercase()) })
            }
        }
        Text("MESSAGE PREVIEW")
        Text(preview)
        OutlinedTextField(
            profile.commitmentMessage,
            { profile = profile.copy(commitmentMessage = it.take(280)) },
            label = { Text("OPTIONAL MESSAGE TO YOUR FUTURE SELF") },
            supportingText = { Text("${profile.commitmentMessage.length}/280. Stored locally and encrypted.") },
            modifier = Modifier.fillMaxWidth(), minLines = 3,
        )
        AegisButton("[ SAVE PROFILE ]", { onSave(profile) }, accent = true, enabled = profile.subjects.isNotEmpty())
        AegisOutlineButton("[ CANCEL ]", onBack)
    }
}
