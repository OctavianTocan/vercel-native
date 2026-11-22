package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

internal data class DashboardDesignCase(
    val frameId: String, val title: String, val group: String, val symbol: String, val route: DashboardRoute,
)

private val designCases = listOf(
// BEGIN GENERATED DESIGN CASES
    DashboardDesignCase("402:4957", "Projects with Favorites", "Projects and Favorites", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "content", null)),
    DashboardDesignCase("228:2651", "Projects main", "Projects and Favorites", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "plain", null)),
    DashboardDesignCase("228:2750", "Icon-only navigation", "Projects and Favorites", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "icons", null)),
    DashboardDesignCase("604:747", "Loading projects", "Projects and Favorites", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "loading", null)),
    DashboardDesignCase("479:3002", "Empty Projects", "Projects and Favorites", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "empty", null)),
    DashboardDesignCase("604:893", "Projects could not load", "Projects and Favorites", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "error", null)),
    DashboardDesignCase("604:4911", "Add Favorite menu", "Projects and Favorites", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "menu-add", null)),
    DashboardDesignCase("604:3584", "Remove Favorite menu", "Projects and Favorites", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "menu-remove", null)),
    DashboardDesignCase("340:386", "Earlier project menu", "Projects and Favorites", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "menu-earlier", null)),
    DashboardDesignCase("552:2501", "Favorite update failed", "Projects and Favorites", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "favorite-failed", null)),
    DashboardDesignCase("535:3158", "Project overview", "Project Sheet and Domains", "ProjectScreen", DashboardRoute(Destination.Project, "prj-personal-1", "content", null)),
    DashboardDesignCase("468:4699", "Compact Project Sheet", "Project Sheet and Domains", "ProjectScreen", DashboardRoute(Destination.Project, "prj-personal-1", "compact", null)),
    DashboardDesignCase("604:3083", "GitHub sync status", "Project Sheet and Domains", "GitHubSync", DashboardRoute(Destination.GitHubSync, "prj-personal-1", "content", null)),
    DashboardDesignCase("604:938", "Project domains", "Project Sheet and Domains", "ProjectDomainsScreen", DashboardRoute(Destination.ProjectDomains, "prj-personal-1", "content", null)),
    DashboardDesignCase("604:1084", "Verify DNS", "Project Sheet and Domains", "DomainDetails", DashboardRoute(Destination.Domain, "dom-prj-personal-1-1", "verify", null)),
    DashboardDesignCase("604:3224", "Domain management access", "Project Sheet and Domains", "DomainDetails", DashboardRoute(Destination.Domain, "dom-prj-personal-1-2", "restricted", null)),
    DashboardDesignCase("584:2553", "Choose destination team", "Project Transfer and Settings", "TransferSheet", DashboardRoute(Destination.Transfer, "prj-personal-1", "content", null)),
    DashboardDesignCase("535:1457", "Review transfer impact", "Project Transfer and Settings", "TransferSheet", DashboardRoute(Destination.Transfer, "prj-personal-1", "review", null)),
    DashboardDesignCase("535:1680", "Transfer in progress", "Project Transfer and Settings", "TransferSheet", DashboardRoute(Destination.Transfer, "prj-personal-1", "progress", null)),
    DashboardDesignCase("552:2113", "Transfer complete", "Project Transfer and Settings", "TransferSheet", DashboardRoute(Destination.Transfer, "prj-personal-1", "done", null)),
    DashboardDesignCase("535:2057", "Project opened in Studio", "Project Transfer and Settings", "ProjectScreen", DashboardRoute(Destination.Project, "prj-studio-1", "transferred", null)),
    DashboardDesignCase("604:3363", "Transfer failed", "Project Transfer and Settings", "TransferSheet", DashboardRoute(Destination.Transfer, "prj-personal-1", "failed", null)),
    DashboardDesignCase("511:701", "Project settings", "Project Transfer and Settings", "ProjectSettingsScreen", DashboardRoute(Destination.ProjectSettings, "prj-personal-1", "content", null)),
    DashboardDesignCase("511:775", "Edit project name", "Project Transfer and Settings", "ProjectSettingsScreen", DashboardRoute(Destination.ProjectSettings, "prj-personal-1", "editing", null)),
    DashboardDesignCase("552:1922", "Project name conflict", "Project Transfer and Settings", "ProjectSettingsScreen", DashboardRoute(Destination.ProjectSettings, "prj-personal-2", "conflict", null)),
    DashboardDesignCase("468:4800", "Project Deployments", "Deployments and Previews", "ProjectScreen", DashboardRoute(Destination.Project, "prj-personal-1", "deployments", null)),
    DashboardDesignCase("604:1231", "Project deployment options", "Deployments and Previews", "ProjectScreen", DashboardRoute(Destination.Project, "prj-personal-1", "deployment-menu", null)),
    DashboardDesignCase("535:2969", "Branch filter applied", "Deployments and Previews", "ProjectScreen", DashboardRoute(Destination.Project, "prj-personal-1", "branch", null)),
    DashboardDesignCase("512:1592", "Branch filter cleared", "Deployments and Previews", "ProjectScreen", DashboardRoute(Destination.Project, "prj-personal-1", "cleared", null)),
    DashboardDesignCase("535:3264", "Deployment details", "Deployments and Previews", "DeploymentScreen", DashboardRoute(Destination.Deployment, "dpl-personal-1-1", "content", null)),
    DashboardDesignCase("468:6631", "Centered deployment heading", "Deployments and Previews", "DeploymentScreen", DashboardRoute(Destination.Deployment, "dpl-personal-1-1", "heading", null)),
    DashboardDesignCase("586:176", "Open preview in browser", "Deployments and Previews", "DeploymentSheetContent", DashboardRoute(Destination.Preview, "dpl-personal-1-1", "content", null)),
    DashboardDesignCase("586:274", "Protected preview", "Deployments and Previews", "DeploymentSheetContent", DashboardRoute(Destination.Preview, "dpl-personal-1-1", "protected", null)),
    DashboardDesignCase("604:3774", "Preview unavailable", "Deployments and Previews", "DeploymentSheetContent", DashboardRoute(Destination.Preview, "dpl-personal-1-1", "unavailable", null)),
    DashboardDesignCase("552:2311", "Toolbar unavailable", "Deployments and Previews", "DeploymentSheetContent", DashboardRoute(Destination.Toolbar, "dpl-personal-1-1", "content", null)),
    DashboardDesignCase("225:2", "Recents", "Activity · Recents", "RecentsList", DashboardRoute(Destination.Recents, null, "content", null)),
    DashboardDesignCase("479:3051", "Empty Recents", "Activity · Recents", "RecentsList", DashboardRoute(Destination.Recents, null, "empty", null)),
    DashboardDesignCase("512:976", "Recents deployment options", "Activity · Recents", "RecentsList", DashboardRoute(Destination.Recents, null, "recents-menu", null)),
    DashboardDesignCase("468:6678", "Evenly spaced Recents menu", "Activity · Recents", "RecentsList", DashboardRoute(Destination.Recents, null, "menu-spacing", null)),
    DashboardDesignCase("339:740", "Earlier Recents menu", "Activity · Recents", "RecentsList", DashboardRoute(Destination.Recents, null, "menu-earlier", null)),
    DashboardDesignCase("604:3882", "Confirm Recents removal", "Activity · Recents", "DeploymentSheetContent", DashboardRoute(Destination.RemoveRecent, "dpl-personal-1-1", "content", null)),
    DashboardDesignCase("586:708", "Removed from Recents", "Activity · Recents", "RecentsList", DashboardRoute(Destination.Recents, null, "removed", null)),
    DashboardDesignCase("225:257", "Alerts inbox", "Activity · Alerts", "AlertsList", DashboardRoute(Destination.Alerts, null, "content", null)),
    DashboardDesignCase("604:4355", "Loading Alerts", "Activity · Alerts", "AlertsList", DashboardRoute(Destination.Alerts, null, "loading", null)),
    DashboardDesignCase("479:3118", "Empty Alerts", "Activity · Alerts", "AlertsList", DashboardRoute(Destination.Alerts, null, "empty", null)),
    DashboardDesignCase("604:4529", "Alerts unavailable on this plan", "Activity · Alerts", "AlertsList", DashboardRoute(Destination.Alerts, null, "plan", null)),
    DashboardDesignCase("587:1267", "Function error alert", "Activity · Alerts", "AlertScreen", DashboardRoute(Destination.Alert, "alt-personal-0", "content", null)),
    DashboardDesignCase("587:1391", "CDN requests alert", "Activity · Alerts", "AlertScreen", DashboardRoute(Destination.Alert, "alt-personal-1", "content", null)),
    DashboardDesignCase("587:1515", "Custom monitor alert", "Activity · Alerts", "AlertScreen", DashboardRoute(Destination.Alert, "alt-personal-2", "content", null)),
    DashboardDesignCase("225:163", "Daily Usage", "Activity · Usage", "UsageScreen", DashboardRoute(Destination.Usage, null, "content", null)),
    DashboardDesignCase("587:484", "Weekly Usage", "Activity · Usage", "UsageScreen", DashboardRoute(Destination.Usage, null, "weekly", null)),
    DashboardDesignCase("587:718", "Monthly Usage", "Activity · Usage", "UsageScreen", DashboardRoute(Destination.Usage, null, "monthly", null)),
    DashboardDesignCase("513:2445", "Usage in Studio", "Activity · Usage", "UsageScreen", DashboardRoute(Destination.Usage, null, "studio", null)),
    DashboardDesignCase("587:949", "No recorded usage", "Activity · Usage", "UsageScreen", DashboardRoute(Destination.Usage, null, "empty", null)),
    DashboardDesignCase("604:4293", "Usage could not load", "Activity · Usage", "UsageScreen", DashboardRoute(Destination.Usage, null, "error", null)),
    DashboardDesignCase("479:1344", "Search entry from Projects", "Global Search", "ProjectsScreen", DashboardRoute(Destination.Projects, null, "content", null)),
    DashboardDesignCase("479:1505", "Search suggestions", "Global Search", "SearchOverlay", DashboardRoute(Destination.Search, null, "suggestions", null)),
    DashboardDesignCase("513:1112", "Typed search results", "Global Search", "SearchOverlay", DashboardRoute(Destination.Search, null, "typed", null)),
    DashboardDesignCase("479:1876", "Mixed results over Projects", "Global Search", "SearchOverlay", DashboardRoute(Destination.Search, null, "mixed", null)),
    DashboardDesignCase("479:2571", "Mixed results over Activity", "Global Search", "SearchOverlay", DashboardRoute(Destination.Search, null, "activity", null)),
    DashboardDesignCase("513:1485", "Navigation Assistant entry", "Global Search", "SearchOverlay", DashboardRoute(Destination.Search, null, "no-match", null)),
    DashboardDesignCase("479:2249", "Search fallback", "Global Search", "SearchOverlay", DashboardRoute(Destination.Search, null, "fallback", null)),
    DashboardDesignCase("604:4113", "Navigation Assistant after Send", "Global Search", "SearchOverlay", DashboardRoute(Destination.Search, null, "submitted", null)),
    DashboardDesignCase("552:2673", "Search query failed", "Global Search", "SearchOverlay", DashboardRoute(Destination.Search, null, "error", null)),
    DashboardDesignCase("513:2132", "Workspace switcher", "Workspace and Tools", "WorkspaceRail", DashboardRoute(Destination.More, null, "workspace", null)),
    DashboardDesignCase("228:2849", "More menu", "Workspace and Tools", "WorkspaceRail", DashboardRoute(Destination.More, null, "content", null)),
    DashboardDesignCase("344:410", "Account Settings entry", "Workspace and Tools", "WorkspaceRail", DashboardRoute(Destination.More, null, "account", null)),
    DashboardDesignCase("513:2671", "Workspace switch failed", "Workspace and Tools", "WorkspaceRail", DashboardRoute(Destination.More, null, "workspace-failed", null)),
    DashboardDesignCase("513:3535", "Help", "Workspace and Tools", "HelpSheet", DashboardRoute(Destination.Help, null, "content", null)),
    DashboardDesignCase("513:2989", "Observability", "Workspace and Tools", "ObservabilityScreen", DashboardRoute(Destination.Observability, null, "content", null)),
    DashboardDesignCase("588:927", "Analytics", "Workspace and Tools", "WorkspaceToolScreen", DashboardRoute(Destination.Analytics, null, "content", null)),
    DashboardDesignCase("588:1052", "Speed Insights", "Workspace and Tools", "WorkspaceToolScreen", DashboardRoute(Destination.Speed, null, "content", null)),
    DashboardDesignCase("588:1181", "Firewall", "Workspace and Tools", "WorkspaceToolScreen", DashboardRoute(Destination.Firewall, null, "content", null)),
    DashboardDesignCase("588:1333", "Workspace Domains", "Workspace and Tools", "WorkspaceDomains", DashboardRoute(Destination.WorkspaceDomains, null, "content", null)),
    DashboardDesignCase("604:4587", "Integrations", "Workspace and Tools", "WorkspaceToolScreen", DashboardRoute(Destination.Integrations, null, "content", null)),
    DashboardDesignCase("604:4656", "Analytics empty", "Workspace and Tools", "WorkspaceToolScreen", DashboardRoute(Destination.Analytics, null, "empty", null)),
    DashboardDesignCase("604:4704", "Firewall access required", "Workspace and Tools", "WorkspaceToolScreen", DashboardRoute(Destination.Firewall, null, "restricted", null)),
    DashboardDesignCase("468:6875", "Project logs", "Logs", "LogsScreen", DashboardRoute(Destination.Logs, "prj-personal-1", "content", null)),
    DashboardDesignCase("468:6985", "Project log search", "Logs", "LogsScreen", DashboardRoute(Destination.Logs, "prj-personal-1", "query", null)),
    DashboardDesignCase("468:7128", "Project log filters", "Logs", "LogFilters", DashboardRoute(Destination.LogFilters, "prj-personal-1", "content", null)),
    DashboardDesignCase("468:7051", "Project logs · Errors", "Logs", "LogsScreen", DashboardRoute(Destination.Logs, "prj-personal-1", "errors", null)),
    DashboardDesignCase("468:7260", "Workspace logs", "Logs", "LogsScreen", DashboardRoute(Destination.Logs, null, "content", null)),
    DashboardDesignCase("468:7380", "Workspace log search", "Logs", "LogsScreen", DashboardRoute(Destination.Logs, null, "query", null)),
    DashboardDesignCase("468:7516", "Workspace log filters", "Logs", "LogFilters", DashboardRoute(Destination.LogFilters, null, "content", null)),
    DashboardDesignCase("468:7448", "Workspace logs · Errors", "Logs", "LogsScreen", DashboardRoute(Destination.Logs, null, "errors", null)),
    DashboardDesignCase("590:1355", "Expanded log event", "Logs", "LogSheetContent", DashboardRoute(Destination.LogEvent, "log-prj-personal-1-4", "content", null)),
    DashboardDesignCase("590:1520", "Live Logs", "Logs", "LogsScreen", DashboardRoute(Destination.Logs, null, "live", null)),
    DashboardDesignCase("624:1304", "Logs refreshed", "Logs", "LogsScreen", DashboardRoute(Destination.Logs, null, "refreshed", null)),
    DashboardDesignCase("590:1752", "Download logs", "Logs", "LogDownload", DashboardRoute(Destination.LogDownload, null, "content", null)),
    DashboardDesignCase("590:1867", "No matching logs", "Logs", "LogsScreen", DashboardRoute(Destination.Logs, null, "empty", null)),
    DashboardDesignCase("604:4840", "Logs could not load", "Logs", "LogsScreen", DashboardRoute(Destination.Logs, null, "error", null)),
    DashboardDesignCase("344:494", "Profile settings", "Profile, Appearance and Agent", "ProfileScreen", DashboardRoute(Destination.Profile, null, "content", null)),
    DashboardDesignCase("589:1046", "Profile photo preview", "Profile, Appearance and Agent", "AccountSheetContent", DashboardRoute(Destination.Photo, null, "content", null)),
    DashboardDesignCase("589:1117", "Edit display name", "Profile, Appearance and Agent", "ProfileScreen", DashboardRoute(Destination.Profile, null, "editing", null)),
    DashboardDesignCase("605:1910", "Username conflict", "Profile, Appearance and Agent", "ProfileScreen", DashboardRoute(Destination.Profile, null, "conflict", null)),
    DashboardDesignCase("605:2118", "Default team picker", "Profile, Appearance and Agent", "AccountSheetContent", DashboardRoute(Destination.DefaultScope, null, "content", null)),
    DashboardDesignCase("605:2226", "Landing page picker", "Profile, Appearance and Agent", "AccountSheetContent", DashboardRoute(Destination.DefaultScope, null, "landing", null)),
    DashboardDesignCase("552:3001", "Theme picker", "Profile, Appearance and Agent", "AccountSheetContent", DashboardRoute(Destination.Theme, null, "content", null)),
    DashboardDesignCase("605:2334", "Light theme applied", "Profile, Appearance and Agent", "AccountSheetContent", DashboardRoute(Destination.Theme, null, "light", null)),
    DashboardDesignCase("605:2516", "Enabling Agent", "Profile, Appearance and Agent", "AgentScreen", DashboardRoute(Destination.Agent, null, "enabling", null)),
    DashboardDesignCase("589:2085", "First Agent conversation", "Profile, Appearance and Agent", "AgentScreen", DashboardRoute(Destination.Agent, null, "conversation", null)),
    DashboardDesignCase("605:2543", "Agent could not enable", "Profile, Appearance and Agent", "AgentScreen", DashboardRoute(Destination.Agent, null, "failed", null)),
    DashboardDesignCase("605:2570", "Agent usage limit", "Profile, Appearance and Agent", "AgentScreen", DashboardRoute(Destination.Agent, null, "limit", null)),
    DashboardDesignCase("606:634", "Neo Financial", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "0")),
    DashboardDesignCase("606:865", "Rocket Money", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "1")),
    DashboardDesignCase("606:1103", "Buddy", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "2")),
    DashboardDesignCase("606:1341", "Revolut", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "3")),
    DashboardDesignCase("606:1566", "Glow", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "4")),
    DashboardDesignCase("606:1791", "Copilot Categories", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "5")),
    DashboardDesignCase("606:2009", "Fidelity", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "6")),
    DashboardDesignCase("606:2230", "Public", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "7")),
    DashboardDesignCase("606:2471", "CRED", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "8")),
    DashboardDesignCase("606:2696", "eBay", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "9")),
    DashboardDesignCase("606:2882", "Copilot Investments", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "10")),
    DashboardDesignCase("606:3116", "Yahoo Finance", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "11")),
    DashboardDesignCase("606:3376", "Coinbase", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "12")),
    DashboardDesignCase("606:3610", "Wise", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "13")),
    DashboardDesignCase("606:3851", "YouTube Studio", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "14")),
    DashboardDesignCase("606:4092", "YNAB", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "15")),
    DashboardDesignCase("606:4333", "Origin", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "16")),
    DashboardDesignCase("606:4558", "Acorns", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "17")),
    DashboardDesignCase("606:4796", "Shopify", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "18")),
    DashboardDesignCase("606:5017", "Binance", "Usage row design studies", "UsageScreen", DashboardRoute(Destination.Usage, null, "study", "19")),
