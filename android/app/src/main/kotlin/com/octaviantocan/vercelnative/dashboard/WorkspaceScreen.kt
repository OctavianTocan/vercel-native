package com.octaviantocan.vercelnative.dashboard

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.rotate
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

private val workspaceRoutes = listOf(
    Destination.Logs to R.drawable.dashboard_icon_logs,
    Destination.Analytics to R.drawable.dashboard_icon_analytics,
    Destination.Speed to R.drawable.dashboard_icon_speed,
    Destination.Firewall to R.drawable.dashboard_icon_firewall,
    Destination.WorkspaceDomains to R.drawable.dashboard_icon_domains,
    Destination.Integrations to R.drawable.dashboard_icon_integrations,
)

@Composable
internal fun WorkspaceRail(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit, backdrop: GraphicsLayer) {
    val palette = LocalDashboardPalette.current
    val safeInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val verticalInsets = safeInsets.only(WindowInsetsSides.Vertical)
    var failed by rememberSaveable { mutableStateOf(route.variant == "workspace-failed") }
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(safeInsets.only(WindowInsetsSides.Horizontal))) {
        val panelWidth = (maxWidth - Tokens.Touch).coerceIn(
            Tokens.WorkspaceRailWidth, Tokens.WorkspaceRailWidth + Tokens.WorkspacePanelWidth,
        ).coerceAtMost(maxWidth)
        Box(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(maxWidth - panelWidth)
                .background(Color.Black.copy(alpha = Tokens.SidebarScrimOpacity))
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "Close workspace menu" },
        )
        Surface(
            Modifier.align(Alignment.CenterStart).fillMaxHeight().width(panelWidth),
            color = Color.Transparent,
        ) {
            Row {
                WorkspaceSwitcher(navigation, backdrop, verticalInsets)
                Box(Modifier.fillMaxHeight().width(Tokens.Stroke).background(palette.insetSurface))
                Box(Modifier.weight(1f).fillMaxHeight().dashboardBackdrop(backdrop).background(palette.sidebarPanel)) {
                    Column(
                        Modifier.fillMaxSize().windowInsetsPadding(verticalInsets)
                            .padding(bottom = Tokens.Space20),
                    ) {
                        Column(Modifier.weight(1f).dashboardScroll(surface = palette.sidebarPanel).padding(top = Tokens.WorkspacePanelTop)) {
                            WorkspaceMenuRow("Observability", R.drawable.dashboard_icon_observability) { navigation.show(Destination.Observability) }
                            WorkspaceMenuRow("Help", R.drawable.dashboard_icon_help) { navigation.show(Destination.Help) }
                            WorkspaceMenuRow("Docs", R.drawable.dashboard_icon_docs) { navigation.show(Destination.Help, variant = "docs") }
                            DividerLine(Modifier.padding(horizontal = Tokens.Space24, vertical = Tokens.Space8), palette.insetSurface)
                            for ((destination, icon) in workspaceRoutes) {
                                WorkspaceMenuRow(destination.label, icon) { navigation.show(destination) }
                            }
                            if (failed) DashboardNotice(
                                "Can’t access Studio", tone = Tone.Error,
                                modifier = Modifier.padding(horizontal = Tokens.WorkspaceRowInset),
                                retry = {
                                    failed = false
                                    navigation.chooseWorkspace(DashboardFixtures.studioWorkspace)
                                },
                            )
                        }
                        WorkspaceMenuRow("Theme", R.drawable.dashboard_icon_theme) { navigation.show(Destination.Theme) }
                        Spacer(Modifier.height(Tokens.Space8))
                        WorkspaceMenuRow("Account Settings", R.drawable.dashboard_icon_account) { navigation.show(Destination.Profile) }
                    }
                }
                Box(Modifier.fillMaxHeight().width(Tokens.Stroke).background(palette.insetSurface))
            }
        }
    }
}

