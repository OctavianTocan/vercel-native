package com.octaviantocan.vercelnative.dashboard

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlinx.coroutines.delay

@Composable
internal fun LogsScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
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
    val logs = remember(project, navigation.projects()) {
        if (project != null) DashboardFixtures.logs(project.id) else navigation.projects()
            .flatMap { project -> DashboardFixtures.logs(project.id) }
            .sortedByDescending(LogEvent::time)
    }
    val minutes = when (remembered.range) {
        "Last 15 min" -> 15
        "Last hour" -> 60
        else -> 30
    }
    val shown = logs.filter { event ->
        event.time.substring(3, 5).toInt() >= 60 - minutes &&
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
            placeholder = "Search logs", shape = CircleShape,
            leading = { DashboardIcon(Glyph.Search, LocalDashboardPalette.current.secondary, Modifier.size(Tokens.Space20)) },
            trailing = {
                IconControl(Glyph.Filter, "Filter logs", {
                    navigation.show(Destination.LogFilters, scopeId, detail = errors.toString())
                }, active = errors, visualSize = Tokens.LogFilterFace)
            },
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset).padding(top = Tokens.Space12, bottom = Tokens.Space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space6),
        ) {
            LogControl(remembered.range, Glyph.Chevron, trailing = true) {
                navigation.show(Destination.LogFilters, scopeId, detail = errors.toString())
            }
            Spacer(Modifier.weight(1f))
            LogControl(if (live) "Pause live" else "Live", Glyph.Live) {
                live = !live
                navigation.updateLogOptions(scopeId) { it.copy(live = live) }
            }
            IconControl(Glyph.Refresh, "Refresh logs", { refreshed = true }, visualSize = Tokens.LogActionSize)
            IconControl(Glyph.Download, "Download logs", {
                navigation.show(Destination.LogDownload, scopeId, detail = scopeId)
            }, visualSize = Tokens.LogActionSize)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                rendered == "error" -> DashboardEmpty(
                    "Couldn’t load logs", "Try again to load logs for this selected range.", Glyph.Activity,
                    Modifier.align(Alignment.Center), action = { rendered = "content" },
                )
                shown.isEmpty() -> DashboardEmpty(
                    "No matching logs", "No events match this search in the selected time range.", Glyph.Search,
                    Modifier.align(Alignment.Center), action = {
                        query = ""
                        caseErrors = false
                        navigation.updateLogOptions(scopeId) { it.copy(query = "", errors = false) }
                    }, actionLabel = "Clear search",
                )
                else -> DashboardList(Modifier.fillMaxSize(), contentPadding = PaddingValues(
                    start = Tokens.Inset, end = Tokens.Inset, bottom = Tokens.DockScrollClearance,
                )) {
                    item { LogTimeline(shown, minutes) }
                    items(shown, key = LogEvent::id) { event ->
                        LogRow(event, navigation, project == null) {
                            navigation.show(Destination.LogEvent, event.id, detail = scopeId)
                        }
                    }
                    item { MutedText("No more logs in this time range", Modifier.padding(Tokens.Inset)) }
                }
            }
            if (refreshed) DashboardNotice(
                "Logs refreshed",
                Modifier.align(Alignment.BottomCenter).padding(horizontal = Tokens.Inset, vertical = Tokens.Space8),
                Tone.Success,
            )
        }
    }
}

@Composable
private fun LogControl(label: String, glyph: Glyph, trailing: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.heightIn(min = Tokens.LogActionSize).dashboardGlass().clickable(onClick = onClick).padding(horizontal = Tokens.Space12),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!trailing) DashboardIcon(glyph)
        Text(label, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine, fontWeight = FontWeight.Medium)
        if (trailing) DashboardIcon(glyph, modifier = Modifier.rotate(90f))
    }
}

@Composable
private fun LogTimeline(events: List<LogEvent>, minutes: Int) {
    val palette = LocalDashboardPalette.current
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(Tokens.LogTimelineHeight)) {
            drawLine(palette.quietBorder, Offset(0f, size.height), Offset(size.width, size.height), Tokens.Stroke.toPx())
            for ((index, event) in events.take(48).withIndex()) {
                val x = (1f - ((index + 1f) / 49f)) * size.width
                val height = (if (event.level == LogLevel.Error) .75f else .16f + (event.durationMs % 7) / 50f) * size.height
                val color = when (event.level) {
                    LogLevel.Error -> palette.error
                    LogLevel.Warning -> palette.warning
                    LogLevel.Info -> palette.secondary
                }
                drawRect(color, Offset(x, size.height - height), Size(Tokens.Space4.toPx(), height))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = Tokens.Space4, bottom = Tokens.Space12), horizontalArrangement = Arrangement.SpaceBetween) {
            val start = 60 - minutes
            val middle = 60 - minutes / 2
            listOf("14:" + start.toString().padStart(2, '0'), "14:" + middle.toString().padStart(2, '0'), "15:00").forEach {
                Text(it, color = palette.secondary, fontSize = Tokens.ChartCaption, lineHeight = Tokens.MetadataLine)
            }
        }
        DividerLine()
        Row(
            Modifier.fillMaxWidth().heightIn(min = Tokens.Space32).padding(horizontal = Tokens.Space8 + Tokens.Space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Time", Modifier.width(Tokens.LogTimestampWidth), color = palette.secondary, fontSize = Tokens.Caption)
            Text("Source / message", Modifier.weight(1f), color = palette.secondary, fontSize = Tokens.Caption)
            Text("${events.size} events", color = palette.secondary, fontSize = Tokens.Caption)
        }
    }
}

