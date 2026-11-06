package no.muscly.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.muscly.app.domain.*
import no.muscly.app.ui.components.BigButton
import no.muscly.app.ui.components.ExerciseAnimation

@Composable
fun OnboardingScreen(onDone: (UserProfile, SelfAssessment) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var p by remember { mutableStateOf(UserProfile()) }
    var sa by remember { mutableStateOf(SelfAssessment()) }
    val total = 7
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)) {
        if (step > 0) {
            LinearProgressIndicator(progress = { step / (total - 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
            Spacer(Modifier.height(16.dp))
        }
        AnimatedContent(step, Modifier.weight(1f), transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } }, label = "step") { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (s) {
                    0 -> Welcome()
                    1 -> {
                        H("Hvem trener?", "Kallenavn er valgfritt. Alt lagres bare på denne telefonen.")
                        OutlinedTextField(p.name, { p = p.copy(name = it.take(20)) }, label = { Text("Kallenavn (valgfritt)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth())
                        Text("Alder", style = MaterialTheme.typography.titleMedium)
                        AgePicker(p.age) { p = p.copy(age = it, goals = if (it < 18) p.goals - Goal.VEKTNEDGANG else p.goals) }
                    }
                    2 -> {
                        H("Hvor trent er du?", "Ingen tester – bare gjett. Appen justerer seg etter hvert.")
                        LevelPicker(p.level) { p = p.copy(level = it) }
                        Text("Hvor mange push-ups klarer du?", style = MaterialTheme.typography.titleMedium)
                        AbilityRow(sa.pushups, listOf("Vet ikke", "0", "1–3", "4–9", "10+")) { sa = sa.copy(pushups = it) }
                        Text("Pull-ups?", style = MaterialTheme.typography.titleMedium)
                        AbilityRow(sa.pullups, listOf("Vet ikke", "0", "1–2", "3–5", "6+")) { sa = sa.copy(pullups = it) }
                        Text("Planke?", style = MaterialTheme.typography.titleMedium)
                        AbilityRow(sa.plank, listOf("Vet ikke", "Under 10 s", "10–20 s", "20–45 s", "45 s+")) { sa = sa.copy(plank = it) }
                    }
                    3 -> {
                        H("Hva vil du bli bedre på?", "Velg et hovedfokus. «Balansert» er et trygt valg.")
                        FocusPicker(p.focus) { p = p.copy(focus = it) }
                        Text("Muskelgrupper du vil prioritere ekstra:", style = MaterialTheme.typography.titleMedium)
                        ChipGroup(listOf(Category.ARMER, Category.OVERKROPP, Category.BEIN, Category.KJERNE, Category.KONDISJON), p.priorities, { it.label }) { p = p.copy(priorities = p.priorities.toggle(it)) }
                        Text("Spesielle mål:", style = MaterialTheme.typography.titleMedium)
                        GoalPicker(p) { p = it }
                        Text("Tips: Du kan også beskrive målene dine med egne ord til AI-treneren senere, f.eks. «sterkere rygg og større biceps».",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    4 -> {
                        H("Når vil du trene?", "Appen legger inn hviledager så samme muskler ikke belastes hardt to dager på rad.")
                        SchedulePicker(p) { p = it }
                        Text("Tid per økt", style = MaterialTheme.typography.titleMedium)
                        MinutesPicker(p.minutes) { p = p.copy(minutes = it) }
                    }
                    5 -> {
                        H("Utstyr og kropp", "Velg det du har tilgjengelig.")
                        ChipGroup(Equipment.values().toList(), p.equipment, { it.label }) { p = p.copy(equipment = p.equipment.toggle(it)) }
                        Text("Noe som gjør vondt eller bør skånes?", style = MaterialTheme.typography.titleMedium)
                        ChipGroup(Injury.values().toList(), p.injuries, { it.label }) { p = p.copy(injuries = p.injuries.toggle(it)) }
                        Text("Øvelser som belaster disse områdene tas ut. Har du en skade, snakk med lege før du trener.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Øvelser du liker / vil unngå", style = MaterialTheme.typography.titleMedium)
                        ExercisePrefs(p) { p = it }
                    }
                    6 -> {
                        H("Påminnelser", "Vi sender et vennlig varsel på treningsdagene dine. Aldri mas!")
                        ReminderEditor(p, { p = it })
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) OutlinedButton(onClick = { step-- }, Modifier.height(58.dp)) { Text("Tilbake") }
            BigButton(when (step) { 0 -> "Kom i gang"; total - 1 -> "Lag programmet mitt 🚀"; else -> "Neste" }, Modifier.weight(1f),
                enabled = !(step == 4 && p.schedule == ScheduleType.EGEN && p.customDays.isEmpty())) {
                if (step < total - 1) step++ else onDone(p.copy(maxHardPerWeek = if (p.level == Level.ERFAREN) 4 else 3), sa)
            }
        }
    }
}

@Composable
private fun Welcome() {
    Spacer(Modifier.height(12.dp))
    Text("MUSCLY", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
    Text("Din personlige trener på mobilen", style = MaterialTheme.typography.headlineSmall)
    ExerciseAnimation("pull", Modifier.fillMaxWidth().height(240.dp), showCaption = false)
    listOf("💪 Program laget for deg – uten treningsstudio", "📈 Blir vanskeligere når DU blir sterkere", "🤖 Egen AI-trener du kan spørre om alt", "🔔 Vennlige påminnelser").forEach {
        Text(it, style = MaterialTheme.typography.titleMedium)
    }
    Text("Ingen innlogging, ingen reklame. Alt lagres på telefonen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun H(title: String, sub: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Text(sub, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilityRow(value: Ability, labels: List<String>, onChange: (Ability) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Ability.values().forEachIndexed { i, a -> FilterChip(selected = value == a, onClick = { onChange(a) }, label = { Text(labels[i], fontSize = 14.sp) }) }
    }
}
