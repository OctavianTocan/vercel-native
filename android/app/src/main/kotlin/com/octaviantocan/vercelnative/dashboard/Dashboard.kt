package com.octaviantocan.vercelnative.dashboard

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlinx.coroutines.launch

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
    var lastActivity by mutableStateOf(Destination.Recents)
        private set
    val favorites = mutableStateListOf<String>().apply { addAll(DashboardFixtures.favorites) }
    val removedRecents = mutableStateListOf<String>()
    private val transfers = mutableStateMapOf<String, String>()
    private val projectNames = mutableStateMapOf<String, String>()
    private val logOptions = mutableStateMapOf<String, LogOptions>()
    private val agentEnablement = mutableStateMapOf<String, Boolean>()

    val current get() = backStack.last()
    val screen get() = backStack.last { it.route.destination.presentation == Presentation.Screen }
    val hasParentSheet get() = backStack.getOrNull(backStack.lastIndex - 1)?.route?.destination?.presentation == Presentation.Sheet
    val currentWorkspace get() = DashboardFixtures.workspace(workspaceId)

    fun workspaceOf(project: Project) = transfers[project.id] ?: project.workspaceId
    fun projects(scope: String = workspaceId) = DashboardFixtures.projects.filter { workspaceOf(it) == scope }
    fun project(id: String?) = DashboardFixtures.project(id ?: projects().firstOrNull()?.id ?: DashboardFixtures.defaultProject)
    fun alerts() = DashboardFixtures.alerts.filter { workspaceOf(project(it.projectId)) == workspaceId }
    fun projectName(project: Project) = projectNames[project.id] ?: project.name
    fun rename(project: Project, name: String) { projectNames[project.id] = name }
    fun transfer(project: Project, scope: String) { transfers[project.id] = scope }
    fun logScope(projectId: String?) = projectId ?: workspaceId
    fun logOptions(projectId: String?) = logOptions[logScope(projectId)] ?: LogOptions()
    fun isAgentEnabled(workspaceId: String) = agentEnablement[workspaceId] == true
    fun enableAgent(workspaceId: String) { agentEnablement[workspaceId] = true }

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

internal class DashboardPreviewViewModel : ViewModel() {
    var navigation by mutableStateOf<DashboardNavigation?>(null)
        private set

    fun open(showDesigns: Boolean) {
        navigation = DashboardNavigation(showDesigns)
    }

    fun close() {
        navigation = null
    }
}

@Composable
internal fun Dashboard(navigation: DashboardNavigation, onExit: () -> Unit) {
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
    DashboardSystemBars(dark)
    val systemInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)

    MaterialTheme(colorScheme = scheme, typography = DashboardTypography) {
        CompositionLocalProvider(LocalDashboardPalette provides palette) {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    val content = rememberGraphicsLayer()
                    val backdrop = rememberGraphicsLayer()
                    val underlay = if (base.route.destination == Destination.DesignCases &&
                        top != base && route.destination.presentation != Presentation.Screen
                    ) {
                        DashboardEntry(-top.key, reviewedUnderlay(route))
                    } else base
                    val showDock = route.destination != Destination.Search && underlay.route.destination.hasDock()
                    val searching = route.destination == Destination.Search
                    val blur = if (searching) Tokens.SearchBackdropBlur else Tokens.GlassBlur
                    Box(Modifier.matchParentSize().captureDashboardBackdrop(content, backdrop, blur, searching).background(palette.canvas)) {
                        AnimatedContent(
                            targetState = underlay,
                            contentKey = { entry ->
                                if (entry.route.destination in activityDestinations && !entry.route.frozen) "activity" else entry.key
                            },
                            transitionSpec = {
                                val direction = if (initialState.key > targetState.key) -1 else 1
                                val duration = if (initialState.route.destination in activityDestinations &&
                                    targetState.route.destination in activityDestinations) DashboardMotion.Fast else DashboardMotion.Slow
                                (fadeIn(DashboardMotion.move(duration)) +
                                    slideInHorizontally(DashboardMotion.move(duration)) { direction * it / 6 }) togetherWith
                                    (fadeOut(DashboardMotion.move(DashboardMotion.Quick)) +
                                        slideOutHorizontally(DashboardMotion.move(duration)) { -direction * it / 12 })
                            },
                            label = "Dashboard destination",
                        ) { entry ->
                            val destination = entry.route.destination
                            val root = destination == Destination.Projects || destination in activityDestinations
                                val stateKey = if (root && !entry.route.frozen && entry.key >= 0) {
                                    "root-${if (destination in activityDestinations) "Activity" else destination.name}-${navigation.workspaceId}"
                            } else "screen-${entry.key}"
                            holder.SaveableStateProvider(stateKey) {
                                Box(
                                    Modifier.fillMaxSize().windowInsetsPadding(systemInsets).then(
                                        if (showDock && destination.hasDock() && !root) {
                                            Modifier.padding(bottom = Tokens.BottomBar + Tokens.DockBottomGap)
                                        } else Modifier,
                                    ),
                                ) { DashboardScreen(entry.route, navigation, onBack) }
                            }
                        }
                    }
                    if (showDock) DashboardBottomBar(
                        activity = underlay.route.destination in activityDestinations,
                        iconsOnly = underlay.route.variant == "icons",
                        navigation = navigation,
                        backdrop = content,
                        modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(
                            systemInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                        ),
                    )
                    val overlay = top.takeIf { top != base }
                    AnimatedContent(
                        targetState = overlay?.takeIf { it.route.destination.presentation != Presentation.Sheet },
                        modifier = Modifier.matchParentSize(),
                        transitionSpec = {
                            val entering = targetState?.route?.destination?.presentation
                            val leaving = initialState?.route?.destination?.presentation
                            val enter = fadeIn(DashboardMotion.move(DashboardMotion.Fast)) + when (entering) {
                                Presentation.Rail -> slideInHorizontally(DashboardMotion.move(DashboardMotion.Medium)) { -it / 8 }
                                else -> slideInVertically(DashboardMotion.move(DashboardMotion.Fast)) { it / 18 }
                            }
                            val exit = fadeOut(DashboardMotion.move(DashboardMotion.Quick)) + when (leaving) {
                                Presentation.Rail -> slideOutHorizontally(DashboardMotion.move(DashboardMotion.Fast)) { -it / 8 }
                                else -> slideOutVertically(DashboardMotion.move(DashboardMotion.Fast)) { it / 18 }
                            }
                            enter togetherWith exit
                        },
                        label = "Dashboard overlay",
                    ) { entry ->
                        if (entry != null) holder.SaveableStateProvider("overlay-${entry.key}") {
                            when (entry.route.destination.presentation) {
                                Presentation.Search -> SearchOverlay(entry.route, navigation, onBack, content, entry == overlay)
                                Presentation.Rail -> WorkspaceRail(entry.route, navigation, onBack, backdrop)
                                else -> Unit
                            }
                        }
                    }
                    if (overlay?.route?.destination?.presentation == Presentation.Sheet) {
                        DashboardSheetHost(overlay, navigation, onBack, holder)
                    }
                }
            }
        }
    }
}

