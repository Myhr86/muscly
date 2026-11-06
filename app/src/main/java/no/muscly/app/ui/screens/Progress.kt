package no.muscly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.muscly.app.data.Dates
import no.muscly.app.data.ExerciseLogEntity
import no.muscly.app.data.Repository
import no.muscly.app.domain.*
import no.muscly.app.ui.AppState
import no.muscly.app.ui.components.*
import no.muscly.app.ui.theme.Orange
import no.muscly.app.ui.theme.Pink
import no.muscly.app.ui.theme.Sky
import no.muscly.app.ui.theme.Violet

/** Data til en graf: beste sett per økt for en øvelse. Fargen viser trinnet på stigen. */
data class Series(val values: List<Float>, val labels: List<String>, val colors: List<Color>, val unit: String, val current: String)

private val stageColors = listOf(Sky, Violet, Orange, Color(0xFFB4F43C), Pink)

fun exerciseSeries(logs: List<ExerciseLogEntity>, st: AppState, id: String, weighted: Boolean = false): Series {
    val ex = ExerciseLibrary.get(id)
    val l = logs.filter { it.exerciseId == id && !it.pain && it.values().isNotEmpty() }.takeLast(20)
    val vals = l.map { if (weighted) it.weightKg * (it.values().maxOrNull() ?: 0) else (it.values().maxOrNull() ?: 0).toFloat() }
    val s = st.states[id]
    val stg = ex.stages[(s?.stage ?: 0).coerceIn(0, ex.stages.lastIndex)]
    val cur = "${stg.name} · mål ${s?.target ?: stg.start} ${stg.measure.unit}" + if (stg.weighted && s != null) " · ${ProgressionEngine.fmtKg(s.weightKg)} kg" else ""
    return Series(vals, l.map { Dates.label(it.day) }, l.map { stageColors[it.stage % stageColors.size] },
        if (weighted) "kg×reps" else stg.measure.unit.substringBefore(" "), cur)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(st: AppState) {
    val cs = MaterialTheme.colorScheme
    val snap = Repository.snapshot(st.sessions, st.logs, st.today)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Fremgang", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${st.sessions.size}", "økter totalt", Modifier.weight(1f))
            StatTile("${st.sessionsThisWeek.size}", "denne uka", Modifier.weight(1f), accent = cs.secondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${snap.weeksInARow}", "uker på rad", Modifier.weight(1f), accent = cs.tertiary)
            StatTile("${snap.cardioMinutes}", "min kondisjon", Modifier.weight(1f), accent = Pink)
        }

        MCard {
            Text("Treningsaktivitet per uke", style = MaterialTheme.typography.titleMedium)
            Text("Grønn = ukemålet nådd (${st.plannedThisWeek} økter)", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            val thisWeek = Dates.weekIndex(st.today)
            val weeks = (7 downTo 0).map { thisWeek - it }
            val counts = weeks.map { w -> st.sessions.count { Dates.weekIndex(it.day) == w } }
            BarChart(counts, weeks.map { w -> if (w == thisWeek) "Nå" else "${(thisWeek - w)}u" }, goal = st.plannedThisWeek)
        }

        ChartCard("💪 Push-ups", "Beste sett per økt", exerciseSeries(st.logs, st, "pushup"))
        ChartCard("🧱 Planken", "Lengste hold i sekunder", exerciseSeries(st.logs, st, "plank"))
        ChartCard("🧗 Pull-ups", "Beste sett per økt (heng måles i sekunder)", exerciseSeries(st.logs, st, "pullup"))
        ChartCard("💪 Bicepscurl", "Beste sett: kg × repetisjoner", exerciseSeries(st.logs, st, "curl", weighted = true))
        ChartCard("🦵 Knebøy", "Beste sett per økt", exerciseSeries(st.logs, st, "squat"))

        MCard {
            Text("🚴 Kondisjon", style = MaterialTheme.typography.titleMedium)
            Text("Minutter kondisjonstrening per uke", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            val thisWeek = Dates.weekIndex(st.today)
            val weeks = (7 downTo 0).map { thisWeek - it }
            BarChart(weeks.map { w -> st.logs.filter { Dates.weekIndex(it.day) == w }.sumOf { Repository.cardioMinutes(it) } }, weeks.map { w -> if (w == thisWeek) "Nå" else "${thisWeek - w}u" }, goal = 20)
            Text("Nivå: " + exerciseSeries(st.logs, st, "bike_interval").current, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }

        SectionTitle("Milepæler (${st.milestones.size}/${Milestones.all.size})")
        val got = st.milestones.associate { it.id to it.day }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 3) {
            Milestones.all.forEach { m ->
                val day = got[m.id]
                Column(Modifier.weight(1f).clip(MaterialTheme.shapes.large)
                    .background(if (day != null) cs.primaryContainer else cs.surfaceContainer).padding(10.dp).alpha(if (day != null) 1f else 0.5f),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (day != null) m.emoji else "🔒", fontSize = 28.sp)
                    Text(m.title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    Text(if (day != null) Dates.label(day) else m.description, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = cs.onSurfaceVariant)
                }
            }
        }
        Text("Vi belønner innsats, teknikk og kontinuitet – ikke kroppsvekt eller utseende. 💙", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ChartCard(title: String, sub: String, s: Series) {
    MCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        LineChart(s.values, s.labels, unit = s.unit, pointColors = s.colors)
        Text("Nå: ${s.current}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}
