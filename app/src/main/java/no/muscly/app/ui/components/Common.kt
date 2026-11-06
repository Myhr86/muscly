package no.muscly.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.muscly.app.ui.theme.NumberStyle
import kotlin.math.max

@Composable
fun MCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, color: Color = MaterialTheme.colorScheme.surfaceContainer, content: @Composable ColumnScope.() -> Unit) {
    val m = modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(color)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(18.dp)
    Column(m, content = content)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 58.dp), shape = MaterialTheme.shapes.large) {
        if (icon != null) { icon(); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Column(modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large).padding(14.dp)) {
        Text(value, style = NumberStyle.copy(fontSize = 32.sp), color = accent)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondaryContainer, textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = textColor,
        modifier = modifier.clip(CircleShape).background(color).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
fun ProgressRing(fraction: Float, size: Dp, stroke: Dp = 12.dp, center: @Composable () -> Unit) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val c1 = MaterialTheme.colorScheme.primary; val c2 = MaterialTheme.colorScheme.secondary
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arc = Size(this.size.width - sw, this.size.height - sw)
            drawArc(track, -90f, 360f, false, Offset(inset, inset), arc, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(listOf(c1, c2, c1)), -90f, 360f * anim, false, Offset(inset, inset), arc, style = Stroke(sw, cap = StrokeCap.Round))
        }
        center()
    }
}

/** Enkel linjegraf. Verdier tegnes som punkter med linje; farge per punkt valgfritt (trinn). */
@Composable
fun LineChart(values: List<Float>, labels: List<String>, modifier: Modifier = Modifier, unit: String = "", pointColors: List<Color>? = null) {
    val cs = MaterialTheme.colorScheme
    if (values.isEmpty()) {
        Box(modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Text("Ingen data ennå – gjennomfør en økt!", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    val maxV = max(values.max(), 1f) * 1.15f
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val w = size.width; val h = size.height
            for (i in 0..3) { val y = h * i / 3f; drawLine(cs.outlineVariant, Offset(0f, y), Offset(w, y), 1f) }
            val n = values.size
            val pts = values.mapIndexed { i, v -> Offset(if (n == 1) w / 2 else w * i / (n - 1).toFloat(), h - (v / maxV) * h) }
            if (n > 1) {
                val area = Path().apply { moveTo(pts.first().x, h); pts.forEach { lineTo(it.x, it.y) }; lineTo(pts.last().x, h); close() }
                drawPath(area, Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.35f), Color.Transparent)))
                val line = Path().apply { moveTo(pts.first().x, pts.first().y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
                drawPath(line, cs.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            pts.forEachIndexed { i, p ->
                drawCircle(cs.surface, 6.dp.toPx(), p)
                drawCircle(pointColors?.getOrNull(i) ?: cs.primary, 4.dp.toPx(), p)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(labels.firstOrNull() ?: "", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("Siste: ${fmt(values.last())} $unit", style = MaterialTheme.typography.labelMedium, color = cs.primary, fontWeight = FontWeight.Bold)
            Text(labels.lastOrNull() ?: "", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}

private fun fmt(v: Float) = if (v % 1f == 0f) v.toInt().toString() else String.format("%.1f", v)

@Composable
fun BarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier, goal: Int? = null) {
    val cs = MaterialTheme.colorScheme
    val maxV = max((values.maxOrNull() ?: 0), goal ?: 1).coerceAtLeast(1)
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(130.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEachIndexed { i, v ->
                val frac by animateFloatAsState(v / maxV.toFloat(), tween(700), label = "bar$i")
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (v > 0) "$v" else "", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    Box(Modifier.fillMaxWidth().fillMaxHeight(frac.coerceAtLeast(0.02f) * 0.85f).clip(MaterialTheme.shapes.small)
                        .background(if (goal != null && v >= goal) Brush.verticalGradient(listOf(cs.primary, cs.secondary)) else SolidColor(cs.surfaceVariant.copy(alpha = if (v > 0) 1f else 0.5f))))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 999, step: Int = 1, suffix: String = "", big: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, modifier = Modifier.size(if (big) 48.dp else 40.dp)) { Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        Text("$value$suffix", style = if (big) NumberStyle.copy(fontSize = 30.sp) else MaterialTheme.typography.titleLarge,
            modifier = Modifier.widthIn(min = if (big) 72.dp else 56.dp), textAlign = TextAlign.Center)
        FilledTonalIconButton(onClick = { onChange((value + step).coerceAtMost(max)) }, modifier = Modifier.size(if (big) 48.dp else 40.dp)) { Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun ChoiceCard(title: String, subtitle: String, selected: Boolean, modifier: Modifier = Modifier, emoji: String? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
        .background(if (selected) cs.primaryContainer else cs.surfaceContainer)
        .border(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant, MaterialTheme.shapes.large)
        .clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (emoji != null) { Text(emoji, fontSize = 26.sp); Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) cs.onPrimaryContainer else cs.onSurface)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (selected) cs.onPrimaryContainer.copy(alpha = 0.8f) else cs.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}
