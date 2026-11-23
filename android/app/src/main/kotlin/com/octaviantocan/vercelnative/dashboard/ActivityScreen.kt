package com.octaviantocan.vercelnative.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

internal enum class ActivityTab(val destination: Destination) {
    Recents(Destination.Recents), Usage(Destination.Usage), Alerts(Destination.Alerts),
}

@Composable
internal fun ActivityTabs(selected: ActivityTab, onSelect: (ActivityTab) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalDashboardPalette.current
    BoxWithConstraints(modifier.fillMaxWidth().height(Tokens.Touch).selectableGroup()) {
        val segment = maxWidth / ActivityTab.entries.size
        val underline by animateDpAsState(segment * selected.ordinal, DashboardMotion.move(DashboardMotion.Fast), label = "Activity underline")
        DividerLine(Modifier.align(Alignment.BottomCenter))
        Row(Modifier.fillMaxSize()) {
            ActivityTab.entries.forEach { tab ->
                val active = tab == selected
                Box(
                    Modifier.weight(1f).fillMaxHeight().selectable(active, role = Role.Tab, onClick = { onSelect(tab) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.name, style = GeistText, color = if (active) palette.ink else palette.secondary,
                        fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
        Box(Modifier.align(Alignment.BottomStart).offset { androidx.compose.ui.unit.IntOffset(underline.roundToPx(), 0) }
            .width(segment).height(Tokens.Stroke).background(palette.ink))
    }
}

@Composable
internal fun ActivityScreen(route: DashboardRoute, navigation: DashboardNavigation) {
    val tab = ActivityTab.entries.first { it.destination == route.destination }
    val state = rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize()) {
        DashboardRootHeader("Activity") { navigation.show(Destination.More) }
        ActivityTabs(tab, { navigation.main(it.destination) })
        AnimatedContent(
            route,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                val direction = if (targetState.destination.ordinal > initialState.destination.ordinal) 1 else -1
                (fadeIn(DashboardMotion.move(DashboardMotion.Fast)) +
                    slideInHorizontally(DashboardMotion.move(DashboardMotion.Fast)) { direction * it / 8 }) togetherWith
                    (fadeOut(DashboardMotion.move(DashboardMotion.Quick)) +
                        slideOutHorizontally(DashboardMotion.move(DashboardMotion.Fast)) { -direction * it / 8 })
            },
            label = "Activity tab content",
        ) { shown ->
            state.SaveableStateProvider(shown.destination.name + shown.variant) {
                when (shown.destination) {
                    Destination.Recents -> RecentsList(shown, navigation)
                    Destination.Usage -> UsageScreen(shown, navigation)
                    Destination.Alerts -> AlertsList(shown, navigation)
                    else -> Unit
                }
            }
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
            DashboardEmpty("Quiet on the deploy front.", "New deployments will appear here.", Glyph.Activity,
                illustration = R.drawable.dashboard_figma_recents_empty)
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
        DashboardList(
            Modifier.weight(1f),
            contentPadding = PaddingValues(top = Tokens.RecentsStart, bottom = Tokens.DockScrollClearance),
        ) {
            items(visible, key = Deployment::id) { deployment ->
                val project = navigation.project(deployment.projectId)
                DeploymentRow(
                    deployment, project,
                    onOpen = { navigation.show(Destination.Deployment, deployment.id) },
                    options = {
                        RowOptions(
                            actions = listOf(
                                MenuAction("Open project", Glyph.Project, { navigation.show(Destination.Project, project.id) }, project),
                                MenuAction("Preview", Glyph.MenuPreview, { navigation.show(Destination.Preview, deployment.id) }),
                                MenuAction("Source", Glyph.GitHub, null),
                                MenuAction("Deployment", Glyph.MenuDeployment, { navigation.show(Destination.Deployment, deployment.id) }),
                                MenuAction("Branch Deployments", Glyph.MenuBranch, { navigation.show(Destination.Project, project.id, "branch", deployment.branch) }),
                                MenuAction("Remove", Glyph.MenuRemove, { navigation.show(Destination.RemoveRecent, deployment.id) }),
                            ),
                            label = "Options for ${deployment.title}",
                            expanded = expanded == deployment.id,
                            onOpen = { expanded = deployment.id },
                            onClose = { expanded = null },
                            authored = deployment.elapsed,
                            separated = route.variant == "menu-earlier",
                            recents = true,
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
    val scale = if (workspace.id == DashboardFixtures.studioWorkspace) 1.7 else 1.0
    val resources = DashboardFixtures.resources
    val breakdown = resources.filter(UsageResource::showInBreakdown)
    val credit = if (empty) 0.0 else breakdown.sumOf { it.cost } * scale
    val sections = resources.withIndex().groupBy({ (index, _) ->
        when (index) {
            in 0..1 -> "Vercel Delivery Network"
            in 2..4 -> "Vercel Functions"
            in 5..8, in 12..16 -> "Content, Caching & Optimization"
            11, in 18..19, 23 -> "Build & Deploy"
            else -> "Observability"
        }
    }) { it.value }
    DashboardList(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Tokens.Space24, top = Tokens.AlertTopGap, end = Tokens.Space24, bottom = Tokens.DockScrollClearance),
        verticalArrangement = Arrangement.spacedBy(Tokens.Inset),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Inset)) {
                Text("Pro Plan Usage", fontSize = Tokens.UsageTitle, lineHeight = Tokens.UsageTitleLine, fontWeight = FontWeight.SemiBold)
                val billing = if (period == UsagePeriod.Monthly) "Billing cycle · Nov 1–30 · Through Nov 21" else "Billing cycle · Nov 1–30"
                Text(billing, color = palette.secondary, fontSize = Tokens.AlertProjectSize, lineHeight = Tokens.ListSubtitleLine)
            }
        }
        item {
            val shape = RoundedCornerShape(Tokens.ControlRadius)
            Column(
                Modifier.fillMaxWidth().heightIn(min = Tokens.UsageCreditHeight)
                    .background(palette.control, shape).border(Tokens.Stroke, palette.quietBorder, shape).padding(Tokens.BodyInset),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space8),
            ) {
                Text("Included Credit", color = palette.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine)
                Text("${sampleMoney(credit)} / $20.00", fontSize = Tokens.Title, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium)
                Box(Modifier.fillMaxWidth().height(Tokens.Space4).clip(CircleShape).background(palette.insetSurface)) {
                    if (credit > 0) Box(Modifier.fillMaxWidth((credit / 20).toFloat().coerceAtMost(1f)).fillMaxHeight().background(palette.link))
                }
            }
        }
        if (empty) {
            item { DashboardEmpty("No usage recorded yet", "New consumption appears here once it’s recorded.", Glyph.Usage) }
        } else {
            item { UsageBreakdown(period, { period = it }, breakdown, scale) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                    Text("Product", Modifier.weight(.6f), color = palette.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium)
                    Text("Usage", Modifier.weight(.24f), color = palette.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium)
                    Text("Charge", Modifier.weight(.16f), color = palette.secondary, fontSize = Tokens.Metadata,
                        lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                }
            }
            sections.forEach { (group, values) ->
                item(group) {
                    Column {
                        Text(group, Modifier.fillMaxWidth().background(palette.control).padding(Tokens.Space8),
                            fontSize = Tokens.Metadata, fontWeight = FontWeight.Medium, lineHeight = Tokens.ListSubtitleLine)
                        for (resource in values) {
                            UsageResourceRow(resource, scale, preset, expanded == resource.id) {
                                expanded = if (expanded == resource.id) null else resource.id
                            }
                        }
                    }
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

@Composable
private fun UsageBreakdown(period: UsagePeriod, select: (UsagePeriod) -> Unit, products: List<UsageResource>, scale: Double) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.ControlRadius)
    val daily = period == UsagePeriod.Daily
    val colors = listOf(palette.ready, palette.warning)
    val charges = products.map { it.cost * scale }
    val (dates, gridStep, shares) = when (period) {
        UsagePeriod.Daily -> Triple(
            listOf("Nov 1", "Nov 8", "Nov 15", "Nov 22", "Nov 30"), 1.0,
            listOf(listOf(1.0, 0.0), listOf(0.0, 1.0)),
        )
        UsagePeriod.Weekly -> Triple(
            listOf("Nov 1", "Nov 8", "Nov 15", "Nov 22", "Nov 29"), .20,
            listOf(listOf(1.0 / 6, 3.0 / 13), listOf(1.0 / 3, 1.0 / 13), listOf(1.0 / 6, 4.0 / 13),
                listOf(1.0 / 4, 3.6 / 13), listOf(1.0 / 12, 1.4 / 13)),
        )
        UsagePeriod.Monthly -> Triple(
            listOf("Sep", "Oct", "Nov to date"), if (scale > 1) 1.0 else .50,
            listOf(listOf(5.0 / 6, .65), listOf(1.20, .74), listOf(1.0, 1.0)),
        )
    }
    val intervals = if (daily) 5 else 3
    Column(
        Modifier.fillMaxWidth().heightIn(min = Tokens.UsageChartPanel).border(Tokens.Stroke, palette.quietBorder, shape)
            .padding(Tokens.BodyInset),
    ) {
        Text("Consumption Breakdown", fontSize = Tokens.ListTitle, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().padding(top = Tokens.Space12).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
            UsagePeriod.entries.forEach { choice ->
                val chosen = choice == period
                Box(
                    Modifier.weight(1f).height(Tokens.Space32).clip(RoundedCornerShape(Tokens.AlertLoadingRadius))
                        .then(if (chosen) Modifier.background(palette.selected) else Modifier)
                        .selectable(chosen, role = Role.Tab, onClick = { select(choice) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(choice.name, fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine,
                        color = if (chosen) palette.ink else palette.secondary, fontWeight = if (chosen) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = Tokens.Inset)
            .height(Tokens.UsageChartGrid + if (daily) Tokens.Space12 else Tokens.Space32 + Tokens.Space12)) {
            Column(Modifier.width(if (daily) Tokens.Space24 else Tokens.Space32).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween) {
                (intervals downTo 0).forEach { tick ->
                    Text(if (daily) "$$tick" else sampleMoney(tick * gridStep), fontSize = Tokens.ChartCaption, color = palette.secondary)
                }
            }
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val top = Tokens.Space6.toPx()
                val bottom = size.height - top
                for (i in 0..intervals) {
                    val y = top + (bottom - top) * i / intervals
                    drawLine(palette.quietBorder, Offset(0f, y), Offset(size.width, y), Tokens.Stroke.toPx())
                }
                shares.forEachIndexed { index, bucket ->
                    val position = if (daily) listOf(.10f, .59f)[index] else (index + .5f) / shares.size
                    val bar = (if (period == UsagePeriod.Monthly) Tokens.Space32 else Tokens.Space20).toPx()
                    var y = bottom
                    bucket.forEachIndexed { product, share ->
                        val height = (bottom - top) * (charges[product] * share / (gridStep * intervals)).toFloat()
                        y -= height
                        if (height > 0) drawRoundRect(
                            colors[product],
                            Offset(position * size.width - bar / 2, y),
                            androidx.compose.ui.geometry.Size(bar, height),
                            androidx.compose.ui.geometry.CornerRadius(Tokens.Space2.toPx()),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(if (daily) Tokens.Space32 + Tokens.Space12 else Tokens.Space8))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dates.forEach { date ->
                Text(date, fontSize = Tokens.ChartCaption, lineHeight = Tokens.ListSubtitleLine, color = palette.secondary)
            }
        }
        products.forEachIndexed { index, product ->
            Row(
                Modifier.padding(top = if (index == 0) Tokens.Space12 else Tokens.Space4),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!daily) Box(Modifier.size(Tokens.Space6).background(colors[index], CircleShape))
                Text("${product.name}   ${sampleMoney(charges[index])}", fontSize = Tokens.Caption,
                    lineHeight = Tokens.ListSubtitleLine, color = palette.secondary)
            }
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
    if (treatment == null) {
        Row(Modifier.fillMaxWidth().heightIn(min = Tokens.SearchRow).clickable(onClick = onOpen)
            .padding(horizontal = Tokens.Stroke, vertical = Tokens.Space8), horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
            verticalAlignment = Alignment.CenterVertically) {
            Text(resource.name, Modifier.weight(.6f), fontSize = Tokens.AlertProjectSize, lineHeight = Tokens.ListSubtitleLine)
            Text(quantity, Modifier.weight(.24f), color = palette.secondary, fontSize = Tokens.Caption, maxLines = 1)
            Text(cost, Modifier.weight(.16f), fontSize = Tokens.Metadata, textAlign = TextAlign.End, maxLines = 1)
        }
        if (expanded) {
            KeyValueRow("Recorded usage", quantity)
            KeyValueRow("Estimated charge", cost)
        }
        DividerLine()
        return
    }
    Surface(
        shape = if (treatment.card) RoundedCornerShape(Tokens.ControlRadius) else androidx.compose.ui.graphics.RectangleShape,
        color = if (treatment.card) palette.group else palette.canvas,
    ) {
        Column {
            DashboardRow(
                resource.name,
                subtitle = when {
                    treatment.stacked -> if (treatment.labelOrder) cost else quantity
                    else -> null
                },
                glyph = Glyph.Usage.takeIf { treatment.icon },
                trailing = {
                    if (treatment.stacked) {
                        MutedText(if (treatment.labelOrder) quantity else cost)
                    } else Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                        MutedText(if (treatment.labelOrder) cost else quantity)
                        Text(if (treatment.labelOrder) quantity else cost, fontSize = Tokens.Metadata)
                    }
                },
                onClick = onOpen,
            )
            if (treatment.meter) LinearProgressIndicator(
                progress = { (resource.amount.rem(100.0) / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset, vertical = Tokens.Space4),
            )
            if (expanded) {
                KeyValueRow("Recorded usage", quantity)
                KeyValueRow("Estimated charge", cost)
            }
            if (!treatment.card) DividerLine()
        }
    }
}

@Composable
internal fun AlertsList(route: DashboardRoute, navigation: DashboardNavigation) {
    val alerts = navigation.alerts()
    val palette = LocalDashboardPalette.current
    val padding = PaddingValues(start = Tokens.Space24, end = Tokens.Space24, top = Tokens.AlertTopGap, bottom = Tokens.DockScrollClearance)
    when {
        route.variant == "loading" -> DashboardList(
            Modifier.fillMaxSize().semantics { contentDescription = "Loading Alerts" },
            contentPadding = padding,
        ) {
            items(Tokens.AlertLoadingRows) { index -> AlertSkeleton(index) }
        }
        route.variant == "plan" -> Box(
            Modifier.fillMaxSize().padding(bottom = Tokens.DockScrollClearance + Tokens.Space32),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.dashboard_figma_alerts_plan), null,
                    Modifier.size(Tokens.IllustrationWidth, Tokens.IllustrationHeight))
                Text("Alerts aren’t on this plan", Modifier.padding(top = Tokens.Inset),
                    fontSize = Tokens.Heading, lineHeight = Tokens.PlanTitleLine, fontWeight = FontWeight.SemiBold)
                Text(
                    "Check the current workspace plan for access to Alerts.",
                    Modifier.padding(top = Tokens.Space12, start = Tokens.Space24 * 2, end = Tokens.Space24 * 2),
                    fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectOptionsLine, color = palette.secondary,
                    textAlign = TextAlign.Center,
                )
                GlassAction("View plan", { navigation.main(Destination.Usage) },
                    Modifier.padding(top = Tokens.Space20).width(Tokens.AlertPlanButton))
            }
        }
        alerts.isEmpty() || route.variant == "empty" -> Box(
            Modifier.fillMaxSize().padding(bottom = Tokens.BottomBar + Tokens.Inset),
            contentAlignment = Alignment.Center,
        ) {
            DashboardEmpty("All quiet here.", "There are no alerts in this workspace.", Glyph.Activity,
                illustration = R.drawable.dashboard_figma_alerts_empty)
        }
        else -> DashboardList(Modifier.fillMaxSize(), contentPadding = padding) {
            items(alerts, key = DashboardAlert::id) { alert ->
                val project = navigation.project(alert.projectId)
                AlertInboxRow(alert, project, navigation.projectName(project)) {
                    navigation.show(Destination.Alert, alert.id)
                }
            }
        }
    }
}

@Composable
private fun AlertInboxRow(alert: DashboardAlert, project: Project, projectName: String, onClick: () -> Unit) {
    val palette = LocalDashboardPalette.current
    Column(Modifier.fillMaxWidth().heightIn(min = Tokens.AlertRow).clickable(role = Role.Button, onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(top = Tokens.Space12),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
        ) {
            Box(
                Modifier.size(Tokens.AlertMark).clip(RoundedCornerShape(Tokens.ProjectTileRadius)).background(palette.projectBadge),
                contentAlignment = Alignment.Center,
            ) { ProjectArtwork(project, Tokens.AlertMark, Tokens.MetadataIcon) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.AlertTextGap)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                    Text(projectName, Modifier.weight(1f), fontSize = Tokens.AlertProjectSize, lineHeight = Tokens.ListSubtitleLine,
                        fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(alert.elapsed, fontSize = Tokens.AlertTimeSize, lineHeight = Tokens.ListSubtitleLine, color = palette.secondary, maxLines = 1)
                }
                Text(alert.title, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(alert.summary, fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine, color = palette.secondary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        DividerLine(Modifier.padding(top = Tokens.Space8, bottom = Tokens.Space12), palette.insetSurface)
    }
}

@Composable
private fun AlertSkeleton(index: Int) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.AlertLoadingRadius)
    Column(Modifier.fillMaxWidth().height(Tokens.AlertRow)) {
        Row(Modifier.fillMaxWidth().padding(top = Tokens.Inset), horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
            Box(Modifier.size(Tokens.AlertMark).background(palette.selected, RoundedCornerShape(Tokens.ProjectTileRadius)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space12 + Tokens.Space2)) {
                Box(Modifier.fillMaxWidth().height(Tokens.Space8)) {
                    Box(Modifier.fillMaxWidth(if (index % 3 == 2) .60f else .20f).fillMaxHeight().background(palette.selected, shape))
                    Box(Modifier.align(Alignment.CenterEnd).width(Tokens.AlertSkeletonTime).fillMaxHeight().background(palette.selected, shape))
                }
                Box(Modifier.fillMaxWidth(if (index % 2 == 0) .39f else .52f).height(Tokens.Space8).background(palette.selected, shape))
                Box(Modifier.fillMaxWidth(.51f + .05f * (index % 4)).height(Tokens.Space8).background(palette.selected, shape))
            }
        }
        Spacer(Modifier.weight(1f))
        DividerLine(Modifier.padding(bottom = Tokens.Space12), palette.insetSurface)
    }
}

@Composable
internal fun AlertScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val alert = route.target?.let(DashboardFixtures::alert) ?: navigation.alerts().firstOrNull()
    val palette = LocalDashboardPalette.current
    val project = alert?.let { navigation.project(it.projectId) }
    Column(Modifier.fillMaxSize()) {
        DashboardHeader(
            "Alert details", onBack, subtitle = project?.let(navigation::projectName),
            modifier = Modifier.padding(top = Tokens.AlertDetailHeaderInset), titleSize = Tokens.DetailTitle,
            onSubtitleClick = project?.let { { navigation.show(Destination.Project, it.id) } },
        )
        if (alert == null || project == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DashboardEmpty("All quiet here.", "There are no alerts in this workspace.", Glyph.Activity,
                    illustration = R.drawable.dashboard_figma_alerts_empty)
            }
            return
        }
        val accent = if (alert.kind.isUsage) palette.warning else palette.error
        val shade = if (alert.kind.isUsage) palette.warningSurface else palette.errorSurface
        val shape = RoundedCornerShape(Tokens.ControlRadius)
        DashboardList(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Tokens.Inset, end = Tokens.Inset, top = Tokens.Space24, bottom = Tokens.DockScrollClearance),
            verticalArrangement = Arrangement.spacedBy(Tokens.Inset),
        ) {
            item {
                Column(Modifier.padding(horizontal = Tokens.Space8)) {
                    Row(
                        Modifier.heightIn(min = Tokens.AlertPillHeight).background(shade, CircleShape).padding(horizontal = Tokens.Space8),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space6),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(Tokens.Space6).background(accent, CircleShape))
                        Text("${alert.severity} severity", color = accent, fontSize = Tokens.Caption, lineHeight = Tokens.AlertPillLine,
                            fontWeight = FontWeight.Medium)
                    }
                    Text(alert.title, Modifier.padding(top = Tokens.Space8 + Tokens.Space2, bottom = Tokens.Space8),
                        fontSize = Tokens.AlertDetailTitle, lineHeight = Tokens.AlertDetailTitleLine, fontWeight = FontWeight.SemiBold)
                    Text("Detected ${alert.elapsed}", color = palette.secondary, fontSize = Tokens.AlertProjectSize, lineHeight = Tokens.SearchDetailLine)
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Tokens.AlertMetricsHeight)
                        .background(palette.control, shape).border(Tokens.Stroke, palette.quietBorder, shape)
                        .padding(horizontal = Tokens.AlertMeasurePad, vertical = Tokens.Space12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlertMetric(alert.kind.metric, sampleNumber(alert.value) + alert.kind.unit, Modifier.weight(1f), accent)
                    Box(Modifier.height(Tokens.Touch).width(Tokens.Stroke).background(palette.quietBorder))
                    AlertMetric(
                        if (alert.kind == AlertKind.Monitor) "Threshold" else "Baseline",
                        sampleNumber(alert.baseline) + alert.kind.unit,
                        Modifier.weight(1f).padding(start = Tokens.AlertMeasurePad), palette.ink,
                    )
                }
            }
            item { AlertTrend(alert, accent) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                    Text("Alert context", Modifier.padding(horizontal = Tokens.AlertMeasurePad),
                        fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine, fontWeight = FontWeight.Medium, color = palette.secondary)
                    Column(Modifier.fillMaxWidth().clip(shape).background(palette.group).padding(horizontal = Tokens.Inset)) {
                        AlertContext(alert.kind.resourceLabel, alert.resource)
                        DividerLine()
                        AlertContext("Environment", "Production")
                    }
                    GlassAction(if (alert.kind.isUsage) "View usage" else "View logs", {
                        if (alert.kind.isUsage) navigation.show(Destination.Usage)
                        else navigation.show(Destination.Logs, project.id)
                    }, Modifier.fillMaxWidth().padding(top = Tokens.Space4))
                }
            }
        }
    }
}

@Composable
private fun AlertMetric(label: String, value: String, modifier: Modifier, ink: androidx.compose.ui.graphics.Color) {
    val palette = LocalDashboardPalette.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Tokens.AlertTextGap)) {
        Text(label, fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine, color = palette.secondary)
        Text(value, fontSize = Tokens.AlertDetailTitle, lineHeight = Tokens.AlertMetricLine, fontWeight = FontWeight.Medium, color = ink)
    }
}

@Composable
private fun AlertContext(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = Tokens.AlertContextRow), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = Tokens.Body, lineHeight = Tokens.ProjectOptionsLine)
        Text(value, fontSize = Tokens.AlertProjectSize, lineHeight = Tokens.ListSubtitleLine, color = LocalDashboardPalette.current.secondary)
    }
}

@Composable
private fun AlertTrend(alert: DashboardAlert, accent: androidx.compose.ui.graphics.Color) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.ControlRadius)
    val kind = alert.kind
    val step = kotlin.math.ceil(alert.value / (kind.chartStep * 3)).toInt() * kind.chartStep
    Column(
        Modifier.fillMaxWidth().heightIn(min = Tokens.AlertChartHeight).border(Tokens.Stroke, palette.insetSurface, shape)
            .padding(horizontal = Tokens.AlertMeasurePad, vertical = Tokens.Space12),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space8),
    ) {
        Text(kind.chartTitle, fontSize = Tokens.Body, lineHeight = Tokens.ProjectOptionsLine, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().height(Tokens.AlertPlotHeight)) {
            Column(Modifier.width(if (kind == AlertKind.Transfer) Tokens.Touch else Tokens.Space32).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween) {
                (3 downTo 0).forEach { level ->
                    val label = "${level * step}${kind.unit}"
                    Text(label, fontSize = Tokens.Caption, color = palette.secondary)
                }
            }
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val gridHeight = size.height * .88f
                val gridTop = (size.height - gridHeight) / 2
                for (i in 0..3) {
                    val y = gridTop + gridHeight * i / 3
                    drawLine(palette.quietBorder, Offset(0f, y), Offset(size.width, y), Tokens.Stroke.toPx())
                }
                val samples = when (kind) {
                    AlertKind.Cdn, AlertKind.Transfer -> listOf(-.35, -.35, -.15, .0, .08, .35, .74, 1.0, .66, .43)
                    AlertKind.Monitor -> listOf(-.40, -.36, -.32, -.25, .12, 1.0, .86, .68, .19, .08)
                    AlertKind.Function -> listOf(.04, .08, .07, .16, 1.0, .93, .73, .53, .42, .28)
                }
                val plot = Path()
                samples.forEachIndexed { i, value ->
                    val x = size.width * i / samples.lastIndex
                    val measurement = alert.baseline + (alert.value - alert.baseline) * value
                    val y = gridTop + gridHeight * (1f - (measurement / (step * 3)).toFloat())
                    if (i == 0) plot.moveTo(x, y) else plot.lineTo(x, y)
                }
                drawPath(plot, accent, style = Stroke(Tokens.Space2.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("09:10", "09:20", "09:30", "09:40").forEach { label ->
                Text(label, fontSize = Tokens.ChartCaption, color = palette.secondary, fontFamily = GeistMono)
            }
        }
    }
}
