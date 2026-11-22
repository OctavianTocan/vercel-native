package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

internal val LocalDashboardPalette = staticCompositionLocalOf { DashboardPalette.Dark }

internal enum class Glyph { Back, Close, Menu, More, Chevron, Search, Project, Activity, Globe, Branch, Filter, Refresh, Download, Send, Check, Warn, Settings, Help, Usage, Agent, Theme, Account }

@Composable
internal fun DashboardIcon(glyph: Glyph, tint: Color = LocalDashboardPalette.current.ink, modifier: Modifier = Modifier) {
    Canvas(modifier.size(Tokens.Icon)) {
        val unit = size.minDimension / 24f
        fun point(x: Int, y: Int) = Offset(x * unit, y * unit)
        fun line(x: Int, y: Int, toX: Int, toY: Int) =
            drawLine(tint, point(x, y), point(toX, toY), unit * 1.8f, StrokeCap.Round)
        fun circle(x: Int, y: Int, radius: Int, filled: Boolean = false) {
            if (filled) drawCircle(tint, unit * radius, point(x, y))
            else drawCircle(tint, unit * radius, point(x, y), style = Stroke(unit * 1.6f))
        }
        when (glyph) {
            Glyph.Back -> { line(5, 12, 20, 12); line(5, 12, 11, 6); line(5, 12, 11, 18) }
            Glyph.Close -> { line(6, 6, 18, 18); line(18, 6, 6, 18) }
            Glyph.Menu -> { line(4, 8, 20, 8); line(4, 15, 14, 15) }
            Glyph.More -> for (x in listOf(5, 12, 19)) circle(x, 12, 2, true)
            Glyph.Chevron -> { line(9, 6, 15, 12); line(15, 12, 9, 18) }
            Glyph.Search -> { circle(10, 10, 6); line(15, 15, 20, 20) }
            Glyph.Project -> drawPath(Path().apply {
                moveTo(12 * unit, 4 * unit)
                lineTo(22 * unit, 20 * unit)
                lineTo(2 * unit, 20 * unit)
                close()
            }, tint)
            Glyph.Activity -> { line(2, 12, 6, 12); line(6, 12, 9, 5); line(9, 5, 14, 19); line(14, 19, 17, 12); line(17, 12, 22, 12) }
            Glyph.Globe -> { circle(12, 12, 8); line(4, 12, 20, 12); line(12, 4, 12, 20) }
            Glyph.Branch -> { circle(7, 5, 2); circle(7, 19, 2); circle(18, 6, 2); line(7, 7, 7, 17); line(7, 14, 18, 8) }
            Glyph.Filter -> {
                for ((y, stop) in listOf(5 to 8, 12 to 16, 19 to 10)) {
                    line(4, y, 20, y)
                    line(stop, y - 2, stop, y + 2)
                }
            }
            Glyph.Refresh -> {
                drawArc(tint, 35f, 290f, false, point(5, 5), androidx.compose.ui.geometry.Size(unit * 14, unit * 14), style = Stroke(unit * 1.8f))
                line(15, 8, 20, 8); line(20, 8, 20, 3)
            }
            Glyph.Download -> { line(12, 4, 12, 15); line(8, 11, 12, 15); line(16, 11, 12, 15); line(4, 20, 20, 20) }
            Glyph.Send -> { line(12, 20, 12, 4); line(5, 11, 12, 4); line(19, 11, 12, 4) }
            Glyph.Check -> { line(5, 12, 10, 17); line(10, 17, 19, 7) }
            Glyph.Warn -> { circle(12, 12, 9); line(12, 7, 12, 13); circle(12, 17, 1, true) }
            Glyph.Settings -> { circle(12, 12, 8); circle(12, 12, 3); line(12, 2, 12, 4); line(12, 20, 12, 22) }
            Glyph.Help -> { circle(12, 12, 9); line(9, 8, 15, 8); line(15, 8, 12, 13); circle(12, 17, 1, true) }
            Glyph.Usage -> { line(4, 21, 4, 12); line(12, 21, 12, 4); line(20, 21, 20, 8) }
            Glyph.Agent -> { line(12, 3, 12, 21); line(3, 12, 21, 12); line(6, 6, 18, 18); line(18, 6, 6, 18) }
            Glyph.Theme -> { circle(12, 12, 5); line(12, 2, 12, 4); line(12, 20, 12, 22); line(2, 12, 4, 12); line(20, 12, 22, 12) }
            Glyph.Account -> { circle(12, 8, 4); line(4, 19, 7, 15); line(7, 15, 17, 15); line(17, 15, 20, 19) }
        }
    }
}

