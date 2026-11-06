package no.muscly.app.domain

enum class Muscle(val label: String) {
    BICEPS("Biceps"), TRICEPS("Triceps"), BRYST("Bryst"), SKULDRE("Skuldre"),
    RYGG("Rygg"), UNDERARM("Underarmer/grep"), FORSIDE_LAR("Forside lår"),
    BAKSIDE_LAR("Bakside lår"), SETE("Sete"), LEGG("Legger"), MAGE("Mage"),
    SKRA_MAGE("Skrå magemuskler"), KORSRYGG("Korsrygg"), HJERTE_LUNGE("Hjerte og lunger"),
    HOFTE("Hofter")
}

enum class Category(val label: String) {
    OVERKROPP("Overkropp"), ARMER("Armer"), BEIN("Bein"), KJERNE("Mage og kjerne"),
    KONDISJON("Kondisjon"), BEVEGELIGHET("Bevegelighet")
}

enum class Equipment(val label: String) {
    KROPPSVEKT("Kroppsvekt"), MANUALER("Manualer"), SYKKEL("Treningssykkel"),
    TRAPP("Trapp"), STANG("Pull-up-stang"), STOL("Stol/benk")
}

enum class Injury(val label: String) {
    KNE("Knær"), RYGG("Rygg"), SKULDER("Skuldre"), HANDLEDD("Håndledd"), ANKEL("Ankler")
}

/** Hvordan en øvelse måles. */
enum class Measure(val unit: String, val perSide: Boolean = false) {
    REPS("reps"), REPS_PER_SIDE("reps per side", true), SECONDS("sek"),
    SECONDS_PER_SIDE("sek per side", true), MINUTES("min"), ROUNDS("runder")
}

enum class Level(val label: String, val description: String) {
    NYBEGYNNER("Nybegynner", "Har ikke trent mye før"),
    LITT_ERFAREN("Litt erfaren", "Har trent av og til"),
    ERFAREN("Erfaren", "Trener jevnlig")
}

enum class Focus(val label: String, val description: String) {
    AUTO("Balansert (anbefalt)", "Appen setter opp et balansert program"),
    HELKROPP("Helkropp", "Hele kroppen hver økt"),
    OVERKROPP("Overkropp", "Bryst, rygg og skuldre"),
    ARMER("Armer og biceps", "Ekstra fokus på armene"),
    BEIN("Bein og lår", "Sterke bein"),
    KJERNE("Mage og kjerne", "Sterk midtseksjon"),
    KONDISJON("Kondisjon", "Bedre utholdenhet"),
    KOMBINASJON("Kombinasjon", "Styrke og kondisjon i samme økt")
}

enum class ScheduleType(val label: String, val description: String) {
    TRE("3 dager i uka", "Man, ons, fre – anbefalt for nybegynnere"),
    FIRE("4 dager i uka", "Man, tir, tor, lør"),
    ANNENHVER("Med hviledager mellom", "Trening annenhver dag"),
    HVER_DAG("Hver dag", "Varierende intensitet – lette dager innimellom"),
    EGEN("Velg dager selv", "Du velger ukedagene")
}

enum class Rating(val label: String, val emoji: String) {
    LETT("Lett", "😎"), PASSE("Passe", "💪"), VANSKELIG("Vanskelig", "😅")
}

enum class Intensity(val label: String) { HARD("Styrke"), MODERAT("Moderat"), LETT("Lett"), HVILE("Hvile") }

/** Type treningsdag i ukeplanen. */
enum class DayType(val title: String, val intensity: Intensity, val emoji: String, val loads: Set<BodyArea>) {
    HELKROPP("Helkropp", Intensity.HARD, "🔥", setOf(BodyArea.OVER, BodyArea.UNDER)),
    HELKROPP_OVER("Helkropp – overkroppsfokus", Intensity.HARD, "💪", setOf(BodyArea.OVER, BodyArea.UNDER)),
    OVERKROPP("Overkropp og armer", Intensity.HARD, "💪", setOf(BodyArea.OVER)),
    ARMER("Armer og biceps", Intensity.HARD, "💪", setOf(BodyArea.OVER)),
    BEIN_KJERNE("Bein og mage", Intensity.HARD, "🦵", setOf(BodyArea.UNDER)),
    KJERNE_KONDIS("Mage og kondisjon", Intensity.MODERAT, "⚡", setOf()),
    KONDISJON("Kondisjon – intervaller", Intensity.MODERAT, "🚴", setOf()),
    LETT("Lett sykling og bevegelighet", Intensity.LETT, "🧘", setOf()),
    HVILE("Hvile", Intensity.HVILE, "😴", setOf())
}

enum class BodyArea { OVER, UNDER }

/** Ett trinn på en øvelses progresjonsstige. */
data class Stage(
    val name: String,
    val poseKey: String,
    val measure: Measure,
    val start: Int,
    val max: Int,
    val step: Int,
    val note: String = "",
    val weighted: Boolean = false,
    /** For intervaller: sekunder arbeid / pause per runde. */
    val workSec: Int = 0,
    val restSec: Int = 0,
    /** Ekstra utstyr som trengs for akkurat dette trinnet. */
    val needs: Equipment? = null,
)

data class Exercise(
    val id: String,
    val name: String,
    val category: Category,
    val primary: List<Muscle>,
    val secondary: List<Muscle> = emptyList(),
    val equipment: Set<Equipment>,
    val startPosition: String,
    val steps: List<String>,
    val mistakes: List<String>,
    val tips: List<String>,
    val stages: List<Stage>,
    val restSec: Int,
    val avoidWith: Set<Injury> = emptySet(),
    val alternatives: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
) {
    val weighted get() = stages.any { it.weighted }
}

data class UserProfile(
    val name: String = "",
    val age: Int = 14,
    val level: Level = Level.NYBEGYNNER,
    val focus: Focus = Focus.AUTO,
    val schedule: ScheduleType = ScheduleType.TRE,
    /** 0 = mandag … 6 = søndag. Brukes ved EGEN. */
    val customDays: Set<Int> = setOf(0, 2, 4),
    val minutes: Int = 30,
    val equipment: Set<Equipment> = Equipment.values().toSet(),
    val liked: Set<String> = emptySet(),
    val avoided: Set<String> = emptySet(),
    val injuries: Set<Injury> = emptySet(),
    val priorities: Set<Category> = emptySet(),
    val remindersOn: Boolean = true,
    val reminderHour: Int = 17,
    val reminderMinute: Int = 0,
    val followUpOn: Boolean = true,
    val weeklySummaryOn: Boolean = true,
    val theme: String = "dark",
    val aiEnabled: Boolean = true,
    val coachUrl: String = "",
    val parentPin: String = "",
    val maxHardPerWeek: Int = 3,
    val onboarded: Boolean = false,
)

/** En planlagt øvelse i en økt. */
data class PlannedExercise(
    val exerciseId: String,
    val stageIndex: Int,
    val sets: Int,
    val target: Int,
    val weightKg: Float,
    val restSec: Int,
)

data class PlannedWorkout(
    val dayType: DayType,
    val title: String,
    val exercises: List<PlannedExercise>,
    val warmup: List<String>,
    val cooldown: List<String>,
    val estimatedMinutes: Int,
)
