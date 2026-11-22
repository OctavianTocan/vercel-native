package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

internal enum class ActivityTab(val destination: Destination) {
    Recents(Destination.Recents), Usage(Destination.Usage), Alerts(Destination.Alerts),
}

@Composable
internal fun ActivityTabs(selected: ActivityTab, onSelect: (ActivityTab) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalDashboardPalette.current
    Row(modifier.fillMaxWidth().height(Tokens.Touch).selectableGroup()) {
        ActivityTab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                Modifier.weight(1f).fillMaxHeight().selectable(active, role = Role.Tab, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(tab.name, fontSize = Tokens.Body, color = if (active) palette.ink else palette.secondary)
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .height(Tokens.Stroke)
                        .background(if (active) palette.ink else palette.quietBorder)
                )
            }
        }
    }
}

@Composable
internal fun ActivityScreen(route: DashboardRoute, navigation: DashboardNavigation) {
    val tab = ActivityTab.entries.first { it.destination == route.destination }
    Column(Modifier.fillMaxSize()) {
        DashboardHeader("Activity", { navigation.show(Destination.More) }, Glyph.Menu)
        ActivityTabs(tab, { navigation.main(it.destination) })
        when (tab) {
            ActivityTab.Recents -> RecentsList(route, navigation)
            ActivityTab.Usage -> UsageScreen(route, navigation)
            ActivityTab.Alerts -> AlertsList(route, navigation)
        }
    }
}

@Composable
private fun RecentsList(route: DashboardRoute, navigation: DashboardNavigation) {
    val recent = remember(navigation.projects()) {
        navigation.projects().flatMap { DashboardFixtures.deployments(it.id).take(5) }
            .sortedBy(Deployment::ageHours)
            .take(72)
    }
    val sampleRemovedId = DashboardFixtures.defaultDeployment.takeIf { route.variant == "removed" }
    val visible = recent.filterNot { it.id in navigation.removedRecents || it.id == sampleRemovedId }
    var expanded by rememberSaveable {
        mutableStateOf(visible.firstOrNull()?.id.takeIf { route.variant in setOf("recents-menu", "menu-spacing", "menu-earlier") })
    }
    if (route.variant == "empty") {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DashboardEmpty("Quiet on the deploy front.", "New deployments will appear here.", Glyph.Activity)
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        val removed = sampleRemovedId ?: navigation.removedRecents.lastOrNull()
        if (removed != null) DashboardNotice(
            "Removed from Recents",
            Modifier.padding(horizontal = Tokens.Inset, vertical = Tokens.Space6),
            Tone.Success,
        )
        LazyColumn(Modifier.weight(1f)) {
            items(visible, key = Deployment::id) { deployment ->
                val project = navigation.project(deployment.projectId)
                DeploymentRow(
                    deployment, project,
                    onOpen = { navigation.show(Destination.Deployment, deployment.id) },
                    options = {
                        RowOptions(
                            actions = listOf(
                                MenuAction("Open project", Glyph.Project, { navigation.show(Destination.Project, project.id) }, project),
                                MenuAction("Preview", Glyph.Globe, { navigation.show(Destination.Preview, deployment.id) }),
                                MenuAction("Source", Glyph.Branch, null),
                                MenuAction("Deployment", Glyph.Project, { navigation.show(Destination.Deployment, deployment.id) }),
                                MenuAction("Branch Deployments", Glyph.Branch, { navigation.show(Destination.Project, project.id, "branch", deployment.branch) }),
                                MenuAction("Remove", Glyph.Close, { navigation.show(Destination.RemoveRecent, deployment.id) }),
                            ),
                            label = "Options for ${deployment.title}",
                            expanded = expanded == deployment.id,
                            onOpen = { expanded = deployment.id },
                            onClose = { expanded = null },
                            separated = route.variant == "menu-earlier",
                        )
                    },
                )
            }
        }
    }
}

private enum class UsagePeriod { Daily, Weekly, Monthly }

