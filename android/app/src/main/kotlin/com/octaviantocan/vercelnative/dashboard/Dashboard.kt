package com.octaviantocan.vercelnative.dashboard

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlinx.coroutines.launch

internal enum class Presentation { Screen, Sheet, Rail, Search }

private val activityDestinations = setOf(Destination.Recents, Destination.Usage, Destination.Alerts)
private val activityDetailDesigns = setOf(Destination.Deployment, Destination.Alert, Destination.Logs,
    Destination.Observability, Destination.Analytics, Destination.Speed, Destination.Firewall)

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
    var defaultTeam by mutableStateOf<String?>(null)
    var landingPage by mutableStateOf<String?>(null)
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
    val sheets get() = backStack.takeLastWhile { it.route.destination.presentation == Presentation.Sheet }
    val parentRail get() = backStack.getOrNull(backStack.lastIndex - sheets.size)
        ?.takeIf { it.route.destination.presentation == Presentation.Rail }
    val currentWorkspace get() = DashboardFixtures.workspace(workspaceId)

    fun highlightsActivity(entry: DashboardEntry): Boolean {
        val destination = entry.route.destination
        if (destination == Destination.Projects) return false
        if (destination in activityDestinations) return true
        val root = backStack.lastOrNull { route ->
            route.route.destination == Destination.Projects || route.route.destination in activityDestinations
        }
        return root?.let { it.route.destination in activityDestinations }
            ?: (destination in activityDetailDesigns)
    }

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

    fun chooseAppearance(value: Appearance) {
        appearance = value
        val parent = backStack.lastIndex - 1
        if (backStack.getOrNull(parent)?.route?.destination == Destination.More) backStack.removeAt(parent)
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
    } && route.variant != "light" && route.variant != "menu-add" &&
        !(route.destination == Destination.Projects && route.variant == "menu-earlier")
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
    val density = LocalDensity.current
    val imeUp = WindowInsets.ime.getBottom(density) > systemInsets.getBottom(density)

    MaterialTheme(colorScheme = scheme, typography = DashboardTypography) {
        CompositionLocalProvider(
            LocalDashboardPalette provides palette,
            LocalTextStyle provides GeistText.copy(fontSize = Tokens.Body),
        ) {
            Surface(Modifier.fillMaxSize(), color = palette.canvas, contentColor = palette.ink) {
                Box(Modifier.fillMaxSize()) {
                    val content = rememberGraphicsLayer()
                    val backdrop = rememberGraphicsLayer()
                    val underlay = if (base.route.destination == Destination.DesignCases &&
                        top != base && route.destination.presentation != Presentation.Screen
                    ) {
                        DashboardEntry(-top.key, reviewedUnderlay(route))
                    } else base
                    val showDock = route.destination != Destination.Search && !imeUp && underlay.route.destination.hasDock()
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
                                val from = initialState.route.destination
                                val to = targetState.route.destination
                                if ((from == Destination.Projects || from in activityDestinations) &&
                                    (to == Destination.Projects || to in activityDestinations)
                                ) {
                                    fadeIn(DashboardMotion.prototype(DashboardMotion.Dissolve)) togetherWith
                                        fadeOut(DashboardMotion.prototype(DashboardMotion.Dissolve))
                                } else {
                                    slideInHorizontally(DashboardMotion.push(direction < 0)) { direction * it } togetherWith
                                        slideOutHorizontally(DashboardMotion.push(direction < 0)) { -direction * it }
                                }
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
                        activity = navigation.highlightsActivity(underlay),
                        iconsOnly = underlay.route.variant == "icons",
                        navigation = navigation,
                        backdrop = content,
                        modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(
                            systemInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                        ),
                    )
                    val overlay = top.takeIf { top != base }
                    val railUnderSheet = navigation.parentRail ?: if (route.frozen && route.variant != "light" &&
                        route.destination in setOf(Destination.Theme, Destination.Help)
                    ) DashboardEntry(-top.key - 1, DashboardRoute(Destination.More)) else null
                    AnimatedContent(
                        targetState = overlay?.takeIf { it.route.destination.presentation != Presentation.Sheet } ?: railUnderSheet,
                        modifier = Modifier.matchParentSize(),
                        transitionSpec = {
                            val entering = targetState?.route?.destination?.presentation
                            val leaving = initialState?.route?.destination?.presentation
                            val enter = when (entering) {
                                Presentation.Rail -> slideInHorizontally(DashboardMotion.prototype(DashboardMotion.Rail)) { -it }
                                Presentation.Search -> slideInVertically(DashboardMotion.prototype(DashboardMotion.Search)) { it }
                                else -> fadeIn(DashboardMotion.prototype(DashboardMotion.Dissolve))
                            }
                            val exit = when (leaving) {
                                Presentation.Rail -> slideOutHorizontally(DashboardMotion.dismiss()) { -it }
                                Presentation.Search -> slideOutVertically(DashboardMotion.dismiss()) { it }
                                else -> fadeOut(DashboardMotion.prototype(DashboardMotion.Dissolve))
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
                        val projectUnderlay = if (route.frozen && route.destination in
                            setOf(Destination.ProjectDomains, Destination.Domain, Destination.GitHubSync)
                        ) {
                            val projectId = if (route.destination == Destination.Domain) {
                                route.target?.let(DashboardFixtures::domain)?.projectId
                            } else route.target
                            DashboardEntry(-top.key - 1, DashboardRoute(Destination.Project, projectId))
                        } else null
                        val shown = listOfNotNull(projectUnderlay) + navigation.sheets
                        shown.forEachIndexed { index, sheet ->
                            key(index) { DashboardSheetHost(sheet, navigation, onBack, holder) }
                        }
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
        Destination.LogFilters -> DashboardRoute(Destination.Logs, route.target, frozen = true)
        Destination.LogDownload -> DashboardRoute(Destination.Logs, route.target, "query", frozen = true)
        Destination.LogEvent -> DashboardRoute(Destination.Logs, route.detail, frozen = true)
        Destination.RemoveRecent -> DashboardRoute(Destination.Recents)
        Destination.Photo, Destination.DefaultScope -> DashboardRoute(Destination.Profile)
        Destination.Help -> DashboardRoute(Destination.Usage)
        Destination.Theme -> if (route.variant == "light") DashboardRoute(Destination.Projects, variant = "theme-plain")
            else DashboardRoute(Destination.Usage)
        Destination.Preview -> DashboardRoute(Destination.Deployment, route.target)
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
    val destination = entry.route.destination
    val project = destination == Destination.Project
    val sheetPaper = when (destination) {
        Destination.Theme -> if (palette == DashboardPalette.Light) palette.themeLightSheet else palette.sheet
        Destination.Photo, Destination.DefaultScope -> if (palette == DashboardPalette.Light) palette.sheet else palette.projectOptionsSurface
        else -> palette.sheet
    }
    val handleColor = when {
        project -> palette.projectSheetHandle
        destination == Destination.LogFilters -> palette.logFilterHandle
        palette == DashboardPalette.Light -> palette.sheetHandle
        destination in setOf(Destination.Theme, Destination.Toolbar, Destination.RemoveRecent, Destination.Help) -> palette.muted
        destination == Destination.Transfer && entry.route.variant in setOf("review", "progress", "done") -> palette.muted
        else -> palette.sheetHandle
    }
    val scope = rememberCoroutineScope()
    var closing by remember(entry.key) { mutableStateOf(false) }
    val close: () -> Unit = {
        if (!closing && navigation.current == entry) {
            closing = true
            scope.launch {
                sheetState.hide()
                if (!sheetState.isVisible && navigation.current == entry) onBack()
                closing = false
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = {
            if (navigation.current == entry) onBack()
        },
        modifier = if (project) {
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom))
                .padding(top = Tokens.Inset, bottom = Tokens.Inset)
        } else Modifier,
        sheetState = sheetState,
        containerColor = sheetPaper,
        contentColor = palette.ink,
        scrimColor = if (project) palette.canvas else Color.Black.copy(
            alpha = if (palette == DashboardPalette.Dark) Tokens.ScrimOpacity else Tokens.LightSheetScrimOpacity,
        ),
        contentWindowInsets = { if (project) WindowInsets(0, 0, 0, 0) else BottomSheetDefaults.windowInsets },
        shape = if (project) RoundedCornerShape(Tokens.ProjectSheetRadius)
        else {
            val radius = if (destination in setOf(Destination.Photo, Destination.DefaultScope) ||
                (destination == Destination.Theme && palette == DashboardPalette.Light)) Tokens.AccountSheetRadius else Tokens.SheetRadius
            RoundedCornerShape(topStart = radius, topEnd = radius)
        },
        dragHandle = {
            Box(
                Modifier.padding(top = if (project) Tokens.Space4 else Tokens.Space8,
                    bottom = if (project) Tokens.Space2 else Tokens.Space4)
                    .size(width = Tokens.SheetHandleWidth, height = Tokens.Space4)
                    .background(handleColor, CircleShape)
            )
        },
    ) {
        AnimatedContent(
            targetState = entry,
            transitionSpec = {
                val from = initialState.route
                val to = targetState.route
                if (from.destination == Destination.Transfer && from.variant == "content" &&
                    to.destination == Destination.Transfer && to.variant == "review"
                ) {
                    (slideInHorizontally(DashboardMotion.push(back = true)) { -it } togetherWith
                        slideOutHorizontally(DashboardMotion.push(back = true)) { it }).using(
                        SizeTransform { _, _ -> DashboardMotion.push(back = true) },
                    )
                } else {
                    (fadeIn(DashboardMotion.prototype(DashboardMotion.Tabs)) togetherWith
                        fadeOut(DashboardMotion.prototype(DashboardMotion.Tabs))).using(
                        SizeTransform { _, _ -> DashboardMotion.prototype(DashboardMotion.Tabs) },
                    )
                }
            },
            label = "Sheet destination",
        ) { current ->
            val route = current.route
            val maxHeight = when (route.destination) {
                Destination.Project -> Tokens.ProjectSheetBody
                Destination.ProjectDomains -> Tokens.ProjectDomainsBody
                Destination.Domain -> Tokens.DomainVerificationBody
                Destination.LogEvent -> Tokens.LogEventBody
                Destination.Transfer -> Tokens.SheetMaxHeight
                else -> Tokens.CompactSheetMaxHeight
            }
            holder.SaveableStateProvider("overlay-${current.key}") {
                CompositionLocalProvider(LocalDashboardScrollPaper provides sheetPaper) {
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
