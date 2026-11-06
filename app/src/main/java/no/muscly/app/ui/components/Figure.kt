package no.muscly.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import no.muscly.app.domain.*
import kotlin.math.max
import kotlin.math.min

/** Kan slås av (f.eks. i tester eller for redusert bevegelse). */
object FigureAnimations { @Volatile var enabled = true }

/** Beregner hvilken pose som skal vises ved et gitt tidspunkt i animasjonen. */
object AnimTimeline {
    fun durations(ill: Illustration): List<Int> {
        val n = ill.frames.size
        if (n < 2) return listOf(1)
        val steps = mutableListOf<Int>()
        when (ill.mode) {
            AnimMode.LOOP -> repeat(n) { steps += ill.msPerFrame + ill.holdMs }
            else -> {
                repeat(n - 1) { steps += ill.msPerFrame + ill.holdMs }
                repeat(n - 1) { steps += (ill.returnMs ?: ill.msPerFrame) + ill.holdMs }
            }
        }
        return steps
    }

    fun total(ill: Illustration) = durations(ill).sum().coerceAtLeast(1)

    /** Returnerer posen og hvilken bildetekst (indeks) som gjelder. */
    fun poseAt(ill: Illustration, timeMs: Int): Pair<Pose, Int> {
        val f = ill.frames
        if (f.size < 2) return f.first() to 0
        val d = durations(ill)
        var t = Math.floorMod(timeMs, total(ill))
        var i = 0
        while (i < d.size - 1 && t >= d[i]) { t -= d[i]; i++ }
        val n = f.size
        val (from, to, move) = when (ill.mode) {
            AnimMode.LOOP -> Triple(i % n, (i + 1) % n, ill.msPerFrame)
            else -> if (i < n - 1) Triple(i, i + 1, ill.msPerFrame) else { val k = i - (n - 1); Triple(n - 1 - k, n - 2 - k, ill.returnMs ?: ill.msPerFrame) }
        }
        val raw = (t.toFloat() / move.coerceAtLeast(1)).coerceIn(0f, 1f)
        val eased = if (ill.mode == AnimMode.LOOP) raw else FastOutSlowInEasing.transform(raw)
        val pose = Kinematics.lerp(f[from], f[to], eased)
        val caption = if (ill.mode == AnimMode.LOOP) 0 else (if (eased > 0.5f) to else from).coerceIn(0, ill.caption.lastIndex.coerceAtLeast(0))
        return pose to caption
    }
}

