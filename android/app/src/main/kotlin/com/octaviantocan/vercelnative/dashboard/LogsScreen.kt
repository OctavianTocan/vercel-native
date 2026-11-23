package com.octaviantocan.vercelnative.dashboard

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlinx.coroutines.delay

@Composable
internal fun LogsScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val project = route.target?.let(navigation::project)
    val scopeName = project?.let(navigation::projectName) ?: "All projects"
    val scopeId = project?.id
    var rendered by rememberSaveable { mutableStateOf(route.variant) }
    val remembered = navigation.logOptions(scopeId)
    var query by rememberSaveable {
        mutableStateOf(when (rendered) {
            "query", "refreshed", "error" -> "timed out"
            "empty" -> "connection reset"
            else -> remembered.query
        })
    }
    var caseErrors by rememberSaveable { mutableStateOf(rendered == "errors") }
    var live by rememberSaveable { mutableStateOf(rendered == "live" || remembered.live) }
    var refreshed by rememberSaveable { mutableStateOf(rendered == "refreshed") }
    val errors = caseErrors || remembered.errors
    val projects = navigation.projects()
    val examples = remember(route.frozen, scopeId, projects) {
        if (route.frozen) DashboardFixtures.reviewedLogs(scopeId, projects) else emptyList()
    }
    val logs = remember(project, projects, examples) {
        if (examples.isNotEmpty()) examples
        else if (project != null) DashboardFixtures.logs(project.id) else projects
            .flatMap { project -> DashboardFixtures.logs(project.id) }
            .sortedByDescending(LogEvent::time)
    }
    val minutes = logMinutes(remembered.range)
    val shown = logs.filter { event ->
        event.minutes >= DashboardFixtures.logTimeEnd - minutes &&
            (!errors || event.level == LogLevel.Error) &&
            (query.isBlank() || event.path.contains(query, true) || event.message.contains(query, true) || event.level.label.contains(query, true))
    }
    LaunchedEffect(refreshed, route.frozen) {
        if (refreshed && !route.frozen) {
            delay(1900)
            refreshed = false
        }
    }
    Column(Modifier.fillMaxSize()) {
        DashboardHeader("Logs", onBack, subtitle = scopeName,
            modifier = Modifier.padding(top = Tokens.AlertDetailHeaderInset), titleSize = Tokens.DetailTitle)
        DashboardField(
            query, { term -> query = term; navigation.updateLogOptions(scopeId) { it.copy(query = term) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset).padding(top = Tokens.Inset),
            placeholder = "Search logs", shape = RoundedCornerShape(Tokens.Space20), trailingInset = Tokens.Space2,
            outline = palette.border,
            leading = {
                Box(Modifier.width(Tokens.Space24)) {
                    DashboardIcon(Glyph.Search, palette.secondary, Modifier.size(Tokens.Space20))
                }
            },
            trailing = {
                val face = RoundedCornerShape(Tokens.Inset)
                Box(Modifier.size(Tokens.Filter).clip(face).background(palette.group)
                    .then(if (errors) Modifier.border(Tokens.Stroke, palette.error, face) else Modifier)
                    .clickable(role = Role.Button) { navigation.show(Destination.LogFilters, scopeId, detail = errors.toString()) }
                    .semantics { contentDescription = "Filter logs" }, contentAlignment = Alignment.Center) {
                    DashboardIcon(Glyph.Filter, if (errors) palette.error else palette.ink)
                }
            },
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset).padding(top = Tokens.Space12, bottom = Tokens.Space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
        ) {
            LogControl(remembered.range, Glyph.Chevron, trailing = true) {
                navigation.show(Destination.LogFilters, scopeId, detail = errors.toString())
            }
            Spacer(Modifier.weight(1f))
            LogControl("Live", if (live) Glyph.PauseLive else Glyph.Live, active = live) {
                live = !live
                navigation.updateLogOptions(scopeId) { it.copy(live = live) }
            }
            val actionFace = Modifier.clip(CircleShape).background(palette.control).border(Tokens.Stroke, palette.border, CircleShape)
            IconControl(Glyph.Refresh, "Refresh logs", { refreshed = true }, modifier = actionFace,
                visualSize = Tokens.LogActionSize, glass = false)
            IconControl(Glyph.Download, "Download logs", {
                navigation.show(Destination.LogDownload, scopeId, detail = scopeId)
            }, modifier = actionFace, visualSize = Tokens.LogActionSize, glass = false)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                rendered == "error" -> DashboardEmpty(
                    "Couldn’t load logs", "Try again to load logs for this selection.", Glyph.Activity,
                    Modifier.align(Alignment.TopCenter).dashboardScroll().padding(top = Tokens.LogErrorFeedbackGap),
                    action = { rendered = "content" }, glassAction = true,
                    illustration = R.drawable.dashboard_figma_logs_error, prominentTitle = true,
                )
                shown.isEmpty() -> Column(Modifier.fillMaxSize().dashboardScroll()
                    .padding(horizontal = Tokens.Inset).padding(bottom = Tokens.DockScrollClearance)) {
                    LogTimeline(shown, minutes, showColumns = false)
                    DashboardEmpty(
                        "No matching logs", "No events match this search in the selected time range.", Glyph.Search,
                        Modifier.padding(top = Tokens.LogEmptyFeedbackGap), action = {
                            query = ""
                            caseErrors = false
                            navigation.updateLogOptions(scopeId) { it.copy(query = "", errors = false) }
                        }, actionLabel = "Clear search", glassAction = true, prominentTitle = true,
                    )
                }
                else -> DashboardList(Modifier.fillMaxSize(), contentPadding = PaddingValues(
                    start = Tokens.Inset, end = Tokens.Inset, bottom = Tokens.DockScrollClearance,
                )) {
                    item { LogTimeline(shown, minutes, useEventTimes = examples.isNotEmpty()) }
                    items(shown, key = LogEvent::id) { event ->
                        LogRow(event, navigation, project == null) {
                            navigation.show(Destination.LogEvent, event.id, detail = scopeId)
                        }
                    }
                    item {
                        Text("No more logs in this time range", Modifier.fillMaxWidth().padding(vertical = Tokens.Space20),
                            color = palette.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.LogMessageLine,
                            textAlign = TextAlign.Center)
                    }
                }
            }
            if (refreshed) {
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = Tokens.Inset)
                        .padding(bottom = Tokens.Space24).heightIn(min = Tokens.SearchRow)
                        .background(palette.popover, CircleShape).border(Tokens.Stroke, palette.border, CircleShape)
                        .padding(horizontal = Tokens.Inset),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space12), verticalAlignment = Alignment.CenterVertically,
                ) {
                    DashboardArtwork(R.drawable.dashboard_figma_log_success, Tokens.Space20, Color.Unspecified)
                    Text("Logs refreshed", fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine)
                }
            }
        }
    }
}