// END GENERATED DESIGN CASES
)

@Composable
internal fun DashboardDesignPicker(navigation: DashboardNavigation, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val openUri = LocalUriHandler.current
    val visibleGroups = remember(query) {
        val term = query.trim()
        val frameId = term.replace('-', ':')
        designCases.filter {
            it.title.contains(term, ignoreCase = true) || it.group.contains(term, ignoreCase = true) ||
                it.frameId.contains(frameId)
        }.groupBy(DashboardDesignCase::group)
    }
    Column(Modifier.fillMaxSize()) {
        DashboardHeader("Design states", onBack)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset),
            singleLine = true, placeholder = { Text("Search design states") }, leadingIcon = { DashboardIcon(Glyph.Search) })
        LazyColumn(contentPadding = PaddingValues(bottom = Tokens.Space20)) {
            visibleGroups.forEach { (group, cases) ->
                item(key = group) { SectionLabel(group, Modifier.padding(horizontal = Tokens.Inset)) }
                items(cases, key = DashboardDesignCase::frameId) { case ->
                    DashboardRow(case.title, "${case.symbol} · ${case.frameId}", trailing = {
                        TextButton(onClick = {
                            openUri.openUri("https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=" + case.frameId.replace(':', '-'))
                        }) { Text("Figma") }
                    }, onClick = { navigation.openCase(case) })
                    DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                }
            }
        }
    }
}
