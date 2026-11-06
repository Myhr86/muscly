package no.muscly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import no.muscly.app.domain.*
import no.muscly.app.ui.AppState
import no.muscly.app.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(id: String, st: AppState, nav: NavHostController) {
    val ex = ExerciseLibrary.find(id) ?: return
    val s = st.states[id]
    val stageIdx = (s?.stage ?: 0).coerceIn(0, ex.stages.lastIndex)
    var shown by remember(id) { mutableIntStateOf(stageIdx) }
    val stage = ex.stages[shown]
    val cs = MaterialTheme.colorScheme
    val p = st.profile ?: UserProfile()
    val sets = WorkoutBuilder.setsFor(ex, p, st.sessions.size, DayType.HELKROPP)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tilbake") }
            Text(ex.name, style = MaterialTheme.typography.headlineSmall)
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ExerciseAnimation(stage.poseKey, Modifier.fillMaxWidth().aspectRatio(1.2f))
            // Start- og sluttposisjon side om side
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val ill = Illustrations.get(stage.poseKey)
                listOf(0, ill.frames.lastIndex).forEachIndexed { k, f ->
                    Column(Modifier.weight(1f)) {
                        ExerciseAnimation(stage.poseKey, Modifier.fillMaxWidth().aspectRatio(1.3f), playing = false, showCaption = false, staticFrame = f)
                        Text(if (k == 0) "Start: ${ill.caption.getOrElse(0) { "" }}" else "Slutt: ${ill.caption.getOrElse(1) { "" }}",
                            style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ex.primary.forEach { Pill("● ${it.label}", color = cs.primary, textColor = cs.onPrimary) }
                ex.secondary.forEach { Pill(it.label) }
            }

            // Ditt nivå
            MCard(color = cs.surfaceContainerHigh) {
                Text("DITT NIVÅ NÅ", style = MaterialTheme.typography.labelMedium, color = cs.primary)
                val cur = ex.stages[stageIdx]
                val target = s?.target ?: cur.start
                Text(cur.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("$sets", "sett", Modifier.weight(1f))
                    StatTile("$target", cur.measure.unit, Modifier.weight(1.4f))
                    StatTile(if (ex.restSec > 0) "${ex.restSec}s" else "–", "pause", Modifier.weight(1f), accent = cs.secondary)
                }
                if (cur.weighted && s != null) Text("Manualvekt: ${ProgressionEngine.fmtKg(s.weightKg)} kg", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                if (cur.workSec > 0) Text("${cur.workSec} sek hardt / ${cur.restSec} sek rolig per runde", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                if (s != null && s.painUntilDay > st.today) Text("⚠️ Pause fra øvelsen etter at du meldte smerte.", color = cs.error, modifier = Modifier.padding(top = 8.dp))
            }

            if (ex.stages.size > 1) {
                SectionTitle("Progresjonsstige")
                ex.stages.forEachIndexed { i, sg ->
                    val reached = i <= stageIdx
                    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(if (i == shown) cs.surfaceContainerHigh else cs.surface)
                        .padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(if (reached) cs.primary else cs.surfaceVariant), contentAlignment = Alignment.Center) {
                            Text(if (i < stageIdx) "✓" else "${i + 1}", color = if (reached) cs.onPrimary else cs.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sg.name + if (i == stageIdx) "  ← du er her" else "", style = MaterialTheme.typography.titleSmall)
                            Text("${sg.start}–${sg.max} ${sg.measure.unit}" + if (sg.note.isNotBlank()) " · ${sg.note}" else "", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        TextButton(onClick = { shown = i }) { Text("Vis") }
                    }
                }
            }

            SectionTitle("Startposisjon")
            Text(ex.startPosition, style = MaterialTheme.typography.bodyLarge)
            SectionTitle("Slik gjør du det")
            ex.steps.forEachIndexed { i, t ->
                Row { Text("${i + 1}", Modifier.width(28.dp), color = cs.primary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium); Text(t, style = MaterialTheme.typography.bodyLarge) }
            }
            SectionTitle("Vanlige feil")
            ex.mistakes.forEach { Text("✗  $it", style = MaterialTheme.typography.bodyLarge, color = cs.onSurface) }
            SectionTitle("Tips")
            ex.tips.forEach { Text("✓  $it", style = MaterialTheme.typography.bodyLarge) }
            if (ex.alternatives.isNotEmpty()) {
                SectionTitle("Alternativer")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ex.alternatives.mapNotNull { ExerciseLibrary.find(it) }.forEach { a -> AssistChip(onClick = { nav.navigate("exercise/${a.id}") }, label = { Text(a.name) }) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
