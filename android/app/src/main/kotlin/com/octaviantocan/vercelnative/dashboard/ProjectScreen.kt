package com.octaviantocan.vercelnative.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlinx.coroutines.delay

@Composable
internal fun ProjectSheetContent(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    when (route.destination) {
        Destination.Project -> ProjectScreen(route, navigation, onBack)
        Destination.ProjectDomains -> ProjectDomainsScreen(navigation.project(route.target), navigation, onBack)
        Destination.Domain -> DomainDetails(route, navigation, onBack)
        Destination.GitHubSync -> GitHubSync(navigation.project(route.target), onBack)
        else -> TransferSheet(route, navigation, onBack)
    }
}

@Composable
internal fun ProjectScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val project = navigation.project(route.target)
    val latest = DashboardFixtures.deployments(project.id).first()
    val palette = LocalDashboardPalette.current
    var deployments by rememberSaveable {
        mutableStateOf(route.variant in setOf("deployments", "deployment-menu", "branch", "cleared"))
    }
    var branch by rememberSaveable { mutableStateOf(route.detail ?: if (route.variant == "branch") "main" else null) }
    var expanded by rememberSaveable { mutableStateOf(if (route.variant == "deployment-menu") latest.id else null) }
    val listState = rememberLazyListState()
    val overviewState = rememberLazyListState()
    Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
        ) {
            ProjectMark(project, Modifier.size(Tokens.ProjectSheetMark))
            Text(navigation.projectName(project), Modifier.weight(1f), fontSize = Tokens.Title,
                lineHeight = Tokens.SheetTitleLine, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconControl(Glyph.Close, "Close project", onBack)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset)
                .padding(top = Tokens.Space8 + Tokens.Space2, bottom = Tokens.Space4)
                .height(Tokens.ProjectSheetSegments)
                .background(palette.input, CircleShape).border(Tokens.Stroke, palette.border, CircleShape)
                .padding(Tokens.Space6),
        ) {
            for ((selected, label) in listOf(false to "Overview", true to "Deployments")) {
                Surface(
                    onClick = { deployments = selected },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    color = if (deployments == selected) palette.selected else palette.input,
                    shape = CircleShape,
                ) { Box(contentAlignment = Alignment.Center) { Text(label, fontSize = Tokens.Body, lineHeight = Tokens.ProjectSectionLine) } }
            }
        }
        AnimatedContent(
            targetState = deployments,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                fadeIn(DashboardMotion.move(DashboardMotion.Fast)) togetherWith fadeOut(DashboardMotion.move(DashboardMotion.Quick))
            },
            label = "Project tab content",
        ) { selected ->
            if (selected) DashboardList(
                Modifier.fillMaxSize().padding(horizontal = Tokens.Inset),
                state = listState, surface = palette.sheet,
                contentPadding = PaddingValues(top = Tokens.ProjectSheetListTop, bottom = Tokens.Space20),
            ) {
                if (branch != null) item {
                    Row(
                        Modifier.padding(horizontal = Tokens.Inset, vertical = Tokens.Space8)
                            .background(palette.input, CircleShape).border(Tokens.Stroke, palette.border, CircleShape)
                            .padding(start = Tokens.Space8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
                    ) {
                        DashboardIcon(Glyph.Branch, palette.muted)
                        Text("Branch · $branch", fontSize = Tokens.Body)
                        IconControl(Glyph.Close, "Clear branch filter", { branch = null })
                    }
                }
                items(DashboardFixtures.deployments(project.id).filter { branch == null || it.branch == branch }, key = Deployment::id) { item ->
                    DeploymentRow(
                        item,
                        project = null,
                        onOpen = { navigation.show(Destination.Deployment, item.id) },
                        options = {
                            RowOptions(
                                actions = listOf(
                                    MenuAction("Preview", Glyph.Eye, { navigation.show(Destination.Preview, item.id) }),
                                    MenuAction("Source", Glyph.GitHub, null),
                                    MenuAction("Deployment", Glyph.MenuDeployment, { navigation.show(Destination.Deployment, item.id) }),
                                    MenuAction("Branch Deployments", Glyph.MenuBranch, { branch = item.branch }),
                                ),
                                label = "Options for ${item.title}",
                                expanded = expanded == item.id,
                                onOpen = { expanded = item.id },
                                onClose = { expanded = null },
                                authored = item.elapsed,
                            )
                        },
                    )
                }
            } else DashboardList(
                Modifier.fillMaxSize(),
                state = overviewState, surface = palette.sheet,
                contentPadding = PaddingValues(start = Tokens.Inset, top = Tokens.ProjectSheetListTop, end = Tokens.Inset, bottom = Tokens.Space20),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space12),
            ) {
                item {
                    DeploymentPreviewCard(project, latest, tinted = route.variant != "compact") {
                        navigation.show(Destination.Preview, latest.id)
                    }
                }
                item {
                    ProjectSectionLabel("Project details")
                    Surface(shape = RoundedCornerShape(Tokens.CardRadius), color = palette.group) {
                        Column {
                            ProjectInfoRow("Team", DashboardFixtures.workspace(navigation.workspaceOf(project)).name, Glyph.Team)
                            DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                            ProjectInfoRow("Project", navigation.projectName(project), Glyph.Layers)
                            DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                            ProjectInfoRow("Domains", DashboardFixtures.domains(project.id).size.toString(), Glyph.ProjectDomain) {
                                navigation.show(Destination.ProjectDomains, project.id)
                            }
                        }
                    }
                }
                item {
                    ProjectSectionLabel("GitHub")
                    Surface(shape = RoundedCornerShape(Tokens.CardRadius), color = palette.group) {
                        Column {
                            ProjectInfoRow("Branch", latest.branch, Glyph.Branch, mono = true)
                            DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                            ProjectInfoRow("Commit", latest.sha, Glyph.Commit, mono = true)
                            DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                            ProjectInfoRow("Status", "Up to date", Glyph.Info) {
                                navigation.show(Destination.GitHubSync, project.id)
                            }
                        }
                    }
                }
            }
        }
        if (!deployments) Row(
            Modifier.fillMaxWidth().padding(end = Tokens.Inset, bottom = Tokens.Space32),
            horizontalArrangement = Arrangement.End,
        ) {
            IconControl(Glyph.Settings, "Project Settings", { navigation.show(Destination.ProjectSettings, project.id) },
                visualSize = Tokens.ProjectSettingsButton)
        }
    }
}