@Composable
internal fun UsageScreen(route: DashboardRoute, navigation: DashboardNavigation) {
    val palette = LocalDashboardPalette.current
    val workspace = if (route.variant == "studio") DashboardFixtures.workspace(DashboardFixtures.studioWorkspace) else navigation.currentWorkspace
    var period by rememberSaveable {
        mutableStateOf(when (route.variant) {
            "weekly" -> UsagePeriod.Weekly
            "monthly" -> UsagePeriod.Monthly
            else -> UsagePeriod.Daily
        })
    }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val preset = route.detail?.toIntOrNull()?.takeIf { route.variant == "study" }
    if (route.variant == "error") {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DashboardEmpty("Couldn’t load usage", "Try again to load this billing cycle.", Glyph.Usage, action = {
                navigation.main(Destination.Usage)
            })
        }
        return
    }
    val empty = route.variant == "empty"
    val scale = (period.ordinal + 1) * if (workspace.id == DashboardFixtures.studioWorkspace) 1.7 else 1.0
    val resources = DashboardFixtures.resources
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(Tokens.Inset),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space8),
    ) {
        item {
            Text("Pro Plan Usage", fontSize = Tokens.Metric, fontWeight = FontWeight.SemiBold)
            MutedText("${workspace.name} · Billing cycle · Nov 1–30", Modifier.padding(top = Tokens.Space4))
        }
        item {
            Surface(shape = RoundedCornerShape(Tokens.CardRadius), color = palette.group) {
                Column(Modifier.fillMaxWidth().padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
                    MutedText("Included credit")
                    Text(if (empty) "$0.00 / $20.00" else "${sampleMoney(1.25 * scale)} / $20.00", fontSize = Tokens.Title)
                    if (!empty) LinearProgressIndicator(progress = { (1.25 * scale / 20).toFloat() }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        if (empty) {
            item { DashboardEmpty("No usage recorded yet", "New consumption appears here once it’s recorded.", Glyph.Usage) }
        } else {
            item {
                Surface(shape = RoundedCornerShape(Tokens.CardRadius), color = palette.group) {
                    Column(Modifier.padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                        Text("Consumption breakdown", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
                            UsagePeriod.entries.forEach { choice ->
                                Surface(
                                    onClick = { period = choice },
                                    modifier = Modifier.weight(1f).height(Tokens.IconSlot),
                                    color = if (period == choice) palette.selected else palette.group,
                                    shape = CircleShape,
                                ) { Box(contentAlignment = Alignment.Center) { MutedText(choice.name) } }
                            }
                        }
                        DashboardChart(List(14) { index -> (4 + (index * 19 + period.ordinal * 11) % 63) / 73f })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MutedText("Nov 1")
                            MutedText("Nov 15")
                            MutedText("Nov 30")
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionLabel("Product")
                    SectionLabel("Usage          Charge")
                }
            }
            items(resources, key = UsageResource::id) { resource ->
                UsageResourceRow(resource, scale, preset, expanded == resource.id) {
                    expanded = if (expanded == resource.id) null else resource.id
                }
            }
        }
        item {
            SectionLabel("Totals")
            KeyValueRow("Subscription licenses", "$9.98")
            KeyValueRow("Infrastructure", sampleMoney(if (empty) 0.0 else resources.sumOf { it.cost } * scale))
            KeyValueRow("Total", sampleMoney(9.98 + if (empty) 0.0 else resources.sumOf { it.cost } * scale))
        }
    }
}

private data class UsageTreatment(val icon: Boolean, val card: Boolean, val labelOrder: Boolean, val meter: Boolean, val stacked: Boolean)

private val usageTreatments = listOf(
    UsageTreatment(true, false, false, false, false),
    UsageTreatment(true, false, true, true, false),
    UsageTreatment(true, true, true, false, false),
    UsageTreatment(true, true, false, true, false),
    UsageTreatment(false, true, true, true, false),
    UsageTreatment(true, false, false, true, true),
    UsageTreatment(false, false, false, true, false),
    UsageTreatment(false, true, false, false, false),
    UsageTreatment(true, true, false, false, true),
    UsageTreatment(true, false, true, false, true),
    UsageTreatment(true, false, true, true, true),
    UsageTreatment(false, false, true, false, true),
    UsageTreatment(true, true, true, true, false),
    UsageTreatment(true, true, false, true, true),
    UsageTreatment(false, false, true, true, true),
    UsageTreatment(false, true, false, true, false),
    UsageTreatment(true, true, true, false, true),
    UsageTreatment(false, true, true, true, true),
    UsageTreatment(true, false, false, true, false),
    UsageTreatment(false, false, false, false, true),
)

@Composable
private fun UsageResourceRow(resource: UsageResource, scale: Double, preset: Int?, expanded: Boolean, onOpen: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val treatment = preset?.let(usageTreatments::getOrNull)
    val quantity = "${sampleNumber(resource.amount * scale)} ${resource.unit}"
    val cost = sampleMoney(resource.cost * scale)
    Surface(
        shape = if (treatment?.card == true) RoundedCornerShape(Tokens.ControlRadius) else androidx.compose.ui.graphics.RectangleShape,
        color = if (treatment?.card == true) palette.group else palette.canvas,
    ) {
        Column {
            DashboardRow(
                resource.name,
                subtitle = when {
                    treatment?.stacked == true -> if (treatment.labelOrder) cost else quantity
                    else -> null
                },
                glyph = Glyph.Usage.takeIf { treatment?.icon == true },
                trailing = {
                    if (treatment?.stacked == true) {
                        MutedText(if (treatment.labelOrder) quantity else cost)
                    } else Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                        MutedText(if (treatment?.labelOrder == true) cost else quantity)
                        Text(if (treatment?.labelOrder == true) quantity else cost, fontSize = Tokens.Metadata)
                    }
                },
                onClick = onOpen,
            )
            if (treatment?.meter == true) LinearProgressIndicator(
                progress = { (resource.amount.rem(100.0) / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset, vertical = Tokens.Space4),
            )
            if (expanded) {
                KeyValueRow("Recorded usage", quantity)
                KeyValueRow("Estimated charge", cost)
            }
            if (treatment?.card != true) DividerLine()
        }
    }
}

@Composable
private fun AlertsList(route: DashboardRoute, navigation: DashboardNavigation) {
    if (route.variant in setOf("loading", "empty", "plan")) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (route.variant) {
                "loading" -> DashboardEmpty("Loading Alerts", "Getting alerts for this workspace.", Glyph.Activity, loading = true)
                "empty" -> DashboardEmpty("All quiet here.", "There are no alerts in this workspace.", Glyph.Activity)
                else -> DashboardEmpty("Alerts aren’t on this plan", "Check the current workspace plan for alerts.", Glyph.Warn, action = {
                    navigation.main(Destination.Usage)
                }, actionLabel = "View plan")
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(DashboardFixtures.alerts(navigation.workspaceId), key = DashboardAlert::id) { alert ->
            val project = navigation.project(alert.projectId)
            DashboardRow(
                navigation.projectName(project), "${alert.kind.label} · ${alert.elapsed}",
                leading = { ProjectMark(project) },
                onClick = { navigation.show(Destination.Alert, alert.id) },
            )
            DividerLine(Modifier.padding(horizontal = Tokens.Inset))
        }
    }
}

@Composable
internal fun AlertScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val alert = route.target?.let(DashboardFixtures::alert) ?: DashboardFixtures.alerts(navigation.workspaceId).first()
    val project = navigation.project(alert.projectId)
    val title = when (alert.kind) {
        AlertKind.Function -> "Function error spike"
        AlertKind.Cdn -> "CDN requests above baseline"
        AlertKind.Monitor -> "Checkout error threshold crossed"
    }
    Column(Modifier.fillMaxSize()) {
        DashboardHeader("Alert details", onBack, subtitle = navigation.projectName(project))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
            item {
                StatusPill(if (alert.kind == AlertKind.Cdn) "Medium severity" else "High severity",
                    if (alert.kind == AlertKind.Cdn) Tone.Warning else Tone.Error)
                Text(title, Modifier.padding(vertical = Tokens.Space8), fontSize = Tokens.Heading, fontWeight = FontWeight.SemiBold)
                MutedText("Detected ${alert.elapsed}")
            }
            item {
                KeyValueRow(alert.kind.metric, alert.value)
                KeyValueRow(if (alert.kind == AlertKind.Monitor) "Threshold" else "Baseline", alert.threshold)
                DashboardChart(listOf(.18f, .27f, .31f, .39f, .74f, .61f, .82f, .88f, .79f))
            }
            item {
                DashboardRow("What happened", alert.detail)
                DashboardRow("Open project", glyph = Glyph.Project, onClick = { navigation.show(Destination.Project, project.id) })
                if (alert.kind == AlertKind.Cdn) DashboardRow("View usage", glyph = Glyph.Usage, onClick = { navigation.main(Destination.Usage) })
                else DashboardRow("View logs", glyph = Glyph.Activity, onClick = { navigation.show(Destination.Logs, project.id) })
            }
        }
    }
}
