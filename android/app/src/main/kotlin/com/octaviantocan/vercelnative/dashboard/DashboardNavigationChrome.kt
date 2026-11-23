package com.octaviantocan.vercelnative.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

internal val GeistText = TextStyle(
    fontFamily = FontFamily(
        Font(R.font.geist, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.geist, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.geist, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    ),
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
    iconsOnly: Boolean,
    navigation: DashboardNavigation,
    backdrop: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    val palette = LocalDashboardPalette.current
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
                DashboardNavItem("Home", R.drawable.dashboard_icon_projects, !activity, iconsOnly, Modifier.weight(1f)) {
                    navigation.main(Destination.Projects)
                }
                DashboardNavItem("Activity", R.drawable.dashboard_icon_activity, activity, iconsOnly, Modifier.weight(1f)) {
                    navigation.main(navigation.lastActivity)
                }
                DashboardNavItem("More", R.drawable.dashboard_icon_more, false, iconsOnly, Modifier.weight(1f)) {
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
    title: String, @DrawableRes icon: Int, selected: Boolean, iconsOnly: Boolean,
    modifier: Modifier, onClick: () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    Column(
        modifier.fillMaxHeight().heightIn(min = Tokens.BottomBar - Tokens.DockInset * 2)
            .clip(CircleShape)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .then(if (iconsOnly) Modifier.semantics { contentDescription = title } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.DockLabelGap, Alignment.CenterVertically),
    ) {
        DashboardArtwork(icon, Tokens.DockIcon, palette.glassInk)
        if (!iconsOnly) Text(
            title, color = palette.glassInk, style = GeistText, fontWeight = FontWeight.Medium,
            fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine,
            letterSpacing = Tokens.DockLabelTracking,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
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
