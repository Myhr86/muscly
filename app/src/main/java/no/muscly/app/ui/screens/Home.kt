package no.muscly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import no.muscly.app.data.Dates
import no.muscly.app.data.Repository
import no.muscly.app.domain.*
import no.muscly.app.ui.AppState
import no.muscly.app.ui.components.*
import no.muscly.app.ui.startWorkout
import no.muscly.app.ui.theme.NumberStyle

val habitTips = listOf(
    "😴 Sov 9–10 timer – det er da musklene vokser.",
    "💧 Drikk et glass vann før og etter trening.",
    "🥚 Proteinrik mat som egg, melk og fisk hjelper musklene å bygge seg opp.",
    "🚶 Gå eller sykle til skolen – all aktivitet teller!",
    "🍎 Spis frokost – kroppen trenger energi for å bli sterk.",
    "📱 Ta en aktiv pause fra skjermen: 20 knebøy hver time!",
    "🧘 Lette dager og hviledager er en del av treningen.",
)

@Composable
fun HomeScreen(st: AppState, nav: NavHostController) {
    val p = st.profile ?: return
    val type = st.todayType
    val workout = st.buildWorkout(type)
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hilsen
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${Dates.dayNames[Dates.dow(st.today)]} ${Dates.label(st.today)}", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                Text(if (p.name.isNotBlank()) "Hei, ${p.name}! 👋" else "Hei! 👋", style = MaterialTheme.typography.headlineMedium)
            }
            val weeks = Repository.weeksInARow(st.sessions, st.today)
            if (weeks > 0) Pill("🔥 $weeks ${if (weeks == 1) "uke" else "uker"} på rad", color = cs.tertiaryContainer, textColor = cs.onTertiaryContainer)
        }

        // Ukens fremdrift
        MCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val done = st.sessionsThisWeek.size; val planned = st.plannedThisWeek
                ProgressRing(done / planned.coerceAtLeast(1).toFloat(), 96.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$done/$planned", style = NumberStyle.copy(fontSize = 26.sp))
                        Text("økter", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column {
                    Text("Denne uka", style = MaterialTheme.typography.titleMedium)
                    Text(when {
                        done == 0 -> "Uka er klar – første økt venter!"
                        done >= planned -> "Ukemålet er nådd! Sterkt jobbet 🎉"
                        else -> "Bra! ${planned - done} økter igjen denne uka."
                    }, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            WeekStrip(st)
        }

        // Dagens økt
        TodayCard(st, type, workout, nav)

        // Neste milepæl
        val snap = Repository.snapshot(st.sessions, st.logs, st.today)
        val next = Milestones.all.firstOrNull { m -> st.milestones.none { it.id == m.id } && !m.check(snap) }
        if (next != null) MCard(onClick = { nav.navigate("progress") }) {
            Text("NESTE MILEPÆL", style = MaterialTheme.typography.labelMedium, color = cs.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(next.emoji, fontSize = 30.sp); Spacer(Modifier.width(12.dp))
                Column { Text(next.title, style = MaterialTheme.typography.titleMedium); Text(next.description, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant) }
            }
        }

        // Dagens tips
        val tips = if (p.adult) habitTips.map { it.replace("Sov 9–10 timer", "Sov 7–9 timer").replace("Gå eller sykle til skolen", "Gå eller sykle til jobb eller butikken").replace("Spis frokost – kroppen trenger energi for å bli sterk", "Proteinrik frokost gjør det lettere å holde energien oppe") } else habitTips
        MCard(color = cs.secondaryContainer) {
            Text("DAGENS VANE", style = MaterialTheme.typography.labelMedium, color = cs.onSecondaryContainer)
            Text(tips[Math.floorMod(st.today, tips.size.toLong()).toInt()], style = MaterialTheme.typography.bodyLarge, color = cs.onSecondaryContainer)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun WeekStrip(st: AppState) {
    val cs = MaterialTheme.colorScheme
    val doneDays = st.sessions.map { it.day }.toSet()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        st.weekPlan.forEachIndexed { i, d ->
            val day = st.weekStart + i
            val isToday = day == st.today
            val done = day in doneDays
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Dates.dayShort[i].take(1), style = MaterialTheme.typography.labelMedium, color = if (isToday) cs.primary else cs.onSurfaceVariant, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.size(38.dp).clip(CircleShape)
                    .background(when { done -> cs.primary; d == DayType.HVILE -> cs.surfaceVariant.copy(alpha = 0.4f); else -> cs.surfaceVariant })
                    .border(if (isToday) 2.dp else 0.dp, if (isToday) cs.primary else cs.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(if (done) "✓" else d.emoji, fontSize = if (done) 18.sp else 15.sp, color = if (done) cs.onPrimary else cs.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TodayCard(st: AppState, type: DayType, w: PlannedWorkout, nav: NavHostController) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
        .background(Brush.linearGradient(listOf(cs.primaryContainer, cs.secondaryContainer))).padding(20.dp)) {
        Text("DAGENS ØKT", style = MaterialTheme.typography.labelLarge, color = cs.onPrimaryContainer)
        when {
            st.doneToday -> {
                Text("Fullført ✅", style = MaterialTheme.typography.headlineMedium, color = cs.onPrimaryContainer)
                Text("Kjempebra! Nå er det hvile, god mat og søvn som gjør jobben.", style = MaterialTheme.typography.bodyLarge, color = cs.onPrimaryContainer)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { nav.startWorkout(DayType.LETT, 15) }) { Text("Lyst på mer? Lett tøyeøkt") }
            }
            type == DayType.HVILE -> {
                Text("😴 Hviledag", style = MaterialTheme.typography.headlineMedium, color = cs.onPrimaryContainer)
                Text("Musklene blir sterkere når de hviler. En gåtur, sykkeltur eller lek ute er fint!", style = MaterialTheme.typography.bodyLarge, color = cs.onPrimaryContainer)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { nav.startWorkout(DayType.LETT, 15) }) { Text("Lett aktivitet i stedet") }
            }
            else -> {
                Text("${type.emoji} ${type.title}", style = MaterialTheme.typography.headlineSmall, color = cs.onPrimaryContainer)
                Text("ca. ${w.estimatedMinutes} min · ${w.exercises.size} øvelser · ${type.intensity.label}", style = MaterialTheme.typography.bodyMedium, color = cs.onPrimaryContainer.copy(alpha = 0.8f))
                Spacer(Modifier.height(12.dp))
                w.exercises.forEach { pe ->
                    val ex = ExerciseLibrary.get(pe.exerciseId); val stg = ex.stages[pe.stageIndex]
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("•", color = cs.onPrimaryContainer); Spacer(Modifier.width(8.dp))
                        Text(stg.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = cs.onPrimaryContainer)
                        Text("${pe.sets} × ${pe.target} ${stg.measure.unit.substringBefore(" ")}", style = MaterialTheme.typography.labelLarge, color = cs.onPrimaryContainer, textAlign = TextAlign.End)
                    }
                }
                Spacer(Modifier.height(16.dp))
                BigButton("Start økt", icon = { Icon(Icons.Filled.PlayArrow, null) }) { nav.startWorkout(type) }
            }
        }
    }
}
