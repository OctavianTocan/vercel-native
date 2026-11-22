package com.octaviantocan.vercelnative.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

internal enum class Presentation { Screen, Sheet, Rail, Search }

private val activityDestinations = setOf(Destination.Recents, Destination.Usage, Destination.Alerts)

internal enum class Destination(val label: String, val presentation: Presentation = Presentation.Screen) {
    Projects("Projects"),
    Recents("Recents"),
    Usage("Usage"),
    Alerts("Alerts"),
    Deployment("Deployment › Details"),
    Alert("Alert details"),
    ProjectSettings("Project Settings"),
    WorkspaceDomains("Domains"),
    Logs("Logs"),
    Observability("Observability"),
    Analytics("Analytics"),
    Speed("Speed Insights"),
    Firewall("Firewall"),
    Integrations("Integrations"),
    Profile("Profile Settings"),
    Agent("Agent"),
    DesignCases("Design states"),
    Project("Project", Presentation.Sheet),
    ProjectDomains("Domains", Presentation.Sheet),
    Domain("Domain details", Presentation.Sheet),
    GitHubSync("GitHub sync", Presentation.Sheet),
    Transfer("Transfer project", Presentation.Sheet),
    Preview("Preview deployment", Presentation.Sheet),
    Toolbar("Visit with Toolbar", Presentation.Sheet),
    RemoveRecent("Remove from Recents?", Presentation.Sheet),
    LogFilters("Filters", Presentation.Sheet),
    LogEvent("Log event", Presentation.Sheet),
    LogDownload("Download logs", Presentation.Sheet),
    Help("Help", Presentation.Sheet),
    Photo("Preview photo", Presentation.Sheet),
    DefaultScope("Default team", Presentation.Sheet),
    Theme("Theme", Presentation.Sheet),
    More("More", Presentation.Rail),
    Search("Search Vercel", Presentation.Search),
}

internal data class DashboardRoute(
    val destination: Destination,
    val target: String? = null,
    val variant: String = "content",
    val detail: String? = null,
    val frozen: Boolean = false,
)

internal enum class Appearance { System, Light, Dark }

internal data class LogOptions(
    val query: String = "",
    val errors: Boolean = false,
    val range: String = "Last 30 min",
    val live: Boolean = false,
)

internal data class DashboardEntry(val key: Int, val route: DashboardRoute)

internal class DashboardNavigation(showDesigns: Boolean) {
    private var nextKey = 1
    private val backStack = mutableStateListOf(
        DashboardEntry(0, DashboardRoute(if (showDesigns) Destination.DesignCases else Destination.Projects))
    )
    private val rootEntries = mutableMapOf<Destination, DashboardEntry>().apply {
        if (!showDesigns) put(Destination.Projects, backStack.first())
    }

    var workspaceId by mutableStateOf(DashboardFixtures.primaryWorkspace)
        private set
    var appearance by mutableStateOf(Appearance.Dark)
    var displayName by mutableStateOf("Jamie Example")
    var username by mutableStateOf("jamie-example")
    var defaultTeam by mutableStateOf("Personal")
    var landingPage by mutableStateOf("Personal")
    var samplePhoto by mutableStateOf(false)
    var agentEnabled by mutableStateOf(false)
    var lastActivity by mutableStateOf(Destination.Recents)
        private set
    val favorites = mutableStateListOf<String>().apply { addAll(DashboardFixtures.favorites) }
    val removedRecents = mutableStateListOf<String>()
    private val transfers = mutableStateMapOf<String, String>()
    private val projectNames = mutableStateMapOf<String, String>()
    private val logOptions = mutableStateMapOf<String, LogOptions>()

    val current get() = backStack.last()
    val screen get() = backStack.last { it.route.destination.presentation == Presentation.Screen }
    val currentWorkspace get() = DashboardFixtures.workspace(workspaceId)

    fun workspaceOf(project: Project) = transfers[project.id] ?: project.workspaceId
    fun projects(scope: String = workspaceId) = DashboardFixtures.projects.filter { workspaceOf(it) == scope }
    fun project(id: String?) = DashboardFixtures.project(id ?: projects().firstOrNull()?.id ?: DashboardFixtures.defaultProject)
    fun projectName(project: Project) = projectNames[project.id] ?: project.name
    fun rename(project: Project, name: String) { projectNames[project.id] = name }
    fun transfer(project: Project, scope: String) { transfers[project.id] = scope }
    fun logScope(projectId: String?) = projectId ?: workspaceId
    fun logOptions(projectId: String?) = logOptions[logScope(projectId)] ?: LogOptions()

    fun updateLogOptions(projectId: String?, update: (LogOptions) -> LogOptions) {
        val scope = logScope(projectId)
        logOptions[scope] = update(logOptions[scope] ?: LogOptions())
    }