@Composable
private fun ProjectInfoRow(
    name: String, value: String, glyph: Glyph, mono: Boolean = false, onClick: (() -> Unit)? = null,
) {
    val palette = LocalDashboardPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = Tokens.Touch)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = Tokens.Inset),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(Tokens.DockIcon), contentAlignment = Alignment.Center) {
            DashboardIcon(glyph, palette.secondary)
        }
        Text(name, fontSize = Tokens.ListTitle, lineHeight = Tokens.ListSubtitleLine)
        Text(
            value, Modifier.weight(1f), color = palette.secondary,
            fontSize = if (mono) Tokens.Body else Tokens.ListTitle,
            lineHeight = Tokens.ListSubtitleLine, fontFamily = if (mono) GeistMono else null,
            textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (mono) DashboardIcon(Glyph.Outbound, palette.secondary)
        if (onClick != null && glyph == Glyph.ProjectDomain) DashboardIcon(Glyph.Chevron, palette.secondary)
    }
}

@Composable
private fun ProjectSectionLabel(label: String) {
    Text(
        label, Modifier.padding(start = Tokens.Inset, top = Tokens.Space6, bottom = Tokens.Space6),
        fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine,
        color = LocalDashboardPalette.current.secondary,
    )
}

@Composable
internal fun KeyValueRow(label: String, value: String, glyph: Glyph? = null) {
    val palette = LocalDashboardPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = Tokens.Touch).padding(horizontal = Tokens.Inset, vertical = Tokens.Space8),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) DashboardIcon(glyph, palette.muted)
        Text(label, Modifier.weight(1f), fontSize = Tokens.Body)
        Text(value, Modifier.widthIn(max = Tokens.DialogWidth / 2), fontSize = Tokens.Body, color = palette.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ProjectSettingsScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val project = navigation.project(route.target)
    var name by rememberSaveable { mutableStateOf(if (route.variant == "conflict") "atlas-catalog" else navigation.projectName(project)) }
    var conflict by rememberSaveable { mutableStateOf(route.variant == "conflict") }
    var editing by rememberSaveable { mutableStateOf(route.variant in setOf("editing", "conflict")) }
    val requester = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val commitName = {
        if (name.isNotBlank()) {
            conflict = navigation.projects(navigation.workspaceOf(project)).any { it.id != project.id && navigation.projectName(it) == name.trim() }
            if (!conflict) {
                navigation.rename(project, name.trim())
                focus.clearFocus()
                editing = false
            }
        }
    }
    LaunchedEffect(editing) { if (editing) requester.requestFocus() }
    val palette = LocalDashboardPalette.current
    val card = RoundedCornerShape(Tokens.Inset)
    Column(Modifier.fillMaxSize()) {
        DashboardHeader("Project Settings", onBack, modifier = Modifier.padding(top = Tokens.AlertDetailHeaderInset))
        Column(Modifier.dashboardScroll().padding(Tokens.Inset)) {
            SectionLabel("General")
            Column(Modifier.fillMaxWidth().background(palette.group, card)) {
                if (editing) {
                    Text("Project name", Modifier.padding(start = Tokens.Inset, top = Tokens.Space8), fontSize = Tokens.ListSubtitle)
                    DashboardField(
                        name, { name = it; conflict = false }, Modifier.fillMaxWidth().padding(Tokens.Space8).focusRequester(requester),
                        height = Tokens.ProfileFieldHeight, error = conflict,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commitName() }),
                    )
                } else {
                    Box(Modifier.clickable {
                        editing = true
                    }) { KeyValueRow("Project name", navigation.projectName(project)) }
                    DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                }
                KeyValueRow("Project ID", project.id)
            }
            if (conflict) DashboardNotice("Name already in use", tone = Tone.Error, retry = { requester.requestFocus() })
            Spacer(Modifier.height(Tokens.Inset))
            SectionLabel("Project ownership")
            Column(Modifier.fillMaxWidth().background(palette.group, card)) {
                KeyValueRow("Current team", DashboardFixtures.workspace(navigation.workspaceOf(project)).name)
                DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                DashboardRow("Transfer project", onClick = { navigation.show(Destination.Transfer, project.id) }, minHeight = Tokens.Touch)
            }
        }
    }
}

