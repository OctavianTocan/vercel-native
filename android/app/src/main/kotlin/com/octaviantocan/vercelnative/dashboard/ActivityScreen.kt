package com.octaviantocan.vercelnative.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlinx.coroutines.delay

internal enum class ActivityTab(val destination: Destination) {
    Recents(Destination.Recents), Usage(Destination.Usage), Alerts(Destination.Alerts),
}

@Composable
internal fun ActivityTabs(selected: ActivityTab, onSelect: (ActivityTab) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalDashboardPalette.current
    BoxWithConstraints(modifier.fillMaxWidth().height(Tokens.Touch).selectableGroup()) {
        val segment = maxWidth / ActivityTab.entries.size
        val underline by animateDpAsState(segment * selected.ordinal, DashboardMotion.prototype(DashboardMotion.Tabs), label = "Activity underline")
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
                (fadeIn(DashboardMotion.prototype(DashboardMotion.Tabs)) +
                    slideInHorizontally(DashboardMotion.prototype(DashboardMotion.Tabs)) { direction * it / 8 }) togetherWith
                    (fadeOut(DashboardMotion.prototype(DashboardMotion.Tabs)) +
                        slideOutHorizontally(DashboardMotion.prototype(DashboardMotion.Tabs)) { -direction * it / 8 })
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
        mutableStateOf(when (route.variant) {
            "menu-earlier" -> visible.getOrNull(1)?.id
            "recents-menu", "menu-spacing" -> visible.firstOrNull()?.id
            else -> null
        })
    }
    if (route.variant == "empty") {
        Box(Modifier.fillMaxSize().padding(bottom = Tokens.BottomBar + Tokens.Space8), contentAlignment = Alignment.Center) {
            DashboardEmpty("Quiet on the deploy front.", "New deployments will appear here.", Glyph.Activity,
                illustration = R.drawable.dashboard_figma_recents_empty, descriptionGap = Tokens.Space4)
        }
        return
    }
    val removed = sampleRemovedId ?: navigation.removedRecents.lastOrNull()
    var showRemoval by rememberSaveable(removed) { mutableStateOf(removed != null) }
    LaunchedEffect(removed) {
        if (removed != null && sampleRemovedId == null && showRemoval) {
            delay(Tokens.RecentsFeedbackMillis)
            showRemoval = false
        }
    }
    Box(Modifier.fillMaxSize()) {
        DashboardList(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = Tokens.RecentsStart, bottom = Tokens.DockScrollClearance),
        ) {
            items(visible, key = Deployment::id) { deployment ->
                val project = navigation.project(deployment.projectId)
                val earlier = route.variant == "menu-earlier"
                DeploymentRow(
                    deployment, project,
                    onOpen = { navigation.show(Destination.Deployment, deployment.id) },
                    options = {
                        val projectAction = MenuAction(if (earlier) "Project" else "Open project", Glyph.Project,
                            { navigation.show(Destination.Project, project.id) }, project)
                        val remove = MenuAction("Remove", Glyph.MenuRemove, { navigation.show(Destination.RemoveRecent, deployment.id) }, destructive = true)
                        val actions = listOf(
                            MenuAction("Preview", Glyph.MenuPreview, { navigation.show(Destination.Preview, deployment.id) }),
                            MenuAction("Source", Glyph.GitHub, null),
                            MenuAction("Deployment", Glyph.MenuDeployment, { navigation.show(Destination.Deployment, deployment.id) }),
                            MenuAction("Branch Deployments", Glyph.MenuBranch, { navigation.show(Destination.Project, project.id, "branch", deployment.branch) }),
                        )
                        RowOptions(
                            actions = if (earlier) actions + projectAction + remove else listOf(projectAction) + actions + remove,
                            label = "Options for ${deployment.title}",
                            expanded = expanded == deployment.id,
                            onOpen = { expanded = deployment.id },
                            onClose = { expanded = null },
                            authored = deployment.elapsed,
                            separated = earlier,
                            recents = true,
                        )
                    },
                )
            }
        }
        if (removed != null && showRemoval) {
            val deployment = DashboardFixtures.deployment(removed)
            val palette = LocalDashboardPalette.current
            val shape = CircleShape
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(horizontal = Tokens.Inset).padding(bottom = Tokens.DockScrollClearance)
                    .heightIn(min = Tokens.RecentsFeedbackHeight).background(palette.popover, shape)
                    .border(Tokens.Stroke, palette.border, shape).padding(horizontal = Tokens.Inset),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space12), verticalAlignment = Alignment.CenterVertically,
            ) {
                DashboardIcon(Glyph.Check, palette.ready)
                Column(Modifier.weight(1f)) {
                    val subject = deployment.pullRequest?.let { "PR #$it" } ?: "deployment"
                    Text("Removed $subject from Recents", fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine)
                    Text(deployment.title, color = palette.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
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
    val studyHasColumns = preset != null && preset in 5..7
    val studyStart = with(LocalDensity.current) {
        (if (studyHasColumns) Tokens.UsageStudyColumnScroll else Tokens.UsageStudyScroll).roundToPx()
    }
    val listState = rememberLazyListState(initialFirstVisibleItemScrollOffset = if (preset == null) 0 else studyStart)
    val failed = route.variant == "error"
    val empty = route.variant == "empty"
    val scale = if (workspace.id == DashboardFixtures.studioWorkspace && route.variant != "studio") 1.7 else 1.0
    val resources = DashboardFixtures.resources
    val breakdown = resources.filter(UsageResource::showInBreakdown)
    val credit = if (empty) 0.0 else breakdown.sumOf { it.cost } * scale
    val sections = resources.withIndex().groupBy({ (index, _) ->
        when (index) {
            in 0..1 -> "Vercel Delivery Network"
            in 2..5 -> "Vercel Functions"
            in 6..8, in 12..16 -> "Content, Caching & Optimization"
            11, in 18..19, 23 -> "Build & Deploy"
            else -> "Observability"
        }
    }) { it.value }.mapValues { (category, values) ->
        if (category == "Vercel Delivery Network") listOf(DashboardFixtures.edgeCpuDuration) + values.reversed() else values
    }
    DashboardList(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = Tokens.Space24, top = Tokens.AlertTopGap, end = Tokens.Space24, bottom = Tokens.DockScrollClearance),
        verticalArrangement = Arrangement.spacedBy(Tokens.Inset),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Inset)) {
                Text("Pro Plan Usage", fontSize = Tokens.UsageTitle, lineHeight = Tokens.UsageTitleLine, fontWeight = FontWeight.SemiBold)
                val billing = when {
                    workspace.id == DashboardFixtures.studioWorkspace && period != UsagePeriod.Monthly -> "${workspace.name} · Billing cycle Nov 1–30"
                    period == UsagePeriod.Monthly -> "Billing cycle · Nov 1–30 · Through Nov 21"
                    else -> "Billing cycle · Nov 1–30"
                }
                Text(billing, color = palette.secondary, fontSize = Tokens.AlertProjectSize, lineHeight = Tokens.ListSubtitleLine)
            }
        }
        if (failed) {
            item { DividerLine() }
            item {
                DashboardEmpty("Couldn’t load usage", "Try again to load this billing cycle.", Glyph.Usage,
                    modifier = Modifier.padding(top = Tokens.Space32 + Tokens.Space6),
                    action = { navigation.main(Destination.Usage) }, illustration = R.drawable.dashboard_figma_usage_error,
                    glassAction = true, prominentTitle = true)
            }
            return@DashboardList
        }
        item {
            val shape = RoundedCornerShape(if (empty) Tokens.CardRadius else Tokens.ControlRadius)
            val creditHeight = if (empty) Tokens.UsageEmptyCreditHeight else Tokens.UsageCreditHeight
            Column(
                Modifier.fillMaxWidth().heightIn(min = creditHeight)
                    .background(if (empty) palette.projectSheetGroup else palette.control, shape)
                    .then(if (empty) Modifier else Modifier.border(Tokens.Stroke, palette.quietBorder, shape))
                    .padding(if (empty) Tokens.ProjectOptionsInset else Tokens.BodyInset),
            ) {
                Text(if (empty) "Included credit" else "Included Credit", color = palette.secondary,
                    fontSize = Tokens.Metadata, lineHeight = if (empty) Tokens.MetadataLine else Tokens.ListSubtitleLine)
                Text("${sampleMoney(credit)} / $20.00", Modifier.padding(top = if (empty) Tokens.Space4 else Tokens.Space8),
                    fontSize = if (empty) Tokens.AlertDetailTitle else Tokens.Title,
                    lineHeight = if (empty) Tokens.AlertDetailTitleLine else Tokens.ListSubtitleLine,
                    fontWeight = if (empty) FontWeight.SemiBold else FontWeight.Medium)
                if (empty) Text("Current billing cycle", Modifier.padding(top = Tokens.Space8 + Tokens.Space2),
                    color = palette.secondary, fontSize = Tokens.Caption, lineHeight = Tokens.AlertPillLine)
                else Box(Modifier.fillMaxWidth().padding(top = Tokens.Space8).height(Tokens.Space4)
                    .clip(CircleShape).background(palette.insetSurface)) {
                    if (credit > 0) Box(Modifier.fillMaxWidth((credit / 20).toFloat().coerceAtMost(1f)).fillMaxHeight()
                        .background(palette.studySparklineBlue, CircleShape))
                }
            }
        }
        if (empty) {
            item {
                Box(Modifier.padding(top = Tokens.Space6)) {
                    DashboardGroup {
                        Column(Modifier.fillMaxWidth().heightIn(min = Tokens.UsageEmptyMessageHeight).padding(Tokens.Inset),
                            verticalArrangement = Arrangement.spacedBy(Tokens.DockInset)) {
                            Text("No usage recorded yet", fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine,
                                fontWeight = FontWeight.Medium)
                            Text("New consumption appears here once it’s recorded.", fontSize = Tokens.ListSubtitle,
                                lineHeight = Tokens.ProjectSectionLine, color = palette.secondary)
                        }
                    }
                }
            }
        } else {
            item { UsageBreakdown(period, { period = it }, breakdown, scale) }
            if (studyHasColumns) item { UsageStudyColumnHeader() }
            else if (preset == null) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                        Text("Product", Modifier.weight(145f), color = palette.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(Tokens.UsageSparklineWidth))
                        Text("Usage", Modifier.weight(82f), color = palette.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium)
                        Text("Charge", Modifier.weight(54f), color = palette.secondary, fontSize = Tokens.Metadata,
                            lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                    }
                }
            }
            val toggle: (UsageResource) -> Unit = { resource -> expanded = if (expanded == resource.id) null else resource.id }
            if (preset != null) {
                item("usage-study-groups") {
                    Column(Modifier.padding(top = if (studyHasColumns) Dp.Hairline else Tokens.UsageStudyFirstGroupGap)) {
                        for ((group, values) in DashboardFixtures.usageStudySections) {
                            UsageStudyGroup(preset, group, values, scale, expanded, toggle)
                        }
                    }
                }
            } else {
                sections.forEach { (group, values) ->
                    item(group) {
                        Column {
                            Text(group, Modifier.fillMaxWidth().background(palette.control).padding(Tokens.Space8),
                                fontSize = Tokens.Metadata, fontWeight = FontWeight.Medium, lineHeight = Tokens.ListSubtitleLine)
                            for (resource in values) {
                                UsageResourceRow(resource, scale, expanded == resource.id) { toggle(resource) }
                            }
                        }
                    }
                }
            }
        }
        item {
            if (empty) Text("Totals", Modifier.padding(top = Tokens.AlertMeasurePad, bottom = Tokens.Space12),
                fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.MetadataLine, fontWeight = FontWeight.Medium, color = palette.secondary)
            else SectionLabel("Totals")
            DashboardGroup {
                val rowHeight = if (empty) Tokens.SettingsOwnershipRow else Tokens.Touch
                KeyValueRow("Subscription licenses", "$9.98", minHeight = rowHeight, compactValue = empty)
                DividerLine(Modifier.padding(horizontal = Tokens.Inset), palette.border)
                KeyValueRow("Infrastructure", sampleMoney(if (empty) 0.0 else resources.sumOf { it.cost } * scale),
                    minHeight = rowHeight, compactValue = empty)
                DividerLine(Modifier.padding(horizontal = Tokens.Inset), palette.border)
                KeyValueRow("Total", sampleMoney(9.98 + if (empty) 0.0 else resources.sumOf { it.cost } * scale),
                    minHeight = rowHeight, compactValue = empty)
            }
        }
    }
}