@Composable
private fun LogRow(event: LogEvent, navigation: DashboardNavigation, showProject: Boolean, onOpen: () -> Unit) {
    val deployment = DashboardFixtures.deployment(event.deploymentId)
    val palette = LocalDashboardPalette.current
    val summary = if (showProject) "${event.message} · ${navigation.projectName(navigation.project(deployment.projectId))}" else event.message
    val (ink, surface) = when (event.level) {
        LogLevel.Error -> palette.error to palette.errorSurface
        LogLevel.Warning -> palette.warning to palette.warningSurface
        LogLevel.Info -> palette.secondary to palette.event
    }
    Column(Modifier.fillMaxWidth().background(surface).clickable(onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = Tokens.LogRowHeight).padding(horizontal = Tokens.Space8 + Tokens.Space2, vertical = Tokens.Space8),
            verticalAlignment = Alignment.Top,
        ) {
            Text(event.time, Modifier.width(Tokens.LogTimestampWidth), color = ink, fontFamily = GeistMono,
                fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine, maxLines = 1)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
                    DashboardArtwork(com.octaviantocan.vercelnative.R.drawable.dashboard_figma_terminal, Tokens.MetadataIcon, ink)
                    Text(event.path, color = ink, fontFamily = GeistMono, fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(summary, fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine, fontFamily = GeistMono,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(event.level.label, Modifier.padding(start = Tokens.Space4), color = ink, fontSize = Tokens.ChartCaption, lineHeight = Tokens.MetadataLine)
        }
        DividerLine()
    }
}

@Composable
internal fun LogSheetContent(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    DashboardHeader(route.destination.label, onBack)
    when (route.destination) {
        Destination.LogFilters -> LogFilters(route, navigation, onBack)
        Destination.LogDownload -> LogDownload(route, navigation)
        else -> {
            val event = route.target?.let(DashboardFixtures::log)
                ?: DashboardFixtures.logs(navigation.project(null).id).first { it.level == LogLevel.Error }
            val deployment = DashboardFixtures.deployment(event.deploymentId)
            val project = navigation.project(deployment.projectId)
            Column(Modifier.dashboardScroll().padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                StatusPill(event.level.label, if (event.level == LogLevel.Error) Tone.Error else Tone.Neutral)
                Text(event.path, fontSize = Tokens.Heading, fontFamily = GeistMono, fontWeight = FontWeight.SemiBold)
                MutedText("${event.time} · Function")
                Text(event.message, fontSize = Tokens.Body)
                KeyValueRow("Project", navigation.projectName(project))
                KeyValueRow("Environment", deployment.environment)
                KeyValueRow("Duration", "${event.durationMs} ms")
                DashboardRow("Deployment", deployment.sha, glyph = Glyph.Branch,
                    onClick = { navigation.show(Destination.Deployment, deployment.id) })
            }
        }
    }
}

@Composable
private fun LogFilters(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val options = navigation.logOptions(route.target)
    var range by rememberSaveable { mutableStateOf(options.range) }
    var errors by rememberSaveable { mutableStateOf(route.detail?.toBooleanStrictOrNull() ?: (options.errors || route.frozen)) }
    Column(Modifier.dashboardScroll().padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
        SectionLabel("Log scope")
        KeyValueRow("Project", route.target?.let { navigation.projectName(navigation.project(it)) } ?: "All projects")
        SectionLabel("Time range")
        for (choice in listOf("Last 15 min", "Last 30 min", "Last hour")) {
            DashboardRow(choice, trailing = { RadioButton(range == choice, onClick = null) }, onClick = { range = choice })
        }
        SectionLabel("Level")
        DashboardRow("Errors", trailing = { Checkbox(errors, onCheckedChange = null) }, onClick = { errors = !errors })
        DashboardButton("Apply filters", {
            navigation.updateLogOptions(route.target) { it.copy(range = range, errors = errors) }
            onBack()
        }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LogDownload(route: DashboardRoute, navigation: DashboardNavigation) {
    var matching by rememberSaveable { mutableStateOf(true) }
    var completed by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.dashboardScroll().padding(Tokens.Inset)) {
        SectionLabel("Time range")
        KeyValueRow("Range", navigation.logOptions(route.target).range)
        SectionLabel("Events to download")
        DashboardRow("Matching logs", "Current search and filters", trailing = { RadioButton(matching, null) }, onClick = { matching = true; completed = false })
        DashboardRow("All logs in this time range", trailing = { RadioButton(!matching, null) }, onClick = { matching = false; completed = false })
        if (completed) DashboardNotice(if (matching) "Sample export includes matching logs" else "Sample export includes all logs in the range", tone = Tone.Success)
        DashboardButton("Download logs", { completed = true }, Modifier.fillMaxWidth())
    }
}
