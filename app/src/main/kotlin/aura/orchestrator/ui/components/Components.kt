package aura.orchestrator.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aura.orchestrator.ui.theme.DragonIndigo
import aura.orchestrator.ui.theme.EmberViolet
import aura.orchestrator.ui.theme.FrostCyan
import aura.orchestrator.ui.theme.InkBlack
import aura.orchestrator.ui.theme.MutedText
import aura.orchestrator.ui.theme.OnDark

/** Dragon Core orchestration state. */
enum class DragonState(val label: String, val glow: Color) {
    IDLE("Ready", EmberViolet),
    THINKING("Understanding", DragonIndigo),
    PLANNING("Planning", DragonIndigo),
    EXECUTING("Working", FrostCyan),
    WARNING("Attention", Color(0xFFFFB020)),
    ERROR("Error", Color(0xFFFF6B6B))
}

@Composable
fun AuraCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MutedText,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun StatusPill(status: String) {
    val (color, bg) = statusColor(status)
    Box(
        modifier = Modifier
            .background(bg, CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text = status, color = color, fontSize = 11.sp)
    }
}

private fun statusColor(status: String): Pair<Color, Color> = when (status) {
    "SUCCEEDED" -> Color(0xFF7BE09B) to Color(0x247BE09B)
    "RUNNING", "QUEUED", "RETRYING", "PENDING", "WAITING" -> Color(0xFF62D9FF) to Color(0x2462D9FF)
    "FAILED" -> Color(0xFFFF6B6B) to Color(0x24FF6B6B)
    "CANCELLED", "BLOCKED" -> MutedText to Color(0x24FFFFFF)
    else -> Color.White to Color(0x18FFFFFF)
}

/**
 * The Dragon Core orb. Pulsing rings run only when the orchestrator is active to
 * honour battery-conscious animation; the core stays near-static at rest.
 */
@Composable
fun DragonCore(
    state: DragonState,
    size: Int = 120
) {
    val active = state != DragonState.IDLE
    val transition = rememberInfiniteTransition(label = "dragon")
    val pulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val radiusScale = if (active) 0.15f * pulse else 0f

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val c = Offset(cx, cy)
            val r = minOf(w, h) / 2f
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke
            // soft outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(state.glow.copy(alpha = 0.25f + 0.20f * pulse), Color.Transparent),
                    center = c, radius = r
                ),
                radius = r, center = c
            )
            // flowing ring(s)
            drawCircle(color = state.glow.copy(alpha = 0.9f), radius = r * 0.52f, center = c, style = stroke(width = 2.dp.toPx()))
            if (radiusScale > 0f) {
                drawCircle(color = state.glow.copy(alpha = 0.5f), radius = r * (0.6f + radiusScale), center = c, style = stroke(width = 1.5.dp.toPx()))
            }
            // core
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, state.glow, InkBlack), center = Offset(cx - r * 0.1f, cy - r * 0.1f), radius = r * 0.4f),
                radius = r * 0.34f, center = c
            )
        }
        Text(text = "AURA", color = Color.White, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun ThinkingSteps(steps: List<String>, activeIndex: Int, done: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEachIndexed { i, label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val glyph = when {
                    done -> "✓"
                    i < activeIndex -> "✓"
                    i == activeIndex -> "●"
                    else -> "○"
                }
                val c = when {
                    done -> Color(0xFF7BE09B)
                    i < activeIndex -> Color(0xFF7BE09B)
                    i == activeIndex -> FrostCyan
                    else -> MutedText
                }
                Text(glyph, color = c, fontSize = 13.sp)
                Text("  $label", color = if (i == activeIndex) OnDark else MutedText, fontSize = 13.sp)
            }
        }
    }
}
