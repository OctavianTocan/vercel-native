package com.octaviantocan.vercelnative.dashboard

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

/** Inventory's backdrop shape and movement follow the sheet's visible progress. */
internal fun Modifier.dashboardRecede(top: Dp, veil: Color, progress: () -> Float): Modifier = graphicsLayer {
    val depth = progress()
    val scale = 1f - .08f * depth
    scaleX = scale
    scaleY = scale
    transformOrigin = TransformOrigin(.5f, 0f)
    translationY = (top + Tokens.Space6).toPx() * depth
    shape = RoundedCornerShape(Tokens.ProjectSheetRadius * depth / scale)
    clip = depth > 0f
}.drawWithContent {
    drawContent()
    val depth = progress()
    if (depth > 0f) drawRect(veil, alpha = .12f * depth)
}

@Composable
internal fun DashboardSheetHost(
    entry: DashboardEntry,
    navigation: DashboardNavigation,
    onBack: () -> Unit,
    holder: SaveableStateHolder,
    active: Boolean,
    reviewed: Boolean,
    childDepth: () -> Float,
    reportDepth: (Float) -> Unit,
) {
    val palette = LocalDashboardPalette.current
    val destination = entry.route.destination
    val project = destination == Destination.Project
    val paper = when (destination) {
        Destination.Theme -> if (palette == DashboardPalette.Light) palette.themeLightSheet else palette.sheet
        Destination.Photo, Destination.DefaultScope -> if (palette == DashboardPalette.Light) palette.sheet else palette.projectOptionsSurface
        else -> palette.sheet
    }
    val handleColor = when {
        project -> palette.projectSheetHandle
        destination == Destination.LogFilters -> palette.logFilterHandle
        palette == DashboardPalette.Light -> palette.sheetHandle
        destination in setOf(Destination.Theme, Destination.Toolbar, Destination.RemoveRecent, Destination.Help) -> palette.muted
        destination == Destination.Transfer && entry.route.variant in setOf("review", "progress", "done") -> palette.muted
        else -> palette.sheetHandle
    }
    val handleTop = if (project) Tokens.Space4 else Tokens.Space8
    val handleBottom = if (project) Tokens.Space2 else Tokens.Space4
    val handleSize = handleTop + Tokens.Space4 + handleBottom
    val radius = when {
        project -> Tokens.ProjectSheetRadius
        destination in setOf(Destination.Photo, Destination.DefaultScope) -> Tokens.AccountSheetRadius
        destination == Destination.Theme && palette == DashboardPalette.Light -> Tokens.AccountSheetRadius
        else -> Tokens.SheetRadius
    }
    val shape = if (project) RoundedCornerShape(radius) else RoundedCornerShape(topStart = radius, topEnd = radius)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val motion = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val haptic by rememberUpdatedState(LocalHapticFeedback.current)
    var tall by rememberSaveable { mutableStateOf(project && !reviewed) }
    val position = remember {
        DashboardSheetPosition(
            onLand = { haptic.performHapticFeedback(HapticFeedbackType.Confirm) },
            onThreshold = { haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
        ).apply { if (tall) openTall() }
    }
    val latestReport by rememberUpdatedState(reportDepth)
    val currentBack by rememberUpdatedState(onBack)
    val currentEntry by rememberUpdatedState(entry)
    val close: () -> Unit = {
        if (active && navigation.current == currentEntry) {
            position.dismiss(scope, motion) {
                if (navigation.current == currentEntry) currentBack()
            }
        }
    }
    val latestClose by rememberUpdatedState(close)
    LaunchedEffect(position) {
        snapshotFlow { position.scrim to position.tall }.collect { (depth, expanded) ->
            tall = expanded
            latestReport(depth)
        }
    }
    DisposableEffect(position) { onDispose { latestReport(0f) } }
    BackHandler(enabled = active, onBack = close)

    val density = LocalDensity.current
    val safe = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val top = with(density) { safe.getTop(this).toDp() }
    val systemBottom = with(density) { safe.getBottom(this).toDp() }
    val keyboardBottom = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val keyboardGap = (keyboardBottom - systemBottom).coerceAtLeast(Dp.Hairline)
    val focus = LocalFocusManager.current
    LaunchedEffect(keyboardGap) { if (keyboardGap > Dp.Hairline) position.lift(scope, motion) }
    LaunchedEffect(position.tall) {
        if (!position.tall && keyboardGap > Dp.Hairline) focus.clearFocus()
    }

    val speed = 200f * density.density
    val scroll = remember(position, speed, motion) {
        DashboardSheetScroll(position, scope, speed, { motion }, { latestClose() })
    }
    val drag = rememberDraggableState { position.dragBy(it) }
    val scrim = when {
        reviewed && project -> palette.canvas
        project -> Color.Black.copy(alpha = Tokens.LightSheetScrimOpacity)
        palette == DashboardPalette.Dark -> Color.Black.copy(alpha = Tokens.ScrimOpacity)
        else -> Color.Black.copy(alpha = Tokens.LightSheetScrimOpacity)
    }

    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().windowInsetsPadding(safe.only(WindowInsetsSides.Horizontal))) {
        val bottomGap = keyboardGap + if (project) systemBottom + Tokens.Inset else Dp.Hairline
        val innerBottom = if (project) Dp.Hairline else systemBottom
        val topGap = top + if (project) Tokens.Inset else Tokens.Space8
        val available = (maxHeight - topGap - bottomGap).coerceAtLeast(Tokens.Stroke)
        val bodyLimit = when (destination) {
            Destination.Project -> Tokens.ProjectSheetBody
            Destination.ProjectDomains -> Tokens.ProjectDomainsBody
            Destination.Domain -> Tokens.DomainVerificationBody
            Destination.LogEvent -> Tokens.LogEventBody
            Destination.Transfer -> Tokens.SheetMaxHeight
            else -> Tokens.CompactSheetMaxHeight
        }
        val panelHeight = minOf(available, bodyLimit + handleSize + innerBottom)
        val halfHeight = if (maxWidth > maxHeight) panelHeight else maxHeight / 2
        val half = with(density) { halfHeight.toPx() }
        SideEffect {
            position.expandable = !reviewed
            position.fills = project
            position.updateHalf(half)
            position.follow(scope, motion)
        }
        Box(
            Modifier.matchParentSize().graphicsLayer { alpha = position.scrim }.background(scrim)
                .then(if (active) Modifier.clickable(interactionSource = null, indication = null, onClick = close)
                    .semantics { contentDescription = "Close sheet" } else Modifier),
        )
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = bottomGap)
                .widthIn(max = Tokens.ProjectSheetBody).fillMaxWidth().height(panelHeight)
                .onSizeChanged { position.measured(it.height, scope, motion) }
                .graphicsLayer {
                    translationY = position.shift(size.height)
                    alpha = position.reveal
                    val blur = Tokens.Space2.toPx() * (1f - position.reveal)
                    renderEffect = if (blur > .1f) BlurEffect(blur, blur, TileMode.Decal) else null
                }
                .then(if (reviewed) Modifier else Modifier.dashboardRecede(Dp.Hairline, palette.secondary, childDepth))
                .clip(shape).background(paper)
                .then(if (active) Modifier.semantics {
                    paneTitle = destination.label
                    dismiss { close(); true }
                    if (position.expandable) {
                        if (position.tall) collapse { position.expand(scope, false, motion); true }
                        else expand { position.expand(scope, true, motion); true }
                    }
                }.nestedScroll(scroll).draggable(
                    state = drag, orientation = Orientation.Vertical, enabled = !position.closing,
                    onDragStopped = { position.settle(scope, it, speed, motion, latestClose) },
                ) else Modifier.clearAndSetSemantics { hideFromAccessibility() })
                .then(if (position.closing) Modifier.pointerInput(Unit) {
                    awaitEachGesture { awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).consume() }
                } else Modifier),
        ) {
            val shown = with(density) { position.shown().toDp() }
            val measuredHeight = when {
                project && shown > Dp.Hairline -> minOf(shown, panelHeight)
                !reviewed && !project -> minOf(maxOf(shown, halfHeight), panelHeight)
                else -> panelHeight
            }
            Column(
                Modifier.fillMaxWidth().heightIn(max = measuredHeight)
                    .padding(bottom = innerBottom)
                    .onSizeChanged { position.measuredContent(it.height + with(density) { innerBottom.roundToPx() }, scope, motion) },
            ) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally)
                        .padding(top = handleTop, bottom = handleBottom)
                        .size(width = Tokens.SheetHandleWidth, height = Tokens.Space4)
                        .background(handleColor, CircleShape),
                )
                DashboardSheetPage(entry, navigation, holder, paper, close)
            }
        }
    }
}

