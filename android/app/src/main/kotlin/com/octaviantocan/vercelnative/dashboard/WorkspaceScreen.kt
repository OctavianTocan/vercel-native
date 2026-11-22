package com.octaviantocan.vercelnative.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    var failed by rememberSaveable { mutableStateOf(route.variant == "workspace-failed") }
    BoxWithConstraints(Modifier.fillMaxSize()) {
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
                WorkspaceSwitcher(navigation, backdrop)
                Box(Modifier.fillMaxHeight().width(Tokens.Stroke).background(palette.insetSurface))
                Box(Modifier.weight(1f).fillMaxHeight().dashboardBackdrop(backdrop).background(palette.sidebarPanel)) {
                    Column(
                        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                            .padding(bottom = Tokens.Space20),
                    ) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = Tokens.WorkspacePanelTop)) {
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
private fun WorkspaceSwitcher(navigation: DashboardNavigation, backdrop: GraphicsLayer) {
    val palette = LocalDashboardPalette.current
    Column(
        Modifier.fillMaxHeight().width(Tokens.WorkspaceRailWidth - Tokens.Stroke)
            .dashboardBackdrop(backdrop).background(palette.workspaceRail)
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(top = Tokens.WorkspaceBadgeTop).selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Tokens.WorkspaceBadgeGap),
        ) {
            for (workspace in DashboardFixtures.workspaces) {
                val selected = workspace.id == navigation.workspaceId
                Box(Modifier.fillMaxWidth().height(Tokens.WorkspaceBadgeSize)) {
                    if (selected) Box(
                        Modifier.align(Alignment.CenterStart)
                            .size(Tokens.WorkspaceSelectionWidth, Tokens.WorkspaceSelectionHeight)
                            .background(palette.glassInk, RoundedCornerShape(Tokens.WorkspaceSelectionRadius)),
                    )
                    Box(
                        Modifier.align(Alignment.Center).size(Tokens.WorkspaceBadgeSize).clip(CircleShape)
                            .background(palette.selected)
                            .then(if (selected) Modifier.border(Tokens.Stroke, palette.glassInk, CircleShape) else Modifier)
                            .selectable(selected, role = Role.RadioButton) { navigation.chooseWorkspace(workspace.id) }
                            .semantics { contentDescription = workspace.name },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            workspace.name.take(1), Modifier.clearAndSetSemantics {}, style = GeistText, color = palette.muted,
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
    var query by rememberSaveable { mutableStateOf("") }
    val domains = navigation.projects().flatMap { DashboardFixtures.domains(it.id).take(3) }.filter {
        it.hostname.contains(query, ignoreCase = true)
    }
    Column(Modifier.fillMaxSize()) {
        DashboardHeader("Domains", onBack, subtitle = navigation.currentWorkspace.name)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset), singleLine = true,
            placeholder = { Text("Search domains") }, leadingIcon = { DashboardIcon(Glyph.Search) })
        LazyColumn(Modifier.fillMaxSize()) {
            item { SectionLabel("Domains in this workspace", Modifier.padding(horizontal = Tokens.Inset)) }
            items(domains, key = ProjectDomain::id) { DomainRow(it, navigation, showProject = true) }
        }
    }
}

@Composable
internal fun ObservabilityScreen(navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Tokens.Space12)) {
        item { DashboardHeader("Observability", onBack, subtitle = navigation.currentWorkspace.name) }
        item {
            Column(Modifier.padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                StatusPill("Last 24 hours")
                Text("Production traffic", fontSize = Tokens.Heading, fontWeight = FontWeight.SemiBold)
                Surface(color = palette.group, shape = RoundedCornerShape(Tokens.CardRadius)) {
                    Column(Modifier.padding(Tokens.Space12)) {
                        KeyValueRow("Requests", "18.4k")
                        KeyValueRow("Error rate", "0.27%")
                        DashboardChart(List(18) { index -> (7 + index * 17 % 74) / 90f })
                    }
                }
                SectionLabel("Investigate")
            }
        }
        items(workspaceRoutes.filter { (dest, _) -> dest in setOf(Destination.Logs, Destination.Analytics, Destination.Speed) }, key = { it.first.name }) {
            DashboardRow(it.first.label, leading = { DashboardArtwork(it.second, Tokens.Inset, palette.muted) },
                onClick = { navigation.show(it.first) })
        }
    }
}

