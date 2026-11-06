package no.muscly.app.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import no.muscly.app.domain.*
import no.muscly.app.ui.*
import no.muscly.app.ui.components.*
import no.muscly.app.ui.theme.NumberStyle

private fun cueFeedback(ctx: Context, cue: Cue, tone: ToneGenerator?) {
    runCatching {
        val vib = if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                  else @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        when (cue) {
            Cue.TICK -> tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            Cue.DONE -> { tone?.startTone(ToneGenerator.TONE_PROP_ACK, 400); vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1)) }
            Cue.SET_DONE -> vib.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

@Composable
fun WorkoutScreen(vm: AppViewModel, st: AppState, type: DayType, minutes: Int?, nav: NavHostController) {
    val session = remember { WorkoutSession(st.buildWorkout(type, minutes)) }
    val ctx = LocalContext.current
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(Unit) { session.onCue = { cueFeedback(ctx, it, tone) } }
    // Skjermen holdes på under økten
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    // Klokke for timere
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.timer) {
        while (session.timer != null) { now = System.currentTimeMillis(); session.tick(now); delay(200) }
    }
    var confirmExit by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler { confirmExit = true }

    fun finish() {
        if (saving) return
        saving = true
        scope.launch {
            val ok = runCatching { vm.saveWorkout(session.workout, session.results(), session.startedAt, System.currentTimeMillis()) }.isSuccess
            withContext(Dispatchers.Main) {
                if (ok) nav.navigate("summary") { popUpTo("home") } else saving = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Topp: lukk + fremdrift
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { confirmExit = true }) { Icon(Icons.Filled.Close, "Avslutt") }
            Column(Modifier.weight(1f)) {
                Text("${type.emoji} ${session.workout.title}", style = MaterialTheme.typography.titleMedium)
                Text(when (session.phase) {
                    Phase.WARMUP -> "Oppvarming"
                    Phase.EXERCISE -> "Øvelse ${session.index + 1} av ${session.items.size}"
                    Phase.COOLDOWN -> "Avslutning"
                }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LinearProgressIndicator(progress = { session.progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(8.dp).clip(CircleShape))

        Box(Modifier.weight(1f)) {
            AnimatedContent(session.phase to session.index, label = "page") { (ph, _) ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (ph) {
                        Phase.WARMUP -> WarmupPage(session)
                        Phase.EXERCISE -> session.current?.let { ExercisePage(session, it, nav) }
                        Phase.COOLDOWN -> CooldownPage(session) { finish() }
                    }
                }
            }
            session.timer?.let { TimerOverlay(session, it, now) }
        }
    }
    if (saving) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false },
        title = { Text("Avslutte økten?") },
        text = { Text(if (session.completedCount > 0) "Du har fullført ${session.completedCount} øvelser. Vil du lagre det du har gjort?" else "Ingenting er lagret ennå.") },
        confirmButton = {
            if (session.completedCount > 0) TextButton(onClick = { confirmExit = false; finish() }) { Text("Lagre og avslutt") }
            else TextButton(onClick = { confirmExit = false; nav.popBackStack() }) { Text("Avslutt") }
        },
        dismissButton = {
            Row {
                if (session.completedCount > 0) TextButton(onClick = { confirmExit = false; nav.popBackStack() }) { Text("Forkast") }
                TextButton(onClick = { confirmExit = false }) { Text("Fortsett") }
            }
        })
}

@Composable
private fun WarmupPage(s: WorkoutSession) {
    Text("Oppvarming 🔥", style = MaterialTheme.typography.headlineMedium)
    Text("Ca. 5 minutter. Gjør musklene klare og minsker risikoen for skader.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    ExerciseAnimation("jacks", Modifier.fillMaxWidth().height(200.dp), showCaption = false)
    s.workout.warmup.forEachIndexed { i, w ->
        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(s.warmupDone[i], { s.warmupDone[i] = it }); Text(w, style = MaterialTheme.typography.bodyLarge)
        }
    }
    BigButton("Start øvelsene") { s.startExercises() }
}

@Composable
private fun CooldownPage(s: WorkoutSession, onFinish: () -> Unit) {
    Text("Nesten ferdig! 🙌", style = MaterialTheme.typography.headlineMedium)
    Text("Du har fullført ${s.completedCount} av ${s.items.size} øvelser.", style = MaterialTheme.typography.titleMedium)
    if (s.workout.cooldown.isNotEmpty()) {
        ExerciseAnimation("stretch_child", Modifier.fillMaxWidth().height(180.dp), showCaption = false)
        Text("Rolig avslutning (2–3 min):", style = MaterialTheme.typography.titleMedium)
        s.workout.cooldown.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
    }
    val missing = s.items.filter { !it.anyDone && !it.pain && !it.skipped }
    if (missing.isNotEmpty()) Text("Ikke gjort: ${missing.joinToString { it.stage.name }} – helt greit!", color = MaterialTheme.colorScheme.onSurfaceVariant)
    BigButton("Fullfør økten ✅") { onFinish() }
    OutlinedButton(onClick = { s.prev(); s.phase = Phase.EXERCISE; s.index = s.items.lastIndex }, Modifier.fillMaxWidth()) { Text("Tilbake til øvelsene") }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisePage(s: WorkoutSession, c: ExerciseProgress, nav: NavHostController) {
    val cs = MaterialTheme.colorScheme
    val st = c.stage
    var showHow by remember(c) { mutableStateOf(false) }
    var confirmPain by remember(c) { mutableStateOf(false) }
    ExerciseAnimation(st.poseKey, Modifier.fillMaxWidth().height(230.dp))
    Text(st.name, style = MaterialTheme.typography.headlineMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Pill("${c.planned.sets} sett", color = cs.primary, textColor = cs.onPrimary)
        Pill("${c.planned.target} ${st.measure.unit}")
        if (c.planned.restSec > 0) Pill("Pause ${c.planned.restSec} s")
        if (st.workSec > 0) Pill("${st.workSec}s hardt / ${st.restSec}s rolig")
        c.ex.primary.take(2).forEach { Pill(it.label, color = cs.surfaceVariant, textColor = cs.onSurfaceVariant) }
    }
    TextButton(onClick = { showHow = !showHow }) { Text(if (showHow) "Skjul instruksjoner" else "Hvordan gjør jeg det? ▾") }
    if (showHow) MCard {
        c.ex.steps.forEachIndexed { i, t -> Text("${i + 1}. $t", style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(6.dp))
        Text("Unngå: ${c.ex.mistakes.take(2).joinToString(", ").lowercase()}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        TextButton(onClick = { nav.navigate("exercise/${c.ex.id}") }) { Text("Se hele øvelsessiden") }
    }
    if (st.weighted) Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Manualvekt", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        FilledTonalIconButton(onClick = { c.weight = (c.weight - 0.5f).coerceAtLeast(0.5f) }) { Text("−", fontSize = 20.sp) }
        Text("${ProgressionEngine.fmtKg(c.weight)} kg", style = MaterialTheme.typography.titleLarge, modifier = Modifier.widthIn(min = 72.dp), textAlign = TextAlign.Center)
        FilledTonalIconButton(onClick = { c.weight += 0.5f }) { Text("+", fontSize = 20.sp) }
    }

    // Sett
    val timed = st.measure in listOf(Measure.SECONDS, Measure.SECONDS_PER_SIDE, Measure.MINUTES) || (st.measure == Measure.ROUNDS && st.workSec > 0)
    for (i in 0 until c.planned.sets) {
        val done = c.done[i]
        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(if (done) cs.primaryContainer else cs.surfaceContainer)
            .border(1.dp, if (done) cs.primary else cs.outlineVariant, MaterialTheme.shapes.large).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Sett ${i + 1}", Modifier.width(60.dp), style = MaterialTheme.typography.titleSmall)
            Stepper(c.values[i], { s.index = s.items.indexOf(c); s.setValue(i, it) }, min = 0, max = 999, step = if (st.measure.unit.startsWith("sek")) 5 else 1)
            Text(st.measure.unit.substringBefore(" "), Modifier.weight(1f).padding(start = 4.dp), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            if (done) {
                FilledIconButton(onClick = { s.undoSet(i) }) { Icon(Icons.Filled.Check, "Ferdig") }
            } else if (timed) {
                FilledTonalIconButton(onClick = { s.startTimer(i) }) { Icon(Icons.Filled.Timer, "Start tid") }
                IconButton(onClick = { s.completeSet(i) }) { Icon(Icons.Filled.Check, "Marker ferdig") }
            } else {
                FilledIconButton(onClick = { s.completeSet(i) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = cs.surfaceVariant, contentColor = cs.onSurface)) { Icon(Icons.Filled.Check, "Ferdig") }
            }
        }
    }
    Text(if (timed) "Trykk ⏱ for nedtelling, eller ✓ når du er ferdig. Juster tallet til det du faktisk klarte."
         else "Juster tallet til det du faktisk klarte, og trykk ✓. Færre enn målet er helt greit!",
        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)

    // Vurdering
    if (c.anyDone) {
        Text("Hvordan kjentes det?", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Rating.values().forEach { r ->
                val sel = c.rating == r
                Column(Modifier.weight(1f).clip(MaterialTheme.shapes.large).background(if (sel) cs.primary else cs.surfaceContainer)
                    .border(1.dp, if (sel) cs.primary else cs.outlineVariant, MaterialTheme.shapes.large)
                    .clickableNoRipple { s.rate(r) }.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(r.emoji, fontSize = 24.sp); Text(r.label, color = if (sel) cs.onPrimary else cs.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    BigButton(if (s.index < s.items.lastIndex) "Neste øvelse →" else "Til avslutning →", enabled = !c.anyDone || c.rating != null) { s.next() }
    if (c.anyDone && c.rating == null) Text("Velg hvordan det kjentes – da tilpasser programmet seg.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { s.prev() }) { Text("← Forrige") }
        TextButton(onClick = { confirmPain = true }, colors = ButtonDefaults.textButtonColors(contentColor = cs.error)) { Text("Gjør vondt") }
        TextButton(onClick = { s.skip() }) { Text("Hopp over") }
    }
    if (confirmPain) AlertDialog(onDismissRequest = { confirmPain = false },
        title = { Text("Gjør det vondt? 🛑") },
        text = { Text("Stopp øvelsen. Smerte er ikke det samme som at det er tungt. Vi tar pause fra «${c.ex.name}» i en uke og øker ikke belastningen.\n\nFortell en voksen hjemme. Går det ikke over, kontakt helsesykepleier eller lege.") },
        confirmButton = { TextButton(onClick = { confirmPain = false; s.reportPain() }) { Text("Ja, stopp øvelsen") } },
        dismissButton = { TextButton(onClick = { confirmPain = false }) { Text("Nei, det går fint") } })
}

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(Modifier.clickable(onClick = onClick))

@Composable
private fun TimerOverlay(s: WorkoutSession, t: RunningTimer, now: Long) {
    val cs = MaterialTheme.colorScheme
    val rem = ((t.remainingMs(now) + 999) / 1000).toInt()
    val (phaseIdx, phase, phaseRem) = if (t.phases.isNotEmpty()) t.phaseAt(now) else Triple(0, null, rem)
    val isWork = phase?.work ?: (t.kind != TimerKind.REST)
    val bg = if (t.kind == TimerKind.REST || !isWork) cs.secondaryContainer else cs.primaryContainer
    val fg = if (t.kind == TimerKind.REST || !isWork) cs.onSecondaryContainer else cs.onPrimaryContainer
    Box(Modifier.fillMaxSize().background(cs.background.copy(alpha = 0.92f)).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(bg).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(when (t.kind) { TimerKind.REST -> "PAUSE"; else -> phase?.label ?: t.label }.uppercase(), style = MaterialTheme.typography.titleLarge, color = fg)
            val shown = if (phase != null) phaseRem else rem
            Text(if (t.kind == TimerKind.MINUTES || shown >= 60) "%d:%02d".format(shown / 60, shown % 60) else "$shown",
                style = NumberStyle.copy(fontSize = 96.sp), color = fg)
            if (t.phases.isNotEmpty()) {
                val works = t.phases.count { it.work }
                Text("Runde ${t.phases.take(phaseIdx + 1).count { it.work }.coerceAtMost(works)} av $works · totalt %d:%02d igjen".format(rem / 60, rem % 60), color = fg)
            }
            if (t.kind == TimerKind.REST) {
                val nextSet = (s.items.getOrNull(t.ex)?.done?.indexOfFirst { !it } ?: -1) + 1
                if (nextSet > 0) Text("Neste: sett $nextSet", color = fg, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { s.addRest(15) }) { Text("+15 s") }
                    Button(onClick = { s.skipRest() }) { Text("Hopp over pause") }
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Button(onClick = { s.stopTimerEarly(now) }) { Text("Stopp – lagre det jeg klarte") }
            }
        }
    }
}
