package no.muscly.app.domain

/** Oppsummert statistikk som milepælene sjekkes mot. */
data class StatsSnapshot(
    val totalSessions: Int,
    /** Beste sett per (øvelse, trinn). */
    val best: Map<Pair<String, Int>, Int>,
    val weeksInARow: Int,
    val cardioMinutes: Int,
    val distinctExercises: Int,
) {
    fun bestOf(id: String, stage: Int) = best[id to stage] ?: 0
    fun reachedStage(id: String, stage: Int) = best.keys.any { it.first == id && it.second >= stage }
}

data class Milestone(val id: String, val title: String, val description: String, val emoji: String, val check: (StatsSnapshot) -> Boolean)

object Milestones {
    val all = listOf(
        Milestone("first", "Første økt", "Du har startet reisen!", "🚀") { it.totalSessions >= 1 },
        Milestone("s5", "5 økter", "Fem økter gjennomført", "⭐") { it.totalSessions >= 5 },
        Milestone("s10", "10 økter", "Treningen er blitt en vane", "🔥") { it.totalSessions >= 10 },
        Milestone("s25", "25 økter", "Skikkelig innsats!", "🏅") { it.totalSessions >= 25 },
        Milestone("s50", "50 økter", "Du er en treningsmaskin", "🏆") { it.totalSessions >= 50 },
        Milestone("push1", "Første ekte push-up", "En push-up fra tærne", "💥") { it.bestOf("pushup", 3) >= 1 },
        Milestone("push5", "5 push-ups", "Fem push-ups i ett sett", "💪") { it.bestOf("pushup", 3) >= 5 },
        Milestone("push10", "10 push-ups", "Ti push-ups i ett sett", "💪") { it.bestOf("pushup", 3) >= 10 },
        Milestone("push20", "20 push-ups", "Tjue push-ups i ett sett!", "🦾") { it.bestOf("pushup", 3) >= 20 },
        Milestone("hang30", "30 sek heng", "Hang i stanga i 30 sekunder", "🐒") { it.bestOf("pullup", 0) >= 30 || it.reachedStage("pullup", 1) },
        Milestone("neg1", "Første negative pull-up", "Kontrollert ned fra stanga", "⬇️") { it.bestOf("pullup", 2) >= 1 || it.reachedStage("pullup", 3) },
        Milestone("pull1", "Første pull-up", "Haka over stanga – helt selv!", "🎉") { it.bestOf("pullup", 3) >= 1 },
        Milestone("pull5", "5 pull-ups", "Fem pull-ups i ett sett", "🏆") { it.bestOf("pullup", 3) >= 5 },
        Milestone("plank30", "30 sek planke", "Planke i 30 sekunder", "🧱") { it.bestOf("plank", 1) >= 30 },
        Milestone("plank60", "60 sek planke", "Ett helt minutt!", "🧱") { it.bestOf("plank", 1) >= 60 },
        Milestone("squat20", "20 knebøy", "20 knebøy i ett sett", "🦵") { it.bestOf("squat", 1) >= 20 || it.reachedStage("squat", 2) },
        Milestone("wall60", "60 sek wall sit", "Et minutt mot veggen", "🧊") { it.bestOf("wallsit", 0) >= 60 },
        Milestone("weeks2", "2 uker på rad", "Trent minst to ganger hver uke i 2 uker", "📅") { it.weeksInARow >= 2 },
        Milestone("weeks4", "4 uker på rad", "En hel måned med jevn trening", "📆") { it.weeksInARow >= 4 },
        Milestone("weeks8", "8 uker på rad", "To måneder – imponerende!", "👑") { it.weeksInARow >= 8 },
        Milestone("cardio60", "60 min kondisjon", "Totalt en time med kondisjonstrening", "🚴") { it.cardioMinutes >= 60 },
        Milestone("cardio300", "300 min kondisjon", "Fem timer kondisjon totalt", "❤️") { it.cardioMinutes >= 300 },
        Milestone("variety", "Allsidig", "Prøvd 10 forskjellige øvelser", "🎯") { it.distinctExercises >= 10 },
    )
    fun get(id: String) = all.firstOrNull { it.id == id }
}