private fun logMinutes(range: String) = when (range) {
    "Last 15 min" -> 15
    "Last hour" -> 60
    else -> 30
}

@Composable
private fun LogControl(label: String, glyph: Glyph, trailing: Boolean = false, active: Boolean = false, onClick: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val fill = if (active) palette.readySurface else palette.control
    val border = if (active) palette.ready.copy(alpha = .35f) else palette.border
    Row(
        Modifier.heightIn(min = Tokens.LogActionSize)
            .widthIn(min = if (trailing) Tokens.LogRangeWidth else Tokens.LogLiveWidth)
            .clip(CircleShape).background(fill).border(Tokens.Stroke, border, CircleShape)
            .clickable(role = Role.Button, onClickLabel = if (active) "Pause live" else null, onClick = onClick)
            .padding(horizontal = if (trailing) Tokens.Inset else Tokens.Space12),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!trailing) DashboardIcon(glyph)
        Text(label, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.LogMessageLine, fontWeight = FontWeight.Medium,
            color = if (active) palette.ready else palette.ink)
        if (trailing) DashboardIcon(glyph, modifier = Modifier.size(Tokens.MetadataIcon).rotate(90f))
    }
}

@Composable
private fun LogTimeline(events: List<LogEvent>, minutes: Int, showColumns: Boolean = true, useEventTimes: Boolean = false) {
    val palette = LocalDashboardPalette.current
    val bars = if (useEventTimes) events.groupBy(LogEvent::minutes).values else events.take(48).map(::listOf)
    Column(Modifier.fillMaxWidth()) {
        val timelineHeight = Tokens.LogTimelineHeight + when {
            !showColumns -> Tokens.Space12
            useEventTimes -> Tokens.Space4
            else -> androidx.compose.ui.unit.Dp.Hairline
        }
        Canvas(Modifier.fillMaxWidth().height(timelineHeight)) {
            val stroke = Tokens.Stroke.toPx()
            drawRect(palette.border, Offset(0f, size.height - stroke), Size(size.width, stroke))
            for ((index, bucket) in bars.withIndex()) {
                val fraction = if (useEventTimes) {
                    ((bucket.first().minutes - (DashboardFixtures.logTimeEnd - minutes)).toFloat() / minutes).coerceIn(0f, 1f)
                } else 1f - (index + 1f) / 49f
                val x = fraction * size.width
                for ((stack, event) in bucket.withIndex()) {
                    val height = when {
                        useEventTimes -> Tokens.Space4.toPx()
                        event.level == LogLevel.Error -> .75f * size.height
                        else -> (.16f + (event.durationMs % 7) / 50f) * size.height
                    }
                    val color = when (event.level) {
                        LogLevel.Error -> palette.error
                        LogLevel.Warning -> palette.warning
                        LogLevel.Info -> palette.secondary
                    }
                    val width = (if (useEventTimes) Tokens.Space6 else Tokens.Space4).toPx()
                    drawRect(color, Offset(x, size.height - (stack + 1) * height), Size(width, height))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = Tokens.Space4, bottom = if (showColumns) Tokens.Space6 else Tokens.Space12),
            horizontalArrangement = Arrangement.SpaceBetween) {
            val end = DashboardFixtures.logTimeEnd
            val ticks = if (showColumns) listOf(end - minutes, end - minutes / 2, end) else listOf(end - minutes, end)
            ticks.map(::sampleLogClock).forEach {
                Text(it, color = palette.secondary, fontSize = Tokens.ChartCaption, lineHeight = Tokens.MetadataLine)
            }
        }
        if (!showColumns) return@Column
        Row(
            Modifier.fillMaxWidth().heightIn(min = Tokens.Space32).padding(horizontal = Tokens.Space8 + Tokens.Space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Time", Modifier.width(Tokens.LogTimestampWidth), color = palette.secondary, fontSize = Tokens.Caption,
                lineHeight = Tokens.ChartTickLine, fontWeight = FontWeight.Medium)
            Text("Source / message", Modifier.weight(1f), color = palette.secondary, fontSize = Tokens.Caption,
                lineHeight = Tokens.ChartTickLine, fontWeight = FontWeight.Medium)
            Text(if (events.size == 1) "1 event" else "${events.size} events", color = palette.secondary, fontSize = Tokens.Caption,
                lineHeight = Tokens.ChartTickLine, fontWeight = FontWeight.Medium)
        }
        DividerLine(color = palette.insetSurface)
    }
}

@Composable
private fun LogRow(event: LogEvent, navigation: DashboardNavigation, showProject: Boolean, onOpen: () -> Unit) {
    val deployment = DashboardFixtures.deployment(event.deploymentId)
    val palette = LocalDashboardPalette.current
    val rowHeight = if (showProject) Tokens.LogWorkspaceRowHeight else Tokens.LogRowHeight
    val (ink, surface) = when (event.level) {
        LogLevel.Error -> palette.error to palette.errorSurface
        LogLevel.Warning -> palette.warning to palette.warningSurface
        LogLevel.Info -> palette.secondary to palette.event
    }
    Column(Modifier.fillMaxWidth().background(surface).clickable(onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = rowHeight - Tokens.Stroke)
                .padding(horizontal = Tokens.Space8 + Tokens.Space2, vertical = Tokens.Space8),
            verticalAlignment = Alignment.Top,
        ) {
            Text(event.time, Modifier.width(Tokens.LogTimestampWidth), color = ink, fontFamily = GeistMono,
                fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine, maxLines = 1)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space2)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
                    DashboardIcon(Glyph.Function, ink)
                    Text(event.path, color = ink, fontFamily = GeistMono, fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(event.message, fontSize = Tokens.Metadata, lineHeight = Tokens.LogMessageLine, fontFamily = GeistMono,
                    maxLines = if (showProject) 1 else 2, overflow = TextOverflow.Ellipsis)
                if (showProject) Text(navigation.projectName(navigation.project(deployment.projectId)),
                    fontSize = Tokens.Caption, lineHeight = Tokens.ChartTickLine,
                    color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(event.level.label, Modifier.padding(start = Tokens.Space4), color = ink, fontSize = Tokens.Caption,
                lineHeight = Tokens.MetadataLine, fontWeight = FontWeight.Medium)
        }
        DividerLine()
    }
}

@Composable
internal fun LogSheetContent(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    if (route.destination == Destination.LogEvent) {
        LogEventSheet(route, navigation, onBack)
        return
    }
    DashboardHeader(route.destination.label, onBack, titleLine = Tokens.DeploymentDetailTitleLine)
    when (route.destination) {
        Destination.LogFilters -> LogFilters(route, navigation, onBack)
        Destination.LogDownload -> LogDownload(route, navigation)
        else -> Unit
    }
}

@Composable
private fun LogEventSheet(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val copy = rememberCopyText()
    val event = route.target?.let(DashboardFixtures::log)
        ?: DashboardFixtures.logs(navigation.project(null).id).first { it.level == LogLevel.Error }
    val deployment = DashboardFixtures.deployment(event.deploymentId)
    val project = navigation.project(deployment.projectId)
    val (ink, fill) = when (event.level) {
        LogLevel.Error -> palette.error to palette.errorSurface
        LogLevel.Warning -> palette.warning to palette.warningSurface
        LogLevel.Info -> palette.secondary to palette.control
    }
    Column(Modifier.fillMaxWidth().height(Tokens.LogEventBody)) {
        DashboardHeader("Log event", onBack, titleLine = Tokens.DeploymentDetailTitleLine)
        Column(Modifier.weight(1f).fillMaxWidth().dashboardScroll().padding(horizontal = Tokens.Inset)
            .padding(top = Tokens.Space24 + Tokens.Space4, bottom = Tokens.Inset)) {
            Column(Modifier.padding(horizontal = Tokens.Space8)) {
                DottedStatusPill(event.level.label, ink, fill)
                Text(event.path, Modifier.padding(top = Tokens.ProjectOptionsInset), fontSize = Tokens.Heading,
                    lineHeight = Tokens.UsageTitleLine, fontFamily = GeistMono)
                Text("${event.time} · Function", Modifier.padding(top = Tokens.Space8 + Tokens.Space2),
                    fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.ProjectSectionLine, color = palette.secondary, fontFamily = GeistMono)
            }
            Spacer(Modifier.height(Tokens.Space24))
            DashboardGroup {
                Row(Modifier.fillMaxWidth().heightIn(min = Tokens.LogEventMessageHeight)
                    .padding(start = Tokens.BodyInset, end = Tokens.Space20, top = Tokens.DockIcon, bottom = Tokens.Space8),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space8), verticalAlignment = Alignment.Top) {
                    Text(event.message, Modifier.weight(1f), fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectOptionsLine, fontFamily = GeistMono)
                    IconControl(Glyph.Copy, "Copy log message", { copy(event.message) }, visualSize = Tokens.Space20, glass = false)
                }
            }
            Spacer(Modifier.height(Tokens.Space24 + Tokens.Space2))
            DashboardGroup {
                KeyValueRow("Project", navigation.projectName(project), compactValue = true)
                DividerLine(Modifier.padding(horizontal = Tokens.Inset), palette.border)
                KeyValueRow("Environment", deployment.environment, compactValue = true)
                DividerLine(Modifier.padding(horizontal = Tokens.Inset), palette.border)
                DashboardRow("Level", minHeight = Tokens.Touch, trailing = {
                    Text(event.level.label, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.ListSubtitleLine, color = ink)
                })
            }
            if (!route.frozen) {
                Spacer(Modifier.height(Tokens.Inset))
                DashboardRow("Deployment", deployment.sha, glyph = Glyph.Branch, onClick = {
                    navigation.show(Destination.Deployment, deployment.id)
                })
            }
        }
    }
}

