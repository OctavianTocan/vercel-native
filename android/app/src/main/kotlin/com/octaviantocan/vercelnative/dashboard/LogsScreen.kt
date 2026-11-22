package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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
            .flatMap { project -> DashboardFixtures.logs(project.id).filterIndexed { index, _ -> index % 5 == 0 } }
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
        DashboardHeader("Logs", onBack, subtitle = scopeName)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space6),
        ) {
            OutlinedTextField(
                query, { query = it; navigation.updateLogOptions(scopeId) { current -> current.copy(query = it) } },
                modifier = Modifier.weight(1f), singleLine = true,
                leadingIcon = { DashboardIcon(Glyph.Search) }, placeholder = { Text("Search logs") },
                shape = RoundedCornerShape(Tokens.CardRadius),
            )
            IconControl(Glyph.Filter, "Filter logs", {
                navigation.show(Destination.LogFilters, scopeId, detail = errors.toString())
            }, active = errors)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset, vertical = Tokens.Space6),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space4),
        ) {
            OutlinedButton(
                { navigation.show(Destination.LogFilters, scopeId, detail = errors.toString()) },
                contentPadding = PaddingValues(horizontal = Tokens.Space8),
            ) { Text(remembered.range, fontSize = Tokens.Metadata) }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = {
                live = !live
                navigation.updateLogOptions(scopeId) { it.copy(live = live) }
            }, contentPadding = PaddingValues(horizontal = Tokens.Space8)) {
                Text(if (live) "Pause live" else "Live", fontSize = Tokens.Metadata)
            }
            IconControl(Glyph.Refresh, "Refresh logs", { refreshed = true })
            IconControl(Glyph.Download, "Download logs", {
                navigation.show(Destination.LogDownload, scopeId, detail = scopeId)
            })
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
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Tokens.Touch)) {
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
private fun LogTimeline(events: List<LogEvent>, minutes: Int) {
    Column(Modifier.padding(horizontal = Tokens.Inset, vertical = Tokens.Space8)) {
        DashboardChart(List(16) { index ->
            val position = index * 3
            (events.getOrNull(position)?.durationMs ?: 70).rem(660) / 700f
        })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MutedText("14:" + (60 - minutes).toString().padStart(2, '0'))
            MutedText("14:" + (60 - minutes / 2).toString().padStart(2, '0'))
            MutedText("15:00")
        }
        DividerLine(Modifier.padding(top = Tokens.Space8))
        Row(Modifier.fillMaxWidth().padding(vertical = Tokens.Space8), horizontalArrangement = Arrangement.SpaceBetween) {
            MutedText("Time / message", monospace = true)
            MutedText("${events.size} events")
        }
    }
}

@Composable
private fun LogRow(event: LogEvent, navigation: DashboardNavigation, showProject: Boolean, onOpen: () -> Unit) {
    val deployment = DashboardFixtures.deployment(event.deploymentId)
    val palette = LocalDashboardPalette.current
    val summary = if (showProject) "${event.message} · ${navigation.projectName(navigation.project(deployment.projectId))}" else event.message
    val tone = when (event.level) {
        LogLevel.Error -> Tone.Error
        LogLevel.Warning -> Tone.Warning
        LogLevel.Info -> Tone.Neutral
    }
    Surface(color = if (event.level == LogLevel.Error) palette.errorSurface else palette.canvas) {
        DashboardRow(
            "${event.time}   ${event.path}",
            summary,
            trailing = { StatusPill(event.level.label, tone) },
            onClick = onOpen,
        )
    }
    DividerLine()
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
            Column(Modifier.verticalScroll(rememberScrollState()).padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                StatusPill(event.level.label, if (event.level == LogLevel.Error) Tone.Error else Tone.Neutral)
                Text(event.path, fontSize = Tokens.Heading, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
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
    Column(Modifier.verticalScroll(rememberScrollState()).padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
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
    Column(Modifier.verticalScroll(rememberScrollState()).padding(Tokens.Inset)) {
        SectionLabel("Time range")
        KeyValueRow("Range", navigation.logOptions(route.target).range)
        SectionLabel("Events to download")
        DashboardRow("Matching logs", "Current search and filters", trailing = { RadioButton(matching, null) }, onClick = { matching = true; completed = false })
        DashboardRow("All logs in this time range", trailing = { RadioButton(!matching, null) }, onClick = { matching = false; completed = false })
        if (completed) DashboardNotice(if (matching) "Sample export includes matching logs" else "Sample export includes all logs in the range", tone = Tone.Success)
        DashboardButton("Download logs", { completed = true }, Modifier.fillMaxWidth())
    }
}
