package com.octaviantocan.vercelnative.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

private data class SearchHit(
    val key: String,
    val title: String,
    val context: String,
    @DrawableRes val artwork: Int,
    val route: DashboardRoute? = null,
    val project: Project? = null,
    val keywords: String = "",
)

@Composable
internal fun SearchOverlay(
    route: DashboardRoute,
    navigation: DashboardNavigation,
    onBack: () -> Unit,
    backdrop: GraphicsLayer,
    active: Boolean,
) {
    val palette = LocalDashboardPalette.current
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val initialQuery = when (route.variant) {
            "typed", "error" -> "deployments"
            "mixed", "activity" -> "preview"
            "no-match", "fallback", "submitted" -> "zzqp"
            else -> ""
        }
        mutableStateOf(TextFieldValue(initialQuery, TextRange(initialQuery.length)))
    }
    val query = input.text
    var submitted by rememberSaveable { mutableStateOf(route.variant == "submitted") }
    var failed by rememberSaveable { mutableStateOf(route.variant == "error") }
    val focus = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(active) {
        if (active && !submitted) {
            withFrameNanos { }
            requester.requestFocus()
            withFrameNanos { }
            keyboard?.show()
        }
    }

    val workspace = navigation.currentWorkspace
    val projects = navigation.projects()
    val names = projects.associateWith(navigation::projectName)
    val alerts = navigation.alerts()
    val destinations = remember(workspace, names, alerts) { searchDestinations(workspace, names, alerts) }
    val firstProject = projects.firstOrNull()
    val firstName = firstProject?.let { names.getValue(it) }.orEmpty()
    val suggestions = remember(workspace, firstProject, firstName) {
        listOfNotNull(
            SearchHit("alerts", "Alerts", "${workspace.name} / Observability", R.drawable.dashboard_figma_warning, DashboardRoute(Destination.Alerts)),
            firstProject?.let {
                SearchHit("deployments", "Deployments", firstName, R.drawable.dashboard_figma_deployment,
                    DashboardRoute(Destination.Project, it.id, "deployments"))
            },
            firstProject?.let {
                SearchHit("environment", "Environment Variables", firstName, R.drawable.dashboard_figma_terminal,
                    DashboardRoute(Destination.ProjectSettings, it.id))
            },
            SearchHit("storage", "Storage", workspace.name, R.drawable.dashboard_figma_storage, DashboardRoute(Destination.Usage)),
            SearchHit("integrations", "Integrations", workspace.name, R.drawable.dashboard_figma_search_integrations, DashboardRoute(Destination.Integrations)),
        )
    }
    val term = query.trim()
    val resultState = key(term) { rememberLazyListState() }
    val hits = remember(term, destinations, suggestions, route.variant) {
        if (term.isEmpty()) suggestions
        else if (route.variant == "typed" && term == "deployments") {
            listOfNotNull(
                destinations.firstOrNull { it.project != null },
                suggestions.firstOrNull { it.key == "deployments" }?.copy(context = "$firstName · main"),
                suggestions.firstOrNull { it.key == "alerts" }?.copy(context = workspace.name),
                suggestions.firstOrNull { it.key == "integrations" },
                destinations.firstOrNull { it.key == "support" },
            )
        } else {
            val terms = term.split(Regex("\\s+"))
            (suggestions + destinations).filter { hit ->
                terms.all { word -> hit.title.contains(word, true) || hit.context.contains(word, true) || hit.keywords.contains(word, true) }
            }.sortedWith(compareBy<SearchHit> { hit ->
                val title = hit.title.replace('-', ' ').replace('_', ' ')
                when {
                    title.equals(term, true) || hit.title.equals(term, true) -> 0
                    title.startsWith(term, true) || hit.title.startsWith(term, true) -> 1
                    title.contains(term, true) || hit.title.contains(term, true) -> 2
                    else -> 3
                }
            }.thenBy { it.project == null })
        }
    }
    val assistant = SearchHit(
        "assistant", "“${term.ifBlank { "queued production deployments" }}”", "Navigation Assistant",
        R.drawable.dashboard_figma_assistant,
    )
    val submit = {
        if (term.isNotEmpty()) {
            focus.clearFocus()
            keyboard?.hide()
            failed = false
            submitted = true
        }
    }
    val close = {
        focus.clearFocus()
        keyboard?.hide()
        onBack()
    }
    val panel = RoundedCornerShape(Tokens.SearchPanelRadius)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().background(palette.canvas.copy(alpha = .34f))
            .clickable(enabled = active, interactionSource = null, indication = null, onClick = close))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().imePadding()
                .displayCutoutPadding().padding(horizontal = Tokens.Space20).padding(bottom = Tokens.SearchClearance),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space8),
        ) {
            when {
                failed -> DashboardNotice(
                    "Search couldn’t load", tone = Tone.Neutral,
                    retry = { failed = false; requester.requestFocus(); keyboard?.show() },
                )
                submitted -> Box(Modifier.fillMaxWidth().clip(panel).background(palette.popover).border(Tokens.Stroke, palette.quietBorder, panel).padding(Tokens.Space8)) {
                    SearchResultRow(assistant.copy(title = "Looking for a destination…"), selected = false)
                }
                else -> Column(
                    Modifier.fillMaxWidth().shadow(Tokens.Inset, panel).clip(panel)
                        .background(palette.popover).border(Tokens.Stroke, palette.quietBorder, panel).padding(Tokens.Space8),
                ) {
                    if (hits.isNotEmpty()) {
                        DashboardList(
                            Modifier.fillMaxWidth().heightIn(max = Tokens.SearchPanelHeight - Tokens.SearchRow - Tokens.Space8 * 2 - Tokens.Stroke),
                            state = resultState,
                            surface = palette.popover,
                        ) {
                            itemsIndexed(hits, key = { _, result -> result.key }) { index, result ->
                                SearchResultRow(result, selected = index == 0) {
                                    if (active && result.route != null) {
                                        focus.clearFocus()
                                        keyboard?.hide()
                                        navigation.open(result.route)
                                    }
                                }
                                if (index < hits.lastIndex) DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                            }
                        }
                        DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                    }
                    SearchResultRow(assistant, selected = hits.isEmpty()) {
                        if (active) {
                            if (term.isEmpty()) {
                                val prompt = "queued production deployments"
                                input = TextFieldValue(prompt, TextRange(prompt.length))
                            }
                            focus.clearFocus()
                            keyboard?.hide()
                            failed = false
                            submitted = true
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                BasicTextField(
                    value = input,
                    onValueChange = {
                        if (input.text != it.text) {
                            submitted = false
                            failed = false
                        }
                        input = it
                    },
                    modifier = Modifier.weight(1f).heightIn(min = Tokens.BottomBar)
                        .dashboardGlass(backdrop).focusRequester(requester)
                        .semantics { contentDescription = "Search Vercel" },
                    textStyle = GeistText.copy(color = palette.ink, fontSize = Tokens.ListTitle, lineHeight = Tokens.ListTitleLine),
                    enabled = active,
                    singleLine = true,
                    cursorBrush = SolidColor(palette.ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                    decorationBox = { field ->
                        Row(
                            Modifier.heightIn(min = Tokens.BottomBar).padding(start = Tokens.SearchIconStart, end = Tokens.Inset),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Tokens.SearchTextGap),
                        ) {
                            DashboardIcon(Glyph.Search, palette.secondary)
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) Text("Search Vercel", color = palette.secondary,
                                    style = GeistText, fontSize = Tokens.ListTitle, lineHeight = Tokens.ListTitleLine)
                                field()
                            }
                        }
                    },
                )
                val send = term.isNotEmpty() && !submitted && route.variant != "fallback"
                Box(
                    Modifier.size(Tokens.BottomBar).dashboardGlass(backdrop, CircleShape)
                        .clickable(enabled = active, role = Role.Button, onClick = if (send) submit else close)
                        .semantics { contentDescription = if (send) "Send to Navigation Assistant" else "Close Search" },
                    contentAlignment = Alignment.Center,
                ) { DashboardIcon(if (send) Glyph.Send else Glyph.Close) }
            }
        }
    }
}