@Composable
private fun LogFilters(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val options = navigation.logOptions(route.target)
    var range by rememberSaveable { mutableStateOf(options.range) }
    var errors by rememberSaveable { mutableStateOf(route.detail?.toBooleanStrictOrNull() ?: (options.errors || route.frozen)) }
    var pickingRange by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().dashboardScroll().padding(horizontal = Tokens.Inset)) {
        Text("Log scope", Modifier.padding(start = Tokens.AlertMeasurePad, top = Tokens.Inset, bottom = Tokens.Space6),
            color = palette.secondary, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.CardRadius)).background(palette.projectSheetGroup)) {
            if (route.target == null) LogFilterRow("Project", "All projects")
            else LogFilterRow("Project", "Current project")
            DividerLine(Modifier.padding(horizontal = Tokens.AlertMeasurePad), palette.border)
            Box {
                LogFilterRow("Time range", range) { pickingRange = true }
                val position = with(LocalDensity.current) {
                    val inset = Tokens.Space8.roundToPx()
                    val half = (Tokens.LogFilterRowHeight / 2).roundToPx()
                    remember(inset, half) { ProjectOptionsPosition(0, half, half, inset, 0) }
                }
                DashboardMenu(pickingRange, position, onClose = { pickingRange = false }) {
                    Surface(
                        modifier = Modifier.width(Tokens.DeploymentOptionsWidth),
                        color = palette.popover,
                        shape = RoundedCornerShape(Tokens.ControlRadius),
                    ) {
                        Column(Modifier.padding(vertical = Tokens.Space8)) {
                            for (choice in listOf("Last 15 min", "Last 30 min", "Last hour")) {
                                DropdownMenuItem(text = { Text(choice, color = palette.ink) }, onClick = {
                                    range = choice
                                    pickingRange = false
                                })
                            }
                        }
                    }
                }
            }
            DividerLine(Modifier.padding(horizontal = Tokens.AlertMeasurePad), palette.border)
            LogFilterRow("Level", if (errors) "Errors" else "All levels") { errors = !errors }
        }
        DashboardButton("Apply filters", {
            navigation.updateLogOptions(route.target) { it.copy(range = range, errors = errors) }
            onBack()
        }, Modifier.fillMaxWidth().padding(top = Tokens.Space32 + Tokens.Space6, bottom = Tokens.Space4))
    }
}

