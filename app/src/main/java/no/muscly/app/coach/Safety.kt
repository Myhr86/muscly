package no.muscly.app.coach

/**
 * Sikkerhetsregler for ungdom. Sjekkes ALLTID lokalt før noe sendes til en språkmodell,
 * og svar fra modellen kontrolleres før de vises.
 */
object Safety {
    enum class Topic { CRISIS, PAIN, DIET, SUPPLEMENT, EXTREME, BODY_IMAGE }

    private fun norm(s: String) = s.lowercase().replace(Regex("\\s+"), " ")

    private val crisis = listOf("ta livet", "drepe meg", "skade meg selv", "kutte meg", "ikke vil leve", "vil dø", "selvmord")
    private val pain = listOf("vondt", "smerte", "skade", "skadet", "svimmel", "brystsmert", "besvim", "hoven", "vrikket", "knakk", "stikker i", "får ikke puste", "nummen")
    private val diet = listOf("kalori", "slank", "diett", "kur ", "kuren", "faste", "gå ned i vekt", "ned i vekt", "miste vekt", "vekttap", "kilo ned", "kg ned", "spise mindre", "hoppe over måltid", "slutte å spise", "sulte", "fettprosent", "veie meg", "vekta mi", "bmi", "deff", "cutting")
    private val supplements = listOf("kreatin", "proteinpulver", "protein pulver", "proteinshake", "steroid", "anabol", "pre-workout", "preworkout", "pwo", "fettforbrenner", "kosttilskudd", "tilskudd", "energidrikk", "booster", "sarms", "testosteron", "gainer")
    private val extreme = listOf("to ganger om dagen", "2 ganger om dagen", "tre ganger om dagen", "100 push", "200 push", "1000", "hver dag uten hvile", "uten hviledag", "3 timer", "tre timer", "4 timer", "til jeg spyr", "gjennom smerten", "trene selv om det gjør vondt", "maks hver dag", "ingen hvile")
    private val bodyImage = listOf("feit", "tykk", "stygg", "hater kroppen", "hater meg selv", "skammer meg", "for mager", "for liten", "sixpack på", "rask sixpack")

    fun classify(input: String): Topic? {
        val t = norm(input)
        return when {
            crisis.any { t.contains(it) } -> Topic.CRISIS
            supplements.any { t.contains(it) } -> Topic.SUPPLEMENT
            diet.any { t.contains(it) } -> Topic.DIET
            extreme.any { t.contains(it) } -> Topic.EXTREME
            bodyImage.any { t.contains(it) } -> Topic.BODY_IMAGE
            pain.any { t.contains(it) } && !t.contains("støl") -> Topic.PAIN
            else -> null
        }
    }

    fun answer(topic: Topic): String = when (topic) {
        Topic.CRISIS -> "Takk for at du sier ifra – det er modig. Det du forteller er viktigere enn trening, og du skal ikke være alene med det.\n\n" +
            "• Snakk med en voksen du stoler på i dag – foreldre, lærer eller helsesykepleier.\n" +
            "• Kors på halsen (Røde Kors, for ungdom): ring 800 33 321 eller chat på korspahalsen.no\n" +
            "• Mental Helse hjelpetelefon: 116 123 (døgnåpen)\n" +
            "• Er det akutt: ring 113.\n\nJeg er her når du vil trene igjen 💙"
        Topic.PAIN -> "Smerte er kroppens stoppskilt – da skal vi ikke trene gjennom det. 🛑\n\n" +
            "• Stopp øvelsen som gjør vondt, og hvil den kroppsdelen.\n" +
            "• Fortell en voksen hjemme hvordan det kjennes.\n" +
            "• Er det fortsatt vondt etter noen dager, er hovent, eller veldig vondt – kontakt helsesykepleier eller lege.\n" +
            "• Brystsmerter, svimmelhet eller tung pust: stopp helt og si ifra til en voksen med en gang.\n\n" +
            "Jeg kan ikke stille diagnoser, men jeg kan hjelpe deg å finne øvelser som ikke belaster det stedet. Trykk «Gjør vondt» i treningsmodus, så tar appen pause fra øvelsen.\n\n" +
            "PS: Litt stølhet dagen etter trening er normalt – det er noe annet enn smerte."
        Topic.DIET -> "I Muscly fokuserer vi på styrke, energi og helse – ikke på vekt eller kalorier. 💪\n\n" +
            "Når du er 14, vokser kroppen fortsatt, og den trenger NOK mat for å bli sterk:\n" +
            "• 3 hovedmåltider og 1–2 mellommåltider hver dag\n" +
            "• Proteinrik mat i måltidene: egg, fisk, kylling, bønner, melk, yoghurt\n" +
            "• Grønnsaker og frukt – gjerne i alle farger\n" +
            "• Vann som tørstedrikk\n\n" +
            "Jeg gir ikke slankeråd eller vektmål. Er du eller foreldrene dine bekymret for vekten, er helsesykepleier eller fastlegen de rette å snakke med – de hjelper gjerne!"
        Topic.SUPPLEMENT -> "Kosttilskudd som proteinpulver, kreatin, pre-workout eller «fettforbrennere» anbefales ikke for ungdom på 14 år. 🙅\n\n" +
            "Du får alt du trenger fra vanlig mat – og den virker best! Gode «muskelbyggere»:\n" +
            "• Melk, yoghurt og ost\n• Egg\n• Fisk og kylling\n• Bønner og linser\n• Brødskive med pålegg etter trening\n\n" +
            "Energidrikker inneholder mye koffein og passer ikke for barn og unge. Lurer du på noe om tilskudd, snakk med lege eller helsesykepleier."
        Topic.EXTREME -> "Jeg skjønner at du er motivert – det er kjempebra! 🔥 Men mer er ikke alltid bedre.\n\n" +
            "Musklene blir sterkere i HVILEN etter trening, ikke under. For deg anbefaler vi:\n" +
            "• 2–4 styrkeøkter per uke, med minst én dag mellom hver for samme muskelgruppe\n" +
            "• Økter på 20–45 minutter\n" +
            "• Lette dager med rolig sykling eller tøying\n" +
            "• Aldri trene gjennom smerte\n\n" +
            "Følg programmet og øk litt og litt – da blir du sterkere enn om du kjører deg helt tom."
        Topic.BODY_IMAGE -> "Det er vanlig å tenke mye på kroppen sin – men du er mye mer enn utseendet ditt. 💙\n\n" +
            "I Muscly feirer vi det du KAN gjøre: flere push-ups, lengre planke, bedre kondis. Det er ekte fremgang.\n\n" +
            "Kroppen din er i vekst og forandrer seg mye de neste årene. Hvis tankene om kroppen gjør deg lei deg ofte, snakk med en voksen du stoler på eller helsesykepleieren på skolen."
    }

    private val forbiddenOutput = listOf("kaloriunderskudd", "kalorirestriksjon", "spis mindre", "slankekur", "kreatin", "steroid", "anabol", "fettforbrenner", "pre-workout", "tren gjennom smerten", "ignorer smerten", "faste", "kutt karbo")

    /** Sjekker et svar fra språkmodellen. Returnerer false hvis det bryter sikkerhetsreglene. */
    fun outputOk(reply: String): Boolean {
        val t = norm(reply)
        return forbiddenOutput.none { t.contains(it) }
    }
}