private fun Destination.hasDock() = this !in setOf(Destination.DesignCases, Destination.Agent, Destination.Profile)

@Composable
private fun DashboardSystemBars(dark: Boolean) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    DisposableEffect(window, view, dark) {
        val bars = WindowCompat.getInsetsController(window, view)
        val previousStatus = bars.isAppearanceLightStatusBars
        val previousNavigation = bars.isAppearanceLightNavigationBars
        bars.isAppearanceLightStatusBars = !dark
        bars.isAppearanceLightNavigationBars = !dark
        onDispose {
            bars.isAppearanceLightStatusBars = previousStatus
            bars.isAppearanceLightNavigationBars = previousNavigation
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
private fun DashboardSheetHost(entry: DashboardEntry, navigation: DashboardNavigation, onBack: () -> Unit, holder: SaveableStateHolder) {
    val palette = LocalDashboardPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val project = entry.route.destination == Destination.Project
    val scope = rememberCoroutineScope()
    var closing by remember(entry.key) { mutableStateOf(false) }
    val close: () -> Unit = {
        if (navigation.hasParentSheet) onBack()
        else if (!closing) {
            closing = true
            scope.launch {
                sheetState.hide()
                if (!sheetState.isVisible && navigation.current == entry) onBack()
                closing = false
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onBack,
        modifier = if (project) Modifier.padding(bottom = Tokens.Inset) else Modifier,
        sheetState = sheetState,
        containerColor = palette.sheet,
        contentColor = palette.ink,
        scrimColor = palette.canvas.copy(alpha = Tokens.ScrimOpacity),
        shape = if (project) RoundedCornerShape(Tokens.ProjectSheetRadius)
        else RoundedCornerShape(topStart = Tokens.SheetRadius, topEnd = Tokens.SheetRadius),
        dragHandle = {
            Box(
                Modifier.padding(top = if (project) Tokens.Space4 else Tokens.Space8,
                    bottom = if (project) Tokens.Space2 else Tokens.Space4)
                    .size(width = Tokens.RowIcon, height = Tokens.Space4)
                    .background(palette.border, CircleShape)
            )
        },
    ) {
        AnimatedContent(
            targetState = entry,
            transitionSpec = {
                val direction = if (initialState.key > targetState.key) -1 else 1
                (fadeIn(DashboardMotion.move(DashboardMotion.Fast)) +
                    slideInHorizontally(DashboardMotion.move(DashboardMotion.Medium)) { direction * it / 8 }) togetherWith
                    fadeOut(DashboardMotion.move(DashboardMotion.Quick))
            },
            label = "Sheet destination",
        ) { current ->
            val route = current.route
            val maxHeight = when (route.destination) {
                Destination.Project -> Tokens.ProjectSheetBody
                Destination.ProjectDomains, Destination.Transfer -> Tokens.SheetMaxHeight
                else -> Tokens.CompactSheetMaxHeight
            }
            holder.SaveableStateProvider("overlay-${current.key}") {
                CompositionLocalProvider(LocalDashboardScrollPaper provides palette.sheet) {
                    Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
                        when (route.destination) {
                            Destination.Project, Destination.ProjectDomains, Destination.Domain, Destination.GitHubSync, Destination.Transfer ->
                                ProjectSheetContent(route, navigation, close)
                            Destination.Preview, Destination.Toolbar, Destination.RemoveRecent ->
                                DeploymentSheetContent(route, navigation, close)
                            Destination.LogFilters, Destination.LogEvent, Destination.LogDownload ->
                                LogSheetContent(route, navigation, close)
                            Destination.Help -> HelpSheet(close, docsOnly = route.variant == "docs")
                            else -> AccountSheetContent(route, navigation, close)
                        }
                    }
                }
            }
        }
    }
}
