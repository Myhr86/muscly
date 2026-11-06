package no.muscly.app.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Enkel 2D-figur sett fra siden (ansikt mot høyre). Koordinater 0..100, gulv på y = 90.
 * Vinkler i grader: 0 = rett ned, 90 = fremover (høyre), 180 = rett opp, -90 = bakover.
 */
data class Pt(val x: Float, val y: Float) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
    operator fun times(k: Float) = Pt(x * k, y * k)
    fun len() = sqrt(x * x + y * y)
}

fun dir(deg: Float): Pt {
    val r = Math.toRadians(deg.toDouble())
    return Pt(sin(r).toFloat(), cos(r).toFloat())
}

fun angleOf(v: Pt): Float = Math.toDegrees(atan2(v.x.toDouble(), v.y.toDouble())).toFloat()

object Body {
    const val TORSO = 26f; const val NECK = 3f; const val HEAD_R = 5.5f
    const val UPPER_ARM = 13f; const val FOREARM = 12f
    const val THIGH = 19f; const val SHIN = 18f; const val FOOT = 7f
}

/** En lem: enten absolutte vinkler, eller et mål (hånd/fot) som løses med invers kinematikk. */
sealed class Limb {
    data class Ang(val a1: Float, val a2: Float) : Limb()
    data class Reach(val x: Float, val y: Float, val bend: Float = 1f) : Limb()
}

fun ang(a1: Float, a2: Float) = Limb.Ang(a1, a2)
fun reach(x: Float, y: Float, bend: Float = 1f) = Limb.Reach(x, y, bend)

data class Pose(
    val hip: Pt,
    val torso: Float = 180f,
    val armF: Limb = ang(0f, 0f),
    val armB: Limb = ang(0f, 0f),
    val legF: Limb = ang(0f, 0f),
    val legB: Limb = ang(0f, 0f),
    /** Absolutt hodevinkel; null = i forlengelsen av overkroppen. */
    val head: Float? = null,
    /** Fotvinkel relativt til normal (+ = tær opp, - = på tå). */
    val footF: Float = 0f,
    val footB: Float = 0f,
)

/** Ferdig utregnede punkter som tegnes. */
data class Skeleton(
    val head: Pt, val neck: Pt, val hip: Pt,
    val elbowF: Pt, val handF: Pt, val elbowB: Pt, val handB: Pt,
    val kneeF: Pt, val ankleF: Pt, val toeF: Pt,
    val kneeB: Pt, val ankleB: Pt, val toeB: Pt,
)

object Kinematics {
    private fun solve(root: Pt, limb: Limb, l1: Float, l2: Float): Pair<Pt, Pt> = when (limb) {
        is Limb.Ang -> {
            val j = root + dir(limb.a1) * l1
            j to (j + dir(limb.a2) * l2)
        }
        is Limb.Reach -> {
            val t = Pt(limb.x, limb.y)
            val v = t - root
            var d = v.len().coerceAtLeast(0.001f)
            val maxD = l1 + l2 - 0.01f
            val u = v * (1f / d)
            if (d > maxD) d = maxD
            val a = (l1 * l1 - l2 * l2 + d * d) / (2 * d)
            val h = sqrt((l1 * l1 - a * a).coerceAtLeast(0f))
            val perp = Pt(-u.y, u.x)
            val joint = root + u * a + perp * (h * limb.bend)
            joint to (root + u * d)
        }
    }

    /** Gjør om en lem til vinkler (for jevn interpolering). */
    private fun toAngles(root: Pt, limb: Limb, l1: Float, l2: Float): Limb.Ang {
        if (limb is Limb.Ang) return limb
        val (j, e) = solve(root, limb, l1, l2)
        return Limb.Ang(angleOf(j - root), angleOf(e - j))
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun lerpLimb(ra: Pt, rb: Pt, a: Limb, b: Limb, l1: Float, l2: Float, t: Float): Limb {
        if (a is Limb.Reach && b is Limb.Reach) {
            return Limb.Reach(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, if (t < 0.5f) a.bend else b.bend)
        }
        val aa = toAngles(ra, a, l1, l2); val bb = toAngles(rb, b, l1, l2)
        return Limb.Ang(lerpAngle(aa.a1, bb.a1, t), lerpAngle(aa.a2, bb.a2, t))
    }

    fun lerp(a: Pose, b: Pose, t: Float): Pose {
        val hip = a.hip + (b.hip - a.hip) * t
        val torso = lerpAngle(a.torso, b.torso, t)
        val shA = a.hip + dir(a.torso) * Body.TORSO
        val shB = b.hip + dir(b.torso) * Body.TORSO
        val headA = a.head ?: a.torso; val headB = b.head ?: b.torso
        return Pose(
            hip = hip, torso = torso,
            armF = lerpLimb(shA, shB, a.armF, b.armF, Body.UPPER_ARM, Body.FOREARM, t),
            armB = lerpLimb(shA, shB, a.armB, b.armB, Body.UPPER_ARM, Body.FOREARM, t),
            legF = lerpLimb(a.hip, b.hip, a.legF, b.legF, Body.THIGH, Body.SHIN, t),
            legB = lerpLimb(a.hip, b.hip, a.legB, b.legB, Body.THIGH, Body.SHIN, t),
            head = lerpAngle(headA, headB, t),
            footF = lerpAngle(a.footF, b.footF, t), footB = lerpAngle(a.footB, b.footB, t),
        )
    }

    fun skeleton(p: Pose): Skeleton {
        val neck = p.hip + dir(p.torso) * Body.TORSO
        val head = neck + dir(p.head ?: p.torso) * (Body.NECK + Body.HEAD_R)
        val (eF, hF) = solve(neck, p.armF, Body.UPPER_ARM, Body.FOREARM)
        val (eB, hB) = solve(neck, p.armB, Body.UPPER_ARM, Body.FOREARM)
        val (kF, aF) = solve(p.hip, p.legF, Body.THIGH, Body.SHIN)
        val (kB, aB) = solve(p.hip, p.legB, Body.THIGH, Body.SHIN)
        val tF = aF + dir(angleOf(aF - kF) + 90f + p.footF) * Body.FOOT
        val tB = aB + dir(angleOf(aB - kB) + 90f + p.footB) * Body.FOOT
        return Skeleton(head, neck, p.hip, eF, hF, eB, hB, kF, aF, tF, kB, aB, tB)
    }
}

enum class PropType { FLOOR, BAR, CHAIR, STEP, STAIRS, WALL, BIKE, DOOR }

data class Prop(val type: PropType, val x: Float = 0f, val y: Float = 0f, val w: Float = 0f, val h: Float = 0f)

enum class DumbbellStyle { NONE, HORIZONTAL, VERTICAL, GOBLET, ONE_HAND }

enum class AnimMode { PING_PONG, LOOP, HOLD }

data class Illustration(
    val frames: List<Pose>,
    val props: List<Prop> = listOf(Prop(PropType.FLOOR)),
    val dumbbells: DumbbellStyle = DumbbellStyle.NONE,
    val mode: AnimMode = AnimMode.PING_PONG,
    /** Millisekunder per overgang mellom to rammer. */
    val msPerFrame: Int = 1100,
    /** Pause på hver ramme (ms). */
    val holdMs: Int = 350,
    /** Ulik fart på vei ned/tilbake (f.eks. sakte negativ pull-up). */
    val returnMs: Int? = null,
    val frontView: Boolean = false,
    val caption: List<String> = listOf("Start", "Slutt"),
)