private data class ToolRecord(val id: Int, val title: String, val subtitle: String, val detail: String)

@Composable
internal fun WorkspaceToolScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val destination = route.destination
    val scope = route.target?.let(DashboardFixtures::workspace) ?: navigation.currentWorkspace
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    var empty by rememberSaveable { mutableStateOf(route.variant == "empty") }
    val restricted = route.variant == "restricted"
    Column(Modifier.fillMaxSize()) {
        DashboardHeader(destination.label, onBack, subtitle = scope.name)
        when {
            empty -> DashboardEmpty("No visitors in this period", "Try a different range to view traffic.", Glyph.Usage,
                modifier = Modifier.weight(1f), action = { empty = false }, actionLabel = "Change time range")
            restricted -> DashboardEmpty("Firewall access required", "Ask a workspace owner to grant you access.", Glyph.Warn,
                modifier = Modifier.weight(1f), action = onBack, actionLabel = "Go back")
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = Tokens.Space12)) {
                if (destination != Destination.Integrations) item {
                    val (heading, metric, description) = when (destination) {
                        Destination.Analytics -> Triple("Traffic", "1,284", "Visitors")
                        Destination.Speed -> Triple("Real Experience Score", "92", "Mobile score")
                        else -> Triple("Request activity", "18.4k", "Allowed requests")
                    }
                    Column(Modifier.padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                        StatusPill("Last 7 days")
                        Text(heading, fontSize = Tokens.Heading, fontWeight = FontWeight.SemiBold)
                        Text(metric, fontSize = Tokens.Metric, fontWeight = FontWeight.SemiBold)
                        MutedText(description)
                        DashboardChart(List(17) { index -> (18 + index * 21 % 68) / 95f })
                    }
                }
                item {
                    val label = when (destination) {
                        Destination.Analytics -> "Top pages"
                        Destination.Speed -> "Web Vitals · 75th percentile"
                        Destination.Firewall -> "Recent events"
                        else -> "Installed integrations"
                    }
                    SectionLabel(label, Modifier.padding(horizontal = Tokens.Inset))
                }
                items(toolRecords(destination), key = ToolRecord::id) { record ->
                    DashboardRow(
                        record.title, record.subtitle,
                        glyph = if (destination == Destination.Integrations) Glyph.Settings else Glyph.Activity,
                        onClick = { expanded = if (expanded == record.id) null else record.id },
                    )
                    if (expanded == record.id) MutedText(record.detail, Modifier.padding(horizontal = Tokens.Inset, vertical = Tokens.Space8))
                    DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                }
            }
        }
    }
}

private fun toolRecords(destination: Destination) = List(27) { index ->
    val page = listOf("/", "/components", "/docs", "/checkout", "/catalog")[index % 5]
    val number = index + 1
    when (destination) {
        Destination.Analytics -> ToolRecord(index, if (index < 5) page else "$page/$number", "${(1284 - index * 39)} visitors", "${1850 - index * 42} page views")
        Destination.Speed -> {
            val name = listOf("Largest Contentful Paint", "Interaction to Next Paint", "Cumulative Layout Shift")[index % 3]
            ToolRecord(index, name, "$page · Sample $number", "${80 + index * 7} ms")
        }
        Destination.Firewall -> {
            val action = listOf("Blocked request", "Challenge issued", "Allowed request")[index % 3]
            ToolRecord(index, action, "$page · $number min ago", "Request firewall-$number · Production")
        }
        else -> {
            val integration = listOf("Error monitor", "Cache provider", "Workspace notifications", "Analytics export")[index % 4]
            ToolRecord(index, "$integration $number", if (index % 5 == 2) "Needs reconnection" else "Connected", "Installed in the ${if (index % 2 == 0) "workspace" else "project"} scope.")
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