@Composable
private fun LogFilterRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = Tokens.LogFilterRowHeight)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = Tokens.AlertMeasurePad),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine)
        Text(value, color = LocalDashboardPalette.current.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine)
    }
}

@Composable
private fun LogDownload(route: DashboardRoute, navigation: DashboardNavigation) {
    val palette = LocalDashboardPalette.current
    val range = navigation.logOptions(route.target).range
    val end = DashboardFixtures.logTimeEnd
    var matching by rememberSaveable { mutableStateOf(true) }
    var completed by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().dashboardScroll().padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Inset)) {
        Column {
            SectionLabel("Time range", Modifier.padding(horizontal = Tokens.Space8))
            DashboardGroup {
                DashboardRow(range, leading = { DashboardIcon(Glyph.Clock, palette.secondary) }, trailing = {
                    MutedText("${sampleLogClock(end - logMinutes(range))}–${sampleLogClock(end)}")
                }, minHeight = Tokens.Touch)
            }
        }
        Column {
            SectionLabel("Events to download", Modifier.padding(horizontal = Tokens.Space8))
            DashboardGroup {
                listOf("Matching logs" to "Current search and filters", "All logs in this time range" to "All events in the selected time window")
                    .forEachIndexed { index, (title, detail) ->
                        val selected = (index == 0) == matching
                        DashboardRow(title, detail, modifier = Modifier.padding(Tokens.Space4).clip(RoundedCornerShape(Tokens.ControlRadius))
                            .background(if (selected) palette.border else Color.Transparent),
                            trailing = { if (selected) DashboardIcon(Glyph.Check, palette.link) }, onClick = { matching = index == 0; completed = false })
                    }
            }
        }
        if (completed) DashboardNotice(if (matching) "Sample export includes matching logs" else "Sample export includes all logs in the range", tone = Tone.Success)
        GlassAction("Download logs", { completed = true }, Modifier.fillMaxWidth().padding(top = Tokens.Space32), glyph = Glyph.Download)
    }
}
