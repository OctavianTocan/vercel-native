package com.octaviantocan.vercelnative.dashboard

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Popup
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

internal val LocalDashboardPalette = staticCompositionLocalOf { DashboardPalette.Dark }

internal enum class Glyph(@DrawableRes val artwork: Int, val size: Dp) {
    Back(R.drawable.dashboard_figma_back, Tokens.Space20),
    BackChevron(R.drawable.dashboard_figma_profile_back, Tokens.Icon),
    Close(R.drawable.dashboard_figma_search_close, Tokens.Icon),
    More(R.drawable.dashboard_icon_more, Tokens.DockIcon),
    Chevron(R.drawable.dashboard_figma_chevron, Tokens.Inset),
    Search(R.drawable.dashboard_figma_search, Tokens.Icon),
    Project(R.drawable.dashboard_badge_vercel, Tokens.Icon),
    Activity(R.drawable.dashboard_icon_activity, Tokens.DockIcon),
    Globe(R.drawable.dashboard_figma_globe, Tokens.DockIcon),
    Branch(R.drawable.dashboard_figma_branch, Tokens.Inset),
    Filter(R.drawable.dashboard_figma_filter, Tokens.FilterGlyph),
    Refresh(R.drawable.dashboard_figma_refresh, Tokens.DockIcon),
    Download(R.drawable.dashboard_figma_download, Tokens.DockIcon),
    Send(R.drawable.dashboard_figma_send, Tokens.Icon),
    Check(R.drawable.dashboard_figma_check, Tokens.DockIcon),
    Warn(R.drawable.dashboard_figma_warning, Tokens.Icon),
    Settings(R.drawable.dashboard_icon_settings, Tokens.DockIcon),
    Help(R.drawable.dashboard_icon_help, Tokens.DockIcon),
    Usage(R.drawable.dashboard_icon_analytics, Tokens.DockIcon),
    Agent(R.drawable.dashboard_figma_assistant, Tokens.Icon),
    Theme(R.drawable.dashboard_icon_theme, Tokens.DockIcon),
    Account(R.drawable.dashboard_icon_account, Tokens.DockIcon),
    Edit(R.drawable.dashboard_figma_edit, Tokens.ProjectOptionsInset),
    Live(R.drawable.dashboard_figma_live, Tokens.DockIcon),
    Lock(R.drawable.dashboard_figma_lock, Tokens.DockIcon),
    Team(R.drawable.dashboard_figma_project_team, Tokens.DockIcon),
    Layers(R.drawable.dashboard_figma_project_layers, Tokens.Inset),
    ProjectDomain(R.drawable.dashboard_figma_project_domain, Tokens.Inset),
    Commit(R.drawable.dashboard_figma_commit, Tokens.Inset),
    Info(R.drawable.dashboard_figma_information, Tokens.Inset),
    Outbound(R.drawable.dashboard_figma_outbound, Tokens.OutboundGlyph),
    MenuPreview(R.drawable.dashboard_figma_menu_preview, Tokens.MenuGlyph),
    MenuDeployment(R.drawable.dashboard_figma_menu_deployment, Tokens.MenuGlyph),
    MenuBranch(R.drawable.dashboard_figma_menu_branch, Tokens.MenuGlyph),
    MenuRemove(R.drawable.dashboard_figma_menu_remove, Tokens.MenuRemoveGlyph),
    Eye(R.drawable.dashboard_figma_deployment_preview, Tokens.DockIcon),
    GitHub(R.drawable.dashboard_github_mark, Tokens.DockIcon),
}

@Composable
internal fun DashboardIcon(glyph: Glyph, tint: Color = LocalDashboardPalette.current.ink, modifier: Modifier = Modifier) {
    Icon(painterResource(glyph.artwork), contentDescription = null, modifier.size(glyph.size), tint = tint)
}

@Composable
internal fun IconControl(
    glyph: Glyph, label: String, onClick: () -> Unit, active: Boolean = false,
    modifier: Modifier = Modifier, enabled: Boolean = true, visualSize: Dp = Tokens.Filter,
) {
    val palette = LocalDashboardPalette.current
    Box(
        modifier.size(visualSize).dashboardGlass()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        DashboardIcon(glyph, if (active) palette.error else if (enabled) palette.ink else palette.disabled)
    }
}

