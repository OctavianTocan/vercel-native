package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

@Composable
internal fun ProjectsScreen(route: DashboardRoute, navigation: DashboardNavigation) {
    val projects = navigation.projects()
    val showFavorites = route.variant != "plain"
    val favorite = projects.filter { showFavorites && it.id in navigation.favorites }
    val other = projects.filterNot { it in favorite }
    var expanded by rememberSaveable {
        mutableStateOf(
            when (route.variant) {
                "menu-add", "menu-earlier" -> projects.firstOrNull { it.id !in navigation.favorites }?.id
                "menu-remove" -> favorite.firstOrNull()?.id
                else -> null
            }
        )
    }
    var favoriteFailed by rememberSaveable { mutableStateOf(route.variant == "favorite-failed") }
    if (route.variant in setOf("loading", "empty", "error")) {
        Column(Modifier.fillMaxSize()) {
            DashboardHeader("Projects", { navigation.show(Destination.More) }, Glyph.Menu)
            DashboardEmpty(
                title = when (route.variant) {
                    "loading" -> "Loading projects"
                    "empty" -> "A clean slate."
                    else -> "Couldn’t load projects"
                },
                description = when (route.variant) {
                    "loading" -> "Getting projects for ${navigation.currentWorkspace.name}."
                    "empty" -> "No projects in this workspace yet."
                    else -> "Check your connection and try again."
                },
                glyph = Glyph.Project,
                modifier = Modifier.weight(1f),
                loading = route.variant == "loading",
                action = if (route.variant == "error") ({ navigation.main(Destination.Projects) }) else null,
            )
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Tokens.Space12)) {
        item { DashboardHeader("Projects", { navigation.show(Destination.More) }, Glyph.Menu) }
        if (favoriteFailed) item {
            DashboardNotice(
                "Couldn’t add to Favorites",
                Modifier.padding(horizontal = Tokens.Inset),
                Tone.Error,
                retry = { favoriteFailed = false },
            )
        }
        if (favorite.isNotEmpty()) item {
            SectionLabel("Favorites", Modifier.padding(horizontal = Tokens.Inset))
        }
        items(favorite, key = Project::id) { project ->
            ProjectListRow(project, navigation, expanded == project.id, { expanded = project.id }, { expanded = null }, route.variant == "menu-earlier")
        }
        if (showFavorites) item {
            SectionLabel("Other projects", Modifier.padding(horizontal = Tokens.Inset))
        }
        items(other, key = Project::id) { project ->
            ProjectListRow(project, navigation, expanded == project.id, { expanded = project.id }, { expanded = null }, route.variant == "menu-earlier")
        }
    }
}

@Composable
private fun ProjectListRow(
    project: Project, navigation: DashboardNavigation, expanded: Boolean, onOpen: () -> Unit, onClose: () -> Unit, earlier: Boolean,
) {
    val name = navigation.projectName(project)
    val projectId = project.id
    val deployment = DashboardFixtures.deployments(projectId).first()
    DashboardRow(
        title = name,
        subtitle = project.domain,
        leading = { ProjectMark(project) },
        trailing = {
            RowOptions(
                actions = listOf(
                    MenuAction(if (projectId in navigation.favorites) "Remove Favorite" else "Add Favorite", Glyph.Check, { navigation.changeFavorite(projectId) }),
                    MenuAction("Visit with Toolbar", Glyph.Globe, { navigation.show(Destination.Toolbar, deployment.id) }),
                    MenuAction("View Logs", Glyph.Activity, { navigation.show(Destination.Logs, projectId) }),
                    MenuAction("Manage Domains", Glyph.Globe, { navigation.show(Destination.ProjectDomains, projectId) }),
                    MenuAction("Transfer Project", Glyph.Send, { navigation.show(Destination.Transfer, projectId) }),
                    MenuAction("Settings", Glyph.Settings, { navigation.show(Destination.ProjectSettings, projectId) }),
                ),
                label = "Options for $name",
                expanded = expanded,
                onOpen = onOpen,
                onClose = onClose,
                separated = earlier,
            )
        },
        onClick = { navigation.show(Destination.Project, projectId) },
        minHeight = Tokens.ProjectListRow,
    )
    DividerLine(Modifier.padding(horizontal = Tokens.Inset))
}
