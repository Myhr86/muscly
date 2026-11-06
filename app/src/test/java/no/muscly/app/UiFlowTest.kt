package no.muscly.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import no.muscly.app.data.Dates
import no.muscly.app.domain.*
import no.muscly.app.ui.AppViewModel
import no.muscly.app.ui.MusclyRoot
import no.muscly.app.ui.components.ExerciseAnimation
import no.muscly.app.ui.theme.MusclyTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xhdpi")
class UiFlowTest {
    @get:Rule val rule = createComposeRule()
    private val app: MusclyApp = ApplicationProvider.getApplicationContext()
    private lateinit var container: AppContainer
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val outDir = File(System.getProperty("user.dir"), "../screenshots").apply { mkdirs() }

    @Before fun setup() {
        Dates.clock = { LocalDate.of(2026, 11, 9) } // mandag
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(app, androidx.work.Configuration.Builder().build())
        container = AppContainer(app, inMemory = true)
        vm = AppViewModel(app, container)
        rule.mainClock.autoAdvance = false
        no.muscly.app.ui.components.FigureAnimations.enabled = false
    }
    @After fun tearDown() { container.db.close(); Dates.clock = { LocalDate.now() } }

    private fun shot(name: String) { advance(600); rule.onRoot().captureRoboImage(File(outDir, "$name.png").path) }
    private fun advance(ms: Long = 300) { rule.mainClock.advanceTimeBy(ms); rule.waitForIdle() }
    private fun waitFor(timeoutMs: Long = 8000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!cond()) { if (System.currentTimeMillis() > end) error("Tidsavbrudd"); advance(100); Thread.sleep(15) }
    }
    private fun has(text: String, substring: Boolean = false) = rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    private fun scrollTo(n: SemanticsNodeInteraction): SemanticsNodeInteraction {
        rule.mainClock.autoAdvance = true
        try { n.performScrollTo() } finally { rule.mainClock.autoAdvance = false }
        return n
    }
    private fun click(text: String, substring: Boolean = false) { waitFor { has(text, substring) }; scrollTo(rule.onAllNodesWithText(text, substring = substring)[0]).performClick(); advance() }
    private fun clickNoScroll(text: String) { waitFor { has(text) }; rule.onAllNodesWithText(text)[0].performClick(); advance() }

    private fun start() {
        rule.setContent { nav = rememberNavController(); MusclyRoot(vm, nav) }
        advance(500)
    }

    private fun onboard() {
        waitFor { has("Kom i gang") }
        shot("01_velkommen")
        clickNoScroll("Kom i gang")
        rule.onNodeWithText("Kallenavn (valgfritt)").performTextInput("Elias")
        advance()
        clickNoScroll("Neste")
        click("1–3")
        shot("02_niva")
        clickNoScroll("Neste")
        click("Armer og biceps")
        shot("03_mal")
        clickNoScroll("Neste")
        shot("04_dager")
        clickNoScroll("Neste")
        clickNoScroll("Neste")
        shot("05_paminnelser")
        clickNoScroll("Lag programmet mitt 🚀")
        waitFor { has("Start økt") }
    }

    @Test fun fullFlyt_onboarding_okt_progresjon() {
        start()
        onboard()
        shot("06_hjem")
        val p = runBlocking { container.repository.profileNow()!! }
        assertEquals("Elias", p.name); assertEquals(Focus.ARMER, p.focus)
        assertEquals(2, runBlocking { container.repository.statesNow() }.getValue("pushup").target)

        // Start økt
        click("Start økt")
        waitFor { has("Start øvelsene") }
        shot("07_oppvarming")
        click("Start øvelsene")
        var exercises = 0
        while (!has("Fullfør økten ✅") && exercises < 10) {
            waitFor { has("Sett 1") }
            if (exercises == 0) shot("08_ovelse")
            // Fullfør alle sett
            var guard = 0
            while (rule.onAllNodesWithContentDescription("Ferdig").fetchSemanticsNodes().isNotEmpty() || rule.onAllNodesWithContentDescription("Marker ferdig").fetchSemanticsNodes().isNotEmpty()) {
                if (has("Hopp over pause")) { if (exercises == 0 && guard == 1) shot("09_pause"); clickNoScroll("Hopp over pause"); continue }
                val n = rule.onAllNodesWithContentDescription("Marker ferdig").fetchSemanticsNodes().isNotEmpty()
                val nodes = if (n) rule.onAllNodesWithContentDescription("Marker ferdig") else rule.onAllNodesWithContentDescription("Ferdig")
                // Ferdige sett har også «Ferdig» (angre). Finn første ikke-fullførte: klikk siste i lista hvis alle er like
                val all = nodes.fetchSemanticsNodes()
                val before = runCatching { rule.onAllNodesWithText("Hvordan kjentes det?").fetchSemanticsNodes().size }.getOrDefault(0)
                scrollTo(nodes[0]).performClick(); advance()
                guard++
                if (has("Hopp over pause")) { clickNoScroll("Hopp over pause") }
                if (has("Hvordan kjentes det?") && !has("Marker ferdig") && guard >= 2) break
                if (guard > 8) break
            }
            click("Passe")
            exercises++
            if (has("Neste øvelse →")) click("Neste øvelse →") else click("Til avslutning →")
        }
        waitFor { has("Fullfør økten ✅") }
        click("Fullfør økten ✅")
        waitFor(15000) { has("Bra jobbet!") }
        shot("10_oppsummering")
        assertTrue(has("øvelser i dag", substring = true))
        val sessions = runBlocking { container.repository.dao.sessionsNow() }
        assertEquals(1, sessions.size)
        assertTrue(sessions[0].completedExercises >= 1)
        assertTrue(runBlocking { container.repository.dao.milestonesNow() }.any { it.id == "first" })
        click("Ferdig")
        waitFor { has("Fullført ✅") }
        shot("11_hjem_ferdig")
    }

    private fun seedHistory() = runBlocking {
        val repo = container.repository
        repo.completeOnboarding(UserProfile(name = "Elias", schedule = ScheduleType.TRE, focus = Focus.AUTO), SelfAssessment(pushups = Ability.LITT, pullups = Ability.INGEN))
        var d = LocalDate.of(2026, 9, 14)
        while (d.isBefore(LocalDate.of(2026, 11, 9))) {
            Dates.clock = { d }
            val prof = repo.profileNow()!!
            val type = ProgramGenerator.weekPlan(prof)[Dates.dow(Dates.today())]
            if (type != DayType.HVILE && d.dayOfMonth % 7 != 3) {
                val w = WorkoutBuilder.build(type, prof, repo.statesNow(), repo.dao.sessionsNow().size, Dates.weekIndex(Dates.today()), Dates.today())
                val res = w.exercises.map { ExerciseResult(it.exerciseId, it.stageIndex, it.target, it.sets, List(it.sets) { k -> if (k == 0) it.target + 1 else it.target }, it.weightKg, Rating.PASSE) }
                repo.saveSession(w, res, 0, 25 * 60_000L)
            }
            d = d.plusDays(1)
        }
        Dates.clock = { LocalDate.of(2026, 11, 9) }
        repo.dao.insertChat(no.muscly.app.data.ChatEntity(fromUser = true, text = "Jeg klarer bare to push-ups. Hva gjør jeg?", time = 1))
    }

    @Test fun skjermer_med_historikk() {
        seedHistory()
        val st = runBlocking { container.repository.statesNow() }
        // Progresjon har faktisk skjedd gjennom mestring
        assertTrue(st.getValue("pushup").target > 2 || st.getValue("pushup").stage > 3)
        start()
        waitFor { has("Start økt") }
        shot("20_hjem_historikk")
        rule.runOnUiThread { nav.navigate("progress") }; advance(800)
        shot("21_fremgang")
        rule.onRoot().performTouchInput { swipeUp() }; advance(800); shot("22_fremgang_2")
        rule.onRoot().performTouchInput { swipeUp() }; rule.onRoot().performTouchInput { swipeUp() }; advance(800); shot("23_milepaeler")
        rule.runOnUiThread { nav.navigate("training") }; advance(800)
        shot("24_program")
        clickNoScroll("Øvelser"); shot("25_bibliotek")
        rule.runOnUiThread { nav.navigate("exercise/pullup") }; advance(800)
        shot("26_ovelse_pullup")
        rule.onRoot().performTouchInput { swipeUp() }; advance(800); shot("27_ovelse_pullup_2")
        rule.runOnUiThread { nav.navigate("coach") }; advance(500)
        vm.sendToCoach("Kan du lage en kort treningsøkt for i dag?")
        waitFor { has("Start økten ▶") }
        shot("28_ai_trener")
        vm.sendToCoach("Bør jeg ta kreatin?")
        waitFor { has("anbefales ikke", substring = true) }
        shot("29_ai_sikkerhet")
        rule.runOnUiThread { nav.navigate("profile") }; advance(500)
        shot("30_profil")
        rule.runOnUiThread { nav.navigate("parent") }; advance(500)
        rule.onNode(hasSetTextAction()).performTextInput("1234"); advance()
        clickNoScroll("Lagre PIN")
        waitFor { has("Treningsprogrammet") }
        shot("31_foreldre")
        // Lyst tema
        runBlocking { container.repository.saveProfile(container.repository.profileNow()!!.copy(theme = "light")) }
        rule.runOnUiThread { nav.navigate("home") }; advance(800)
        waitFor { has("Start økt") }
        shot("32_hjem_lyst")
    }

    @Test fun illustrasjoner_oversikt() {
        rule.setContent {
            MusclyTheme {
                Surface {
                    LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxSize().padding(4.dp)) {
                        items(Illustrations.all.keys.toList()) { k ->
                            Column(Modifier.padding(2.dp)) {
                                Row { ExerciseAnimation(k, Modifier.weight(1f).aspectRatio(1f), playing = false, showCaption = false, staticFrame = 0)
                                      ExerciseAnimation(k, Modifier.weight(1f).aspectRatio(1f), playing = false, showCaption = false, staticFrame = Illustrations.get(k).frames.lastIndex) }
                                Text(k, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        advance(500)
        rule.onRoot().captureRoboImage(File(outDir, "90_illustrasjoner.png").path)
    }
}
