package com.octaviantocan.vercelnative.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

internal val GeistText = TextStyle(
    fontFamily = FontFamily(
        Font(R.font.geist, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.geist, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.geist, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    ),
    fontWeight = FontWeight.Normal,
    letterSpacing = Tokens.StandardTracking,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

internal val GeistMono = FontFamily(Font(R.font.geist_mono))

internal val DashboardTypography = Typography(
    displayLarge = GeistText.copy(fontSize = Tokens.Large, fontWeight = FontWeight.SemiBold),
    displayMedium = GeistText.copy(fontSize = Tokens.Metric, fontWeight = FontWeight.SemiBold),
    displaySmall = GeistText.copy(fontSize = Tokens.Heading, fontWeight = FontWeight.SemiBold),
    headlineLarge = GeistText.copy(fontSize = Tokens.Large, fontWeight = FontWeight.SemiBold),
    headlineMedium = GeistText.copy(fontSize = Tokens.Metric, fontWeight = FontWeight.SemiBold),
    headlineSmall = GeistText.copy(fontSize = Tokens.Heading, fontWeight = FontWeight.SemiBold),
    titleLarge = GeistText.copy(fontSize = Tokens.RootTitle, lineHeight = Tokens.ListTitleLine, fontWeight = FontWeight.SemiBold),
    titleMedium = GeistText.copy(fontSize = Tokens.ListTitle, lineHeight = Tokens.ListTitleLine, fontWeight = FontWeight.Medium),
    titleSmall = GeistText.copy(fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium),
    bodyLarge = GeistText.copy(fontSize = Tokens.Body),
    bodyMedium = GeistText.copy(fontSize = Tokens.ListSubtitle),
    bodySmall = GeistText.copy(fontSize = Tokens.Metadata),
    labelLarge = GeistText.copy(fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium),
    labelMedium = GeistText.copy(fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine, fontWeight = FontWeight.Medium),
    labelSmall = GeistText.copy(fontSize = Tokens.Caption, fontWeight = FontWeight.Medium),
)

@Composable
internal fun DashboardArtwork(
    @DrawableRes resource: Int,
    size: Dp,
    tint: Color = LocalDashboardPalette.current.ink,
) {
    Icon(painterResource(resource), contentDescription = null, Modifier.size(size), tint = tint)
}

@Composable
internal fun DashboardRootHeader(title: String, onMenu: () -> Unit) {
    val palette = LocalDashboardPalette.current
    Row(
        Modifier.fillMaxWidth().padding(start = Tokens.Inset, top = Tokens.Space8, end = Tokens.Inset)
            .heightIn(min = Tokens.TopBar),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(Tokens.Touch).dashboardGlass().clickable(role = Role.Button, onClick = onMenu)
                .semantics { contentDescription = "Open workspace menu" },
            contentAlignment = Alignment.Center,
        ) {
            DashboardArtwork(R.drawable.dashboard_icon_sidebar, Tokens.Space24, palette.glassInk)
        }
        Text(
            title, Modifier.weight(1f), color = palette.glassInk, style = GeistText,
            fontSize = Tokens.RootTitle, lineHeight = Tokens.ListTitleLine, fontWeight = FontWeight.SemiBold,
            letterSpacing = Tokens.RootTitleTracking,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DashboardBottomBar(
    activity: Boolean,
    reviewedLabels: Boolean,
    navigation: DashboardNavigation,
    backdrop: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    val palette = LocalDashboardPalette.current
    var named by remember { mutableStateOf<String?>(null) }
    var tapCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(tapCount) {
        if (named != null) {
            delay(1500)
            named = null
        }
    }
    fun showLabel(title: String) {
        named = title
        tapCount++
    }
    Row(
        modifier.fillMaxWidth().padding(start = Tokens.Space20, end = Tokens.Space20, bottom = Tokens.DockBottomGap)
            .heightIn(min = Tokens.BottomBar),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            Modifier.weight(1f).dashboardGlass(backdrop).padding(Tokens.DockInset)
                .height(Tokens.BottomBar - Tokens.DockInset * 2).selectableGroup(),
        ) {
            val itemWidth = maxWidth / 3
            val selection by animateDpAsState(
                if (activity) itemWidth else itemWidth * 0,
                animationSpec = DashboardMotion.move(DashboardMotion.Medium),
                label = "Dock selection",
            )
            Box(Modifier.offset { androidx.compose.ui.unit.IntOffset(selection.roundToPx(), 0) }
                .fillMaxHeight().width(itemWidth).background(palette.glassSelection, CircleShape))
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                DashboardNavItem("Home", !activity, reviewedLabels || named == "Home", Modifier.weight(1f),
                    icon = { DashboardArtwork(R.drawable.dashboard_icon_projects, Tokens.DockIcon, palette.glassInk) }) {
                    showLabel("Home")
                    navigation.main(Destination.Projects)
                }
                ActivityDockItem(activity, reviewedLabels, reviewedLabels || named == "Activity", navigation, Modifier.weight(1f)) {
                    showLabel("Activity")
                }
                DashboardNavItem("More", false, reviewedLabels || named == "More", Modifier.weight(1f),
                    icon = { DashboardArtwork(R.drawable.dashboard_icon_more, Tokens.DockIcon, palette.glassInk) }) {
                    showLabel("More")
                    navigation.show(Destination.More)
                }
            }
        }
        Box(
            Modifier.size(Tokens.BottomBar).dashboardGlass(backdrop)
                .clickable(role = Role.Button) { navigation.show(Destination.Search) }
                .semantics { contentDescription = "Search Vercel" },
            contentAlignment = Alignment.Center,
        ) {
            DashboardArtwork(R.drawable.dashboard_icon_search, Tokens.Icon, palette.glassInk)
        }
    }
}

@Composable
private fun DashboardNavItem(
    title: String,
    selected: Boolean,
    named: Boolean,
    modifier: Modifier,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    val labelReveal by animateFloatAsState(if (named) 1f else 0f, DashboardMotion.move(DashboardMotion.Fast), label = "Dock label")
    Column(
        modifier.fillMaxHeight().heightIn(min = Tokens.BottomBar - Tokens.DockInset * 2)
            .clip(CircleShape)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = title },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Text(
            title,
            Modifier.clearAndSetSemantics {}.clipToBounds().layout { measurable, constraints ->
                val text = measurable.measure(constraints.copy(minHeight = 0))
                val gap = (Tokens.DockLabelGap.toPx() * labelReveal).roundToInt()
                layout(text.width, (text.height * labelReveal).roundToInt() + gap) {
                    text.placeWithLayer(0, gap) { alpha = labelReveal }
                }
            },
            color = palette.glassInk, style = GeistText, fontWeight = FontWeight.Medium,
            fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine,
            letterSpacing = Tokens.DockLabelTracking,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActivityDockItem(
    selected: Boolean,
    reviewed: Boolean,
    named: Boolean,
    navigation: DashboardNavigation,
    modifier: Modifier,
    onTap: () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    val tabs = ActivityTab.entries
    val tab = tabs.first { it.destination == navigation.lastActivity }
    val switching = selected && !reviewed
    val density = LocalDensity.current.density
    var lean by remember { mutableFloatStateOf(0f) }
    var turned by remember { mutableStateOf(false) }
    var forward by remember { mutableStateOf(true) }
    val direction = when {
        lean < -36f * density -> 1
        lean > 36f * density -> -1
        else -> 0
    }
    val step: (Int) -> Unit = { way ->
        val index = tabs.indexOfFirst { it.destination == navigation.lastActivity }
        forward = way > 0
        navigation.main(tabs[(index + way).mod(tabs.size)].destination)
    }
    LaunchedEffect(switching, direction) {
        if (!switching) lean = 0f
        if (!switching || direction == 0) return@LaunchedEffect
        delay(260)
        while (true) {
            turned = true
            step(direction)
            val amount = (abs(lean) / (130f * density)).coerceIn(0f, 1f)
            delay((720 - 340 * amount).toLong())
        }
    }
    val gestures = if (switching) Modifier.dashboardHorizontalDrag(
        onStart = { turned = false; lean = 0f },
        onDelta = { lean += it },
        onStop = { speed, completed ->
            if (completed && !turned) {
                val flick = if (abs(speed) > 650f * density) if (speed < 0f) 1 else -1 else 0
                val way = if (direction != 0) direction else flick
                if (way != 0) step(way)
            }
            lean = 0f
        },
    ).semantics {
        stateDescription = tab.name
        customActions = listOf(
            CustomAccessibilityAction("Previous Activity tab") { step(-1); true },
            CustomAccessibilityAction("Next Activity tab") { step(1); true },
        )
    } else Modifier
    DashboardNavItem("Activity", selected, named, modifier.then(gestures), icon = {
        Box(
            Modifier.fillMaxWidth().height(Tokens.DockIcon).then(if (switching) Modifier.drawBehind {
                val reach = Tokens.Space8.toPx() * .44f
                val stroke = Tokens.Stroke.toPx() * 1.5f
                val center = Offset(size.width / 2f, size.height / 2f)
                val inset = size.height / 2f + Tokens.Space8.toPx() + reach
                for (side in listOf(-1, 1)) {
                    val tip = center.copy(x = center.x + side * inset)
                    val inner = tip.x - side * reach
                    drawLine(palette.secondary, Offset(inner, tip.y - reach), tip, stroke, StrokeCap.Round)
                    drawLine(palette.secondary, tip, Offset(inner, tip.y + reach), stroke, StrokeCap.Round)
                }
            } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = tab.takeIf { switching },
                transitionSpec = {
                    val sign = if (forward) 1 else -1
                    val spec = DashboardMotion.move<androidx.compose.ui.unit.IntOffset>(DashboardMotion.Fast)
                    (slideInHorizontally(spec) { sign * it / 2 } + fadeIn(DashboardMotion.move(DashboardMotion.Fast))) togetherWith
                        (slideOutHorizontally(spec) { -sign * it / 2 } + fadeOut(DashboardMotion.move(DashboardMotion.Quick)))
                },
                label = "Activity category icon",
            ) { shown ->
                val icon = when (shown) {
                    ActivityTab.Recents -> R.drawable.dashboard_figma_log_clock
                    ActivityTab.Usage -> R.drawable.dashboard_icon_analytics
                    else -> R.drawable.dashboard_icon_activity
                }
                DashboardArtwork(icon, Tokens.DockIcon, palette.glassInk)
            }
        }
    }) {
        onTap()
        navigation.main(navigation.lastActivity)
    }
}

internal fun Modifier.captureDashboardBackdrop(
    content: GraphicsLayer, blurred: GraphicsLayer, blur: Dp, showBlurred: Boolean,
): Modifier =
    drawWithCache {
        val radius = blur.toPx()
        blurred.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
        onDrawWithContent {
            content.record { this@onDrawWithContent.drawContent() }
            blurred.record { drawLayer(content) }
            drawLayer(if (showBlurred) blurred else content)
        }
    }

@Composable
internal fun Modifier.dashboardBackdrop(blurred: GraphicsLayer): Modifier {
    var origin by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { origin = it.positionInRoot() }.drawWithContent {
        clipRect { translate(left = -origin.x, top = -origin.y) { drawLayer(blurred) } }
        drawContent()
    }
}
