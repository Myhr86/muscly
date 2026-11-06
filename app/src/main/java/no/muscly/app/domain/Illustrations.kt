package no.muscly.app.domain

import no.muscly.app.domain.PropType.*

/** Alle øvelsesillustrasjoner. Hver har minst en start- og sluttstilling. */
object Illustrations {
    private val floor = Prop(FLOOR)
    private val STAND = Pose(Pt(50f, 51f), legF = ang(1f, 1f), legB = ang(-2f, -2f), armF = ang(4f, 4f), armB = ang(-4f, -4f))

    private fun p(vararg f: Pose) = f.toList()

    val all: Map<String, Illustration> = mapOf(
        // ---------- ARMER ----------
        "curl" to Illustration(
            p(STAND.copy(armF = ang(6f, 8f), armB = ang(-2f, 0f)),
              STAND.copy(armF = ang(8f, 155f), armB = ang(2f, 150f))),
            dumbbells = DumbbellStyle.HORIZONTAL, msPerFrame = 1000, returnMs = 1600,
            caption = listOf("Armene rett ned", "Løft til skuldrene")
        ),
        "hammer" to Illustration(
            p(STAND.copy(armF = ang(6f, 8f), armB = ang(-2f, 0f)),
              STAND.copy(armF = ang(8f, 155f), armB = ang(2f, 150f))),
            dumbbells = DumbbellStyle.VERTICAL, msPerFrame = 1000, returnMs = 1600,
            caption = listOf("Tommelen peker frem", "Løft – tommel opp")
        ),
        "press" to Illustration(
            p(STAND.copy(armF = ang(30f, 172f), armB = ang(22f, 175f)),
              STAND.copy(armF = ang(176f, 180f), armB = ang(172f, 178f))),
            dumbbells = DumbbellStyle.HORIZONTAL, caption = listOf("Manualer ved skuldrene", "Press rett opp")
        ),
        "row" to Illustration(
            p(Pose(Pt(40f, 53f), torso = 105f, armF = ang(0f, 0f), armB = reach(80f, 64f, 1f), legF = ang(25f, -5f), legB = ang(-15f, -10f)),
              Pose(Pt(40f, 53f), torso = 105f, armF = ang(-95f, 5f), armB = reach(80f, 64f, 1f), legF = ang(25f, -5f), legB = ang(-15f, -10f))),
            props = listOf(floor, Prop(CHAIR, 72f, 64f, 18f, 26f)), dumbbells = DumbbellStyle.ONE_HAND,
            caption = listOf("Armen henger rett ned", "Trekk albuen bakover")
        ),
        "dips" to Illustration(
            p(Pose(Pt(46f, 71f), torso = 180f, armF = reach(42f, 70f, 1f), armB = reach(44f, 70f, 1f), legF = reach(72f, 88f, -1f), legB = reach(70f, 88f, -1f)),
              Pose(Pt(48f, 81f), torso = 180f, armF = reach(42f, 70f, 1f), armB = reach(44f, 70f, 1f), legF = reach(72f, 88f, -1f), legB = reach(70f, 88f, -1f))),
            props = listOf(floor, Prop(STEP, 14f, 70f, 30f, 20f)), caption = listOf("Strake armer", "Bøy albuene bakover")
        ),
        // ---------- PUSH-UPS ----------
        "push_wall" to Illustration(
            p(Pose(Pt(50.9f, 51.5f), torso = 171f, armF = reach(80f, 30f, 1f), armB = reach(80f, 32f, 1f), legF = ang(-9f, -9f), legB = ang(-9f, -9f), footF = -25f, footB = -25f),
              Pose(Pt(58.1f, 53.4f), torso = 160f, armF = reach(80f, 30f, 1f), armB = reach(80f, 32f, 1f), legF = ang(-20f, -20f), legB = ang(-20f, -20f), footF = -35f, footB = -35f)),
            props = listOf(floor, Prop(WALL, 80f)), caption = listOf("Hendene på veggen", "Brystet mot veggen")
        ),
        "push_incline" to Illustration(
            p(Pose(Pt(52.4f, 64.3f), torso = 131f, armF = reach(72f, 72f, 1f), armB = reach(72f, 72f, 1f), legF = ang(-49f, -49f), legB = ang(-49f, -49f)),
              Pose(Pt(58f, 73.5f), torso = 114f, armF = reach(72f, 72f, 1f), armB = reach(72f, 72f, 1f), legF = ang(-66f, -66f), legB = ang(-66f, -66f))),
            props = listOf(floor, Prop(STAIRS, 62f, 72f, 38f, 18f)), caption = listOf("Hendene på trappetrinnet", "Senk brystet mot trinnet")
        ),
        "push_knee" to Illustration(
            p(Pose(Pt(46.2f, 77f), torso = 122f, armF = reach(68.8f, 88f, 1f), armB = reach(68.8f, 88f, 1f), legF = ang(-58f, -90f), legB = ang(-58f, -90f), head = 112f),
              Pose(Pt(48.7f, 83.6f), torso = 100f, armF = reach(68.8f, 88f, 1f), armB = reach(68.8f, 88f, 1f), legF = ang(-80f, -100f), legB = ang(-80f, -100f), head = 95f)),
            caption = listOf("Knærne i gulvet, rett rygg", "Senk brystet")
        ),
        "push" to Illustration(
            p(Pose(Pt(45f, 73.6f), torso = 114f, armF = reach(68.8f, 88f, 1f), armB = reach(68.8f, 88f, 1f), legF = ang(-66f, -66f), legB = ang(-66f, -66f), head = 108f),
              Pose(Pt(47.6f, 82.4f), torso = 99f, armF = reach(68.8f, 88f, 1f), armB = reach(68.8f, 88f, 1f), legF = ang(-81f, -81f), legB = ang(-81f, -81f), head = 96f)),
            caption = listOf("Kroppen som en planke", "Brystet nesten i gulvet")
        ),
        // ---------- PULL-UPS ----------
        "hang" to Illustration(
            p(Pose(Pt(52f, 61f), torso = 180f, armF = reach(53f, 12f, 1f), armB = reach(51f, 12f, 1f), legF = ang(18f, -60f), legB = ang(12f, -70f)),
              Pose(Pt(52f, 58f), torso = 180f, armF = reach(53f, 12f, 1f), armB = reach(51f, 12f, 1f), legF = ang(18f, -60f), legB = ang(12f, -70f))),
            props = listOf(Prop(BAR, 52f, 12f)), msPerFrame = 1400, holdMs = 1200,
            caption = listOf("Heng avslappet", "Trekk skuldrene litt ned")
        ),
        "pull_assisted" to Illustration(
            p(Pose(Pt(52f, 61f), torso = 180f, armF = reach(53f, 12f, 1f), armB = reach(51f, 12f, 1f), legF = reach(30f, 70f, -1f), legB = ang(10f, -60f)),
              Pose(Pt(49.5f, 44f), torso = 184f, armF = reach(53f, 12f, 1f), armB = reach(51f, 12f, 1f), legF = reach(30f, 70f, -1f), legB = ang(15f, -50f))),
            props = listOf(floor, Prop(BAR, 52f, 12f), Prop(CHAIR, 20f, 70f, 18f, 20f)),
            caption = listOf("Én fot på stolen", "Trekk deg opp – foten hjelper")
        ),
        "pull_negative" to Illustration(
            p(Pose(Pt(49.5f, 44f), torso = 184f, armF = reach(53f, 12f, 1f), armB = reach(51f, 12f, 1f), legF = ang(20f, -60f), legB = ang(14f, -70f)),
              Pose(Pt(52f, 61f), torso = 180f, armF = reach(53f, 12f, 1f), armB = reach(51f, 12f, 1f), legF = ang(18f, -60f), legB = ang(12f, -70f))),
            props = listOf(Prop(BAR, 52f, 12f)), msPerFrame = 3500, returnMs = 700, holdMs = 500,
            caption = listOf("Start øverst (hopp opp)", "Senk deg SAKTE – 3–5 sek")
        ),
        "pull" to Illustration(
            p(Pose(Pt(52f, 61f), torso = 180f, armF = reach(53f, 12f, 1f), armB = reach(51f, 12f, 1f), legF = ang(18f, -60f), legB = ang(12f, -70f)),
              Pose(Pt(49.5f, 44f), torso = 184f, armF = reach(53f, 12f, 1f), armB = reach(51f, 12f, 1f), legF = ang(20f, -60f), legB = ang(14f, -70f))),
            props = listOf(Prop(BAR, 52f, 12f)), msPerFrame = 1200, returnMs = 1800,
            caption = listOf("Heng med strake armer", "Haka over stanga")
        ),
        "knee_raise_hang" to Illustration(
            p(Pose(Pt(52f, 58f), torso = 180f, armF = reach(53f, 9f, 1f), armB = reach(51f, 9f, 1f), legF = ang(4f, 2f), legB = ang(0f, -2f)),
              Pose(Pt(53f, 57f), torso = 178f, armF = reach(53f, 9f, 1f), armB = reach(51f, 9f, 1f), legF = ang(100f, 5f), legB = ang(96f, 2f))),
            props = listOf(Prop(BAR, 52f, 9f)), caption = listOf("Heng stille", "Løft knærne til hoftehøyde")
        ),
        // ---------- BEIN ----------
        "squat" to Illustration(
            p(Pose(Pt(50f, 51f), armF = ang(88f, 90f), armB = ang(84f, 88f), legF = reach(51f, 88f, -1f), legB = reach(49f, 88f, -1f)),
              Pose(Pt(37f, 67f), torso = 148f, armF = ang(100f, 95f), armB = ang(96f, 92f), legF = reach(51f, 88f, -1f), legB = reach(49f, 88f, -1f))),
            caption = listOf("Stå med føttene i skulderbredde", "Sitt bakover – lårene parallelle")
        ),
        "squat_chair" to Illustration(
            p(Pose(Pt(50f, 51f), armF = ang(88f, 90f), armB = ang(84f, 88f), legF = reach(53f, 88f, -1f), legB = reach(51f, 88f, -1f)),
              Pose(Pt(36f, 66f), torso = 150f, armF = ang(100f, 95f), armB = ang(96f, 92f), legF = reach(53f, 88f, -1f), legB = reach(51f, 88f, -1f))),
            props = listOf(floor, Prop(CHAIR, 22f, 68f, 18f, 22f)), caption = listOf("Stolen bak deg", "Kyss stolen med baken – reis deg")
        ),
        "squat_goblet" to Illustration(
            p(Pose(Pt(50f, 51f), armF = ang(20f, 160f), armB = ang(16f, 158f), legF = reach(51f, 88f, -1f), legB = reach(49f, 88f, -1f)),
              Pose(Pt(37f, 67f), torso = 150f, armF = ang(45f, 168f), armB = ang(41f, 166f), legF = reach(51f, 88f, -1f), legB = reach(49f, 88f, -1f))),
            dumbbells = DumbbellStyle.GOBLET, caption = listOf("Manual foran brystet", "Sitt ned med rett rygg")
        ),
        "lunge" to Illustration(
            p(Pose(Pt(56f, 51f), legF = reach(58f, 88f, -1f), legB = ang(-3f, -3f)),
              Pose(Pt(44f, 66f), legF = reach(58f, 88f, -1f), legB = ang(-24f, -82f), footB = -50f)),
            caption = listOf("Stå oppreist", "Steg bakover – kneet mot gulvet")
        ),
        "lunge_db" to Illustration(
            p(Pose(Pt(56f, 51f), legF = reach(58f, 88f, -1f), legB = ang(-3f, -3f)),
              Pose(Pt(44f, 66f), legF = reach(58f, 88f, -1f), legB = ang(-24f, -82f), footB = -50f)),
            dumbbells = DumbbellStyle.HORIZONTAL, caption = listOf("Manualer i hendene", "Steg bakover – kneet mot gulvet")
        ),
        "stepup" to Illustration(
            p(Pose(Pt(44f, 52f), legF = reach(62f, 70f, -1f), legB = ang(0f, 0f), armF = ang(-10f, -5f), armB = ang(20f, 30f)),
              Pose(Pt(60f, 34f), torso = 172f, legF = reach(63f, 70f, -1f), legB = ang(50f, -10f), armF = ang(25f, 35f), armB = ang(-15f, -5f))),
            props = listOf(floor, Prop(STAIRS, 52f, 72f, 48f, 18f)), caption = listOf("Én fot på trinnet", "Press deg opp med fremre bein")
        ),
        "stepup_db" to Illustration(
            p(Pose(Pt(44f, 52f), legF = reach(62f, 70f, -1f), legB = ang(0f, 0f), armF = ang(2f, 2f), armB = ang(-2f, -2f)),
              Pose(Pt(60f, 34f), torso = 172f, legF = reach(63f, 70f, -1f), legB = ang(50f, -10f), armF = ang(4f, 4f), armB = ang(0f, 0f))),
            props = listOf(floor, Prop(STAIRS, 52f, 72f, 48f, 18f)), dumbbells = DumbbellStyle.HORIZONTAL,
            caption = listOf("Manualer i hendene", "Press deg opp med fremre bein")
        ),
        "wallsit" to Illustration(
            p(Pose(Pt(34f, 52f), armF = ang(2f, 2f), armB = ang(-2f, -2f), legF = reach(50f, 88f, -1f), legB = reach(49f, 88f, -1f)),
              Pose(Pt(34f, 70f), armF = ang(5f, 5f), armB = ang(0f, 0f), legF = reach(52f, 88f, -1f), legB = reach(51f, 88f, -1f))),
            props = listOf(floor, Prop(WALL, 29f)), msPerFrame = 1500, holdMs = 1800,
            caption = listOf("Ryggen mot veggen", "Skli ned – hold 90° i knærne")
        ),
        "calf" to Illustration(
            p(Pose(Pt(42.5f, 45.7f), armF = ang(70f, 70f), armB = ang(66f, 66f), legF = ang(0f, 0f), legB = ang(-1f, -1f), footF = 20f, footB = 20f),
              Pose(Pt(43.4f, 38.4f), armF = ang(80f, 70f), armB = ang(76f, 66f), legF = ang(0f, 0f), legB = ang(-1f, -1f), footF = -35f, footB = -35f)),
            props = listOf(floor, Prop(STEP, 46f, 80f, 40f, 10f), Prop(WALL, 75f)), msPerFrame = 900,
            caption = listOf("Hælene under trinnkanten", "Opp på tærne")
        ),
        "calf_single" to Illustration(
            p(Pose(Pt(42.5f, 45.7f), armF = ang(70f, 70f), armB = ang(66f, 66f), legF = ang(0f, 0f), legB = ang(-10f, -150f), footF = 20f),
              Pose(Pt(43.4f, 38.4f), armF = ang(80f, 70f), armB = ang(76f, 66f), legF = ang(0f, 0f), legB = ang(-10f, -150f), footF = -35f)),
            props = listOf(floor, Prop(STEP, 46f, 80f, 40f, 10f), Prop(WALL, 75f)), msPerFrame = 900,
            caption = listOf("Stå på ett bein", "Opp på tærne")
        ),
        "bridge" to Illustration(
            p(Pose(Pt(50f, 86f), torso = -90f, armF = reach(47f, 88f, -1f), armB = reach(46f, 88f, -1f), legF = reach(66f, 88f, -1f), legB = reach(65f, 88f, -1f)),
              Pose(Pt(46f, 72f), torso = -57.5f, head = -90f, armF = reach(47f, 88f, -1f), armB = reach(46f, 88f, -1f), legF = reach(66f, 88f, -1f), legB = reach(65f, 88f, -1f))),
            caption = listOf("Ligg på ryggen, føttene i gulvet", "Løft hoftene – stram setet")
        ),
        "bridge_single" to Illustration(
            p(Pose(Pt(50f, 86f), torso = -90f, armF = reach(47f, 88f, -1f), armB = reach(46f, 88f, -1f), legF = ang(120f, 120f), legB = reach(65f, 88f, -1f)),
              Pose(Pt(46f, 72f), torso = -57.5f, head = -90f, armF = reach(47f, 88f, -1f), armB = reach(46f, 88f, -1f), legF = ang(122f, 122f), legB = reach(65f, 88f, -1f))),
            caption = listOf("Ett bein strakt", "Løft hoftene på ett bein")
        ),
        // ---------- KJERNE ----------
        "plank" to Illustration(
            p(Pose(Pt(46.1f, 79.9f), torso = 103f, armF = ang(0f, 90f), armB = ang(0f, 90f), legF = ang(-77f, -77f), legB = ang(-77f, -77f), head = 98f),
              Pose(Pt(46.1f, 78.9f), torso = 104f, armF = ang(0f, 90f), armB = ang(0f, 90f), legF = ang(-76f, -76f), legB = ang(-76f, -76f), head = 98f)),
            msPerFrame = 1500, holdMs = 1500, caption = listOf("Albuene under skuldrene", "Rett linje fra hode til hæl")
        ),
        "plank_knee" to Illustration(
            p(Pose(Pt(46.6f, 81.6f), torso = 107f, armF = ang(0f, 90f), armB = ang(0f, 90f), legF = ang(-73f, -120f), legB = ang(-73f, -120f), head = 100f),
              Pose(Pt(46.6f, 81f), torso = 108f, armF = ang(0f, 90f), armB = ang(0f, 90f), legF = ang(-72f, -120f), legB = ang(-72f, -120f), head = 100f)),
            msPerFrame = 1500, holdMs = 1500, caption = listOf("Knærne i gulvet", "Rett linje fra hode til kne")
        ),
        "side_plank" to Illustration(
            p(Pose(Pt(46.1f, 79.9f), torso = 103f, armF = ang(180f, 180f), armB = ang(0f, 90f), legF = ang(-77f, -77f), legB = ang(-77f, -77f)),
              Pose(Pt(46.1f, 77.5f), torso = 105f, armF = ang(176f, 178f), armB = ang(0f, 90f), legF = ang(-75f, -75f), legB = ang(-75f, -75f))),
            msPerFrame = 1500, holdMs = 1500, caption = listOf("Ligg på siden på albuen", "Løft hoften – hold")
        ),
        "side_plank_knee" to Illustration(
            p(Pose(Pt(46.6f, 81.6f), torso = 107f, armF = ang(180f, 180f), armB = ang(0f, 90f), legF = ang(-73f, -120f), legB = ang(-73f, -120f)),
              Pose(Pt(46.6f, 80f), torso = 109f, armF = ang(176f, 178f), armB = ang(0f, 90f), legF = ang(-71f, -120f), legB = ang(-71f, -120f))),
            msPerFrame = 1500, holdMs = 1500, caption = listOf("På siden, knærne bøyd", "Løft hoften – hold")
        ),
        "deadbug" to Illustration(
            p(Pose(Pt(55f, 86f), torso = -90f, armF = ang(180f, 180f), armB = ang(178f, 178f), legF = ang(180f, 90f), legB = ang(178f, 88f)),
              Pose(Pt(55f, 86f), torso = -90f, armF = ang(-80f, -80f), armB = ang(178f, 178f), legF = ang(180f, 90f), legB = ang(97f, 97f))),
            caption = listOf("Armer og knær i 90°", "Strekk motsatt arm og bein")
        ),
        "mountain" to Illustration(
            p(Pose(Pt(45f, 73.6f), torso = 114f, armF = reach(68.8f, 88f, 1f), armB = reach(68.8f, 88f, 1f), legF = ang(70f, -80f), legB = ang(-66f, -66f), head = 108f),
              Pose(Pt(45f, 73.6f), torso = 114f, armF = reach(68.8f, 88f, 1f), armB = reach(68.8f, 88f, 1f), legF = ang(-66f, -66f), legB = ang(70f, -80f), head = 108f)),
            msPerFrame = 450, holdMs = 50, caption = listOf("Høy planke", "Bytt bein raskt")
        ),
        "birddog" to Illustration(
            p(Pose(Pt(40f, 68f), torso = 101f, armF = reach(65f, 88f, 1f), armB = reach(64f, 88f, 1f), legF = ang(0f, -90f), legB = ang(-2f, -90f)),
              Pose(Pt(40f, 68f), torso = 101f, armF = ang(100f, 100f), armB = reach(64f, 88f, 1f), legF = ang(0f, -90f), legB = ang(-96f, -96f))),
            msPerFrame = 1300, holdMs = 800, caption = listOf("På alle fire", "Strekk motsatt arm og bein")
        ),
        "superman" to Illustration(
            p(Pose(Pt(45f, 86f), torso = 90f, armF = ang(90f, 90f), armB = ang(90f, 90f), legF = ang(-90f, -90f), legB = ang(-90f, -90f)),
              Pose(Pt(45f, 86f), torso = 98f, armF = ang(104f, 104f), armB = ang(104f, 104f), legF = ang(-100f, -100f), legB = ang(-100f, -100f))),
            msPerFrame = 1200, holdMs = 900, caption = listOf("Ligg på magen", "Løft armer og bein litt")
        ),
        "knee_raise_lying" to Illustration(
            p(Pose(Pt(55f, 86f), torso = -90f, armF = ang(90f, 90f), armB = ang(90f, 90f), legF = ang(180f, 90f), legB = ang(178f, 88f)),
              Pose(Pt(55f, 84f), torso = -86f, armF = ang(90f, 90f), armB = ang(90f, 90f), legF = ang(-150f, 115f), legB = ang(-152f, 113f))),
            caption = listOf("Ligg på ryggen, bein i 90°", "Rull knærne mot brystet")
        ),
        // ---------- KONDISJON ----------
        "jacks" to Illustration(
            p(Pose(Pt(50f, 52f), armF = ang(12f, 10f), armB = ang(-12f, -10f), legF = ang(5f, 4f), legB = ang(-5f, -4f)),
              Pose(Pt(50f, 55f), armF = ang(150f, 165f), armB = ang(-150f, -165f), legF = ang(20f, 20f), legB = ang(-20f, -20f))),
            frontView = true, msPerFrame = 400, holdMs = 50, caption = listOf("Samlet", "Hopp ut – armene opp")
        ),
        "bike" to Illustration(
            (0 until 8).map { i ->
                val a = Math.toRadians((i * 45).toDouble())
                val ax = 52f + 9f * kotlin.math.cos(a).toFloat(); val ay = 76f + 9f * kotlin.math.sin(a).toFloat()
                val bx = 52f - 9f * kotlin.math.cos(a).toFloat(); val by = 76f - 9f * kotlin.math.sin(a).toFloat()
                Pose(Pt(38f, 47f), torso = 140f, armF = reach(70f, 40f, 1f), armB = reach(70f, 40f, 1f),
                    legF = reach(ax, ay, -1f), legB = reach(bx, by, -1f))
            },
            props = listOf(floor, Prop(BIKE)), mode = AnimMode.LOOP, msPerFrame = 120, holdMs = 0,
            caption = listOf("Rett rygg, avslappede skuldre", "Jevne pedaltråkk")
        ),
        "stairs" to Illustration(
            p(Pose(Pt(44f, 52f), torso = 170f, legF = reach(62f, 70f, -1f), legB = ang(-5f, -10f), armF = ang(-30f, 10f), armB = ang(35f, 120f)),
              Pose(Pt(60f, 34f), torso = 168f, legF = reach(63f, 70f, -1f), legB = ang(60f, -20f), armF = ang(35f, 120f), armB = ang(-30f, 10f))),
            props = listOf(floor, Prop(STAIRS, 52f, 72f, 48f, 18f)), msPerFrame = 650, holdMs = 60,
            caption = listOf("Driv med armene", "Hele foten på trinnet")
        ),
        // ---------- BEVEGELIGHET ----------
        "stretch_quad" to Illustration(
            p(Pose(Pt(50f, 51f), armF = reach(46f, 54f, 1f), armB = ang(80f, 80f), legF = ang(-4f, -168f), legB = ang(0f, 0f)),
              Pose(Pt(50f, 51f), armF = reach(45f, 53f, 1f), armB = ang(80f, 80f), legF = ang(-8f, -170f), legB = ang(0f, 0f))),
            props = listOf(floor, Prop(WALL, 76f)), msPerFrame = 2000, holdMs = 1500,
            caption = listOf("Hold foten bak", "Knærne inntil hverandre")
        ),
        "stretch_hip" to Illustration(
            p(Pose(Pt(44f, 68f), legF = reach(60f, 88f, -1f), legB = ang(-20f, -88f), footB = -50f, armF = ang(30f, 0f), armB = ang(25f, 0f)),
              Pose(Pt(47f, 69f), legF = reach(60f, 88f, -1f), legB = ang(-28f, -88f), footB = -50f, armF = ang(30f, 0f), armB = ang(25f, 0f))),
            msPerFrame = 2000, holdMs = 1500, caption = listOf("Ett kne i gulvet", "Skyv hoften fremover")
        ),
        "stretch_ham" to Illustration(
            p(Pose(Pt(44f, 51f), legF = reach(66f, 78f, -1f), legB = ang(0f, 0f), footF = 25f, armF = ang(30f, 30f), armB = ang(25f, 25f)),
              Pose(Pt(43f, 53f), torso = 140f, legF = reach(66f, 78f, -1f), legB = ang(0f, 0f), footF = 25f, armF = ang(55f, 50f), armB = ang(50f, 45f))),
            props = listOf(floor, Prop(STAIRS, 60f, 80f, 40f, 10f)), msPerFrame = 2000, holdMs = 1500,
            caption = listOf("Hælen på trinnet", "Len deg fremover med rett rygg")
        ),
        "stretch_child" to Illustration(
            p(Pose(Pt(34f, 76f), torso = 120f, legF = ang(70f, -90f), legB = ang(70f, -90f), armF = ang(150f, 140f), armB = ang(150f, 140f)),
              Pose(Pt(30f, 80f), torso = 100f, legF = ang(80f, -90f), legB = ang(80f, -90f), armF = ang(95f, 92f), armB = ang(95f, 92f))),
            msPerFrame = 2200, holdMs = 1500, caption = listOf("Sitt på hælene", "Strekk armene frem og pust rolig")
        ),
    )

    fun get(key: String): Illustration = all[key] ?: all.getValue("squat")
}