@Composable
private fun WorkspaceSwitcher(navigation: DashboardNavigation, backdrop: GraphicsLayer, verticalInsets: WindowInsets) {
    val palette = LocalDashboardPalette.current
    Column(
        Modifier.fillMaxHeight().width(Tokens.WorkspaceRailWidth - Tokens.Stroke)
            .dashboardBackdrop(backdrop).background(palette.workspaceRail)
            .windowInsetsPadding(verticalInsets),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().dashboardScroll(surface = palette.workspaceRail)
                .padding(top = Tokens.WorkspaceBadgeTop).selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Tokens.WorkspaceBadgeGap),
        ) {
            for (workspace in DashboardFixtures.workspaces) {
                val selected = workspace.id == navigation.workspaceId
                Box(Modifier.fillMaxWidth().height(Tokens.WorkspaceBadgeSize)) {
                    if (selected) Box(
                        Modifier.align(Alignment.CenterStart)
                            .size(Tokens.WorkspaceSelectionWidth, Tokens.WorkspaceSelectionHeight)
                            .background(palette.ink, RoundedCornerShape(Tokens.WorkspaceSelectionRadius)),
                    )
                    Box(
                        Modifier.align(Alignment.Center).size(Tokens.WorkspaceBadgeSize).clip(CircleShape)
                            .background(if (selected) palette.workspaceBadgeSelected else palette.selected)
                            .then(if (selected) Modifier.border(Tokens.Stroke, palette.ink, CircleShape) else Modifier)
                            .selectable(selected, role = Role.RadioButton) { navigation.chooseWorkspace(workspace.id) }
                            .semantics { contentDescription = workspace.name },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            workspace.name.take(1), Modifier.clearAndSetSemantics {}, style = GeistText, color = palette.workspaceBadgeInk,
                            fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(Tokens.Touch)
                .clickable(role = Role.Button) { navigation.show(Destination.Agent) }
                .semantics { contentDescription = "Agent" },
            contentAlignment = Alignment.Center,
        ) {
            DashboardArtwork(R.drawable.dashboard_icon_agent, Tokens.Space24, palette.glassInk)
        }
    }
}

@Composable
private fun WorkspaceMenuRow(label: String, @DrawableRes icon: Int, onClick: () -> Unit) {
    val palette = LocalDashboardPalette.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Tokens.WorkspaceRowInset).heightIn(min = Tokens.Touch)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = Tokens.Space24 - Tokens.WorkspaceRowInset),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Inset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DashboardArtwork(icon, Tokens.Inset, palette.glassInk)
        Text(
            label, style = GeistText, color = palette.glassInk,
            fontSize = Tokens.ListTitle, lineHeight = Tokens.ListTitleLine, fontWeight = FontWeight.Medium,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun WorkspaceScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    when (route.destination) {
        Destination.WorkspaceDomains -> WorkspaceDomains(navigation, onBack)
        Destination.Observability -> ObservabilityScreen(navigation, onBack)
        else -> WorkspaceToolScreen(route, navigation, onBack)
    }
}

