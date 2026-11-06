package no.muscly.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import no.muscly.app.data.Dates
import no.muscly.app.domain.*
import no.muscly.app.ui.AppState
import no.muscly.app.ui.AppViewModel
import no.muscly.app.ui.components.*

@Composable
fun ParentScreen(vm: AppViewModel, st: AppState, nav: NavHostController) {
    val p = st.profile ?: return
    var unlocked by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tilbake") }
            Text("Foreldredel", style = MaterialTheme.typography.headlineSmall)
        }
        if (!unlocked) PinGate(p, onSetPin = { vm.updateProfile(p.copy(parentPin = it)); unlocked = true }, onUnlock = { unlocked = true })
        else ParentContent(vm, st, p)
    }
}

@Composable
private fun PinGate(p: UserProfile, onSetPin: (String) -> Unit, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val creating = p.parentPin.isBlank()
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (creating) "Lag en foreldre-PIN" else "Skriv inn foreldre-PIN", style = MaterialTheme.typography.titleLarge)
        Text(if (creating) "Velg 4 sifre. PIN-en beskytter innstillingene for trygghet (f.eks. maks antall harde økter)." else "Foreldredelen er beskyttet med PIN.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(4); error = false }, label = { Text("PIN") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error, supportingText = { if (error) Text("Feil PIN") }, modifier = Modifier.fillMaxWidth())
        BigButton(if (creating) "Lagre PIN" else "Åpne", enabled = pin.length == 4) {
            if (creating) onSetPin(pin) else if (pin == p.parentPin) onUnlock() else error = true
        }
    }
}

@Composable
private fun ParentContent(vm: AppViewModel, st: AppState, p: UserProfile) {
    val cs = MaterialTheme.colorScheme
    val plan = st.weekPlan
    val hard = plan.count { it.intensity == Intensity.HARD }
    val pains = st.logs.filter { it.pain }.takeLast(10).reversed()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${st.sessionsThisWeek.size}/${st.plannedThisWeek}", "økter denne uka", Modifier.weight(1f))
            StatTile("${st.sessions.size}", "økter totalt", Modifier.weight(1f), accent = cs.secondary)
            StatTile("${st.milestones.size}", "milepæler", Modifier.weight(1f), accent = cs.tertiary)
        }
        MCard {
            Text("Treningsprogrammet", style = MaterialTheme.typography.titleMedium)
            Text("${p.level.label} · ${p.focus.label} · ${p.minutes} min per økt · $hard harde styrkeøkter per uke", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            plan.forEachIndexed { i, d ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text(Dates.dayNames[i], Modifier.width(90.dp), color = cs.onSurfaceVariant)
                    Text("${d.emoji} ${d.title}" + if (d != DayType.HVILE) " (${d.intensity.label.lowercase()})" else "")
                }
            }
            Text("✓ Samme muskelgruppe trenes aldri hardt to dager på rad.\n✓ Belastningen økes kun når barnet har mestret målet med god teknikk.\n✓ Nybegynnere starter med 2 sett per øvelse.",
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
        MCard {
            Text("Trygghetsinnstillinger", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Maks harde styrkeøkter per uke", Modifier.weight(1f))
                Stepper(p.maxHardPerWeek, { vm.updateProfile(p.copy(maxHardPerWeek = it)) }, min = 1, max = 4)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tillat AI-trener (internett)", Modifier.weight(1f))
                Switch(p.aiEnabled, { vm.updateProfile(p.copy(aiEnabled = it)) })
            }
            Text("AI-treneren har egne sikkerhetsregler: ingen slankeråd, kaloritelling, kosttilskudd eller trening gjennom smerte. Den innebygde treneren virker uten internett.",
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        MCard {
            Text("Påminnelser", style = MaterialTheme.typography.titleMedium)
            ReminderEditor(p, { vm.updateProfile(it) }, showTest = true)
        }
        if (pains.isNotEmpty()) MCard(color = cs.errorContainer) {
            Text("Meldt smerte", style = MaterialTheme.typography.titleMedium, color = cs.onErrorContainer)
            pains.forEach { l ->
                val ex = ExerciseLibrary.find(l.exerciseId)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${Dates.label(l.day)}: ${ex?.name}", Modifier.weight(1f), color = cs.onErrorContainer)
                    if ((st.states[l.exerciseId]?.painUntilDay ?: 0) > st.today) TextButton(onClick = { vm.clearPain(l.exerciseId) }) { Text("Tillat igjen") }
                }
            }
            Text("Øvelsen er satt på pause i en uke. Går smerten ikke over, kontakt helsesykepleier eller lege.", style = MaterialTheme.typography.bodySmall, color = cs.onErrorContainer)
        }
        MCard {
            Text("Utvikling per øvelse", style = MaterialTheme.typography.titleMedium)
            listOf("pushup", "pullup", "curl", "squat", "plank", "bike_interval").forEach { id ->
                val ex = ExerciseLibrary.get(id); val s = st.states[id] ?: return@forEach
                val stg = ex.stages[s.stage.coerceIn(0, ex.stages.lastIndex)]
                Text("${ex.name}: ${stg.name}, ${s.target} ${stg.measure.unit}" + if (stg.weighted) ", ${ProgressionEngine.fmtKg(s.weightKg)} kg" else "", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
        MCard {
            Text("Gjennomførte økter", style = MaterialTheme.typography.titleMedium)
            if (st.sessions.isEmpty()) Text("Ingen økter ennå.", color = cs.onSurfaceVariant)
            st.sessions.take(20).forEach { s ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(Dates.label(s.day), Modifier.width(70.dp), color = cs.onSurfaceVariant)
                    Text(s.title, Modifier.weight(1f))
                    Text("${s.completedExercises}/${s.totalExercises} · ${(s.durationSec / 60).coerceAtLeast(1)} min", color = cs.onSurfaceVariant)
                }
            }
        }
        MCard {
            Text("Milepæler", style = MaterialTheme.typography.titleMedium)
            if (st.milestones.isEmpty()) Text("Ingen ennå.", color = cs.onSurfaceVariant)
            st.milestones.sortedBy { it.day }.forEach { m -> Milestones.get(m.id)?.let { Text("${it.emoji} ${it.title} – ${Dates.label(m.day)}", modifier = Modifier.padding(top = 4.dp)) } }
        }
        MCard(color = cs.secondaryContainer) {
            Text("Til foreldre", style = MaterialTheme.typography.titleMedium, color = cs.onSecondaryContainer)
            Text("Appen fokuserer på styrke, helse og gode vaner – ikke vekt. Ungdom i vekst trenger nok mat, søvn og variert aktivitet. " +
                "Er dere bekymret for vekt, spisevaner eller helse, er helsesykepleier eller fastlege de rette å kontakte.",
                style = MaterialTheme.typography.bodyMedium, color = cs.onSecondaryContainer)
        }
        Spacer(Modifier.height(16.dp))
    }
}
