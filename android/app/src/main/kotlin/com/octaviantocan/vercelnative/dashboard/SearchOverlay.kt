package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

private data class SearchHit(
    val key: String, val title: String, val context: String, val glyph: Glyph, val route: DashboardRoute,
)

@Composable
internal fun SearchOverlay(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    var query by rememberSaveable {
        mutableStateOf(
            when (route.variant) {
                "typed", "error" -> "catalog"
                "mixed", "activity" -> "preview"
                "no-match", "fallback", "submitted" -> "zzqp"
                else -> ""
            }
        )
    }
    var submitted by rememberSaveable { mutableStateOf(route.variant == "submitted") }
    var failed by rememberSaveable { mutableStateOf(route.variant == "error") }
    val focus = LocalFocusManager.current
    val projects = navigation.projects()
    val candidates = remember(projects) {
        buildList {
            for (project in projects) {
                val name = navigation.projectName(project)
                add(SearchHit(project.id, name, "Project · ${navigation.currentWorkspace.name}", Glyph.Project, DashboardRoute(Destination.Project, project.id)))
                for (domain in DashboardFixtures.domains(project.id).take(4)) {
                    add(SearchHit(domain.id, domain.hostname, "Domain · $name", Glyph.Globe, DashboardRoute(Destination.Domain, domain.id)))
                }
                for (deployment in DashboardFixtures.deployments(project.id).take(5)) {
                    add(SearchHit(deployment.id, deployment.title, "Deployment · $name · ${deployment.sha}", Glyph.Branch, DashboardRoute(Destination.Deployment, deployment.id)))
                }
            }
            for (alert in navigation.alerts()) {
                val name = navigation.projectName(navigation.project(alert.projectId))
                add(SearchHit(alert.id, alert.kind.label, "Alerts · $name", Glyph.Activity, DashboardRoute(Destination.Alert, alert.id)))
            }
            add(SearchHit("obs", "Observability", "Workspace tools", Glyph.Activity, DashboardRoute(Destination.Observability)))
            add(SearchHit("integrations", "Integrations", "Workspace tools", Glyph.Settings, DashboardRoute(Destination.Integrations)))
            add(SearchHit("support", "Support", "Vercel Support", Glyph.Help, DashboardRoute(Destination.Help)))
        }
    }
    val results = if (query.isBlank()) {
        val examples = listOf(Destination.Alert, Destination.Deployment, Destination.Domain, Destination.Integrations, Destination.Observability)
            .mapNotNull { type -> candidates.firstOrNull { it.route.destination == type } }
        examples + candidates.filter { it.route.destination == Destination.Project }.take(24)
    } else {
        candidates.filter { query.trim().let { term -> it.title.contains(term, true) || it.context.contains(term, true) } }.take(60)
    }
    val submit = {
        if (query.isNotBlank()) {
            focus.clearFocus()
            failed = false
            submitted = true
        }
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().background(palette.canvas.copy(alpha = Tokens.ScrimOpacity)).clickable(onClick = onBack))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().imePadding()
                .padding(horizontal = Tokens.Inset, vertical = Tokens.Space12),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space8),
        ) {
            Surface(shape = RoundedCornerShape(Tokens.CardRadius), color = palette.popover) {
                when {
                    failed -> DashboardNotice("Search couldn’t load", Modifier.padding(Tokens.Space8), Tone.Neutral, retry = { failed = false })
                    submitted -> DashboardRow("Looking for a destination…", "Navigation Assistant", glyph = Glyph.Agent)
                    else -> LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = Tokens.SearchResultsHeight),
                        contentPadding = PaddingValues(Tokens.Space8),
                    ) {
                        items(results, key = SearchHit::key) { result ->
                            DashboardRow(result.title, result.context, glyph = result.glyph, trailing = null, onClick = {
                                focus.clearFocus()
                                navigation.open(result.route)
                            })
                            DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                        }
                        if (query.isNotBlank()) item {
                            DashboardRow("“$query”", "Navigation Assistant", glyph = Glyph.Agent, onClick = submit)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                TextField(
                    query,
                    { query = it; submitted = false; failed = false },
                    modifier = Modifier.weight(1f).clip(CircleShape),
                    singleLine = true,
                    placeholder = { Text("Search Vercel") },
                    leadingIcon = { DashboardIcon(Glyph.Search) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = palette.input, unfocusedContainerColor = palette.input,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                )
                if (query.isBlank() || submitted || route.variant == "fallback") IconControl(Glyph.Close, "Close Search", onBack)
                else IconControl(Glyph.Send, "Send to Navigation Assistant", submit)
            }
        }
    }
}