@Composable
internal fun WorkspaceDomains(navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    var query by rememberSaveable { mutableStateOf("") }
    val domains = navigation.projects().flatMap { DashboardFixtures.domains(it.id) }.filter {
        it.hostname.contains(query.trim(), ignoreCase = true)
    }
    Column(Modifier.fillMaxSize()) {
        DashboardHeader("Domains", onBack, subtitle = navigation.currentWorkspace.name,
            modifier = Modifier.padding(top = Tokens.AlertDetailHeaderInset), titleSize = Tokens.DetailTitle)
        DashboardField(
            query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset).padding(top = Tokens.Inset),
            placeholder = "Search domains", shape = CircleShape, height = Tokens.DomainFieldHeight,
            leading = { DashboardIcon(Glyph.Search, palette.secondary, Modifier.size(Tokens.Space20)) },
        )
        DashboardList(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Tokens.Inset, end = Tokens.Inset, top = Tokens.Space20, bottom = Tokens.DockScrollClearance),
        ) {
            item {
                Text("Domains in this workspace", Modifier.padding(start = Tokens.Space8, bottom = Tokens.Inset),
                    fontSize = Tokens.UsageTitle, fontWeight = FontWeight.SemiBold, lineHeight = Tokens.UsageTitleLine)
            }
            items(domains, key = ProjectDomain::id) { domain ->
                val first = domain == domains.first()
                val last = domain == domains.last()
                val shape = RoundedCornerShape(
                    topStart = if (first) Tokens.Inset else Tokens.Stroke, topEnd = if (first) Tokens.Inset else Tokens.Stroke,
                    bottomStart = if (last) Tokens.Inset else Tokens.Stroke, bottomEnd = if (last) Tokens.Inset else Tokens.Stroke,
                )
                val project = navigation.project(domain.projectId)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Tokens.WorkspaceDomainRow).clip(shape).background(palette.group)
                        .clickable { navigation.show(Destination.Domain, domain.id) }.padding(horizontal = Tokens.Inset),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
                ) {
                    DashboardIcon(Glyph.Globe, palette.secondary)
                    Column(Modifier.weight(1f)) {
                        Text(domain.hostname, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine,
                            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${navigation.projectName(project)} · ${domain.status.label}", fontSize = Tokens.SearchDetailSize,
                            lineHeight = Tokens.SearchDetailLine, color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.size(Tokens.Space6).background(
                        if (domain.status == DomainStatus.Verified) palette.ready else palette.warning, CircleShape,
                    ))
                    DashboardIcon(Glyph.Chevron, palette.secondary)
                }
                if (!last) DividerLine(Modifier.padding(horizontal = Tokens.Inset))
            }
        }
    }
}

@Composable
internal fun ObservabilityScreen(navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.ControlRadius)
    var shortRange by rememberSaveable { mutableStateOf(false) }
    var refresh by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        DashboardHeader("Observability", onBack, subtitle = navigation.currentWorkspace.name,
            modifier = Modifier.padding(top = Tokens.AlertDetailHeaderInset), titleSize = Tokens.DetailTitle)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset).padding(top = Tokens.Inset, bottom = Tokens.Space8),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.heightIn(min = Tokens.LogActionSize).dashboardGlass().clickable { shortRange = !shortRange }
                    .padding(horizontal = Tokens.Space12),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
            ) {
                Text(if (shortRange) "Last 30 min" else "Last 24 hours", fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine)
                DashboardIcon(Glyph.Chevron, palette.secondary, modifier = Modifier.rotate(90f))
            }
            IconControl(Glyph.Refresh, "Refresh observability", { refresh += 1 }, visualSize = Tokens.LogActionSize)
        }
        DashboardList(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Tokens.Inset, end = Tokens.Inset, bottom = Tokens.DockScrollClearance),
            verticalArrangement = Arrangement.spacedBy(Tokens.Inset),
        ) {
            item {
                Text("Production traffic", Modifier.padding(horizontal = Tokens.Space8, vertical = Tokens.Space12),
                    fontSize = Tokens.UsageTitle, lineHeight = Tokens.UsageTitleLine, fontWeight = FontWeight.SemiBold)
            }
            item {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Tokens.AlertMetricsHeight)
                        .background(palette.control, shape).border(Tokens.Stroke, palette.quietBorder, shape).padding(Tokens.Inset),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Inset), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        MutedText("Requests")
                        Text(if (shortRange) "2.3k" else "18.4k", fontSize = Tokens.UsageTitle, fontWeight = FontWeight.Medium)
                    }
                    Box(Modifier.width(Tokens.Stroke).height(Tokens.Touch).background(palette.quietBorder))
                    Column(Modifier.weight(1f)) {
                        MutedText("Error rate")
                        Text(if (shortRange) "0.18%" else "0.27%", fontSize = Tokens.UsageTitle, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = Tokens.ObservabilityChartPanel)
                        .border(Tokens.Stroke, palette.quietBorder, shape).padding(Tokens.Inset),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space8),
                ) {
                    Text("Requests over time", fontSize = Tokens.Body, lineHeight = Tokens.ProjectOptionsLine, fontWeight = FontWeight.SemiBold)
                    DashboardChart(List(12) { i -> (40 + (i * 17 + refresh * 5) % 52) / 100f })
                }
            }
            item {
                Column {
                    Text("Investigate", Modifier.fillMaxWidth().background(palette.control).padding(Tokens.Space8),
                        fontSize = Tokens.Metadata, fontWeight = FontWeight.Medium)
                    for (destination in listOf(Destination.Logs, Destination.Analytics, Destination.Speed)) {
                        DashboardRow(destination.label, onClick = { navigation.show(destination) }, minHeight = Tokens.Touch)
                        DividerLine()
                    }
                }
            }
        }
    }
}