@Composable
internal fun DashboardHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    leftIcon: Glyph = Glyph.Back,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    titleSize: androidx.compose.ui.unit.TextUnit = Tokens.RootTitle,
) {
    Box(modifier.fillMaxWidth().heightIn(min = Tokens.TopBar).padding(horizontal = Tokens.Inset)) {
        if (onBack != null) {
            Box(Modifier.align(Alignment.CenterStart)) {
                val label = if (leftIcon == Glyph.Close) "Close" else "Back"
                IconControl(leftIcon, label, onBack)
            }
        }
        Column(Modifier.align(Alignment.Center).padding(horizontal = Tokens.Touch), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = titleSize, lineHeight = Tokens.ListTitleLine,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine,
                color = LocalDashboardPalette.current.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

@Composable
internal fun DashboardField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    shape: Shape = RoundedCornerShape(Tokens.ControlRadius),
    height: Dp = Tokens.Touch,
    error: Boolean = false,
    color: Color = LocalDashboardPalette.current.input,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val palette = LocalDashboardPalette.current
    BasicTextField(
        value, onValueChange,
        modifier.clip(shape).background(color).border(Tokens.Stroke, if (error) palette.error else palette.quietBorder, shape),
        textStyle = GeistText.copy(color = palette.ink, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine),
        singleLine = true,
        cursorBrush = SolidColor(palette.ink),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { input ->
            Row(
                Modifier.heightIn(min = height).padding(horizontal = Tokens.Space12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
            ) {
                leading?.invoke()
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) Text(
                        placeholder, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine, color = palette.secondary,
                    )
                    input()
                }
                trailing?.invoke()
            }
        },
    )
}

@Composable
internal fun MutedText(text: String, modifier: Modifier = Modifier, monospace: Boolean = false) {
    Text(
        text, modifier, fontSize = Tokens.Metadata, color = LocalDashboardPalette.current.secondary,
        fontFamily = if (monospace) GeistMono else null,
    )
}

@Composable
internal fun SectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        title, modifier.padding(top = Tokens.Space8, bottom = Tokens.Space6),
        fontSize = Tokens.Metadata, fontWeight = FontWeight.Medium, color = LocalDashboardPalette.current.secondary,
    )
}

@Composable
internal fun DashboardRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    glyph: Glyph? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = Tokens.SearchRow,
) {
    val palette = LocalDashboardPalette.current
    Row(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = minHeight)
            .padding(horizontal = Tokens.Inset, vertical = Tokens.Space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
    ) {
        if (leading != null) leading() else if (glyph != null) {
            Box(Modifier.width(Tokens.IconSlot), contentAlignment = Alignment.Center) { DashboardIcon(glyph, palette.muted) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space2)) {
            Text(title, fontSize = Tokens.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) MutedText(subtitle)
        }
        if (trailing != null) trailing()
        else if (onClick != null) DashboardIcon(Glyph.Chevron, palette.muted)
    }
}

@Composable
internal fun DividerLine(modifier: Modifier = Modifier, color: Color = LocalDashboardPalette.current.quietBorder) {
    HorizontalDivider(modifier, thickness = Tokens.Stroke, color = color)
}

@Composable
internal fun StatusPill(label: String, tone: Tone = Tone.Neutral, modifier: Modifier = Modifier) {
    val palette = LocalDashboardPalette.current
    val (ink, surface) = when (tone) {
        Tone.Success -> palette.ready to palette.readySurface
        Tone.Warning -> palette.warning to palette.warningSurface
        Tone.Error -> palette.error to palette.errorSurface
        Tone.Neutral -> palette.secondary to palette.control
    }
    Text(
        label,
        modifier.background(surface, CircleShape).padding(horizontal = Tokens.Space8, vertical = Tokens.Space4),
        fontSize = Tokens.Caption,
        color = ink,
        fontWeight = FontWeight.Medium,
    )
}

internal enum class Tone { Neutral, Success, Warning, Error }

