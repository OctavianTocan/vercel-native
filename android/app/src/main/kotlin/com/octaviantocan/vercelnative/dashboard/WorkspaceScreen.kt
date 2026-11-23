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
                        }
                        WorkspaceMenuRow("Theme", R.drawable.dashboard_icon_theme) { navigation.show(Destination.Theme) }
                        Spacer(Modifier.height(Tokens.Space8))
                        WorkspaceMenuRow("Account Settings", R.drawable.dashboard_icon_account,
                            selected = route.variant in setOf("account", "workspace", "workspace-failed")) { navigation.show(Destination.Profile) }
                    }
                }
                Box(Modifier.fillMaxHeight().width(Tokens.Stroke).background(palette.insetSurface))
            }
        }
        if (failed) {
            DashboardGlassNotice("Can’t access Studio", error = true, retryLabel = "Retry",
                modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(verticalInsets.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = Tokens.Inset).padding(bottom = Tokens.DockScrollClearance),
                onRetry = {
                    failed = false
                    navigation.chooseWorkspace(DashboardFixtures.studioWorkspace)
                })
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
            val workspaces = (DashboardFixtures.workspaces + navigation.currentWorkspace).distinct()
            for (workspace in workspaces) {
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
private fun WorkspaceMenuRow(label: String, @DrawableRes icon: Int, selected: Boolean = false, onClick: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val ink = if (selected) palette.canvas else palette.glassInk
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Tokens.WorkspaceRowInset).heightIn(min = Tokens.Touch)
            .then(if (selected) Modifier.background(palette.glassInk, RoundedCornerShape(Tokens.SmallRadius)) else Modifier)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = Tokens.Space24 - Tokens.WorkspaceRowInset),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Inset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DashboardArtwork(icon, Tokens.Inset, ink)
        Text(
            label, style = GeistText, color = ink,
            fontSize = Tokens.ListTitle, lineHeight = Tokens.ListTitleLine, fontWeight = FontWeight.Medium,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun WorkspaceScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    when (route.destination) {
        Destination.WorkspaceDomains -> WorkspaceDomains(route, navigation, onBack)
        Destination.Observability -> ObservabilityScreen(navigation, onBack)
        else -> WorkspaceToolScreen(route, navigation, onBack)
    }
}

@Composable
internal fun WorkspaceDomains(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    var query by rememberSaveable { mutableStateOf("") }
    val projects = navigation.projects()
    val shownProjects = if (route.frozen) projects.take(3) else projects
    val domains = shownProjects.flatMapIndexed { index, project ->
        val projectDomains = DashboardFixtures.domains(project.id)
        if (route.frozen) projectDomains.take(if (index == 0) 2 else 1) else projectDomains
    }.filter {
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
                Modifier.width(Tokens.ObservabilityRangeWidth).heightIn(min = Tokens.LogActionSize).dashboardGlass()
                    .clickable { shortRange = !shortRange }.padding(horizontal = Tokens.BodyInset),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(if (shortRange) "Last 30 min" else "Last 24 hours", fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine)
                DashboardIcon(Glyph.Chevron, palette.secondary, modifier = Modifier.rotate(90f))
            }
            IconControl(Glyph.Refresh, "Refresh observability", { refresh += 1 }, visualSize = Tokens.LogActionSize)
        }
        DashboardList(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Tokens.Space24, end = Tokens.Space24, bottom = Tokens.DockScrollClearance),
            verticalArrangement = Arrangement.spacedBy(Tokens.Inset),
        ) {
            item {
                Text("Production traffic", Modifier.padding(top = Tokens.DeploymentEnvironmentGap),
                    fontSize = Tokens.UsageTitle, lineHeight = Tokens.UsageTitleLine, fontWeight = FontWeight.SemiBold)
            }
            item {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Tokens.ObservabilityMetricsHeight)
                        .background(palette.control, shape).border(Tokens.Stroke, palette.quietBorder, shape).padding(horizontal = Tokens.Inset),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Inset), verticalAlignment = Alignment.CenterVertically,
                ) {
                    ObservabilityMetric("Requests", if (shortRange) "2.3k" else "18.4k", Modifier.weight(1f))
                    Box(Modifier.width(Tokens.Stroke).height(Tokens.TopBar).background(palette.quietBorder))
                    ObservabilityMetric("Error rate", if (shortRange) "0.18%" else "0.27%", Modifier.weight(1f))
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = Tokens.ObservabilityChartPanel)
                        .border(Tokens.Stroke, palette.quietBorder, shape).padding(Tokens.BodyInset),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Inset),
                ) {
                    Text("Requests over time", fontSize = Tokens.ListTitle, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.SemiBold)
                    val readings = listOf(.60f, .68f, .82f, .71f, .85f, .96f, .87f, .84f, .80f, .76f, .70f, .63f)
                    DashboardChart(
                        readings.mapIndexed { index, value ->
                            val sample = if (refresh == 0) value else value + ((index + refresh) % 5 - 2) * .025f
                            if (shortRange) sample * .72f else sample
                        },
                        ticks = listOf("2k", "1k", "0"), dates = listOf("00:00", "06:00", "12:00", "18:00", "24:00"),
                        height = Tokens.ObservabilityTicksHeight, bars = Tokens.ObservabilityBarWidth,
                        observability = true, color = palette.observabilityChart,
                    )
                }
            }
            item {
                Column(Modifier.padding(top = Tokens.Space8)) {
                    Text("Investigate", Modifier.fillMaxWidth().background(palette.control).padding(Tokens.Space8),
                        fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium)
                    for (destination in listOf(Destination.Logs, Destination.Analytics, Destination.Speed)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = Tokens.WorkspaceToolRow)
                            .clickable { navigation.show(destination) }.padding(start = Tokens.Space8, end = Tokens.Space12),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(destination.label, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine)
                            DashboardIcon(Glyph.Chevron, palette.secondary)
                        }
                        DividerLine()
                    }
                }
            }
        }
    }
}