@Composable
private fun UsageBreakdown(period: UsagePeriod, select: (UsagePeriod) -> Unit, products: List<UsageResource>, scale: Double) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.ControlRadius)
    val daily = period == UsagePeriod.Daily
    Column(
        Modifier.fillMaxWidth().heightIn(min = Tokens.UsageChartPanel).border(Tokens.Stroke, palette.quietBorder, shape)
            .padding(horizontal = Tokens.BodyInset, vertical = Tokens.Space12),
    ) {
        Text("Consumption Breakdown", fontSize = Tokens.ListTitle,
            lineHeight = if (daily) Tokens.ListSubtitleLine else Tokens.SheetTitleLine, fontWeight = FontWeight.SemiBold)
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = if (daily) Tokens.Space12 else Tokens.Space8).selectableGroup()) {
            val count = UsagePeriod.entries.size
            val shape = RoundedCornerShape(Tokens.AlertLoadingRadius)
            val buttonWidth = (maxWidth - Tokens.Space8 * (count - 1)) / count
            val selection by animateDpAsState((buttonWidth + Tokens.Space8) * period.ordinal,
                DashboardMotion.prototype(DashboardMotion.Tabs), label = "Usage period selection")
            Box(Modifier.offset(x = selection).width(buttonWidth).height(Tokens.UsagePeriodTabHeight).background(palette.selected, shape))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                for (choice in UsagePeriod.entries) {
                    val chosen = choice == period
                    Box(
                        Modifier.weight(1f).height(Tokens.UsagePeriodTabHeight).clip(shape)
                            .selectable(chosen, role = Role.Tab, onClick = { select(choice) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(choice.name, fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine,
                            color = if (chosen) palette.ink else palette.secondary, fontWeight = if (chosen) FontWeight.Medium else FontWeight.Normal)
                    }
                }
            }
        }
        AnimatedContent(
            targetState = period,
            transitionSpec = {
                (fadeIn(DashboardMotion.prototype(DashboardMotion.Tabs)) togetherWith
                    fadeOut(DashboardMotion.prototype(DashboardMotion.Tabs))).using(
                    SizeTransform { _, _ -> DashboardMotion.prototype(DashboardMotion.Tabs) },
                )
            },
            label = "Usage chart period",
        ) { selected -> UsagePlot(selected, products, scale) }
    }
}

