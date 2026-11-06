package no.muscly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import no.muscly.app.BuildConfig
import no.muscly.app.data.ChatEntity
import no.muscly.app.domain.DayType
import no.muscly.app.ui.AppState
import no.muscly.app.ui.AppViewModel
import no.muscly.app.ui.components.Pill
import no.muscly.app.ui.startWorkout

val coachSuggestions = listOf(
    "Jeg vil trene opp en dårlig rygg og få større biceps – lag et program",
    "Hvordan får jeg større biceps?",
    "Jeg klarer bare to push-ups. Hva gjør jeg?",
    "Kan jeg trene i dag selv om jeg trente i går?",
    "Hvordan blir jeg bedre i pull-ups?",
    "Kan du lage en kort treningsøkt for i dag?",
    "Hvordan går det med fremgangen min?",
    "Hvorfor er hvile viktig?",
)

@Composable
fun CoachScreen(vm: AppViewModel, st: AppState, nav: NavHostController) {
    val chat by vm.chat.collectAsStateWithLifecycle()
    val busy by vm.coachBusy.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    val p = st.profile
    val aiOn = p != null && p.aiEnabled && (p.coachUrl.isNotBlank() || BuildConfig.COACH_URL.isNotBlank())
    LaunchedEffect(chat.size, busy) { if (chat.isNotEmpty()) list.animateScrollToItem(chat.size + if (busy) 1 else 0) }
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Min personlige trener", style = MaterialTheme.typography.headlineSmall)
                Pill(if (aiOn) "🟢 AI tilkoblet (krever internett)" else "⚪ Innebygd trener – virker offline", color = if (aiOn) cs.primaryContainer else cs.surfaceVariant,
                    textColor = if (aiOn) cs.onPrimaryContainer else cs.onSurfaceVariant)
            }
            if (chat.isNotEmpty()) IconButton(onClick = { vm.clearChat() }) { Icon(Icons.Filled.DeleteSweep, "Tøm samtalen") }
        }
        LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Bubble(ChatEntity(fromUser = false, text = "Hei${p?.name?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}! 👋 Jeg er treneren din. Jeg kjenner programmet og fremgangen din, og kan svare på spørsmål om trening, øvelser, hvile og gode vaner.\n\n🎯 Fortell meg gjerne hva du vil oppnå, så setter jeg opp et program – f.eks. «Jeg vil trene opp en dårlig rygg og få større biceps, 3 dager i uka».", time = 0), nav, vm)
            }
            items(chat, key = { it.id }) { Bubble(it, nav, vm) }
            if (busy) item { Text("Treneren skriver …", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            coachSuggestions.forEach { q -> SuggestionChip(onClick = { vm.sendToCoach(q) }, label = { Text(q) }, enabled = !busy) }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it.take(500) }, Modifier.weight(1f), placeholder = { Text("Spør treneren …") }, shape = MaterialTheme.shapes.extraLarge, maxLines = 4)
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { vm.sendToCoach(input); input = "" }, enabled = input.isNotBlank() && !busy, modifier = Modifier.size(52.dp)) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send")
            }
        }
        Text(if (p?.adult == true) "Treneren gir ikke medisinske råd. Ved smerte eller skade: kontakt fastlege eller fysioterapeut."
             else "Treneren gir ikke medisinske råd. Ved smerte eller skade: snakk med en voksen, helsesykepleier eller lege.",
            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp))
    }
}

@Composable
private fun Bubble(m: ChatEntity, nav: NavHostController, vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val shape = if (m.fromUser) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(Modifier.widthIn(max = 320.dp).clip(shape).background(if (m.fromUser) cs.primary else cs.surfaceContainerHigh).padding(14.dp)) {
            Text(m.text, color = if (m.fromUser) cs.onPrimary else cs.onSurface, style = MaterialTheme.typography.bodyLarge)
            if (!m.fromUser && m.action.isNotBlank()) {
                val parts = m.action.split(":")
                Spacer(Modifier.height(8.dp))
                when (parts[0]) {
                    "workout" -> runCatching { DayType.valueOf(parts[1]) }.getOrNull()?.let { t ->
                        Button(onClick = { nav.startWorkout(t, parts.getOrNull(2)?.toIntOrNull()) }) { Text("Start økten ▶") }
                    }
                    "exercise" -> OutlinedButton(onClick = { nav.navigate("exercise/${parts[1]}") }) { Text("Se øvelsen") }
                    "plan" -> {
                        var used by remember(m.id) { mutableStateOf(false) }
                        Button(onClick = { used = true; vm.applyPlan(m.action.removePrefix("plan:")) }, enabled = !used) { Text(if (used) "Tatt i bruk ✓" else "Bruk dette programmet") }
                    }
                }
            }
            if (!m.fromUser && m.source == "ai") Text("AI-svar", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