@Composable
private fun ObservabilityMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = Tokens.Space8), verticalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
        Text(label, fontSize = Tokens.Metadata, lineHeight = Tokens.ChartTickLine, color = LocalDashboardPalette.current.secondary)
        Text(value, fontSize = Tokens.Heading, lineHeight = Tokens.PlanTitleLine, fontWeight = FontWeight.Medium)
    }
}

private data class ToolRecord(val id: Int, val title: String, val subtitle: String, val detail: String)

private data class FirewallActivity(
    val allowed: Int,
    val blocked: Int,
    val mitigated: List<Int>,
    val labels: List<String>,
) {
    val stats get() = listOf(
        "Allowed" to "${sampleNumber(allowed / 1_000.0)}k",
        "Challenged" to (mitigated.sum() - blocked).toString(),
        "Blocked" to blocked.toString(),
    )
}

private val dailyFirewall = FirewallActivity(
    18_400, 28, listOf(3, 5, 7, 9, 8, 11, 15, 12, 10, 8, 7, 5),
    listOf("00:00", "06:00", "12:00", "18:00", "24:00"),
)
private val monthlyFirewall = FirewallActivity(
    492_600, 689, listOf(132, 178, 164, 212, 196, 241, 306, 281, 230, 199, 265, 253),
    listOf("Oct 23", "Oct 30", "Nov 7", "Nov 15", "Nov 22"),
)

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
    val firewall = if (alternateRange) monthlyFirewall else dailyFirewall
    val stats = when (destination) {
        Destination.Analytics -> listOf("Visitors" to "1,284", "Page views" to "2,062")
        Destination.Speed -> listOf((if (mobile) "Mobile score" else "Desktop score") to if (mobile) "92" else "97", "Data points" to "1,020")
        Destination.Firewall -> firewall.stats
        else -> emptyList()
    }
    val projectName = navigation.projectName(navigation.project(null))
    val metricHeading = when (destination) {
        Destination.Analytics -> "Traffic"
        Destination.Speed -> "Real Experience Score"
        else -> "Request activity"
    }
    val recordCount = if (route.frozen) 3 else 27
    val records = remember(destination, recordCount) { toolRecords(destination, recordCount) }
    Column(Modifier.fillMaxSize()) {
        val subtitle = when {
            integration || restricted -> scope.name
            else -> "$projectName · Production"
        }
        DashboardHeader(destination.label, onBack, subtitle = subtitle,
            modifier = Modifier.padding(top = Tokens.AlertDetailHeaderInset), titleSize = Tokens.DetailTitle)
        if (empty || restricted) {
            if (empty) Box(Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset, vertical = Tokens.Inset)) {
                WorkspaceTimeRange("Last 7 days") { empty = false; alternateRange = true }
            }
            val feedbackClearance = if (empty) Tokens.AnalyticsFeedbackClearance else Tokens.DockScrollClearance
            Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = feedbackClearance), contentAlignment = Alignment.Center) {
                if (empty) DashboardEmpty("No visitors in this period", "Analytics is active for $projectName. Try a different time range to see recorded traffic.",
                    Glyph.Usage, action = { empty = false; alternateRange = true }, actionLabel = "Change time range",
                    illustration = R.drawable.dashboard_figma_workspace_empty, glassAction = true, prominentTitle = true,
                    actionWidth = Tokens.AnalyticsFeedbackActionWidth)
                else DashboardEmpty("Firewall access required", "Ask a workspace owner to grant you access to Firewall.", Glyph.Lock,
                    action = onBack, actionLabel = "Go back", illustration = R.drawable.dashboard_figma_workspace_access,
                    glassAction = true, prominentTitle = true)
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
                        WorkspaceTimeRange(range, if (destination == Destination.Speed) Modifier.padding(end = Tokens.Space8) else Modifier) {
                            alternateRange = !alternateRange
                        }
                        if (destination == Destination.Speed) {
                            for ((isMobile, label) in listOf(true to "Mobile", false to "Desktop")) {
                                Box(
                                    Modifier.weight(1f).height(Tokens.LogActionSize).clip(CircleShape)
                                        .background(if (mobile == isMobile) palette.insetSurface else palette.control)
                                        .border(Tokens.Stroke, palette.border, CircleShape)
                                        .clickable { mobile = isMobile },
                                    contentAlignment = Alignment.Center,
                                ) { Text(label, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.ListSubtitleLine,
                                    fontWeight = if (mobile == isMobile) FontWeight.SemiBold else FontWeight.Medium) }
                            }
                        }
                    }
                    Text(metricHeading, Modifier.padding(top = Tokens.Space8, start = Tokens.Space8),
                        fontSize = Tokens.WorkspaceMetricTitle, lineHeight = Tokens.UsageTitleLine, fontWeight = FontWeight.SemiBold)
                    val shape = RoundedCornerShape(Tokens.ControlRadius)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = Tokens.AlertMetricsHeight)
                            .background(palette.control, shape).border(Tokens.Stroke, palette.quietBorder, shape)
                            .padding(horizontal = Tokens.AlertMeasurePad, vertical = Tokens.Space12),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        stats.forEachIndexed { index, (label, value) ->
                            if (index > 0) Box(Modifier.height(Tokens.Touch).width(Tokens.Stroke).background(palette.quietBorder))
                            Column(Modifier.weight(1f)) {
                                Text(label, color = palette.projectOptionsGlyph, fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine)
                                Text(value, fontSize = Tokens.AlertDetailTitle, lineHeight = Tokens.AlertMetricLine, fontWeight = FontWeight.Medium,
                                    color = when (label) {
                                        "Mobile score", "Desktop score" -> palette.speedChart
                                        "Challenged" -> palette.warning
                                        "Blocked" -> palette.error
                                        else -> palette.ink
                                    })
                            }
                        }
                    }
                    Box(Modifier.padding(top = Tokens.Space6)) { ToolTrend(destination, alternateRange, mobile, firewall) }
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
                else Text(label, Modifier.padding(start = Tokens.AlertMeasurePad,
                    top = Tokens.Space24 + if (destination == Destination.Speed) Tokens.Space8 else Tokens.Space4,
                    bottom = Tokens.Space8 + Tokens.Space2),
                    color = palette.projectOptionsGlyph, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine,
                    fontWeight = FontWeight.Medium)
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
                        Modifier.fillMaxWidth().heightIn(min = when {
                            integration -> Tokens.WorkspaceDomainRow
                            event -> Tokens.WorkspaceFirewallRow
                            destination == Destination.Analytics -> Tokens.WorkspaceToolRow + Tokens.Stroke
                            else -> Tokens.WorkspaceToolRow
                        })
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
                            Text(record.title, fontSize = if (integration) Tokens.ListSubtitle else Tokens.Body,
                                lineHeight = if (integration) Tokens.ListSubtitleLine else Tokens.ProjectOptionsLine)
                            if (event || integration) Text(record.subtitle, color = palette.secondary,
                                lineHeight = if (event) Tokens.ChartTickLine else Tokens.SearchDetailLine,
                                fontSize = if (event) Tokens.Metadata else Tokens.SearchDetailSize)
                        }
                        if (integration) Box(Modifier.size(Tokens.Space6)
                            .background(if (record.id % 4 == 2) palette.warning else palette.ready, CircleShape))
                        if (event || integration) DashboardIcon(Glyph.Chevron, palette.secondary)
                        else Text(record.subtitle, color = if (destination == Destination.Speed) palette.speedChart else palette.projectOptionsGlyph,
                            fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.ListSubtitleLine, textAlign = TextAlign.End)
                    }
                    if (expanded == record.id) MutedText(record.detail, Modifier.padding(horizontal = Tokens.Inset, vertical = Tokens.Space8))
                    if (!bottom) DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                }
            }
        }
    }
}