    fun show(destination: Destination, target: String? = null, variant: String = "content", detail: String? = null) {
        open(DashboardRoute(destination, target, variant, detail))
    }

    fun open(route: DashboardRoute) {
        backStack.add(DashboardEntry(nextKey++, route))
    }

    fun replace(route: DashboardRoute) {
        check(backStack.size > 1)
        backStack.removeAt(backStack.lastIndex)
        open(route)
    }

    fun pop(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            return true
        }
        if (screen.route.destination in activityDestinations) {
            main(Destination.Projects)
            return true
        }
        return false
    }

    fun main(destination: Destination) {
        if (destination in activityDestinations) lastActivity = destination
        if (backStack.size == 1 && current.route.destination == destination) return
        val picker = backStack.firstOrNull()?.takeIf { it.route.destination == Destination.DesignCases }
        backStack.clear()
        if (picker != null) backStack.add(picker)
        backStack.add(rootEntries.getOrPut(destination) { DashboardEntry(nextKey++, DashboardRoute(destination)) })
    }

    fun chooseWorkspace(id: String) {
        workspaceId = id
        pop()
    }

    fun changeFavorite(id: String) {
        if (!favorites.remove(id)) favorites.add(id)
    }

    fun removeRecent(id: String) {
        if (id !in removedRecents) removedRecents.add(id)
    }

    fun openCase(case: DashboardDesignCase) {
        open(case.route.copy(frozen = true))
    }
}

