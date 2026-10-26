package com.nobadhabbits.cornytask.features.mood_tracking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * One point on the mood chart. [label] goes on the x axis, [title] heads the tooltip,
 * [notes] are the comments of the entries that make up this point.
 */
internal data class ChartPoint(
    val label: String,
    val value: Float,
    val notes: List<String>,
    val title: String = label
)

private const val MIN_MOOD = -3f
private const val MAX_MOOD = 3f
private const val MAX_NOTES_IN_TOOLTIP = 3

/**
 * Simple line chart for mood scores (-3..3).
 * Tap or drag across the chart to select a point; a tooltip shows its value and comments.
 * Points with comments get an extra ring so it's obvious there's something to read.
 */
@Composable
internal fun MoodLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val emojiStyle = MaterialTheme.typography.bodyMedium

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val leftPad = with(density) { 32.dp.toPx() }
        val rightPad = with(density) { 16.dp.toPx() }
        val topPad = with(density) { 16.dp.toPx() }
        val bottomPad = with(density) { 28.dp.toPx() }
        val plotWidth = (width - leftPad - rightPad).coerceAtLeast(1f)
        val plotHeight = (height - topPad - bottomPad).coerceAtLeast(1f)

        fun xAt(index: Int): Float =
            if (points.size == 1) leftPad + plotWidth / 2
            else leftPad + plotWidth * index / (points.size - 1)

        fun yAt(value: Float): Float = topPad + plotHeight * (MAX_MOOD - value) / (MAX_MOOD - MIN_MOOD)

        fun nearestIndex(x: Float): Int = points.indices.minBy { abs(xAt(it) - x) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points, width, height) {
                    detectTapGestures { offset ->
                        val index = nearestIndex(offset.x)
                        selected = if (selected == index) null else index
                    }
                }
                .pointerInput(points, width, height) {
                    detectHorizontalDragGestures(
                        onDragStart = { selected = nearestIndex(it.x) }
                    ) { change, _ ->
                        selected = nearestIndex(change.position.x)
                    }
                }
        ) {
            // Horizontal grid line + emoji label for every mood level
            for (level in MIN_MOOD.toInt()..MAX_MOOD.toInt()) {
                val y = yAt(level.toFloat())
                drawLine(
                    color = if (level == 0) colors.outline else colors.outlineVariant,
                    start = Offset(leftPad, y),
                    end = Offset(width - rightPad, y),
                    strokeWidth = if (level == 0) 1.5.dp.toPx() else 1.dp.toPx()
                )
                val emoji = textMeasurer.measure(moodEmoji(level), emojiStyle)
                drawText(
                    emoji,
                    topLeft = Offset(
                        (leftPad - emoji.size.width) / 2,
                        y - emoji.size.height / 2
                    )
                )
            }

            if (points.isEmpty()) return@Canvas

            val selectedIndex = selected
            if (selectedIndex != null) {
                val x = xAt(selectedIndex)
                drawLine(
                    color = colors.primary.copy(alpha = 0.4f),
                    start = Offset(x, topPad),
                    end = Offset(x, topPad + plotHeight),
                    strokeWidth = 2.dp.toPx()
                )
            }

            val line = Path()
            points.forEachIndexed { i, p ->
                if (i == 0) line.moveTo(xAt(i), yAt(p.value)) else line.lineTo(xAt(i), yAt(p.value))
            }
            drawPath(
                line,
                color = colors.primary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            points.forEachIndexed { i, p ->
                val center = Offset(xAt(i), yAt(p.value))
                if (p.notes.isNotEmpty()) {
                    drawCircle(
                        color = colors.tertiary,
                        radius = 7.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                drawCircle(
                    color = colors.primary,
                    radius = if (i == selectedIndex) 6.dp.toPx() else 4.dp.toPx(),
                    center = center
                )
            }

            // X labels: skip some when there isn't room for all of them
            val maxLabels = (plotWidth / 56.dp.toPx()).toInt().coerceAtLeast(1)
            val step = ceil(points.size / maxLabels.toFloat()).toInt().coerceAtLeast(1)
            points.forEachIndexed { i, p ->
                if (i % step != 0) return@forEachIndexed
                val text = textMeasurer.measure(p.label, labelStyle, maxLines = 1)
                val x = (xAt(i) - text.size.width / 2)
                    .coerceIn(0f, (width - text.size.width).coerceAtLeast(0f))
                drawText(text, topLeft = Offset(x, topPad + plotHeight + 8.dp.toPx()))
            }
        }

        val selectedIndex = selected
        if (selectedIndex != null && selectedIndex in points.indices) {
            val point = points[selectedIndex]
            MoodTooltip(
                point = point,
                modifier = Modifier.layout { measurable, c ->
                    val placeable = measurable.measure(c.copy(minWidth = 0, minHeight = 0))
                    layout(placeable.width, placeable.height) {
                        val gap = 12.dp.toPx()
                        val px = xAt(selectedIndex)
                        val py = yAt(point.value)
                        val x = (px - placeable.width / 2)
                            .coerceIn(0f, (width - placeable.width).coerceAtLeast(0f))
                        // Prefer above the point, flip below if there's no room
                        val above = py - gap - placeable.height
                        val y = if (above >= 0) above
                        else (py + gap).coerceAtMost((height - placeable.height).coerceAtLeast(0f))
                        placeable.place(x.roundToInt(), y.roundToInt())
                    }
                }
            )
        }
    }
}

@Composable
private fun MoodTooltip(point: ChartPoint, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 240.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${point.title} •${moodEmoji(point.value.roundToInt())} ${formatScore(point.value)}",
                style = MaterialTheme.typography.labelLarge
            )
            if (point.notes.isEmpty()) {
                Text(
                    text = "No comment",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                point.notes.take(MAX_NOTES_IN_TOOLTIP).forEach { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val more = point.notes.size - MAX_NOTES_IN_TOOLTIP
                if (more > 0) {
                    Text(
                        text = "+$more more",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/** Whole numbers stay whole ("2"), averages get one decimal ("1.5"). */
private fun formatScore(value: Float): String =
    if (value == value.roundToInt().toFloat()) value.roundToInt().toString()
    else String.format(Locale.getDefault(), "%.1f", value)