@Composable
private fun SearchResultRow(hit: SearchHit, selected: Boolean, onClick: (() -> Unit)? = null) {
    val palette = LocalDashboardPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = Tokens.SearchRow).clip(RoundedCornerShape(Tokens.SearchSelectionRadius))
            .background(if (selected) palette.selected else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = Tokens.Inset, vertical = Tokens.Space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.SearchTextGap),
    ) {
        Box(Modifier.size(Tokens.SearchIconBox), contentAlignment = Alignment.Center) {
            val project = hit.project
            if (project != null) {
                ProjectMark(project, Modifier.size(Tokens.SearchProjectMark))
            } else {
                Icon(
                    painterResource(hit.artwork), null, Modifier.wrapContentSize(),
                    tint = if (hit.key == "assistant") palette.link else palette.muted,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(hit.title, fontSize = Tokens.Body, fontWeight = FontWeight.Medium, lineHeight = Tokens.ListSubtitleLine,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hit.context, color = palette.secondary, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun searchDestinations(
    workspace: Workspace,
    names: Map<Project, String>,
    alerts: List<DashboardAlert>,
): List<SearchHit> = buildList {
    for ((project, name) in names) {
        add(SearchHit(project.id, name, "Project", R.drawable.dashboard_badge_vercel,
            DashboardRoute(Destination.Project, project.id), project, "${workspace.name} deployments"))
        for (domain in DashboardFixtures.domains(project.id)) {
            add(SearchHit(domain.id, domain.hostname, "Domain · $name", R.drawable.dashboard_figma_globe,
                DashboardRoute(Destination.Domain, domain.id)))
        }
        for (deployment in DashboardFixtures.deployments(project.id)) {
            add(SearchHit(deployment.id, deployment.title, "$name · ${deployment.branch}", R.drawable.dashboard_figma_deployment,
                DashboardRoute(Destination.Deployment, deployment.id), keywords = "deployment deployments ${deployment.environment} ${deployment.sha}"))
        }
    }
    for (alert in alerts) {
        val name = names.entries.firstOrNull { it.key.id == alert.projectId }?.value ?: workspace.name
        add(SearchHit(alert.id, alert.title, "Alerts · $name", R.drawable.dashboard_figma_warning,
            DashboardRoute(Destination.Alert, alert.id), keywords = alert.summary))
    }
    add(SearchHit("observability", "Observability", workspace.name, R.drawable.dashboard_icon_observability, DashboardRoute(Destination.Observability)))
    add(SearchHit("support", "Support", "Vercel Support", R.drawable.dashboard_figma_support, DashboardRoute(Destination.Help), keywords = "help"))
}
