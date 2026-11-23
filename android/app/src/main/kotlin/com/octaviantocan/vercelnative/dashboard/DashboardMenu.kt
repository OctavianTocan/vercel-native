package com.octaviantocan.vercelnative.dashboard

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/** The transitions.dev Menu dropdown recipe, scaled from the actual trigger position. */
@Composable
internal fun DashboardMenu(
    expanded: Boolean,
    position: PopupPositionProvider,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val motion = updateTransition(expanded, label = "Menu")
    if (!motion.currentState && !motion.targetState && !motion.isRunning) return
    val fade by motion.animateFloat(
        transitionSpec = { DashboardMotion.move(if (targetState) DashboardMotion.MenuOpen else DashboardMotion.MenuClose) },
        label = "Menu opacity",
    ) { if (it) 1f else 0f }
    val grow by motion.animateFloat(
        transitionSpec = { DashboardMotion.move(if (targetState) DashboardMotion.MenuOpen else DashboardMotion.MenuClose) },
        label = "Menu scale",
    ) { if (it) 1f else 0f }
    val scale = (if (expanded) .97f else .99f).let { it + (1f - it) * grow }
    var pivot by remember { mutableStateOf(TransformOrigin.Center) }
    val anchor = remember(position) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val point = position.calculatePosition(anchorBounds, windowSize, layoutDirection, popupContentSize)
                pivot = TransformOrigin(
                    ((anchorBounds.center.x - point.x) / popupContentSize.width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                    ((anchorBounds.center.y - point.y) / popupContentSize.height.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                )
                return point
            }
        }
    }
    Popup(anchor, onDismissRequest = onClose, properties = PopupProperties(focusable = expanded)) {
        Box(Modifier.graphicsLayer {
            transformOrigin = pivot
            alpha = fade
            scaleX = scale
            scaleY = scale
        }.then(if (expanded) Modifier else Modifier.semantics { hideFromAccessibility() }.pointerInput(Unit) {
            awaitEachGesture { awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).consume() }
        })) {
            content()
        }
    }
}
