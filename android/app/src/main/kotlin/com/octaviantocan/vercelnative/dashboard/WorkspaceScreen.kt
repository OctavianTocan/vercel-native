package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

private val workspaceRoutes = listOf(
    Destination.Observability to Glyph.Activity,
    Destination.Logs to Glyph.Activity,
    Destination.Analytics to Glyph.Usage,
    Destination.Speed to Glyph.Refresh,
    Destination.Firewall to Glyph.Warn,
    Destination.WorkspaceDomains to Glyph.Globe,
    Destination.Integrations to Glyph.Settings,
    Destination.Help to Glyph.Help,
)

@Composable
internal fun WorkspaceRail(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    var failed by rememberSaveable { mutableStateOf(route.variant == "workspace-failed") }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().background(palette.canvas.copy(alpha = Tokens.ScrimOpacity)).clickable(onClick = onBack))
        Surface(
            Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(Tokens.DrawerFraction)
                .widthIn(max = Tokens.DrawerWidth),
            color = palette.sheet,
            shape = RoundedCornerShape(topEnd = Tokens.SheetRadius, bottomEnd = Tokens.SheetRadius),
        ) {
            Row(Modifier.statusBarsPadding().navigationBarsPadding()) {
                Column(
                    Modifier.fillMaxHeight().width(Tokens.TopBar).background(palette.canvas.copy(alpha = Tokens.RailOpacity))
                        .padding(top = Tokens.Space12),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space8),
                ) {
                    for (workspace in DashboardFixtures.workspaces) {
                        val selected = workspace.id == navigation.workspaceId
                        val border = if (selected) palette.ink else palette.border
                        Surface(
                            onClick = { navigation.chooseWorkspace(workspace.id) },
                            shape = CircleShape,
                            color = if (selected) palette.selected else palette.input,
                            modifier = Modifier.size(Tokens.Filter).border(Tokens.Stroke, border, CircleShape),
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(workspace.name.take(1), fontWeight = FontWeight.Medium) }
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight().padding(vertical = Tokens.Space8)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        SectionLabel(navigation.currentWorkspace.name, Modifier.padding(horizontal = Tokens.Inset))
                        for ((destination, glyph) in workspaceRoutes) {
                            DashboardRow(destination.label, glyph = glyph, trailing = {}, onClick = {
                                navigation.show(destination)
                            })
                        }
                        if (failed) DashboardNotice("Can’t access Studio", tone = Tone.Error,
                            retry = { failed = false; navigation.chooseWorkspace(DashboardFixtures.studioWorkspace) })
                    }
                    DividerLine()
                    DashboardRow("Agent", glyph = Glyph.Agent, trailing = {}, onClick = { navigation.show(Destination.Agent) })
                    DashboardRow("Theme", glyph = Glyph.Theme, trailing = {}, onClick = { navigation.show(Destination.Theme) })
                    DashboardRow("Account Settings", glyph = Glyph.Settings, trailing = {}, onClick = { navigation.show(Destination.Profile) })
                }
            }
        }
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
            DashboardRow(it.first.label, glyph = it.second, onClick = { navigation.show(it.first) })
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
internal fun HelpSheet(onBack: () -> Unit) {
    DashboardHeader("Help", onBack)
    Column(Modifier.padding(Tokens.Inset)) {
        DashboardRow("Contact Support", "Vercel Support", glyph = Glyph.Help)
        DividerLine()
        DashboardRow("Documentation", "Vercel Docs", glyph = Glyph.Globe)
    }
}
