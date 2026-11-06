package no.muscly.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import no.muscly.app.BuildConfig
import no.muscly.app.domain.*
import no.muscly.app.ui.AppState
import no.muscly.app.ui.AppViewModel
import no.muscly.app.ui.components.MCard
import no.muscly.app.ui.components.SectionTitle

@Composable
fun ProfileScreen(vm: AppViewModel, st: AppState, nav: NavHostController) {
    val saved = st.profile ?: return
    var p by remember(saved) { mutableStateOf(saved) }
    var open by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    fun save(np: UserProfile) { p = np; vm.updateProfile(np) }
    val cs = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium)
        MCard(onClick = { nav.navigate("parent") }, color = cs.secondaryContainer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FamilyRestroom, null, tint = cs.onSecondaryContainer); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Foreldredel", style = MaterialTheme.typography.titleMedium, color = cs.onSecondaryContainer)
                    Text("Oversikt og trygghetsinnstillinger for foresatte", style = MaterialTheme.typography.bodySmall, color = cs.onSecondaryContainer)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = cs.onSecondaryContainer)
            }
        }
        Section("Om meg", "${p.name.ifBlank { "Uten kallenavn" }} · ${p.age} år · ${p.level.label}", open == "me", { open = if (open == "me") null else "me" }) {
            OutlinedTextField(p.name, { save(p.copy(name = it.take(20))) }, label = { Text("Kallenavn") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            AgePicker(p.age) { save(p.copy(age = it)) }
            LevelPicker(p.level) { save(p.copy(level = it)) }
        }
        Section("Mål og fokus", p.focus.label, open == "focus", { open = if (open == "focus") null else "focus" }) {
            FocusPicker(p.focus) { save(p.copy(focus = it)) }
            Text("Prioriter ekstra:", style = MaterialTheme.typography.titleSmall)
            ChipGroup(listOf(Category.ARMER, Category.OVERKROPP, Category.BEIN, Category.KJERNE, Category.KONDISJON), p.priorities, { it.label }) { save(p.copy(priorities = p.priorities.toggle(it))) }
        }
        Section("Treningsdager og tid", "${p.schedule.label} · ${p.minutes} min", open == "plan", { open = if (open == "plan") null else "plan" }) {
            SchedulePicker(p) { save(it) }
            MinutesPicker(p.minutes) { save(p.copy(minutes = it)) }
        }
        Section("Utstyr og skader", p.equipment.joinToString { it.label }, open == "eq", { open = if (open == "eq") null else "eq" }) {
            ChipGroup(Equipment.values().toList(), p.equipment, { it.label }) { save(p.copy(equipment = p.equipment.toggle(it))) }
            Text("Skån disse områdene:", style = MaterialTheme.typography.titleSmall)
            ChipGroup(Injury.values().toList(), p.injuries, { it.label }) { save(p.copy(injuries = p.injuries.toggle(it))) }
        }
        Section("Øvelser jeg liker / vil unngå", "${p.liked.size} liker · ${p.avoided.size} unngår", open == "ex", { open = if (open == "ex") null else "ex" }) {
            ExercisePrefs(p) { save(it) }
        }
        Section("Påminnelser", if (p.remindersOn) "På · kl. %02d:%02d".format(p.reminderHour, p.reminderMinute) else "Av", open == "rem", { open = if (open == "rem") null else "rem" }) {
            ReminderEditor(p, { save(it) }, showTest = true)
        }
        Section("Utseende", when (p.theme) { "light" -> "Lyst"; "system" -> "Følg telefonen"; else -> "Mørkt" }, open == "theme", { open = if (open == "theme") null else "theme" }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("dark" to "Mørkt", "light" to "Lyst", "system" to "Følg telefonen").forEach { (k, l) -> FilterChip(p.theme == k, { save(p.copy(theme = k)) }, { Text(l) }) }
            }
        }
        Section("AI-trener", if (p.aiEnabled) "På" else "Av", open == "ai", { open = if (open == "ai") null else "ai" }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bruk AI (språkmodell) når det er internett", Modifier.weight(1f))
                Switch(p.aiEnabled, { save(p.copy(aiEnabled = it)) })
            }
            OutlinedTextField(p.coachUrl, { p = p.copy(coachUrl = it.trim()) }, label = { Text("Adresse til AI-server (valgfritt)") }, placeholder = { Text(BuildConfig.COACH_URL.ifBlank { "https://din-server.no" }) },
                singleLine = true, modifier = Modifier.fillMaxWidth(), supportingText = { Text("API-nøkkelen ligger på serveren, aldri i appen. Uten server brukes den innebygde treneren.") })
            TextButton(onClick = { save(p) }) { Text("Lagre adresse") }
            Text("Til AI-serveren sendes bare spørsmålet ditt og en anonym oppsummering av treningen (alder, nivå, økter) – aldri navn.",
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        SectionTitle("Personvern")
        Text("All treningsdata lagres lokalt på telefonen. Ingen konto, ingen reklame, ingen sporing.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        OutlinedButton(onClick = { confirmReset = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error), modifier = Modifier.fillMaxWidth()) { Text("Slett alle data og start på nytt") }
        Text("Muscly ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Slette alt?") },
        text = { Text("Alle økter, fremgang og innstillinger slettes fra telefonen. Dette kan ikke angres.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; vm.resetAll() }) { Text("Slett") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Avbryt") } })
}

@Composable
private fun Section(title: String, summary: String, open: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    MCard {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(if (open) "▴" else "▾", style = MaterialTheme.typography.titleLarge)
        }
        if (open) { Spacer(Modifier.height(12.dp)); Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
    }
}