@Composable
internal fun DashboardNotice(
    message: String, modifier: Modifier = Modifier, tone: Tone = Tone.Neutral, retry: (() -> Unit)? = null,
) {
    val palette = LocalDashboardPalette.current
    val (glyph, color) = when (tone) {
        Tone.Success -> Glyph.Check to palette.ready
        Tone.Warning -> Glyph.Warn to palette.warning
        Tone.Error -> Glyph.Warn to palette.error
        Tone.Neutral -> Glyph.Help to palette.link
    }
    Surface(
        modifier.fillMaxWidth(), shape = RoundedCornerShape(Tokens.CardRadius),
        color = palette.popover,
    ) {
        Row(
            Modifier.heightIn(min = Tokens.Touch).padding(start = Tokens.Space12, end = Tokens.Space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
        ) {
            DashboardIcon(glyph, color)
            Text(message, Modifier.weight(1f), fontSize = Tokens.Body)
            if (retry != null) IconControl(Glyph.Refresh, "Retry", retry)
        }
    }
}

@Composable
internal fun DashboardButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val palette = LocalDashboardPalette.current
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = Tokens.Touch),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = palette.ink, contentColor = palette.canvas),
    ) { Text(label, fontSize = Tokens.Body) }
}

@Composable
internal fun GlassAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = Tokens.Filter).dashboardGlass()
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = Tokens.Inset),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = LocalDashboardPalette.current.glassInk, fontSize = Tokens.ListSubtitle,
            lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun ProjectMark(project: Project, modifier: Modifier = Modifier) {
    val palette = LocalDashboardPalette.current
    BoxWithConstraints(
        modifier.size(Tokens.RowIcon).background(palette.projectBadge, CircleShape)
            .border(Tokens.Stroke, palette.insetSurface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val mark = maxWidth - Tokens.Space12
        ProjectArtwork(project, mark, mark - Tokens.Space8)
    }
}

@Composable
internal fun ProjectArtwork(project: Project, size: Dp, fallbackSize: Dp) {
    val palette = LocalDashboardPalette.current
    val mark = when (project.number) {
        0 -> R.drawable.dashboard_badge_catalog
        1 -> R.drawable.dashboard_badge_market
        2 -> R.drawable.dashboard_badge_harbor
        3 -> R.drawable.dashboard_badge_docs
        4 -> R.drawable.dashboard_badge_guide
        else -> R.drawable.dashboard_badge_vercel
    }
    val fallback = mark == R.drawable.dashboard_badge_vercel
    val tint = if (fallback || project.number == 1 || project.number == 3) palette.projectEllipsis else Color.Unspecified
    DashboardArtwork(mark, if (fallback) fallbackSize else size, tint)
}

@Composable
internal fun DashboardEmpty(
    title: String, description: String, glyph: Glyph, modifier: Modifier = Modifier,
    loading: Boolean = false, action: (() -> Unit)? = null, actionLabel: String = "Retry",
    @DrawableRes illustration: Int? = null,
) {
    val palette = LocalDashboardPalette.current
    Column(
        modifier.fillMaxWidth().padding(Tokens.Space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Space8, Alignment.CenterVertically),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(Tokens.Filter), color = palette.ink)
        else if (illustration != null) Image(
            painterResource(illustration), null,
            Modifier.size(Tokens.IllustrationWidth, Tokens.IllustrationHeight),
        )
        else Box(
            Modifier.width(Tokens.IllustrationWidth).height(Tokens.IllustrationHeight),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(Tokens.BottomBar).background(palette.group, RoundedCornerShape(Tokens.CardRadius))
                    .border(Tokens.Stroke, palette.border, RoundedCornerShape(Tokens.CardRadius)),
                contentAlignment = Alignment.Center,
            ) { DashboardIcon(glyph, palette.secondary) }
        }
        Text(title, fontSize = Tokens.Title, lineHeight = Tokens.EmptyTitleLine,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Text(description, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine,
            color = palette.secondary, textAlign = TextAlign.Center)
        if (action != null) DashboardButton(actionLabel, action)
    }
}

@Composable
internal fun DashboardChart(
    values: List<Float>, modifier: Modifier = Modifier, color: Color = LocalDashboardPalette.current.chart,
) {
    val palette = LocalDashboardPalette.current
    Canvas(modifier.fillMaxWidth().height(Tokens.ChartHeight)) {
        for (line in 1..4) {
            val y = size.height * line / 4
            drawLine(palette.quietBorder, Offset(0f, y), Offset(size.width, y))
        }
        val step = size.width / (values.size + 1)
        values.forEachIndexed { index, fraction ->
            val height = size.height * fraction.coerceIn(0f, 1f)
            val x = step * (index + 1)
            drawLine(color, Offset(x, size.height), Offset(x, size.height - height), step * 0.48f, StrokeCap.Round)
        }
    }
}

