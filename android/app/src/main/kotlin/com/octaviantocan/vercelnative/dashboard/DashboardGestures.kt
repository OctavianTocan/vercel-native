package com.octaviantocan.vercelnative.dashboard

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Pully's row gesture claims horizontal touch slop before its parent pager can use the drag. */
@Composable
internal fun Modifier.dashboardHorizontalDrag(
    accepts: (Float) -> Boolean = { true },
    onStart: () -> Unit = {},
    onDelta: (Float) -> Unit,
    onStop: (velocity: Float, completed: Boolean) -> Unit,
): Modifier {
    val accept by rememberUpdatedState(accepts)
    val start by rememberUpdatedState(onStart)
    val delta by rememberUpdatedState(onDelta)
    val stop by rememberUpdatedState(onStop)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var claimed = false
            val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                if (accept(over)) {
                    change.consume()
                    claimed = true
                    start()
                    tracker.addPosition(change.uptimeMillis, change.position)
                    delta(over)
                }
            }
            if (slop == null || !claimed) return@awaitEachGesture
            val completed = horizontalDrag(slop.id) { change ->
                tracker.addPosition(change.uptimeMillis, change.position)
                delta(change.positionChange().x)
                change.consume()
            }
            stop(tracker.calculateVelocity().x, completed)
        }
    }
}

/** The tab strip and its pages share one scroll position and settle with the same pager snap. */
@Composable
internal fun Modifier.dragDashboardTabs(pager: PagerState): Modifier = scrollable(
    state = pager,
    orientation = Orientation.Horizontal,
    flingBehavior = PagerDefaults.flingBehavior(pager),
    reverseDirection = ScrollableDefaults.reverseDirection(LocalLayoutDirection.current, Orientation.Horizontal, false),
)

/** Keeps the workspace panel at the finger's position until the drawer settles. */
@Stable
internal class WorkspaceRailMotion {
    var fraction by mutableFloatStateOf(0f)
        private set
    private var moving: Job? = null
    private var held = false

    fun grab() {
        moving?.cancel()
        held = true
    }

    fun drag(delta: Float, width: Float) {
        fraction = (fraction + delta / width.coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    fun release(completed: Boolean, velocity: Float, density: Float, wasOpen: Boolean): Boolean {
        held = false
        return when {
            !completed -> wasOpen
            velocity > 650f * density -> true
            velocity < -650f * density -> false
            else -> fraction >= if (wasOpen) .68f else .32f
        }
    }

    fun settle(scope: CoroutineScope, open: Boolean) {
        if (held) return
        moving?.cancel()
        moving = scope.launch {
            animate(fraction, if (open) 1f else 0f,
                animationSpec = DashboardMotion.move(if (open) DashboardMotion.MenuOpen else DashboardMotion.MenuClose),
            ) { value, _ -> fraction = value }
        }
    }
}

@Composable
internal fun RecentSwipeActions(
    onPreview: () -> Unit,
    onRemove: () -> Unit,
    content: @Composable () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    val density = LocalDensity.current.density
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val preview by rememberUpdatedState(onPreview)
    val remove by rememberUpdatedState(onRemove)
    var shift by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf<Job?>(null) }
    val threshold = minOf(110f * density, width * .45f)
    val reveal = (abs(shift) / (60f * density)).coerceIn(0f, 1f)

    Box(
        Modifier.fillMaxWidth().clipToBounds().onSizeChanged { width = it.width.toFloat() }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Preview") { preview(); true },
                    CustomAccessibilityAction("Remove from Recents") { remove(); true },
                )
            },
    ) {
        if (shift != 0f) {
            val opening = shift > 0f
            val ink = if (opening) palette.link else palette.error
            Row(
                Modifier.matchParentSize().graphicsLayer { alpha = reveal }
                    .background(if (opening) palette.badge else palette.errorSurface)
                    .padding(horizontal = Tokens.Space20),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space8, if (opening) Alignment.Start else Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (opening) DashboardIcon(Glyph.MenuPreview, ink)
                Text(if (opening) "Preview" else "Remove", color = ink, fontWeight = FontWeight.Medium)
                if (!opening) DashboardIcon(Glyph.MenuRemove, ink)
            }
        }
        Box(
            Modifier.graphicsLayer {
                translationX = shift
                val radius = reveal * 1.3f * density
                renderEffect = if (radius > .1f) BlurEffect(radius, radius, TileMode.Decal) else null
            }.background(palette.canvas).dashboardHorizontalDrag(
                onStart = { settling?.cancel() },
                onDelta = { delta ->
                    val armed = abs(shift) >= threshold
                    shift = (shift + delta).coerceIn(-width, width)
                    if (armed != (abs(shift) >= threshold)) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                },
                onStop = { _, completed ->
                    val action = when {
                        completed && shift >= threshold && shift > 0f -> preview
                        completed && shift <= -threshold && shift < 0f -> remove
                        else -> null
                    }
                    settling = scope.launch {
                        if (action != null) {
                            val rest = (if (shift > 0f) 1 else -1) * minOf(72f * density, abs(shift))
                            animate(shift, rest, animationSpec = DashboardMotion.move(DashboardMotion.Quick)) { value, _ -> shift = value }
                            action()
                        }
                        animate(shift, 0f, animationSpec = DashboardMotion.move(DashboardMotion.Fast)) { value, _ -> shift = value }
                    }
                },
            ),
            content = { content() },
        )
    }
}