@Composable
internal fun IconControl(
    glyph: Glyph, label: String, onClick: () -> Unit, active: Boolean = false,
    modifier: Modifier = Modifier, enabled: Boolean = true, visualSize: Dp = Tokens.Filter,
) {
    val palette = LocalDashboardPalette.current
    Box(
        modifier.size(Tokens.Touch).clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(visualSize).background(palette.input, CircleShape)
                .border(Tokens.Stroke, if (active) palette.error else palette.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            DashboardIcon(glyph, if (active) palette.error else if (enabled) palette.ink else palette.disabled)
        }
    }
}

@Composable
internal fun DashboardHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    leftIcon: Glyph = Glyph.Back,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().heightIn(min = Tokens.TopBar).padding(horizontal = Tokens.Inset)) {
        if (onBack != null) {
            Box(Modifier.align(Alignment.CenterStart)) {
                val label = when (leftIcon) {
                    Glyph.Menu -> "Open workspace menu"
                    Glyph.Close -> "Close"
                    else -> "Back"
                }
                IconControl(leftIcon, label, onBack)
            }
        }
        Column(Modifier.align(Alignment.Center).padding(horizontal = Tokens.Touch), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = Tokens.Title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) MutedText(subtitle)
        }
        if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

@Composable
internal fun MutedText(text: String, modifier: Modifier = Modifier, monospace: Boolean = false) {
    Text(
        text, modifier, fontSize = Tokens.Metadata, color = LocalDashboardPalette.current.secondary,
        fontFamily = if (monospace) FontFamily.Monospace else null,
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
internal fun ProjectMark(project: Project, modifier: Modifier = Modifier) {
    val palette = LocalDashboardPalette.current
    Box(
        modifier.size(Tokens.RowIcon).background(
            if (project.number % 3 == 0) palette.badge else if (project.number % 3 == 1) palette.readySurface else palette.warningSurface,
            CircleShape,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (project.number == 0) DashboardIcon(Glyph.Project)
        else Text(project.name.substringBefore('-').take(2).uppercase(), fontSize = Tokens.Metadata, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun DashboardEmpty(
    title: String, description: String, glyph: Glyph, modifier: Modifier = Modifier,
    loading: Boolean = false, action: (() -> Unit)? = null, actionLabel: String = "Retry",
) {
    val palette = LocalDashboardPalette.current
    Column(
        modifier.fillMaxWidth().padding(Tokens.Space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Space12, Alignment.CenterVertically),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(Tokens.Filter), color = palette.ink)
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
        Text(title, fontSize = Tokens.Title, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(description, fontSize = Tokens.Body, color = palette.secondary, textAlign = TextAlign.Center)
        if (action != null) DashboardButton(actionLabel, action)
    }
}

@Composable
internal fun DashboardChart(values: List<Float>, modifier: Modifier = Modifier) {
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
            drawLine(palette.chart, Offset(x, size.height), Offset(x, size.height - height), step * 0.48f, StrokeCap.Round)
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
    separated: Boolean = false,
) {
    val palette = LocalDashboardPalette.current
    Box {
        IconControl(Glyph.More, label, onOpen, visualSize = Tokens.RowIcon)
        DropdownMenu(expanded = expanded, onDismissRequest = onClose, containerColor = palette.popover) {
            actions.forEachIndexed { index, item ->
                if (separated && index > 0) DividerLine(Modifier.padding(horizontal = Tokens.Space12))
                DropdownMenuItem(
                    text = { Text(item.label, fontSize = Tokens.Body) },
                    leadingIcon = {
                        if (item.project == null) DashboardIcon(item.glyph, palette.secondary)
                        else ProjectMark(item.project)
                    },
                    onClick = { onClose(); item.onClick?.invoke() },
                    enabled = item.onClick != null,
                    modifier = Modifier.height(Tokens.MenuRow),
                )
            }
        }
    }
}