@Composable
private fun UsagePlot(period: UsagePeriod, products: List<UsageResource>, scale: Double) {
    val palette = LocalDashboardPalette.current
    val daily = period == UsagePeriod.Daily
    val colors = if (daily) listOf(palette.usageDailyGreen, palette.usageDailyGold)
    else listOf(palette.usagePeriodGreen, palette.warning)
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
    val weekly = period == UsagePeriod.Weekly
    val axisWidth = if (daily) Tokens.Space24 else Tokens.Space32
    val plotStart = if (weekly) Tokens.Space8 + Tokens.Space2 else Dp.Hairline
    val plotEnd = if (weekly) Tokens.AlertLoadingRadius else Dp.Hairline
    val monthOffset = if (period == UsagePeriod.Monthly) Tokens.Space4 else Dp.Hairline
    Column {
        Row(Modifier.fillMaxWidth().padding(top = if (daily) Tokens.Inset else Tokens.Inset + Tokens.Space2)
            .height(Tokens.UsageChartGrid + if (daily) Tokens.Space12 else Tokens.Space32 + Tokens.Space12)) {
            Column(Modifier.width(axisWidth).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween) {
                (intervals downTo 0).forEach { tick ->
                    Text(if (daily) "$$tick" else sampleMoney(tick * gridStep), fontSize = Tokens.ChartCaption, color = palette.secondary)
                }
            }
            Canvas(Modifier.weight(1f).fillMaxHeight().padding(start = plotStart, end = plotEnd)) {
                val top = (if (daily) Tokens.Space6 else Tokens.Stroke).toPx()
                val bottom = size.height - (Tokens.Space6 + if (daily) Tokens.Stroke else Dp.Hairline).toPx()
                for (i in 0..intervals) {
                    val y = top + (bottom - top) * i / intervals
                    drawLine(palette.quietBorder, Offset(0f, y), Offset(size.width, y), Tokens.Stroke.toPx())
                }
                shares.forEachIndexed { index, bucket ->
                    val position = if (daily) listOf(.107f, .59f)[index] else (index + .5f) / shares.size
                    val bar = when (period) {
                        UsagePeriod.Daily -> Tokens.Space20
                        UsagePeriod.Weekly -> Tokens.UsageWeeklyBarWidth
                        UsagePeriod.Monthly -> Tokens.UsageMonthlyBarWidth
                    }.toPx()
                    var y = bottom
                    bucket.forEachIndexed { product, share ->
                        val height = (bottom - top) * (charges[product] * share / (gridStep * intervals)).toFloat()
                        y -= height
                        if (height > 0) drawRoundRect(
                            colors[product],
                            Offset(position * size.width + monthOffset.toPx() - bar / 2, y),
                            androidx.compose.ui.geometry.Size(bar, height),
                            androidx.compose.ui.geometry.CornerRadius(Tokens.Space2.toPx()),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(if (daily) Tokens.Space32 + Tokens.ProjectDetailGap * 2 else Tokens.Space8))
        val dateInsets = if (daily) Modifier else Modifier.padding(start = axisWidth + plotStart, end = plotEnd).offset(x = monthOffset)
        Row(Modifier.fillMaxWidth().then(dateInsets)) {
            dates.forEach { date ->
                Text(date, Modifier.weight(1f), fontSize = Tokens.ChartCaption,
                    lineHeight = if (daily) Tokens.ListSubtitleLine else Tokens.UsageDateLine,
                    fontWeight = if (weekly) FontWeight.Medium else FontWeight.Normal,
                    color = palette.secondary, textAlign = if (daily) TextAlign.Start else TextAlign.Center)
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

@Composable
private fun UsageResourceRow(resource: UsageResource, scale: Double, expanded: Boolean, onOpen: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val quantity = resource.quantity(scale)
    val cost = sampleMoney(resource.cost * scale)
    Row(Modifier.fillMaxWidth().heightIn(min = Tokens.SearchRow).clickable(onClick = onOpen)
        .padding(vertical = Tokens.Space8), horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
        verticalAlignment = Alignment.CenterVertically) {
        Text(resource.name, Modifier.weight(145f), fontSize = Tokens.Metadata, lineHeight = Tokens.SearchDetailLine)
        UsageSparkline(resource)
        Text(quantity, Modifier.weight(82f), color = palette.secondary, fontSize = Tokens.Caption, maxLines = 1)
        Text(cost, Modifier.weight(54f), fontSize = Tokens.Metadata, textAlign = TextAlign.End, maxLines = 1)
    }
    if (expanded) {
        KeyValueRow("Recorded usage", quantity)
        KeyValueRow("Estimated charge", cost)
    }
    DividerLine()
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
                illustration = R.drawable.dashboard_figma_alerts_empty, descriptionGap = Tokens.Space4)
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
                    illustration = R.drawable.dashboard_figma_alerts_empty, descriptionGap = Tokens.Space4)
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
                    }, Modifier.fillMaxWidth().padding(top = Tokens.Space12))
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
        Modifier.fillMaxWidth().border(Tokens.Stroke, palette.insetSurface, shape)
            .padding(start = Tokens.AlertMeasurePad, end = Tokens.AlertMeasurePad,
                top = Tokens.ProjectOptionsInset, bottom = Tokens.PreviewStatusInset),
    ) {
        Text(kind.chartTitle, fontSize = Tokens.Body, lineHeight = Tokens.ProjectOptionsLine, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Tokens.ChartTitleGap))
        Row(Modifier.fillMaxWidth().height(Tokens.CompactChartTicksHeight)) {
            val tickWidth = if (kind == AlertKind.Transfer) Tokens.Touch else Tokens.ChartTickWidth
            Column(Modifier.width(tickWidth).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween) {
                (3 downTo 0).forEach { level ->
                    val label = if (level == 0 && kind == AlertKind.Cdn) "0" else "${level * step}${kind.unit}"
                    ChartTick(label)
                }
            }
            Canvas(Modifier.weight(1f).fillMaxHeight().padding(start = Tokens.DockInset, end = Tokens.Space2)) {
                val gridTop = Tokens.Space8.toPx()
                val gridHeight = size.height - 2 * gridTop
                for (i in 0..3) {
                    val y = gridTop + gridHeight * i / 3
                    drawLine(palette.plotGrid, Offset(0f, y), Offset(size.width, y), Tokens.Stroke.toPx())
                }
                val samples = when (kind) {
                    AlertKind.Cdn, AlertKind.Transfer -> listOf(42, 42, 48, 52, 54, 62, 73, 78, 66, 58)
                    AlertKind.Monitor -> listOf(17, 18, 19, 22, 32, 67, 62, 56, 40, 33)
                    AlertKind.Function -> listOf(13, 16, 15, 21, 84, 79, 61, 44, 36, 25)
                }
                val peak = samples.max()
                val plot = Path()
                samples.forEachIndexed { i, value ->
                    val x = size.width * i / samples.lastIndex
                    val measurement = alert.value * value / peak
                    val y = gridTop + gridHeight * (1f - (measurement / (step * 3)).toFloat())
                    if (i == 0) plot.moveTo(x, y) else plot.lineTo(x, y)
                }
                drawPath(plot, accent, style = Stroke(Tokens.Space2.toPx()))
            }
        }
        ChartDates(listOf("09:10", "09:20", "09:30", "09:40"))
    }
}
