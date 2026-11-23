package com.octaviantocan.vercelnative.dashboard

import android.content.ClipData
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlinx.coroutines.launch

internal val LocalDashboardPalette = staticCompositionLocalOf { DashboardPalette.Dark }

internal enum class Glyph(@DrawableRes val artwork: Int, val size: Dp) {
    Back(R.drawable.dashboard_figma_back, Tokens.Space20),
    BackChevron(R.drawable.dashboard_figma_profile_back, Tokens.Icon),
    AccountBack(R.drawable.dashboard_figma_account_back, Tokens.Space20),
    LightThemeBack(R.drawable.dashboard_figma_theme_back_light, Tokens.Space20),
    Close(R.drawable.dashboard_figma_search_close, Tokens.Icon),
    AgentClose(R.drawable.dashboard_figma_agent_close, Tokens.Space20),
    More(R.drawable.dashboard_icon_more, Tokens.DockIcon),
    Chevron(R.drawable.dashboard_figma_chevron, Tokens.Inset),
    Search(R.drawable.dashboard_figma_search, Tokens.Icon),
    Project(R.drawable.dashboard_badge_vercel, Tokens.Icon),
    Activity(R.drawable.dashboard_icon_activity, Tokens.DockIcon),
    Globe(R.drawable.dashboard_figma_globe, Tokens.DockIcon),
    Toolbar(R.drawable.dashboard_icon_toolbar, Tokens.DockIcon),
    Branch(R.drawable.dashboard_figma_branch, Tokens.Inset),
    Filter(R.drawable.dashboard_figma_filter, Tokens.FilterGlyph),
    Refresh(R.drawable.dashboard_figma_refresh, Tokens.DockIcon),
    RetryNotice(R.drawable.dashboard_figma_retry_notice, Tokens.Icon),
    Download(R.drawable.dashboard_figma_download, Tokens.DockIcon),
    Send(R.drawable.dashboard_figma_send, Tokens.Icon),
    Check(R.drawable.dashboard_figma_check, Tokens.DockIcon),
    Warn(R.drawable.dashboard_figma_warning, Tokens.Icon),
    Settings(R.drawable.dashboard_icon_settings, Tokens.DockIcon),
    ProjectSettings(R.drawable.dashboard_figma_project_settings, Tokens.Space20),
    Help(R.drawable.dashboard_icon_help, Tokens.DockIcon),
    Usage(R.drawable.dashboard_icon_analytics, Tokens.DockIcon),
    Agent(R.drawable.dashboard_figma_assistant, Tokens.Icon),
    Theme(R.drawable.dashboard_icon_theme, Tokens.DockIcon),
    ThemeSystem(R.drawable.dashboard_figma_theme_system, Tokens.DockIcon),
    ThemeSun(R.drawable.dashboard_figma_theme_sun, Tokens.Space20),
    ThemeMoon(R.drawable.dashboard_figma_theme_moon, Tokens.DockIcon),
    AgentAttach(R.drawable.dashboard_figma_agent_attachment, Tokens.DockIcon),
    AgentSend(R.drawable.dashboard_figma_agent_send, Tokens.Space20),
    Account(R.drawable.dashboard_icon_account, Tokens.DockIcon),
    Edit(R.drawable.dashboard_figma_edit, Tokens.ProjectOptionsInset),
    Live(R.drawable.dashboard_figma_live, Tokens.DockIcon),
    PauseLive(R.drawable.dashboard_figma_log_pause, Tokens.DockIcon),
    Copy(R.drawable.dashboard_figma_copy, Tokens.Inset),
    Function(R.drawable.dashboard_figma_log_function, Tokens.BodyInset),
    Clock(R.drawable.dashboard_figma_log_clock, Tokens.DockIcon),
    TransferSuccess(R.drawable.dashboard_figma_transfer_success, Tokens.TransferSuccessGlyph),
    TransferFailure(R.drawable.dashboard_figma_transfer_failure, Tokens.Icon),
    Lock(R.drawable.dashboard_figma_lock, Tokens.DockIcon),
    Team(R.drawable.dashboard_figma_project_team, Tokens.DockIcon),
    Layers(R.drawable.dashboard_figma_project_layers, Tokens.Inset),
    ProjectDomain(R.drawable.dashboard_figma_project_domain, Tokens.Inset),
    Commit(R.drawable.dashboard_figma_commit, Tokens.Inset),
    Info(R.drawable.dashboard_figma_information, Tokens.Inset),
    SearchNoticeInfo(R.drawable.dashboard_figma_search_notice_info, Tokens.Space20),
    PreviewInfo(R.drawable.dashboard_figma_preview_info, Tokens.Space20),
    PreviewDeployment(R.drawable.dashboard_figma_deployment_cube, Tokens.DockIcon),
    ToolbarDocs(R.drawable.dashboard_figma_toolbar_docs, Tokens.Inset),
    Outbound(R.drawable.dashboard_figma_outbound, Tokens.OutboundGlyph),
    BrowserAction(R.drawable.dashboard_figma_browser_action, Tokens.DockIcon),
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
    glass: Boolean = true,
) {
    val palette = LocalDashboardPalette.current
    Box(
        modifier.size(visualSize).then(if (glass) Modifier.dashboardGlass() else Modifier)
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
    onSubtitleClick: (() -> Unit)? = null,
    titleWeight: FontWeight = FontWeight.SemiBold,
    titleLine: androidx.compose.ui.unit.TextUnit = Tokens.ListTitleLine,
    titleGlyph: Glyph? = null,
    titleInk: Color = LocalDashboardPalette.current.ink,
    titleGlyphTint: Color = titleInk,
    backGlass: Boolean = true,
) {
    Box(modifier.fillMaxWidth().heightIn(min = Tokens.TopBar).padding(horizontal = Tokens.Inset)) {
        if (onBack != null) {
            Box(Modifier.align(Alignment.CenterStart)) {
                val label = if (leftIcon == Glyph.Close || leftIcon == Glyph.AgentClose) "Close" else "Back"
                val face = if (backGlass) Modifier else Modifier.background(LocalDashboardPalette.current.sheet, CircleShape)
                    .border(Tokens.Stroke, LocalDashboardPalette.current.border, CircleShape)
                IconControl(leftIcon, label, onBack, modifier = face, glass = backGlass)
            }
        }
        Column(Modifier.align(Alignment.Center).padding(horizontal = Tokens.Touch), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space8), verticalAlignment = Alignment.CenterVertically) {
                if (titleGlyph != null) DashboardIcon(titleGlyph, titleGlyphTint)
                Text(title, fontSize = titleSize, lineHeight = titleLine, color = titleInk,
                    fontWeight = titleWeight, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (subtitle != null) Text(
                subtitle,
                modifier = if (onSubtitleClick == null) Modifier else Modifier.clickable(
                    role = Role.Button, onClickLabel = "Open $subtitle", onClick = onSubtitleClick,
                ),
                fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine,
                color = LocalDashboardPalette.current.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
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
    focused: Boolean = false,
    color: Color = LocalDashboardPalette.current.input,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    trailingInset: Dp = Tokens.Space12,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    outline: Color = Color.Unspecified,
) {
    val palette = LocalDashboardPalette.current
    BasicTextField(
        value, onValueChange,
        modifier.clip(shape).background(color).border(Tokens.Stroke,
            when {
                outline != Color.Unspecified -> outline
                error -> palette.error
                focused -> palette.muted
                else -> palette.quietBorder
            }, shape),
        textStyle = GeistText.copy(color = palette.ink, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine),
        singleLine = true,
        cursorBrush = SolidColor(palette.ink),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { input ->
            Row(
                Modifier.heightIn(min = height).padding(start = Tokens.Space12, end = trailingInset),
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
internal fun DashboardGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.CardRadius))
            .background(LocalDashboardPalette.current.projectSheetGroup), content = content,
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
    titleMaxLines: Int = 2,
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
            Text(title, fontSize = Tokens.Body, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis)
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

@Composable
internal fun DottedStatusPill(label: String, ink: Color, surface: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.heightIn(min = Tokens.AlertPillHeight).background(surface, CircleShape)
            .padding(horizontal = Tokens.PreviewStatusInset),
        horizontalArrangement = Arrangement.spacedBy(Tokens.DockInset), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(Tokens.Space6).background(ink, CircleShape))
        Text(label, fontSize = Tokens.Caption, lineHeight = Tokens.AlertPillLine, fontWeight = FontWeight.Medium, color = ink)
    }
}

internal enum class Tone { Neutral, Success, Warning, Error }

@Composable
internal fun DashboardGlassNotice(
    title: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier,
    detail: String? = null, error: Boolean = false, backdrop: androidx.compose.ui.graphics.layer.GraphicsLayer? = null,
    retryLabel: String? = null,
) {
    val palette = LocalDashboardPalette.current
    val accent = if (error) palette.error else palette.link
    val searchNotice = backdrop != null && !error
    Box(
        modifier.fillMaxWidth().dashboardGlass(backdrop, tint = if (searchNotice) Color.White.copy(alpha = .42f) else null)
            .then(if (error) Modifier.background(palette.errorSurface.copy(alpha = .8f), CircleShape)
                .border(Tokens.Stroke, accent.copy(alpha = .35f), CircleShape) else Modifier)
    ) {
        if (searchNotice) Box(Modifier.matchParentSize().padding(Tokens.Stroke).background(palette.sheet, CircleShape))
        Row(
            Modifier.fillMaxWidth().heightIn(min = Tokens.TopBar)
                .padding(start = if (searchNotice) Tokens.FilterGlyph else Tokens.Inset,
                    end = if (searchNotice) Tokens.Space6 else Tokens.Space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (searchNotice) Tokens.AlertMeasurePad else Tokens.Space12),
        ) {
            val glyph = when {
                searchNotice -> Glyph.SearchNoticeInfo
                error -> Glyph.PreviewInfo
                else -> Glyph.Info
            }
            DashboardIcon(glyph, accent)
            Column(Modifier.weight(1f)) {
                Text(title, color = if (error) accent else palette.ink, fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine)
                if (detail != null) Text(detail, color = palette.secondary, fontSize = Tokens.Metadata,
                    lineHeight = Tokens.MetadataLine, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (onRetry != null) {
                if (retryLabel == null) IconControl(Glyph.RetryNotice, "Retry", onRetry)
                else TextButton(onClick = onRetry) { Text(retryLabel, color = palette.ink, fontSize = Tokens.ListSubtitle) }
            }
        }
    }
}

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
internal fun GlassAction(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, destructive: Boolean = false,
    glyph: Glyph? = null, labelWeight: FontWeight = FontWeight.SemiBold,
) {
    val palette = LocalDashboardPalette.current
    Box(
        modifier.heightIn(min = Tokens.Filter).dashboardGlass()
            .then(if (destructive) Modifier.background(palette.errorSurface, CircleShape)
                .border(Tokens.Stroke, palette.error.copy(alpha = .35f), CircleShape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = Tokens.Inset),
        contentAlignment = Alignment.Center,
    ) {
        val ink = if (destructive) palette.error else palette.glassInk
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space8), verticalAlignment = Alignment.CenterVertically) {
            if (glyph != null) DashboardIcon(glyph, ink)
            Text(label, color = ink, fontSize = Tokens.ListSubtitle,
                lineHeight = Tokens.ListSubtitleLine, fontWeight = labelWeight, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun rememberCopyText(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return { value -> scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, value))) } }
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
    @DrawableRes illustration: Int? = null, glassAction: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
    prominentTitle: Boolean = false,
    actionWidth: Dp = Tokens.AlertPlanButton,
    descriptionGap: Dp = if (prominentTitle) Tokens.FeedbackExplanationGap else Tokens.Space8,
) {
    val palette = LocalDashboardPalette.current
    Column(
        modifier.fillMaxWidth().padding(Tokens.Space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(Tokens.Filter), color = palette.ink)
        else if (illustration != null) Image(
            painterResource(illustration), null,
            Modifier.size(Tokens.IllustrationWidth, Tokens.IllustrationHeight),
        )
        else Box(
            Modifier.width(Tokens.IllustrationWidth)
                .height(if (prominentTitle) Tokens.FeedbackIconSize else Tokens.IllustrationHeight),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(if (prominentTitle) Tokens.FeedbackIconSize else Tokens.BottomBar)
                    .background(palette.group, RoundedCornerShape(Tokens.CardRadius))
                    .border(Tokens.Stroke, palette.border, RoundedCornerShape(Tokens.CardRadius)),
                contentAlignment = Alignment.Center,
            ) { DashboardIcon(glyph, palette.secondary) }
        }
        Text(title, Modifier.padding(top = if (prominentTitle) Tokens.FeedbackTitleGap else Tokens.Space8),
            fontSize = if (prominentTitle) Tokens.Heading else Tokens.Title,
            lineHeight = if (prominentTitle) Tokens.PlanTitleLine else Tokens.EmptyTitleLine,
            fontWeight = if (prominentTitle) FontWeight.SemiBold else FontWeight.Medium, textAlign = TextAlign.Center)
        Text(description, Modifier.padding(top = descriptionGap)
            .widthIn(max = Tokens.FeedbackExplanationWidth),
            fontSize = Tokens.ListSubtitle, lineHeight = if (prominentTitle) Tokens.ProjectOptionsLine else Tokens.ListSubtitleLine,
            color = palette.secondary, textAlign = TextAlign.Center)
        if (action != null) {
            val actionModifier = Modifier.padding(top = if (prominentTitle) Tokens.FeedbackActionGap else Tokens.Space8)
            if (glassAction) GlassAction(actionLabel, action, actionModifier.widthIn(min = actionWidth))
            else DashboardButton(actionLabel, action, actionModifier)
        }
        if (footer != null) Box(Modifier.padding(top = Tokens.Space8)) { footer() }
    }
}

@Composable
internal fun DashboardChart(
    values: List<Float>, ticks: List<String>, dates: List<String>, modifier: Modifier = Modifier,
    color: Color = LocalDashboardPalette.current.chart, line: Boolean = false, height: Dp = Tokens.ChartHeight,
    bars: Dp = Tokens.FirewallBarWidth, observability: Boolean = false,
) {
    val palette = LocalDashboardPalette.current
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(height)) {
            val tickWidth = if (observability) Tokens.ObservabilityTickWidth else Tokens.ChartTickWidth
            Column(Modifier.width(tickWidth).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                for (tick in ticks) ChartTick(tick, observability)
            }
            Canvas(Modifier.weight(1f).fillMaxHeight().padding(
                start = if (observability) Dp.Hairline else Tokens.DockInset,
                end = if (observability) Tokens.Stroke else Tokens.Space2,
            )) {
                val top = (if (observability) Tokens.Space6 + Tokens.Stroke else Tokens.Space8).toPx()
                val bottom = size.height - top
                val plotHeight = bottom - top
                for (tick in ticks.indices) {
                    val y = top + plotHeight * tick / ticks.lastIndex
                    drawLine(if (observability) palette.quietBorder else palette.plotGrid,
                        Offset(0f, y), Offset(size.width, y), Tokens.Stroke.toPx())
                }
                if (line) {
                    val path = androidx.compose.ui.graphics.Path()
                    values.forEachIndexed { index, fraction ->
                        val x = index * size.width / values.lastIndex
                        val y = bottom - plotHeight * fraction.coerceIn(0f, 1f)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(Tokens.Space2.toPx()))
                } else {
                    val step = (size.width - Tokens.Space8.toPx()).coerceAtLeast(0f) / values.size
                    val width = bars.toPx().coerceAtMost(step * .65f)
                    values.forEachIndexed { index, fraction ->
                        val barHeight = plotHeight * fraction.coerceIn(0f, 1f)
                        val x = step * (index + .45f) - width / 2
                        drawRoundRect(color, Offset(x, bottom - barHeight), androidx.compose.ui.geometry.Size(width, barHeight),
                            androidx.compose.ui.geometry.CornerRadius(Tokens.Space2.toPx()))
                    }
                }
            }
        }
        ChartDates(dates, observability)
    }
}

@Composable
internal fun ChartTick(label: String, observability: Boolean = false) {
    val palette = LocalDashboardPalette.current
    Text(label, Modifier.fillMaxWidth(), color = if (observability) palette.secondary else palette.plotInk,
        fontSize = Tokens.Caption, lineHeight = if (observability) Tokens.UsageDateLine else Tokens.ChartTickLine,
        fontFamily = GeistMono, textAlign = if (observability) TextAlign.Start else TextAlign.End)
}

@Composable
internal fun ChartDates(dates: List<String>, observability: Boolean = false) {
    val palette = LocalDashboardPalette.current
    val width = if (observability) Tokens.ObservabilityDateWidth else Tokens.ChartDateWidth
    val gap = if (observability) Tokens.PreviewStatusInset else Tokens.ChartDateGap
    BoxWithConstraints(Modifier.fillMaxWidth().offset(x = if (observability) Tokens.BodyInset else Tokens.Inset).padding(top = gap)) {
        val column = width.coerceAtMost(maxWidth / dates.size)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (date in dates) Text(date, Modifier.width(column), color = if (observability) palette.secondary else palette.plotInk,
                fontSize = Tokens.ChartCaption, lineHeight = if (observability) Tokens.UsageDateLine else Tokens.ChartDateLine,
                fontFamily = GeistMono, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

internal data class MenuAction(
    val label: String, val glyph: Glyph, val onClick: (() -> Unit)?, val project: Project? = null,
    val destructive: Boolean = false,
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
    actionGroupStart: Int? = null,
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
        } else {
            Box(
                Modifier.size(Tokens.Touch).clip(CircleShape).clickable(role = Role.Button, onClick = onOpen)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(Tokens.RowIcon).dashboardGlass(), contentAlignment = Alignment.Center) {
                    DashboardIcon(Glyph.More, palette.projectEllipsis)
                }
            }
        }
        val radius = if (recents) Tokens.ProjectOptionsLightRadius else Tokens.ProjectOptionsRadius
        val shape = RoundedCornerShape(radius)
        val position = with(LocalDensity.current) {
            val edge = (if (recents) Tokens.Space6 else Tokens.Space8 + Tokens.Space2).roundToPx()
            val above = Tokens.DockIcon.roundToPx()
            val below = (if (recents) -Tokens.DockIcon else Tokens.RecentTitleGap).roundToPx()
            val viewport = Tokens.Space8.roundToPx()
            remember(edge, above, below, viewport) { ProjectOptionsPosition(edge, above, below, viewport, 0) }
        }
        DashboardMenu(expanded, position, onClose) {
            Column(
                Modifier.dismissOutsideRoundedCard(radius, Dp.Hairline, onClose)
                    .width(if (recents) Tokens.RecentsOptionsWidth else Tokens.DeploymentOptionsWidth).clip(shape)
                    .background(palette.projectOptionsSurface).border(Tokens.Stroke, palette.projectOptionsBorder, shape)
                    .dashboardScroll(surface = palette.projectOptionsSurface)
                    .padding(vertical = when {
                        separated -> Tokens.Stroke
                        recents -> Tokens.DockInset + Tokens.Stroke
                        else -> Tokens.Space8
                    }),
            ) {
                val timestampHeight = when {
                    separated -> Tokens.EarlierRecentsTimestamp
                    recents -> Tokens.RecentsOptionsTimestamp
                    else -> Tokens.Space32 + Tokens.Space2
                }
                Box(
                    Modifier.fillMaxWidth().heightIn(min = timestampHeight).padding(horizontal = Tokens.Inset),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val timestamp = if (recents) "Authored $authored" else "Deployed $authored"
                    Text(timestamp, fontSize = if (recents) Tokens.ListSubtitle else Tokens.SearchDetailSize,
                        lineHeight = Tokens.SearchDetailLine, color = palette.secondary)
                }
                actions.forEachIndexed { index, item ->
                    if (separated || index == actionGroupStart) {
                        DividerLine(Modifier.padding(horizontal = if (separated && index == 0) Dp.Hairline else Tokens.Inset))
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = if (separated) Tokens.EarlierRecentsMenuRow else Tokens.MenuRow)
                            .clickable(enabled = item.onClick != null,
                            role = Role.Button, onClick = { onClose(); item.onClick?.invoke() })
                            .padding(horizontal = if (separated) Tokens.BodyInset else Tokens.ProjectOptionsInset),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(Tokens.Space20), contentAlignment = Alignment.Center) {
                            if (item.project == null) {
                                DashboardIcon(item.glyph, if (item.destructive) palette.error else palette.projectOptionsGlyph)
                            } else {
                                Box(Modifier.size(Tokens.Space20).background(palette.projectBadge, CircleShape),
                                    contentAlignment = Alignment.Center) {
                                    ProjectArtwork(item.project, Tokens.MetadataIcon, Tokens.MetadataIcon)
                                }
                            }
                        }
                        Text(item.label, color = if (item.destructive) palette.error else palette.projectOptionsInk,
                            fontSize = if (recents) Tokens.ListTitle else Tokens.Body,
                            lineHeight = if (recents) Tokens.ListSubtitleLine else Tokens.ProjectOptionsLine,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
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