@Composable
private fun DashboardSheetPage(
    entry: DashboardEntry,
    navigation: DashboardNavigation,
    holder: SaveableStateHolder,
    paper: Color,
    close: () -> Unit,
) {
    AnimatedContent(
        targetState = entry,
        transitionSpec = {
            val from = initialState.route
            val to = targetState.route
            if (from.destination == Destination.Transfer && from.variant == "content" &&
                to.destination == Destination.Transfer && to.variant == "review"
            ) {
                (slideInHorizontally(DashboardMotion.push(back = true)) { -it } togetherWith
                    slideOutHorizontally(DashboardMotion.push(back = true)) { it }).using(
                    SizeTransform { _, _ -> DashboardMotion.push(back = true) },
                )
            } else {
                (fadeIn(DashboardMotion.prototype(DashboardMotion.Tabs)) togetherWith
                    fadeOut(DashboardMotion.prototype(DashboardMotion.Tabs))).using(
                    SizeTransform { _, _ -> DashboardMotion.prototype(DashboardMotion.Tabs) },
                )
            }
        },
        label = "Sheet destination",
    ) { current ->
        val route = current.route
        holder.SaveableStateProvider("overlay-${current.key}") {
            CompositionLocalProvider(LocalDashboardScrollPaper provides paper) {
                Column(Modifier.fillMaxWidth()) {
                    when (route.destination) {
                        Destination.Project, Destination.ProjectDomains, Destination.Domain, Destination.GitHubSync, Destination.Transfer ->
                            ProjectSheetContent(route, navigation, close)
                        Destination.Preview, Destination.Toolbar, Destination.RemoveRecent ->
                            DeploymentSheetContent(route, navigation, close)
                        Destination.LogFilters, Destination.LogEvent, Destination.LogDownload ->
                            LogSheetContent(route, navigation, close)
                        Destination.Help -> HelpSheet(close, docsOnly = route.variant == "docs")
                        else -> AccountSheetContent(route, navigation, close)
                    }
                }
            }
        }
    }
}