private data class ToolRecord(val id: Int, val title: String, val subtitle: String, val detail: String)

@Composable
internal fun WorkspaceToolScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val destination = route.destination
    val palette = LocalDashboardPalette.current
    val scope = route.target?.let(DashboardFixtures::workspace) ?: navigation.currentWorkspace
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    var empty by rememberSaveable { mutableStateOf(route.variant == "empty") }
    var alternateRange by rememberSaveable { mutableStateOf(false) }
    var mobile by rememberSaveable { mutableStateOf(true) }
    val restricted = route.variant == "restricted"
    val integration = destination == Destination.Integrations
    val stats = when (destination) {
        Destination.Analytics -> listOf("Visitors" to "1,284", "Page views" to "2,062")
        Destination.Speed -> listOf((if (mobile) "Mobile score" else "Desktop score") to if (mobile) "92" else "97", "Data points" to "1,020")
        Destination.Firewall -> listOf("Allowed" to "18.4k", "Challenged" to "72", "Blocked" to "28")
        else -> emptyList()
    }
    val metricHeading = when (destination) {
        Destination.Analytics -> "Traffic"
        Destination.Speed -> "Real Experience Score"
        else -> "Request activity"
    }
    val records = remember(destination) { toolRecords(destination) }
    Column(Modifier.fillMaxSize()) {
        val subtitle = if (integration) scope.name else "${navigation.projectName(navigation.project(null))} · Production"
        DashboardHeader(destination.label, onBack, subtitle = subtitle,
            modifier = Modifier.padding(top = Tokens.AlertDetailHeaderInset), titleSize = Tokens.DetailTitle)
        if (empty || restricted) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (empty) DashboardEmpty("No visitors in this period", "Try a different range to view traffic.", Glyph.Usage,
                    action = { empty = false }, actionLabel = "Change time range")
                else DashboardEmpty("Firewall access required", "Ask a workspace owner to grant you access.", Glyph.Lock,
                    action = onBack, actionLabel = "Go back")
            }
        } else DashboardList(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = Tokens.Inset, end = Tokens.Inset, top = Tokens.Inset, bottom = Tokens.DockScrollClearance),
        ) {
            if (!integration) item {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space6),
                        verticalAlignment = Alignment.CenterVertically) {
                        val range = if (alternateRange) "Last 30 days" else if (destination == Destination.Firewall) "Last 24 hours" else "Last 7 days"
                        Row(
                            Modifier.heightIn(min = Tokens.LogActionSize).dashboardGlass().clickable { alternateRange = !alternateRange }
                                .padding(horizontal = Tokens.Space12),
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Space6), verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(range, fontSize = Tokens.SearchDetailSize, fontWeight = FontWeight.Medium)
                            DashboardIcon(Glyph.Chevron, palette.secondary, Modifier.rotate(90f))
                        }
                        if (destination == Destination.Speed) {
                            for ((isMobile, label) in listOf(true to "Mobile", false to "Desktop")) {
                                Box(
                                    Modifier.weight(1f).height(Tokens.LogActionSize).clip(CircleShape)
                                        .background(if (mobile == isMobile) palette.selected else palette.control)
                                        .clickable { mobile = isMobile },
                                    contentAlignment = Alignment.Center,
                                ) { Text(label, fontSize = Tokens.SearchDetailSize, fontWeight = FontWeight.Medium) }
                            }
                        }
                    }
                    Text(metricHeading, Modifier.padding(top = Tokens.Space8, start = Tokens.Space8),
                        fontSize = Tokens.UsageTitle, lineHeight = Tokens.UsageTitleLine, fontWeight = FontWeight.SemiBold)
                    val shape = RoundedCornerShape(Tokens.ControlRadius)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = Tokens.AlertMetricsHeight)
                            .background(palette.control, shape).border(Tokens.Stroke, palette.quietBorder, shape)
                            .padding(Tokens.Space12),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        stats.forEachIndexed { index, (label, value) ->
                            if (index > 0) Box(Modifier.height(Tokens.Touch).width(Tokens.Stroke).background(palette.quietBorder))
                            Column(Modifier.weight(1f)) {
                                Text(label, color = palette.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine)
                                Text(value, fontSize = Tokens.AlertDetailTitle, lineHeight = Tokens.AlertMetricLine, fontWeight = FontWeight.Medium,
                                    color = when (label) {
                                        "Mobile score", "Desktop score" -> palette.ready
                                        "Challenged" -> palette.warning
                                        "Blocked" -> palette.error
                                        else -> palette.ink
                                    })
                            }
                        }
                    }
                    ToolTrend(destination, alternateRange, mobile)
                }
            }
            item {
                val label = when (destination) {
                    Destination.Analytics -> "Top pages"
                    Destination.Speed -> "Web Vitals · 75th percentile"
                    Destination.Firewall -> "Recent events"
                    else -> "Installed integrations"
                }
                if (integration) Text(label, Modifier.padding(start = Tokens.Space8, top = Tokens.Space12, bottom = Tokens.Inset),
                    fontSize = Tokens.UsageTitle, lineHeight = Tokens.UsageTitleLine, fontWeight = FontWeight.SemiBold)
                else Text(label, Modifier.padding(start = Tokens.Space12, top = Tokens.Space24, bottom = Tokens.Space8),
                    color = palette.secondary, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine)
            }
            items(records, key = ToolRecord::id) { record ->
                val top = record == records.first()
                val bottom = record == records.last()
                val shape = RoundedCornerShape(
                    topStart = if (top) Tokens.Inset else Tokens.Stroke, topEnd = if (top) Tokens.Inset else Tokens.Stroke,
                    bottomStart = if (bottom) Tokens.Inset else Tokens.Stroke, bottomEnd = if (bottom) Tokens.Inset else Tokens.Stroke,
                )
                val event = destination == Destination.Firewall
                Column(Modifier.fillMaxWidth().clip(shape).background(palette.group)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = if (integration) Tokens.WorkspaceDomainRow else Tokens.Touch)
                            .clickable { expanded = if (expanded == record.id) null else record.id }
                            .padding(horizontal = Tokens.Inset, vertical = Tokens.Space8),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (integration) {
                            val icon = listOf(R.drawable.dashboard_figma_integration_alert, R.drawable.dashboard_figma_integration_network,
                                R.drawable.dashboard_figma_integration_plug)[record.id % 3]
                            Box(Modifier.size(Tokens.IconSlot).background(palette.selected, RoundedCornerShape(Tokens.SmallRadius)),
                                contentAlignment = Alignment.Center) { DashboardArtwork(icon, Tokens.Icon, palette.secondary) }
                        } else if (event) {
                            DashboardArtwork(
                                if (record.id % 3 == 1) R.drawable.dashboard_figma_firewall_info else R.drawable.dashboard_figma_lock,
                                Tokens.DockIcon, if (record.id % 3 == 1) palette.warning else palette.error,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(record.title, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine)
                            if (event || integration) Text(record.subtitle, color = palette.secondary, lineHeight = Tokens.SearchDetailLine,
                                fontSize = Tokens.SearchDetailSize)
                        }
                        if (integration) Box(Modifier.size(Tokens.Space6)
                            .background(if (record.id % 4 == 2) palette.warning else palette.ready, CircleShape))
                        if (event || integration) DashboardIcon(Glyph.Chevron, palette.secondary)
                        else Text(record.subtitle, color = if (destination == Destination.Speed) palette.ready else palette.secondary,
                            fontSize = Tokens.SearchDetailSize, textAlign = TextAlign.End)
                    }
                    if (expanded == record.id) MutedText(record.detail, Modifier.padding(horizontal = Tokens.Inset, vertical = Tokens.Space8))
                    if (!bottom) DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                }
            }
        }
    }
}

