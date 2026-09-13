package com.jarvis.aegis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.aegis.profile.AgeBand
import com.jarvis.aegis.profile.LearnerProfile
import com.jarvis.aegis.profile.MotivationProfile
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.components.AegisOutlineButton

private val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "Computer Science")

@Composable
fun ProfileScreen(initial: LearnerProfile, onSave: (LearnerProfile) -> Unit, onBack: () -> Unit) {
    var profile by remember { mutableStateOf(initial) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("> LEARNER_PROFILE")
        OutlinedTextField(profile.displayName, { profile = profile.copy(displayName = it) }, label = { Text("DISPLAY NAME") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(profile.grade, { profile = profile.copy(grade = it) }, label = { Text("CLASS / GRADE") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(profile.curriculum, { profile = profile.copy(curriculum = it) }, label = { Text("CURRICULUM") }, modifier = Modifier.fillMaxWidth())
        Text("AGE BAND")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AgeBand.entries.forEach { band -> AegisButton(band.label, { profile = profile.copy(ageBand = band) }, enabled = profile.ageBand != band) }
        }
        Text("SUBJECTS")
        subjects.forEach { subject ->
            Row {
                Checkbox(subject in profile.subjects, { selected -> profile = profile.copy(subjects = if (selected) profile.subjects + subject else profile.subjects - subject) })
                Text(subject, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Text("ACCOUNTABILITY: ${profile.motivation.label}")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            profile.allowedMotivations.forEach { mode ->
                if (mode != MotivationProfile.HARD_TRUTH || profile.ageBand == AgeBand.ADULT) {
                    androidx.compose.material3.TextButton(onClick = { profile = profile.copy(motivation = mode) }) { Text(mode.name) }
                }
            }
        }
        AegisButton("[ SAVE PROFILE ]", { onSave(profile) }, accent = true, enabled = profile.subjects.isNotEmpty())
        AegisOutlineButton("[ CANCEL ]", onBack)
    }
}
