package no.muscly.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import no.muscly.app.data.Dates
import no.muscly.app.domain.*
import no.muscly.app.notify.Reminders
import no.muscly.app.ui.components.ChoiceCard
import no.muscly.app.ui.components.Stepper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipGroup(options: List<T>, selected: Set<T>, label: (T) -> String, onToggle: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { o -> FilterChip(selected = o in selected, onClick = { onToggle(o) }, label = { Text(label(o)) }) }
    }
}

fun <T> Set<T>.toggle(v: T) = if (v in this) this - v else this + v

@Composable
fun LevelPicker(value: Level, onChange: (Level) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Level.values().forEachIndexed { i, l -> ChoiceCard(l.label, l.description, value == l, emoji = listOf("🌱", "🌿", "🌳")[i]) { onChange(l) } }
    }
}

@Composable
fun FocusPicker(value: Focus, onChange: (Focus) -> Unit) {
    val emojis = mapOf(Focus.AUTO to "⚖️", Focus.HELKROPP to "🔥", Focus.OVERKROPP to "🏋️", Focus.ARMER to "💪", Focus.BEIN to "🦵", Focus.KJERNE to "🧱", Focus.KONDISJON to "🚴", Focus.KOMBINASJON to "⚡")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Focus.values().forEach { f -> ChoiceCard(f.label, f.description, value == f, emoji = emojis[f]) { onChange(f) } }
    }
}

@Composable
fun SchedulePicker(p: UserProfile, onChange: (UserProfile) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScheduleType.values().forEach { s -> ChoiceCard(s.label, s.description, p.schedule == s) { onChange(p.copy(schedule = s)) } }
        if (p.schedule == ScheduleType.EGEN) {
            Text("Velg dager:", style = MaterialTheme.typography.titleSmall)
            ChipGroup((0..6).toList(), p.customDays, { Dates.dayShort[it] }) { onChange(p.copy(customDays = p.customDays.toggle(it))) }
        }
        val plan = ProgramGenerator.weekPlan(p)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(14.dp)) {
                Text("Slik blir uka di:", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                plan.forEachIndexed { i, d ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(Dates.dayShort[i], Modifier.width(44.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${d.emoji} ${d.title}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (p.schedule == ScheduleType.HVER_DAG) Text("Hver dag betyr variasjon: harde dager, lette dager og hvile – musklene trenger det for å vokse.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
fun MinutesPicker(value: Int, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15, 20, 30, 45).forEach { m -> FilterChip(selected = value == m, onClick = { onChange(m) }, label = { Text("$m min") }) }
    }
}

@Composable
fun ExercisePrefs(p: UserProfile, onChange: (UserProfile) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("👍 = liker (kommer oftere)   🚫 = vil unngå", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExerciseLibrary.all.filter { it.category != Category.BEVEGELIGHET }.forEach { e ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(e.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                FilterChip(selected = e.id in p.liked, onClick = { onChange(p.copy(liked = p.liked.toggle(e.id), avoided = p.avoided - e.id)) }, label = { Text("👍") })
                Spacer(Modifier.width(6.dp))
                FilterChip(selected = e.id in p.avoided, onClick = { onChange(p.copy(avoided = p.avoided.toggle(e.id), liked = p.liked - e.id)) }, label = { Text("🚫") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditor(p: UserProfile, onChange: (UserProfile) -> Unit, showTest: Boolean = false) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(Reminders.hasPermission(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Treningspåminnelser", style = MaterialTheme.typography.titleMedium)
                Text("Varsel på treningsdager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(p.remindersOn, onCheckedChange = {
                onChange(p.copy(remindersOn = it))
                if (it && !granted && Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
        }
        if (p.remindersOn) {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Tidspunkt: %02d:%02d".format(p.reminderHour, p.reminderMinute))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vennlig påminnelse senere hvis økten ikke er gjort", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(p.followUpOn, onCheckedChange = { onChange(p.copy(followUpOn = it)) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ukeoppsummering søndag kveld", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(p.weeklySummaryOn, onCheckedChange = { onChange(p.copy(weeklySummaryOn = it)) })
            }
            if (!granted && Build.VERSION.SDK_INT >= 33) {
                AssistChip(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }, label = { Text("⚠️ Varsler er ikke tillatt – trykk for å tillate") })
            }
            if (showTest && granted) TextButton(onClick = {
                val (t, b) = Reminders.Texts.daily(ProgramGenerator.weekPlan(p)[Dates.dow(Dates.today())].let { if (it == DayType.HVILE) DayType.HELKROPP else it }, 0)
                Reminders.post(ctx, 99, t, b)
            }) { Text("Send et testvarsel") }
        }
    }
    if (showPicker) {
        val state = rememberTimePickerState(p.reminderHour, p.reminderMinute, is24Hour = true)
        AlertDialog(onDismissRequest = { showPicker = false },
            confirmButton = { TextButton(onClick = { onChange(p.copy(reminderHour = state.hour, reminderMinute = state.minute)); showPicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Avbryt") } },
            text = { TimePicker(state) })
    }
}

@Composable
fun AgePicker(age: Int, onChange: (Int) -> Unit) = Stepper(age, onChange, min = 10, max = 80, suffix = " år", big = true)

@Composable
fun GoalPicker(p: UserProfile, onChange: (UserProfile) -> Unit) {
    val options = Goal.values().filter { it != Goal.VEKTNEDGANG || p.adult }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { g ->
            ChoiceCard(g.label, g.description, g in p.goals, emoji = if (g == Goal.RYGGHELSE) "🧘" else "⚖️") { onChange(p.copy(goals = p.goals.toggle(g))) }
        }
    }
}