@Composable
private fun ToolTrend(destination: Destination, alternateRange: Boolean, mobile: Boolean) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.ControlRadius)
    val (title, color) = when (destination) {
        Destination.Analytics -> "Visitors by day" to palette.chart
        Destination.Speed -> "Score by day" to palette.ready
        else -> "Mitigated requests" to palette.warning
    }
    Column(
        Modifier.fillMaxWidth().heightIn(min = Tokens.ObservabilityChartPanel)
            .border(Tokens.Stroke, palette.quietBorder, shape).padding(Tokens.Space12),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space8),
    ) {
        Text(title, fontSize = Tokens.Body, lineHeight = Tokens.ProjectOptionsLine, fontWeight = FontWeight.SemiBold)
        if (destination == Destination.Firewall) {
            DashboardChart(listOf(.18f, .26f, .34f, .47f, .41f, .62f, .81f, .58f, .49f, .35f, .31f, .25f), color = color)
        } else Canvas(Modifier.fillMaxWidth().height(Tokens.ChartHeight)) {
            for (i in 0..4) {
                val y = size.height * i / 4f
                drawLine(palette.quietBorder, Offset(0f, y), Offset(size.width, y), Tokens.Stroke.toPx())
            }
            val values = if (destination == Destination.Analytics) listOf(.40f, .51f, .46f, .63f, .77f, .83f, .87f)
            else if (mobile) listOf(.72f, .76f, .75f, .87f, .86f, .91f, .92f)
            else listOf(.84f, .82f, .85f, .88f, .91f, .94f, .97f)
            val path = Path()
            values.forEachIndexed { i, value ->
                val x = i * size.width / values.lastIndex
                val y = (1f - value * if (alternateRange) .92f else 1f) * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(Tokens.Space2.toPx()))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val labels = if (destination == Destination.Firewall) listOf("00:00", "06:00", "12:00", "18:00", "24:00")
            else listOf("Nov 15", "Nov 17", "Nov 19", "Nov 21")
            labels.forEach { Text(it, color = palette.secondary, fontSize = Tokens.ChartCaption, fontFamily = GeistMono) }
        }
    }
}

