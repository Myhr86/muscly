package no.muscly.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import no.muscly.app.domain.ChangeKind
import no.muscly.app.domain.ExerciseLibrary
import no.muscly.app.ui.AppState
import no.muscly.app.ui.AppViewModel
import no.muscly.app.ui.components.*

@Composable
fun SummaryScreen(vm: AppViewModel, st: AppState, nav: NavHostController) {
    val s = vm.lastSummary
    val cs = MaterialTheme.colorScheme
    val pop by rememberInfiniteTransition(label = "pop").animateFloat(0.95f, 1.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Text("🏆", fontSize = 80.sp, modifier = Modifier.scale(pop))
        Text("Bra jobbet!", style = MaterialTheme.typography.displaySmall, color = cs.primary)
        if (s == null) {
            Text("Økten er lagret.", style = MaterialTheme.typography.titleMedium)
        } else {
            Text("Du gjennomførte ${s.completed} av ${s.total} øvelser i dag.", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${s.completed}/${s.total}", "øvelser", Modifier.weight(1f))
                StatTile("${(s.durationSec / 60).coerceAtLeast(1)}", "minutter", Modifier.weight(1f), accent = cs.secondary)
                StatTile("${st.sessions.size}", "økter totalt", Modifier.weight(1f), accent = cs.tertiary)
            }
            if (s.newMilestones.isNotEmpty()) MCard(color = cs.tertiaryContainer) {
                Text("NYE MILEPÆLER 🎉", style = MaterialTheme.typography.labelLarge, color = cs.onTertiaryContainer)
                s.newMilestones.forEach { m -> Text("${m.emoji}  ${m.title} – ${m.description}", style = MaterialTheme.typography.titleMedium, color = cs.onTertiaryContainer, modifier = Modifier.padding(top = 6.dp)) }
            }
            if (s.records.isNotEmpty()) MCard(color = cs.primaryContainer) {
                Text("PERSONLIGE REKORDER 📈", style = MaterialTheme.typography.labelLarge, color = cs.onPrimaryContainer)
                s.records.forEach { r ->
                    val ex = ExerciseLibrary.get(r.exerciseId); val stg = ex.stages[r.stage]
                    Text("${stg.name}: ${r.value} ${stg.measure.unit} (før: ${r.previous})", style = MaterialTheme.typography.titleMedium, color = cs.onPrimaryContainer, modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (s.decisions.isNotEmpty()) MCard {
                Text("NESTE GANG", style = MaterialTheme.typography.labelLarge, color = cs.primary)
                s.decisions.forEach { d ->
                    val icon = when (d.kind) { ChangeKind.UP, ChangeKind.STAGE_UP, ChangeKind.WEIGHT_UP -> "⬆️"; ChangeKind.DOWN, ChangeKind.STAGE_DOWN -> "🔧"; ChangeKind.PAIN -> "🛑"; ChangeKind.MAXED -> "🏆"; else -> "➡️" }
                    Text("$icon  ${d.message}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
                }
                Text("Programmet øker bare når du har mestret målet – aldri bare fordi det har gått en uke.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Text("Husk å drikke vann og spise et godt måltid 💧🍽️", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
        BigButton("Ferdig") { nav.navigate("home") { popUpTo("home") { inclusive = true } } }
    }
}