@Composable
internal fun Dashboard(showDesigns: Boolean, onExit: () -> Unit) {
    val navigation = remember { DashboardNavigation(showDesigns) }
    val holder = rememberSaveableStateHolder()
    val top = navigation.current
    val base = navigation.screen
    val route = top.route
    val dark = when (navigation.appearance) {
        Appearance.System -> isSystemInDarkTheme()
        Appearance.Light -> false
        Appearance.Dark -> true
    } && route.variant != "light" && route.variant != "menu-add"
    val palette = if (dark) DashboardPalette.Dark else DashboardPalette.Light
    val scheme = if (dark) darkColorScheme(
        primary = palette.ink, onPrimary = palette.canvas, surface = palette.canvas,
        surfaceContainer = palette.sheet, onSurface = palette.ink, onSurfaceVariant = palette.secondary,
        outline = palette.border, secondary = palette.link, background = palette.canvas, error = palette.error,
    ) else lightColorScheme(
        primary = palette.ink, onPrimary = palette.canvas, surface = palette.canvas,
        surfaceContainer = palette.sheet, onSurface = palette.ink, onSurfaceVariant = palette.secondary,
        outline = palette.border, secondary = palette.link, background = palette.canvas, error = palette.error,
    )
    val onBack: () -> Unit = { if (!navigation.pop()) onExit() }
    BackHandler(onBack = onBack)

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalDashboardPalette provides palette) {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    val underlay = if (base.route.destination == Destination.DesignCases &&
                        top != base && route.destination.presentation != Presentation.Screen
                    ) {
                        DashboardEntry(-top.key, reviewedUnderlay(route))
                    } else base
                    AnimatedContent(
                        targetState = underlay,
                        transitionSpec = {
                            if (initialState.route.destination in activityDestinations && targetState.route.destination in activityDestinations) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                fadeIn(tween(Tokens.ScreenFadeMillis)) togetherWith fadeOut(tween(Tokens.ScreenFadeMillis))
                            }
                        },
                        label = "Dashboard destination",
                    ) { entry ->
                        val root = entry.route.destination == Destination.Projects || entry.route.destination in activityDestinations
                        val stateKey = if (root && !entry.route.frozen) {
                            "root-${entry.route.destination}-${navigation.workspaceId}"
                        } else "screen-${entry.key}"
                        holder.SaveableStateProvider(stateKey) {
                            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                                Box(Modifier.weight(1f).fillMaxWidth()) { DashboardScreen(entry.route, navigation, onBack) }
                                if (entry.route.destination !in setOf(Destination.DesignCases, Destination.Agent, Destination.Profile) &&
                                    route.destination != Destination.Search
                                ) DashboardBottomBar(entry.route, navigation)
                            }
                        }
                    }
                    if (top != base) {
                        holder.SaveableStateProvider("overlay-${top.key}") {
                            when (route.destination.presentation) {
                                Presentation.Sheet -> DashboardSheetHost(route, navigation, onBack)
                                Presentation.Search -> SearchOverlay(route, navigation, onBack)
                                Presentation.Rail -> WorkspaceRail(route, navigation, onBack)
                                Presentation.Screen -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun reviewedUnderlay(route: DashboardRoute): DashboardRoute {
    return when (route.destination) {
        Destination.LogFilters, Destination.LogDownload -> DashboardRoute(Destination.Logs, route.target, "query")
        Destination.LogEvent -> DashboardRoute(Destination.Logs, route.detail)
        Destination.RemoveRecent -> DashboardRoute(Destination.Recents)
        Destination.Photo, Destination.DefaultScope -> DashboardRoute(Destination.Profile)
        Destination.Preview -> DashboardRoute(Destination.Deployment, route.target)
        Destination.ProjectDomains, Destination.GitHubSync -> DashboardRoute(Destination.Project, route.target)
        Destination.Domain -> DashboardRoute(Destination.Project, route.target?.let(DashboardFixtures::domain)?.projectId)
        Destination.More -> DashboardRoute(if (route.variant in setOf("workspace", "workspace-failed")) Destination.Usage else Destination.Projects)
        Destination.Search -> DashboardRoute(if (route.variant == "activity") Destination.Recents else Destination.Projects)
        else -> DashboardRoute(Destination.Projects)
    }
}

@Composable
private fun DashboardScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    when (route.destination) {
        Destination.Projects -> ProjectsScreen(route, navigation)
        Destination.Project -> ProjectScreen(route, navigation, onBack)
        Destination.Recents, Destination.Usage, Destination.Alerts -> ActivityScreen(route, navigation)
        Destination.Deployment -> DeploymentScreen(route, navigation, onBack)
        Destination.Alert -> AlertScreen(route, navigation, onBack)
        Destination.ProjectSettings -> ProjectSettingsScreen(route, navigation, onBack)
        Destination.Logs -> LogsScreen(route, navigation, onBack)
        Destination.Profile, Destination.Agent -> AccountScreen(route, navigation, onBack)
        Destination.DesignCases -> DashboardDesignPicker(navigation, onBack)
        else -> WorkspaceScreen(route, navigation, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardSheetHost(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onBack,
        sheetState = sheetState,
        containerColor = palette.sheet,
        contentColor = palette.ink,
        scrimColor = palette.canvas.copy(alpha = Tokens.ScrimOpacity),
        shape = RoundedCornerShape(topStart = Tokens.SheetRadius, topEnd = Tokens.SheetRadius),
        dragHandle = {
            Box(
                Modifier.padding(top = Tokens.Space8, bottom = Tokens.Space4)
                    .size(width = Tokens.RowIcon, height = Tokens.Space4)
                    .background(palette.border, CircleShape)
            )
        },
    ) {
        val maxHeight = if (route.destination in setOf(Destination.Project, Destination.ProjectDomains, Destination.Transfer)) {
            Tokens.SheetMaxHeight
        } else Tokens.CompactSheetMaxHeight
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            when (route.destination) {
                Destination.Project, Destination.ProjectDomains, Destination.Domain, Destination.GitHubSync, Destination.Transfer ->
                    ProjectSheetContent(route, navigation, onBack)
                Destination.Preview, Destination.Toolbar, Destination.RemoveRecent ->
                    DeploymentSheetContent(route, navigation, onBack)
                Destination.LogFilters, Destination.LogEvent, Destination.LogDownload ->
                    LogSheetContent(route, navigation, onBack)
                Destination.Help -> HelpSheet(onBack)
                else -> AccountSheetContent(route, navigation, onBack)
            }
        }
    }
}

@Composable
private fun DashboardBottomBar(route: DashboardRoute, navigation: DashboardNavigation) {
    val palette = LocalDashboardPalette.current
    val activity = route.destination in activityDestinations
    Surface(color = palette.canvas) {
        Row(
            Modifier.fillMaxWidth().height(Tokens.BottomBar).padding(horizontal = Tokens.Inset, vertical = Tokens.Space4),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                Modifier.weight(1f).fillMaxHeight(),
                shape = CircleShape, color = palette.input,
                border = androidx.compose.foundation.BorderStroke(Tokens.Stroke, palette.border),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DashboardNavItem("Home", Glyph.Project, !activity, route.variant == "icons", Modifier.weight(1f)) { navigation.main(Destination.Projects) }
                    DashboardNavItem("Activity", Glyph.Activity, activity, route.variant == "icons", Modifier.weight(1f)) { navigation.main(navigation.lastActivity) }
                    DashboardNavItem("More", Glyph.More, false, route.variant == "icons", Modifier.weight(1f)) { navigation.show(Destination.More) }
                }
            }
            IconControl(Glyph.Search, "Search Vercel", { navigation.show(Destination.Search) })
        }
    }
}

@Composable
private fun DashboardNavItem(
    text: String, glyph: Glyph, selected: Boolean, iconsOnly: Boolean, modifier: Modifier, onClick: () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    Surface(onClick, modifier.padding(Tokens.Space4).fillMaxHeight(), shape = CircleShape, color = if (selected) palette.selected else palette.input) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            DashboardIcon(glyph, if (selected) palette.ink else palette.secondary, Modifier)
            if (!iconsOnly) Text(text, fontSize = Tokens.Caption)
        }
    }
}
