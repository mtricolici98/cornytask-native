package com.nobadhabbits.cornytask.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nobadhabbits.cornytask.ui.theme.DeepPink
import com.nobadhabbits.cornytask.ui.theme.LightPink
import com.nobadhabbits.cornytask.ui.theme.Pink40

private val ReorderRowHeight = 52.dp

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = DeepPink,
    unselectedIconColor = Pink40,
    selectedTextColor = DeepPink,
    unselectedTextColor = Pink40,
    indicatorColor = LightPink
)

@Composable
fun MainBottomBar(
    order: List<Destination>,
    currentRoute: String?,
    moreExpanded: Boolean,
    onNavigate: (Destination) -> Unit,
    onToggleMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barItems = order.take(Destination.BAR_SLOTS)
    val current = Destination.forRoute(currentRoute)

    NavigationBar(modifier = modifier) {
        barItems.forEach { destination ->
            NavigationBarItem(
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                selected = !moreExpanded && current == destination,
                onClick = { onNavigate(destination) },
                colors = navItemColors()
            )
        }
        // Highlighted when the menu is open or when we're on a screen that lives inside it.
        NavigationBarItem(
            icon = {
                Icon(
                    if (moreExpanded) Icons.Default.ExpandMore else Icons.Default.Menu,
                    contentDescription = if (moreExpanded) "Close menu" else "More"
                )
            },
            label = { Text("More") },
            selected = moreExpanded || (current != null && current !in barItems),
            onClick = onToggleMore,
            colors = navItemColors()
        )
    }
}

/** The sheet that slides up from the More button. */
@Composable
fun MoreMenuPanel(
    order: List<Destination>,
    currentRoute: String?,
    onNavigate: (Destination) -> Unit,
    onOrderChange: (List<Destination>) -> Unit,
    modifier: Modifier = Modifier
) {
    var arranging by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (arranging) "Arrange buttons" else "More",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { arranging = !arranging }) {
                    Text(if (arranging) "Done" else "Arrange")
                }
            }

            if (arranging) {
                Text(
                    text = "Drag to reorder. The top ${Destination.BAR_SLOTS} show in the bar, the rest stay in here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ReorderableDestinationList(order = order, onOrderChange = onOrderChange)
            } else {
                val menuItems = order.drop(Destination.BAR_SLOTS)
                val current = Destination.forRoute(currentRoute)
                if (menuItems.isEmpty()) {
                    Text(
                        "Everything is in the bar already.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                menuItems.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { destination ->
                            MenuTile(
                                destination = destination,
                                selected = destination == current,
                                onClick = { onNavigate(destination) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Keep tiles the same width on a half-filled last row.
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MenuTile(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) LightPink else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) DeepPink else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(destination.icon, contentDescription = null, tint = content)
        Spacer(Modifier.height(6.dp))
        Text(
            destination.label,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Drag-to-reorder list. Rows have a fixed height, so the dragged row is found from the
 * touch position and swapped with its neighbour once it has moved half a row.
 * The gesture lives on the (non-moving) column so the drag deltas stay stable while rows swap.
 */
@Composable
private fun ReorderableDestinationList(
    order: List<Destination>,
    onOrderChange: (List<Destination>) -> Unit
) {
    val items = remember { order.toMutableStateList() }
    var dragged by remember { mutableStateOf<Destination?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { ReorderRowHeight.toPx() }
    val haptic = LocalHapticFeedback.current

    fun finishDrag() {
        if (dragged != null) onOrderChange(items.toList())
        dragged = null
        dragOffset = 0f
    }

    Column(
        modifier = Modifier.pointerInput(rowHeightPx) {
            detectDragGestures(
                onDragStart = { start ->
                    dragged = items.getOrNull((start.y / rowHeightPx).toInt())
                    if (dragged != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { change, amount ->
                    val item = dragged ?: return@detectDragGestures
                    change.consume()
                    dragOffset += amount.y
                    val index = items.indexOf(item)
                    if (dragOffset > rowHeightPx / 2 && index < items.lastIndex) {
                        items.add(index + 1, items.removeAt(index))
                        dragOffset -= rowHeightPx
                    } else if (dragOffset < -rowHeightPx / 2 && index > 0) {
                        items.add(index - 1, items.removeAt(index))
                        dragOffset += rowHeightPx
                    }
                },
                onDragEnd = { finishDrag() },
                onDragCancel = { finishDrag() }
            )
        }
    ) {
        items.forEachIndexed { index, destination ->
            key(destination) {
                val isDragged = destination == dragged
                ReorderRow(
                    destination = destination,
                    inBar = index < Destination.BAR_SLOTS,
                    lifted = isDragged,
                    modifier = Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                )
            }
        }
    }
}

@Composable
private fun ReorderRow(
    destination: Destination,
    inBar: Boolean,
    lifted: Boolean,
    modifier: Modifier = Modifier
) {
    val container = when {
        lifted -> LightPink
        inBar -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ReorderRowHeight)
            .padding(vertical = 3.dp)
            .shadow(if (lifted) 6.dp else 0.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(destination.icon, contentDescription = null, tint = DeepPink)
        Spacer(Modifier.width(12.dp))
        Text(destination.label, modifier = Modifier.weight(1f))
        if (inBar) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = "Shown in the bar",
                tint = Pink40,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Icon(
            Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
