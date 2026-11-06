package no.muscly.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import no.muscly.app.data.Dates
import no.muscly.app.domain.*
import no.muscly.app.ui.AppState
import no.muscly.app.ui.components.*
import no.muscly.app.ui.startWorkout

@Composable
fun TrainingScreen(st: AppState, nav: NavHostController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Text("Trening", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp))
        TabRow(tab, Modifier.padding(top = 8.dp), containerColor = MaterialTheme.colorScheme.background) {
            listOf("Mitt program", "Øvelser", "Ekstra økt").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> ProgramTab(st, nav)
            1 -> LibraryTab(st, nav)
            else -> ExtraTab(st, nav)
        }
    }
}

@Composable
private fun ProgramTab(st: AppState, nav: NavHostController) {
    var open by remember { mutableIntStateOf(Dates.dow(st.today)) }
    val p = st.profile ?: return
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("${p.focus.label} · ${p.schedule.label} · ${p.minutes} min", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(7) { i ->
            val d = st.weekPlan[i]
            val day = st.weekStart + i
            val done = st.sessions.any { it.day == day }
            MCard(onClick = { open = if (open == i) -1 else i }, color = if (day == st.today) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Dates.dayNames[i] + if (day == st.today) " · i dag" else "", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${d.emoji} ${d.title}", style = MaterialTheme.typography.titleMedium)
                    }
                    if (done) Pill("✓ Fullført", color = MaterialTheme.colorScheme.primary, textColor = MaterialTheme.colorScheme.onPrimary)
                    else if (d != DayType.HVILE) Pill(d.intensity.label)
                }
                if (open == i && d != DayType.HVILE) {
                    val w = st.buildWorkout(d)
                    Spacer(Modifier.height(10.dp))
                    w.exercises.forEach { pe ->
                        val ex = ExerciseLibrary.get(pe.exerciseId); val stg = ex.stages[pe.stageIndex]
                        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { nav.navigate("exercise/${ex.id}") }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stg.name, Modifier.weight(1f))
                            Text("${pe.sets} × ${pe.target} ${stg.measure.unit}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = { nav.startWorkout(d) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start denne økten (ca. ${w.estimatedMinutes} min)")
                    }
                } else if (open == i) {
                    Text("Hvile. Rolig aktivitet som gåtur er fint. 🌿", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        item {
            Text("Programmet passer på at samme muskelgrupper ikke trenes hardt to dager på rad, og at du har lette dager eller hvile mellom.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LibraryTab(st: AppState, nav: NavHostController) {
    var q by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf<Category?>(null) }
    val list = ExerciseLibrary.all.filter { (cat == null || it.category == cat) && (q.isBlank() || it.name.contains(q, true) || it.keywords.any { k -> k.contains(q, true) }) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("Søk etter øvelse") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, shape = MaterialTheme.shapes.large)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(cat == null, { cat = null }, { Text("Alle") })
                Category.values().forEach { c -> FilterChip(cat == c, { cat = if (cat == c) null else c }, { Text(c.label) }) }
            }
        }
        items(list, key = { it.id }) { ex ->
            val s = st.states[ex.id]
            val stg = ex.stages[(s?.stage ?: 0).coerceIn(0, ex.stages.lastIndex)]
            val usable = st.profile?.let { ExerciseLibrary.available(it, ex) } ?: true
            MCard(onClick = { nav.navigate("exercise/${ex.id}") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExerciseAnimation(stg.poseKey, Modifier.size(84.dp), playing = false, showCaption = false, staticFrame = 1)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, style = MaterialTheme.typography.titleMedium)
                        Text(ex.primary.joinToString { it.label }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill(ex.category.label)
                            if (!usable) Pill("Ikke i programmet", color = MaterialTheme.colorScheme.errorContainer, textColor = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
fun Modifier.horizontalScrollCompat(): Modifier = this.then(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()))

@Composable
private fun ExtraTab(st: AppState, nav: NavHostController) {
    var minutes by remember { mutableIntStateOf(15) }
    val recent = st.sessions.map { it.day to (runCatching { DayType.valueOf(it.dayType) }.getOrDefault(DayType.HELKROPP)) }
    val suggested = WorkoutBuilder.suggestForToday(recent, st.today)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Lag en ekstra økt", style = MaterialTheme.typography.titleLarge)
            Text("Velg lengde og type. Anbefalt i dag er basert på hva du har trent de siste dagene.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            MinutesPicker(minutes) { minutes = it }
        }
        items(DayType.values().filter { it != DayType.HVILE }) { d ->
            val rec = d == suggested
            ChoiceCard(d.title + if (rec) " ⭐ Anbefalt" else "", "${d.intensity.label} · ca. ${st.buildWorkout(d, minutes).estimatedMinutes} min", selected = rec, emoji = d.emoji) { nav.startWorkout(d, minutes) }
        }
    }
}
