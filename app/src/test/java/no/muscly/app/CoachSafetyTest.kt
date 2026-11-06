package no.muscly.app

import kotlinx.coroutines.runBlocking
import no.muscly.app.coach.*
import no.muscly.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.ServerSocket
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoachSafetyTest {
    private val ctx = CoachContext(UserProfile(onboarded = true), mapOf("pushup" to ExerciseState("pushup", 3, 2), "pullup" to ExerciseState("pullup", 0, 10)),
        emptyList(), emptyList(), ProgramGenerator.weekPlan(UserProfile()), 20000)

    @Test fun `sikkerhetsemner gjenkjennes`() {
        assertEquals(Safety.Topic.DIET, Safety.classify("Hvor mange kalorier skal jeg spise for å gå ned i vekt?"))
        assertEquals(Safety.Topic.DIET, Safety.classify("Hvordan slanker jeg meg raskt"))
        assertEquals(Safety.Topic.SUPPLEMENT, Safety.classify("Bør jeg ta kreatin?"))
        assertEquals(Safety.Topic.SUPPLEMENT, Safety.classify("Er proteinpulver bra for meg"))
        assertEquals(Safety.Topic.PAIN, Safety.classify("Det gjør vondt i kneet når jeg tar knebøy"))
        assertEquals(Safety.Topic.EXTREME, Safety.classify("Kan jeg trene 3 timer hver dag uten hviledag?"))
        assertEquals(Safety.Topic.EXTREME, Safety.classify("Skal jeg trene gjennom smerten?"))
        assertEquals(Safety.Topic.CRISIS, Safety.classify("Jeg vil skade meg selv"))
        assertEquals(Safety.Topic.BODY_IMAGE, Safety.classify("Jeg føler meg tykk"))
        assertNull(Safety.classify("Hvordan får jeg større biceps?"))
        assertNull(Safety.classify("Jeg er støl i beina"))
    }

    @Test fun `sikkerhetssvar inneholder ikke farlige råd og henviser til voksne`() {
        Safety.Topic.values().forEach { t ->
            val a = Safety.answer(t)
            assertTrue(Safety.outputOk(a) || t == Safety.Topic.SUPPLEMENT) // tilskuddssvaret nevner kreatin for å fraråde det
            assertFalse(a.contains("kaloriunderskudd"))
        }
        assertTrue(Safety.answer(Safety.Topic.DIET).contains("helsesykepleier"))
        assertTrue(Safety.answer(Safety.Topic.PAIN).contains("lege"))
        assertTrue(Safety.answer(Safety.Topic.CRISIS).contains("113"))
    }

    @Test fun `de fem eksempelspørsmålene besvares offline med relevant innhold`() {
        val q = mapOf(
            "Hvordan får jeg større biceps?" to "Chin-ups",
            "Jeg klarer bare to push-ups. Hva gjør jeg?" to "Knestående",
            "Kan jeg trene i dag selv om jeg trente i går?" to "Ja",
            "Hvordan blir jeg bedre i pull-ups?" to "Negative",
            "Kan du lage en kort treningsøkt for i dag?" to "økt",
        )
        q.forEach { (question, expected) ->
            val r = OfflineCoach.answer(question, ctx)
            assertTrue("$question -> ${r.text}", r.text.contains(expected, ignoreCase = true))
            assertTrue(Safety.outputOk(r.text))
        }
        assertTrue(OfflineCoach.answer("Kan du lage en kort treningsøkt for i dag?", ctx).action.startsWith("workout:"))
        // Bruker historikken: push-up-nivået står i svaret
        assertTrue(OfflineCoach.answer("Jeg klarer bare to push-ups", ctx).text.contains("Push-ups: 2 reps"))
    }

    @Test fun `ekstra spørsmål - muskler, alternativer og for vanskelig`() {
        assertTrue(OfflineCoach.answer("Hvilke muskler trener knebøy?", ctx).text.contains("forside lår"))
        assertTrue(OfflineCoach.answer("Hva er et alternativ til push-ups?", ctx).text.contains("Skulderpress") || OfflineCoach.answer("Hva er et alternativ til push-ups?", ctx).text.contains("dips"))
        assertTrue(OfflineCoach.answer("Push-ups er for vanskelig", ctx).text.contains("Knestående"))
    }

    @Test fun `AI-svar som bryter reglene blokkeres, og offline brukes når serveren er nede`() = runBlocking {
        val server = ServerSocket(0)
        var reply = "Prøv å ta kreatin hver dag."
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val sock = runCatching { server.accept() }.getOrNull() ?: break
                sock.use { so ->
                    val inp = so.getInputStream()
                    val head = StringBuilder()
                    while (!head.endsWith("\r\n\r\n")) { val b = inp.read(); if (b < 0) break; head.append(b.toChar()) }
                    val len = Regex("(?i)content-length: *(\\d+)").find(head)?.groupValues?.get(1)?.toInt() ?: 0
                    var r = 0; val buf = ByteArray(len); while (r < len) { val n = inp.read(buf, r, len - r); if (n < 0) break; r += n }
                    val body = """{"reply":"$reply"}""".toByteArray()
                    so.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray() + body)
                    so.getOutputStream().flush()
                }
            }
        }
        val url = "http://127.0.0.1:${server.localPort}"
        val c = ctx.copy(profile = ctx.profile.copy(coachUrl = url))
        val svc = CoachService("")
        val blocked = svc.reply("Hvordan blir jeg sterkere i beina?", c, emptyList())
        assertEquals("lokal", blocked.source); assertFalse(blocked.text.contains("kreatin"))
        reply = "Knebøy og step-ups er supre øvelser for beina!"
        val ok = svc.reply("Hvordan blir jeg sterkere i beina?", c, emptyList())
        assertEquals("ai", ok.source); assertTrue(ok.text.contains("step-ups"))
        // Sikkerhetsemner går aldri til serveren
        assertEquals("sikkerhet", svc.reply("Hvor mange kalorier bør jeg spise?", c, emptyList()).source)
        server.close()
        val down = svc.reply("Hvordan blir jeg sterkere i beina?", c, emptyList())
        assertEquals("lokal", down.source); assertTrue(down.text.contains("ikke tilgjengelig"))
        // AI av: alltid lokal
        assertEquals("lokal", svc.reply("Hei", c.copy(profile = c.profile.copy(aiEnabled = false)), emptyList()).source)
    }

}