internal data class MenuAction(
    val label: String, val glyph: Glyph, val onClick: (() -> Unit)?, val project: Project? = null,
)

@Composable
internal fun RowOptions(
    actions: List<MenuAction>,
    label: String,
    expanded: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    authored: String,
    separated: Boolean = false,
    recents: Boolean = false,
) {
    val palette = LocalDashboardPalette.current
    Box {
        if (recents) {
            Box(
                Modifier.size(Tokens.Touch).clip(CircleShape).clickable(role = Role.Button, onClick = onOpen)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(Tokens.RowIcon).background(palette.canvas, CircleShape)
                        .border(Tokens.Stroke, palette.insetSurface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    DashboardArtwork(R.drawable.dashboard_icon_more, Tokens.DockIcon)
                }
            }
        } else IconControl(Glyph.More, label, onOpen, visualSize = Tokens.RowIcon)
        if (expanded) {
            val radius = if (recents) Tokens.ProjectOptionsLightRadius else Tokens.ProjectOptionsRadius
            val shape = RoundedCornerShape(radius)
            val position = with(LocalDensity.current) {
                val edge = (if (recents) Tokens.Space6 else Tokens.Space8 + Tokens.Space2).roundToPx()
                val above = Tokens.DockIcon.roundToPx()
                val below = (if (recents) -Tokens.DockIcon else Tokens.RecentTitleGap).roundToPx()
                val viewport = Tokens.Space8.roundToPx()
                remember(edge, above, below, viewport) { ProjectOptionsPosition(edge, above, below, viewport, 0) }
            }
            Popup(position, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
                Column(
                    Modifier.dismissOutsideRoundedCard(radius, Dp.Hairline, onClose)
                        .width(if (recents) Tokens.RecentsOptionsWidth else Tokens.DeploymentOptionsWidth).clip(shape)
                        .background(palette.projectOptionsSurface).border(Tokens.Stroke, palette.projectOptionsBorder, shape)
                        .dashboardScroll(surface = palette.projectOptionsSurface)
                        .padding(vertical = if (recents) Tokens.DockInset + Tokens.Stroke else Tokens.Space8),
                ) {
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = if (recents) Tokens.RecentsOptionsTimestamp else Tokens.Space32 + Tokens.Space2)
                            .padding(horizontal = Tokens.Inset),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("Authored $authored", fontSize = if (recents) Tokens.ListSubtitle else Tokens.SearchDetailSize,
                            lineHeight = Tokens.SearchDetailLine, color = palette.secondary)
                    }
                    actions.forEachIndexed { index, item ->
                        if (separated && index > 0) DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = Tokens.MenuRow).clickable(enabled = item.onClick != null,
                                role = Role.Button, onClick = { onClose(); item.onClick?.invoke() })
                                .padding(horizontal = Tokens.ProjectOptionsInset),
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.width(Tokens.Space20), contentAlignment = Alignment.Center) {
                                if (item.project == null) DashboardIcon(item.glyph, palette.projectOptionsGlyph)
                                else ProjectArtwork(item.project, Tokens.MetadataIcon, Tokens.MetadataIcon)
                            }
                            Text(item.label, color = palette.projectOptionsInk,
                                fontSize = if (recents) Tokens.ListTitle else Tokens.Body,
                                lineHeight = if (recents) Tokens.ListSubtitleLine else Tokens.ProjectOptionsLine,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Modifier.dismissOutsideRoundedCard(radius: Dp, padding: Dp, onDismiss: () -> Unit): Modifier {
    val dismiss by rememberUpdatedState(onDismiss)
    return pointerInput(radius, padding) {
        val margin = padding.roundToPx().toFloat()
        val corner = CornerRadius(radius.toPx())
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val bounds = RoundRect(Rect(margin, margin, size.width - margin, size.height - margin), corner)
            if (!bounds.contains(down.position)) {
                down.consume()
                dismiss()
            }
        }
    }
}