@Composable
private fun WorkspaceTimeRange(range: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.width(Tokens.WorkspaceTimeRangeWidth).heightIn(min = Tokens.LogActionSize).dashboardGlass()
            .clickable(onClick = onClick).padding(horizontal = Tokens.Space8 + Tokens.Space2),
        horizontalArrangement = Arrangement.spacedBy(Tokens.DockLabelGap), verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(range, Modifier.weight(1f), fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.ListSubtitleLine,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        DashboardIcon(Glyph.Chevron, LocalDashboardPalette.current.secondary, Modifier.rotate(90f))
    }
}

@Composable
private fun ToolTrend(destination: Destination, alternateRange: Boolean, mobile: Boolean, firewall: FirewallActivity) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.ControlRadius)
    val (title, color) = when (destination) {
        Destination.Analytics -> "Visitors by day" to palette.studyDeliveryInk
        Destination.Speed -> "Score by day" to palette.speedChart
        else -> "Mitigated requests" to palette.warning
    }
    Column(
        Modifier.fillMaxWidth().border(Tokens.Stroke, palette.quietBorder, shape)
            .padding(start = Tokens.AlertMeasurePad, end = Tokens.AlertMeasurePad,
                top = Tokens.ProjectOptionsInset, bottom = Tokens.PreviewStatusInset),
        verticalArrangement = Arrangement.spacedBy(Tokens.ChartTitleGap),
    ) {
        Text(title, fontSize = Tokens.Body, lineHeight = Tokens.ProjectOptionsLine, fontWeight = FontWeight.SemiBold)
        if (destination == Destination.Firewall) {
            val step = if (alternateRange) ((firewall.mitigated.max() + 3) / 4) else 5
            val ticks = (4 downTo 0).map { (it * step).toString() }
            DashboardChart(firewall.mitigated.map { it / (step * 4f) }, ticks, firewall.labels, color = color,
                height = Tokens.FirewallTicksHeight)
        } else {
            val ticks = if (destination == Destination.Analytics) listOf("300", "200", "100", "0")
            else listOf("100", "75", "50", "25", "0")
            val values = if (destination == Destination.Analytics) listOf(46, 58, 52, 71, 87, 93, 96).map { it / 115f }
            else if (mobile) listOf(65, 70, 68, 81, 79, 85, 86).map { it / 93f }
            else listOf(.84f, .82f, .85f, .88f, .91f, .94f, .97f)
            val dates = if (alternateRange) listOf("Nov 1", "Nov 8", "Nov 15", "Nov 21")
            else listOf("Nov 15", "Nov 17", "Nov 19", "Nov 21")
            val data = if (alternateRange) values.map { it * .92f } else values
            DashboardChart(data, ticks, dates, color = color, line = true,
                height = if (destination == Destination.Analytics) Tokens.AnalyticsTicksHeight else Tokens.CompactChartTicksHeight)
        }
    }
}

private fun toolRecords(destination: Destination, count: Int) = List(count) { index ->
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
            val event = when (index) {
                0 -> "/api/search · 2 min ago"
                1 -> "/login · 8 min ago"
                2 -> "/api/catalog · 15 min ago"
                else -> "$page · ${index + 14} min ago"
            }
            ToolRecord(index, action, event, "Request firewall-$number · Production")
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
    Column(Modifier.fillMaxWidth().dashboardScroll().padding(horizontal = Tokens.Inset).padding(top = Tokens.Inset, bottom = Tokens.Space8)) {
        val palette = LocalDashboardPalette.current
        DashboardGroup {
            if (!docsOnly) {
                DashboardRow("Contact Support", "Open Vercel Support in your browser", minHeight = Tokens.HelpActionHeight,
                    trailing = { DashboardIcon(Glyph.Outbound, palette.secondary) })
                DividerLine(Modifier.padding(horizontal = Tokens.Inset), palette.border)
            }
            DashboardRow("Documentation", "Open Vercel Docs in your browser", minHeight = Tokens.HelpActionHeight,
                trailing = { DashboardIcon(Glyph.Outbound, palette.secondary) })
        }
    }
}