@Composable
private fun ProjectDomainsScreen(project: Project, navigation: DashboardNavigation, onBack: () -> Unit) {
    Column(Modifier.fillMaxHeight(Tokens.ProjectSheetFraction)) {
        DashboardHeader("Domains", onBack)
        DashboardList {
            item {
                SectionLabel("Production", Modifier.padding(horizontal = Tokens.Inset))
            }
            items(DashboardFixtures.domains(project.id), key = ProjectDomain::id) { domain ->
                DomainRow(domain, navigation)
            }
        }
    }
}

@Composable
internal fun DomainRow(domain: ProjectDomain, navigation: DashboardNavigation, showProject: Boolean = false) {
    val title = if (showProject) navigation.projectName(navigation.project(domain.projectId)) else domain.status.label
    DashboardRow(domain.hostname, title, glyph = Glyph.Globe,
        trailing = { StatusPill(domain.status.label, if (domain.status == DomainStatus.Verified) Tone.Success else Tone.Warning) },
        onClick = { navigation.show(Destination.Domain, domain.id) })
    DividerLine(Modifier.padding(horizontal = Tokens.Inset))
}

@Composable
private fun DomainDetails(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val domain = route.target?.let(DashboardFixtures::domain) ?: DashboardFixtures.domains(navigation.project(null).id)[1]
    val status = when (route.variant) {
        "restricted" -> DomainStatus.Restricted
        "verify" -> DomainStatus.Unverified
        else -> domain.status
    }
    var checked by rememberSaveable { mutableStateOf(false) }
    DashboardHeader("Domain details", onBack)
    Column(Modifier.dashboardScroll().padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
        DashboardRow(domain.hostname, glyph = Glyph.Globe, trailing = {
            StatusPill(status.label, if (status == DomainStatus.Verified) Tone.Success else Tone.Warning)
        })
        when (status) {
            DomainStatus.Restricted -> {
                DashboardNotice("Domain management is restricted", tone = Tone.Warning)
                MutedText("Ask a project administrator to manage this domain.")
                DashboardButton("Done", onBack, Modifier.fillMaxWidth())
            }
            DomainStatus.Unverified -> {
                Text("Add this record with your DNS provider, then check verification.", fontSize = Tokens.Body)
                SectionLabel("DNS record")
                KeyValueRow("Type", "CNAME")
                KeyValueRow("Name", domain.hostname.substringBefore('.'))
                KeyValueRow("Value", "cname.preview.example")
                if (checked) DashboardNotice("Waiting for DNS verification", tone = Tone.Warning)
                DashboardButton("Check verification", { checked = true }, Modifier.fillMaxWidth())
            }
            DomainStatus.Verified -> {
                KeyValueRow("Project", navigation.projectName(navigation.project(domain.projectId)))
                KeyValueRow("Target", "Production")
                DashboardButton("Done", onBack, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GitHubSync(project: Project, onBack: () -> Unit) {
    DashboardHeader("GitHub sync", onBack)
    Column(Modifier.padding(Tokens.Inset)) {
        DashboardRow("Status", glyph = Glyph.Check, trailing = { StatusPill("Up to date", Tone.Success) })
        KeyValueRow("Repository", project.repository)
        KeyValueRow("Commit", DashboardFixtures.deployments(project.id).first().sha)
        KeyValueRow("Last synced", "5 minutes ago")
    }
}

@Composable
private fun TransferSheet(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val project = navigation.project(route.target)
    val source = DashboardFixtures.workspace(navigation.workspaceOf(project))
    val destination = DashboardFixtures.workspace(
        route.detail ?: DashboardFixtures.workspaces.first { it.id != source.id }.id
    )
    val name = navigation.projectName(project)
    val advance: (String) -> Unit = { phase ->
        navigation.replace(route.copy(variant = phase, detail = destination.id, frozen = false))
    }
    LaunchedEffect(route) {
        if (route.variant == "progress" && !route.frozen) {
            delay(1200)
            navigation.transfer(project, destination.id)
            advance("done")
        }
    }
    DashboardHeader("Transfer project", onBack)
    Column(Modifier.fillMaxWidth().dashboardScroll().padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
        when (route.variant) {
            "review" -> {
                SectionLabel("Project and destination")
                KeyValueRow("Project", name)
                KeyValueRow("From", source.name)
                KeyValueRow("To", destination.name)
                SectionLabel("Transfer impact")
                DashboardRow("Deployments and domains", "Move with this project")
                DashboardRow("Environment variables", "Copied to the destination")
                DashboardRow("Project access", "${destination.name} members gain access")
                DashboardRow("Integrations", "Reconnect in the destination workspace")
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                    OutlinedButton({ advance("content") }, Modifier.weight(1f)) { Text("Cancel") }
                    DashboardButton("Transfer project", { advance("progress") }, Modifier.weight(1f))
                }
            }
            "progress" -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Moving to ${destination.name}", fontSize = Tokens.Title)
                KeyValueRow("Project", name)
            }
            "done" -> {
                DashboardNotice("Transfer complete", tone = Tone.Success)
                SectionLabel("Destination")
                KeyValueRow("Project", name)
                KeyValueRow("Team", destination.name)
                SectionLabel("Actions")
                DashboardRow("Open project", glyph = Glyph.Project, onClick = {
                    if (route.frozen) navigation.show(Destination.Project, project.id, "transferred")
                    else {
                        navigation.chooseWorkspace(destination.id)
                        navigation.show(Destination.Project, project.id, "transferred")
                    }
                })
                DashboardRow("Reconnect integrations", glyph = Glyph.Settings, onClick = { navigation.show(Destination.Integrations, destination.id) })
            }
            "failed" -> {
                DashboardNotice("Transfer couldn’t finish", tone = Tone.Error)
                KeyValueRow("Project", name)
                KeyValueRow("Destination", destination.name)
                DashboardButton("Review transfer", { advance("review") }, Modifier.fillMaxWidth())
            }
            else -> {
                SectionLabel("Current owner")
                KeyValueRow("Current team", source.name)
                SectionLabel("Destination team")
                DashboardRow(destination.name, "Project transfers available", glyph = Glyph.Account, trailing = { DashboardIcon(Glyph.Check) })
                DashboardButton("Review transfer", { advance("review") }, Modifier.fillMaxWidth())
            }
        }
    }
}