@Composable
fun ExerciseAnimation(
    key: String,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    showCaption: Boolean = true,
    staticFrame: Int? = null,
) {
    val ill = remember(key) { Illustrations.get(key) }
    val total = remember(ill) { AnimTimeline.total(ill) }
    val time = if (playing && staticFrame == null && FigureAnimations.enabled) {
        val inf = rememberInfiniteTransition(label = "fig")
        inf.animateFloat(0f, total.toFloat(), infiniteRepeatable(tween(total, easing = LinearEasing)), label = "t").value.toInt()
    } else 0
    val still = staticFrame ?: if (!FigureAnimations.enabled || !playing) ill.frames.lastIndex else null
    val (pose, cap) = if (still != null) ill.frames[still.coerceIn(0, ill.frames.lastIndex)] to still.coerceIn(0, ill.caption.lastIndex.coerceAtLeast(0))
                      else AnimTimeline.poseAt(ill, time)
    val cs = MaterialTheme.colorScheme
    Box(modifier.clip(MaterialTheme.shapes.large).background(Brush.verticalGradient(listOf(cs.surfaceContainerHigh, cs.surfaceContainerLow)))) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            drawFigureScene(ill, pose, cs.primary, cs.onSurface, cs.onSurfaceVariant, cs.outline, cs.secondary)
        }
        if (showCaption && ill.caption.isNotEmpty()) {
            val label = ill.caption.getOrElse(cap) { "" }
            Text(label, style = MaterialTheme.typography.labelLarge, color = cs.onPrimary,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).clip(MaterialTheme.shapes.small).background(cs.primary).padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

private fun DrawScope.drawFigureScene(ill: Illustration, pose: Pose, accent: Color, body: Color, far: Color, prop: Color, metal: Color) {
    val s = min(size.width, size.height) / 100f
    val ox = (size.width - 100f * s) / 2f
    val oy = (size.height - 100f * s) / 2f
    fun o(p: Pt) = Offset(ox + p.x * s, oy + p.y * s)
    fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    val propStroke = 2.2f * s

    // --- Utstyr/omgivelser ---
    for (p in ill.props) when (p.type) {
        PropType.FLOOR -> drawLine(prop, o(2f, 90f), o(98f, 90f), propStroke, StrokeCap.Round)
        PropType.WALL -> { drawLine(prop, o(p.x + 1.5f, 4f), o(p.x + 1.5f, 90f), propStroke * 1.6f) }
        PropType.BAR -> {
            drawLine(prop, o(p.x, p.y), o(96f, p.y), propStroke * 0.8f, StrokeCap.Round)
            drawLine(prop, o(96f, 2f), o(96f, p.y + 10f), propStroke * 1.6f)
            drawCircle(metal, 2.2f * s, o(p.x, p.y))
        }
        PropType.CHAIR -> {
            drawLine(prop, o(p.x, p.y), o(p.x + p.w, p.y), propStroke * 1.4f, StrokeCap.Round)
            drawLine(prop, o(p.x + 1, p.y), o(p.x + 1, 90f), propStroke)
            drawLine(prop, o(p.x + p.w - 1, p.y), o(p.x + p.w - 1, 90f), propStroke)
        }
        PropType.STEP -> drawRoundRect(prop.copy(alpha = 0.55f), o(p.x, p.y), Size(p.w * s, (90f - p.y) * s), CornerRadius(1.5f * s))
        PropType.STAIRS -> {
            val stepW = 12f; val stepH = 90f - p.y
            var x = p.x; var top = p.y; var i = 0
            while (x < 100f && i < 4) {
                drawRect(prop.copy(alpha = 0.45f + 0.1f * i), o(x, top), Size((100f - x) * s, (90f - top) * s))
                x += stepW; top -= stepH; i++
                if (top < 8f) break
            }
        }
        PropType.BIKE -> {
            drawCircle(prop, 13f * s, o(64f, 76f), style = Stroke(propStroke))
            drawCircle(prop.copy(alpha = 0.5f), 9f * s, o(52f, 76f), style = Stroke(propStroke * 0.6f))
            drawLine(prop, o(38f, 49f), o(52f, 76f), propStroke * 1.5f)
            drawLine(prop, o(52f, 76f), o(70f, 42f), propStroke * 1.5f)
            drawLine(prop, o(70f, 42f), o(74f, 40f), propStroke * 1.5f, StrokeCap.Round)
            drawLine(prop, o(33f, 47f), o(43f, 47f), propStroke * 2f, StrokeCap.Round)
            drawLine(prop, o(46f, 90f), o(76f, 90f), propStroke * 2f, StrokeCap.Round)
            drawLine(prop, o(52f, 76f), o(52f, 90f), propStroke * 1.5f)
        }
        PropType.DOOR -> drawLine(prop, o(p.x, 10f), o(p.x, 90f), propStroke * 2)
    }

    val sk = Kinematics.skeleton(pose)
    val w = 4.6f * s
    val farC = far.copy(alpha = 0.65f)
    fun limb(c: Color, vararg pts: Pt, width: Float = w) {
        for (i in 0 until pts.size - 1) drawLine(c, o(pts[i]), o(pts[i + 1]), width, StrokeCap.Round)
    }
    // Bakre lemmer (dempet)
    limb(farC, sk.hip, sk.kneeB, sk.ankleB); limb(farC, sk.ankleB, sk.toeB, width = w * 0.8f)
    limb(farC, sk.neck, sk.elbowB, sk.handB, width = w * 0.85f)
    // Overkropp
    drawLine(accent, o(sk.hip), o(sk.neck), w * 1.6f, StrokeCap.Round)
    drawCircle(body, Body.HEAD_R * s, o(sk.head))
    // Fremre lemmer
    limb(body, sk.hip, sk.kneeF, sk.ankleF); limb(body, sk.ankleF, sk.toeF, width = w * 0.8f)
    limb(body, sk.neck, sk.elbowF, sk.handF, width = w * 0.9f)
    // Ledd
    listOf(sk.kneeF, sk.elbowF, sk.hip).forEach { drawCircle(accent, w * 0.35f, o(it)) }

    // Manualer
    fun dumbbell(at: Pt, vertical: Boolean, c: Color) {
        val len = 5f; val plate = 2.6f
        val a = if (vertical) Offset(0f, -len * s) else Offset(-len * s, 0f)
        val b = if (vertical) Offset(0f, len * s) else Offset(len * s, 0f)
        val center = o(at)
        drawLine(c, center + a, center + b, 1.6f * s, StrokeCap.Round)
        drawCircle(c, plate * s, center + a); drawCircle(c, plate * s, center + b)
    }
    when (ill.dumbbells) {
        DumbbellStyle.HORIZONTAL -> { dumbbell(sk.handB, false, metal.copy(alpha = 0.6f)); dumbbell(sk.handF, false, metal) }
        DumbbellStyle.VERTICAL -> { dumbbell(sk.handB, true, metal.copy(alpha = 0.6f)); dumbbell(sk.handF, true, metal) }
        DumbbellStyle.GOBLET -> dumbbell(Pt((sk.handF.x + sk.handB.x) / 2, (sk.handF.y + sk.handB.y) / 2), true, metal)
        DumbbellStyle.ONE_HAND -> dumbbell(sk.handF, false, metal)
        DumbbellStyle.NONE -> {}
    }
}