private fun toolRecords(destination: Destination) = List(27) { index ->
    val pages = listOf("/", "/components", "/docs", "/checkout", "/catalog")
    val page = pages[index % pages.size]
    val number = index + 1
    when (destination) {
        Destination.Analytics -> ToolRecord(
            index, if (index < pages.size) page else "$page/$number",
            "${listOf(684, 332, 168, 124, 88)[index % pages.size]} visitors", "${1850 - index * 42} page views",
        )
        Destination.Speed -> {
            val name = listOf("Largest Contentful Paint", "Interaction to Next Paint", "Cumulative Layout Shift")[index % 3]
            val sample = listOf("1.8 s", "142 ms", "0.04")[index % 3]
            ToolRecord(index, name, sample, "$page · Sample $number")
        }
        Destination.Firewall -> {
            val action = listOf("Blocked request", "Challenge issued", "Blocked request")[index % 3]
            ToolRecord(index, action, "$page · $number min ago", "Request firewall-$number · Production")
        }
        else -> {
            val integration = listOf("Sentry", "Upstash", "Slack", "Analytics export")[index % 4]
            val scope = listOf("atlas-catalog", "weekend-market", "Workspace", "harbor-notes")[index % 4]
            val label = if (index < 4) integration else "$integration $number"
            ToolRecord(index, label, "$scope · ${if (index % 4 == 2) "Needs reconnection" else "Connected"}", "Installed in $scope.")
        }
    }
}

@Composable
internal fun HelpSheet(onBack: () -> Unit, docsOnly: Boolean = false) {
    DashboardHeader(if (docsOnly) "Docs" else "Help", onBack)
    Column(Modifier.padding(Tokens.Inset)) {
        if (!docsOnly) {
            DashboardRow("Contact Support", "Vercel Support", glyph = Glyph.Help)
            DividerLine()
        }
        DashboardRow("Documentation", "Vercel Docs", glyph = Glyph.Globe)
    }
}
