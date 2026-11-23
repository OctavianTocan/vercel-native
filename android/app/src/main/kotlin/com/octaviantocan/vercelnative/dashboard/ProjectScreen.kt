package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val firstTab = if (route.variant in setOf("deployments", "deployment-menu", "branch", "cleared")) 1 else 0
    val pager = rememberPagerState(initialPage = firstTab) { 2 }
    val scope = rememberCoroutineScope()
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
        BoxWithConstraints(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset)
                .padding(top = Tokens.Space8 + Tokens.Space2, bottom = Tokens.Space4)
                .height(Tokens.ProjectSheetSegments)
                .dashboardGlass()
                .padding(Tokens.Space6)
                .selectableGroup()
                .dragDashboardTabs(pager),
        ) {
            val segment = (maxWidth - Tokens.Space4) / 2
            Box(Modifier.offset {
                IntOffset(((segment + Tokens.Space4) * (pager.currentPage + pager.currentPageOffsetFraction)).roundToPx(), 0)
            }.width(segment).fillMaxHeight().background(palette.glassSelection, CircleShape))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
                listOf("Overview", "Deployments").forEachIndexed { index, label ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                            .selectable(pager.currentPage == index, role = Role.Tab) {
                                scope.launch { pager.animateScrollToPage(index, animationSpec = DashboardMotion.move(DashboardMotion.MenuOpen)) }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(label, fontSize = Tokens.Body, lineHeight = Tokens.ProjectSectionLine) }
                }
            }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { tab ->
            if (tab == 1) DashboardList(
                Modifier.fillMaxSize().padding(horizontal = Tokens.Inset),
                state = listState, surface = palette.sheet,
                contentPadding = PaddingValues(top = if (branch == null) Tokens.ProjectSheetListTop else Tokens.ProjectDetailGap,
                    bottom = Tokens.Space20),
            ) {
                if (branch != null) item {
                    Row(
                        Modifier.padding(bottom = Tokens.Inset).width(Tokens.BranchFilterWidth)
                            .background(palette.input, CircleShape).border(Tokens.Stroke, palette.border, CircleShape)
                            .padding(start = Tokens.Space12),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
                    ) {
                        DashboardIcon(Glyph.Branch, palette.muted)
                        Text("Branch · $branch", Modifier.weight(1f), fontSize = Tokens.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Box(Modifier.size(Tokens.Filter).clip(CircleShape).clickable(role = Role.Button) { branch = null }
                            .semantics { contentDescription = "Clear branch filter" }, contentAlignment = Alignment.Center) {
                            Box(Modifier.size(Tokens.BranchClearFace).dashboardGlass(), contentAlignment = Alignment.Center) {
                                DashboardArtwork(R.drawable.dashboard_figma_search_close, Tokens.MetadataIcon)
                            }
                        }
                    }
                }
                val filtered = DashboardFixtures.deployments(project.id).filter { branch == null || it.branch == branch }
                itemsIndexed(filtered, key = { _, item -> item.id }) { index, item ->
                    DeploymentRow(
                        item,
                        project = null,
                        first = index == 0,
                        last = index == filtered.lastIndex,
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
            } else Column(Modifier.fillMaxSize()) {
                DashboardList(
                    Modifier.weight(1f).fillMaxWidth(),
                    state = overviewState, surface = palette.sheet,
                    contentPadding = PaddingValues(start = Tokens.Inset, top = Tokens.ProjectSheetListTop, end = Tokens.Inset, bottom = Tokens.Space20),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space12),
                ) {
                    item {
                        DeploymentPreviewCard(project, latest, outlined = route.variant != "compact") {
                            navigation.show(Destination.Preview, latest.id, "project-overview")
                        }
                    }
                    item {
                        ProjectSectionLabel("Project details")
                        Surface(shape = RoundedCornerShape(Tokens.ProjectPreviewRadius), color = palette.projectSheetGroup, contentColor = palette.ink) {
                            Column {
                                ProjectInfoRow("Team", DashboardFixtures.workspace(navigation.workspaceOf(project)).name, Glyph.Team,
                                    valuePrefix = {
                                        Box(Modifier.size(Tokens.DockIcon).background(
                                            Brush.linearGradient(listOf(palette.link, palette.chart)), CircleShape,
                                        ))
                                    })
                                ProjectInfoDivider()
                                ProjectInfoRow("Project", navigation.projectName(project), Glyph.Layers)
                                ProjectInfoDivider()
                                ProjectInfoRow("Domains", DashboardFixtures.domains(project.id).size.toString(), Glyph.ProjectDomain, trailing = Glyph.Chevron) {
                                    navigation.show(Destination.ProjectDomains, project.id)
                                }
                            }
                        }
                    }
                    item {
                        ProjectSectionLabel("GitHub")
                        Surface(shape = RoundedCornerShape(Tokens.ProjectPreviewRadius), color = palette.projectSheetGroup, contentColor = palette.ink) {
                            Column {
                                val outbound = Glyph.Outbound.takeUnless { route.variant == "compact" }
                                ProjectInfoRow("Branch", latest.branch, Glyph.Branch, mono = true, trailing = outbound)
                                ProjectInfoDivider()
                                ProjectInfoRow("Commit", latest.sha, Glyph.Commit, mono = true, trailing = outbound)
                                ProjectInfoDivider()
                                ProjectInfoRow("Status", "Up to date", Glyph.Info, valuePrefix = {
                                    Box(Modifier.size(Tokens.Space8).background(palette.projectSyncDot, CircleShape))
                                }) {
                                    navigation.show(Destination.GitHubSync, project.id)
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(end = Tokens.Inset, bottom = Tokens.Space32),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconControl(Glyph.ProjectSettings, "Project Settings", { navigation.show(Destination.ProjectSettings, project.id) },
                        visualSize = Tokens.ProjectSettingsButton)
                }
            }
        }
    }
}

@Composable
private fun ProjectInfoRow(
    name: String, value: String, glyph: Glyph, mono: Boolean = false, trailing: Glyph? = null,
    valuePrefix: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null,
) {
    val palette = LocalDashboardPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = Tokens.Touch)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = Tokens.Inset),
        horizontalArrangement = Arrangement.spacedBy(Tokens.ProjectOptionsInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(Tokens.DockIcon), contentAlignment = Alignment.Center) {
            DashboardIcon(glyph, palette.secondary)
        }
        Text(name, fontSize = Tokens.ListTitle, lineHeight = Tokens.ListSubtitleLine)
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Tokens.ProjectDetailGap, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            valuePrefix?.invoke()
            Text(
                value, Modifier.weight(1f, fill = false), color = palette.muted,
                fontSize = if (mono) Tokens.Body else Tokens.ListTitle,
                lineHeight = Tokens.ListSubtitleLine, fontFamily = if (mono) GeistMono else null,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (trailing != null) DashboardIcon(trailing, palette.muted)
        }
    }
}

@Composable
private fun ProjectInfoDivider() {
    DividerLine(Modifier.padding(horizontal = Tokens.Inset), LocalDashboardPalette.current.border)
}

@Composable
private fun ProjectSectionLabel(label: String) {
    Text(
        label, Modifier.padding(start = Tokens.Inset, top = Tokens.Space8, bottom = Tokens.ProjectDetailGap),
        fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine,
        color = LocalDashboardPalette.current.secondary,
    )
}

@Composable
internal fun KeyValueRow(
    label: String, value: String, glyph: Glyph? = null, mono: Boolean = false,
    minHeight: Dp = Tokens.Touch, compactValue: Boolean = false, trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalDashboardPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = minHeight).padding(horizontal = Tokens.Inset, vertical = Tokens.Space8),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) DashboardIcon(glyph, palette.muted)
        Text(label, Modifier.weight(1f), fontSize = Tokens.Body)
        Text(value, Modifier.widthIn(max = Tokens.DialogWidth / 2), fontSize = if (compactValue) Tokens.SearchDetailSize else Tokens.Body,
            fontFamily = if (mono) GeistMono else null, color = palette.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        trailing?.invoke()
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
    Column(Modifier.fillMaxSize().imePadding()) {
        DashboardHeader("Project Settings", onBack, modifier = Modifier.padding(top = Tokens.AlertDetailHeaderInset),
            titleSize = Tokens.ListTitle, titleLine = Tokens.SheetTitleLine)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().dashboardScroll().padding(horizontal = Tokens.Inset)) {
                ProjectSettingsLabel("General", Tokens.AlertMeasurePad)
                Column(Modifier.fillMaxWidth().clip(card).background(palette.projectSheetGroup)) {
                    if (editing) {
                        val topShape = RoundedCornerShape(topStart = Tokens.Inset, topEnd = Tokens.Inset)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = Tokens.SettingsGeneralRow)
                                .border(Tokens.Stroke, if (conflict) palette.error else palette.border, topShape)
                                .padding(horizontal = Tokens.Inset),
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Inset), verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Project name", fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine)
                            BasicTextField(name, { name = it; conflict = false }, Modifier.weight(1f).focusRequester(requester),
                                singleLine = true, cursorBrush = SolidColor(palette.ink),
                                textStyle = GeistText.copy(color = palette.ink, fontSize = Tokens.SearchDetailSize,
                                    lineHeight = Tokens.ListSubtitleLine, textAlign = TextAlign.End),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { commitName() }))
                        }
                    } else {
                        Box(Modifier.clickable { editing = true }) {
                            KeyValueRow("Project name", navigation.projectName(project),
                                minHeight = Tokens.SettingsGeneralRow, compactValue = true)
                        }
                    }
                    ProjectInfoDivider()
                    KeyValueRow("Project ID", project.id, minHeight = Tokens.SettingsGeneralRow, compactValue = true)
                }
                ProjectSettingsLabel("Project ownership", Tokens.SettingsOwnershipGap)
                Column(Modifier.fillMaxWidth().clip(card).background(palette.projectSheetGroup)) {
                    KeyValueRow("Current team", DashboardFixtures.workspace(navigation.workspaceOf(project)).name,
                        minHeight = Tokens.SettingsOwnershipRow, compactValue = true)
                    ProjectInfoDivider()
                    DashboardRow("Transfer project", onClick = { navigation.show(Destination.Transfer, project.id) },
                        minHeight = Tokens.SettingsOwnershipRow)
                }
                Spacer(Modifier.height(Tokens.Inset))
            }
            if (conflict) {
                DashboardGlassNotice("Name already in use", onRetry = { requester.requestFocus() }, error = true,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(Tokens.Inset))
            }
        }
    }
}

@Composable
private fun ProjectSettingsLabel(label: String, top: Dp) {
    Text(label, Modifier.padding(start = Tokens.Inset, top = top, bottom = Tokens.Space12),
        color = LocalDashboardPalette.current.secondary, fontSize = Tokens.SearchDetailSize,
        lineHeight = Tokens.MetadataLine, fontWeight = FontWeight.Medium)
}

@Composable
private fun ProjectDomainsScreen(project: Project, navigation: DashboardNavigation, onBack: () -> Unit) {
    val domains = DashboardFixtures.domains(project.id)
    val deployment = DashboardFixtures.deployments(project.id).first()
    val palette = LocalDashboardPalette.current
    Column(Modifier.fillMaxHeight()) {
        DashboardHeader("Domains", onBack, titleLine = Tokens.DeploymentDetailTitleLine)
        DashboardList(contentPadding = PaddingValues(start = Tokens.Inset, top = Tokens.Space20,
            end = Tokens.Inset, bottom = Tokens.Space24)) {
            item {
                Text("Production", Modifier.padding(start = Tokens.BodyInset, bottom = Tokens.Space8 + Tokens.Space2),
                    fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine, fontWeight = FontWeight.Medium, color = palette.secondary)
                DashboardGroup {
                    domains.take(2).forEachIndexed { index, domain ->
                        if (index > 0) ProjectInfoDivider()
                        ProjectDomainListRow(domain, index == 0, navigation)
                    }
                }
            }
            item {
                Text("Deployment URL", Modifier.padding(start = Tokens.BodyInset, top = Tokens.Space24 + Tokens.AlertTextGap,
                    bottom = Tokens.PreviewStatusInset), fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine,
                    fontWeight = FontWeight.Medium, color = palette.secondary)
                DashboardGroup {
                    DashboardRow(deployment.url, "Vercel managed", minHeight = Tokens.DomainStatusRow,
                        leading = { DashboardIcon(Glyph.ProjectDomain, palette.secondary, Modifier.size(Tokens.DockIcon)) },
                        onClick = { navigation.show(Destination.Preview, deployment.id) })
                }
            }
            item {
                SectionLabel("Preview domains", Modifier.padding(horizontal = Tokens.Space8, vertical = Tokens.Space12))
            }
            items(domains.drop(2), key = ProjectDomain::id) { domain ->
                DashboardGroup { ProjectDomainListRow(domain, false, navigation) }
                Spacer(Modifier.height(Tokens.Space2))
            }
        }
    }
}

@Composable
private fun ProjectDomainListRow(domain: ProjectDomain, primary: Boolean, navigation: DashboardNavigation) {
    val palette = LocalDashboardPalette.current
    val accent = if (domain.status == DomainStatus.Verified) palette.ready else palette.warning
    DashboardRow(domain.hostname, if (primary) "Primary · ${domain.status.label}" else domain.status.label,
        minHeight = Tokens.ProjectDomainRowHeight,
        leading = { DashboardIcon(Glyph.ProjectDomain, if (domain.status == DomainStatus.Verified) palette.secondary else accent,
            Modifier.size(Tokens.DockIcon)) },
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space6), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(Tokens.Space6).background(accent, CircleShape))
                DashboardIcon(Glyph.Chevron, palette.secondary)
            }
        },
        onClick = { navigation.show(Destination.Domain, domain.id) })
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
    val palette = LocalDashboardPalette.current
    val domain = route.target?.let(DashboardFixtures::domain) ?: DashboardFixtures.domains(navigation.project(null).id)[1]
    val status = when (route.variant) {
        "restricted" -> DomainStatus.Restricted
        "verify" -> DomainStatus.Unverified
        else -> domain.status
    }
    var checked by rememberSaveable { mutableStateOf(false) }
    val copy = rememberCopyText()
    val body = when (status) {
        DomainStatus.Unverified -> Modifier.height(Tokens.DomainVerificationBody)
        DomainStatus.Restricted -> Modifier.height(Tokens.DomainRestrictedBody)
        DomainStatus.Verified -> Modifier
    }
    Column(body.fillMaxWidth()) {
        DashboardHeader("Domain details", onBack, titleLine = Tokens.DeploymentDetailTitleLine)
        Column(
            Modifier.fillMaxWidth().then(if (status == DomainStatus.Verified) Modifier else Modifier.weight(1f))
                .dashboardScroll().padding(horizontal = Tokens.Inset).padding(top = Tokens.Space20),
            verticalArrangement = Arrangement.spacedBy(Tokens.Inset),
        ) {
            DomainStatusCard(domain.hostname, status != DomainStatus.Unverified)
            when (status) {
                DomainStatus.Restricted -> DashboardGroup {
                    Row(
                        Modifier.padding(Tokens.Inset), horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
                        verticalAlignment = Alignment.Top,
                    ) {
                        DashboardIcon(Glyph.Lock, palette.secondary, Modifier.size(Tokens.Icon))
                        Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
                            Text("Domain management is restricted", fontSize = Tokens.ListTitle,
                                lineHeight = Tokens.ListTitleLine, fontWeight = FontWeight.SemiBold)
                            Text("Ask a project administrator to manage this domain or grant you access.",
                                color = palette.secondary, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine)
                        }
                    }
                }
                DomainStatus.Unverified -> {
                    Text("Add this record with your DNS provider, then check verification.", color = palette.secondary,
                        fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectOptionsLine, modifier = Modifier.padding(horizontal = Tokens.Space8))
                    Column {
                        Text("DNS record", Modifier.padding(start = Tokens.AlertMeasurePad, bottom = Tokens.ProjectDetailGap),
                            color = palette.secondary, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine,
                            fontWeight = FontWeight.Medium)
                        DashboardGroup {
                            KeyValueRow("Type", "CNAME", minHeight = Tokens.DnsRecordRow, compactValue = true)
                            ProjectInfoDivider()
                            KeyValueRow("Name", domain.hostname.substringBefore('.'), minHeight = Tokens.DnsRecordRow, compactValue = true)
                            ProjectInfoDivider()
                            val dnsValue = "cname.preview.example"
                            KeyValueRow("Value", dnsValue, minHeight = Tokens.DnsRecordRow, compactValue = true, trailing = {
                            IconControl(Glyph.Copy, "Copy DNS value", { copy(dnsValue) }, visualSize = Tokens.Space20, glass = false)
                            })
                        }
                    }
                    if (checked) DashboardNotice("Waiting for DNS verification", tone = Tone.Warning)
                }
                DomainStatus.Verified -> {
                    KeyValueRow("Project", navigation.projectName(navigation.project(domain.projectId)))
                    KeyValueRow("Target", "Production")
                }
            }
        }
        val footer = Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset, vertical = Tokens.Space4)
        when (status) {
            DomainStatus.Restricted -> GlassAction("Done", onBack, footer)
            DomainStatus.Unverified -> GlassAction("Check verification", { checked = true }, footer)
            DomainStatus.Verified -> DashboardButton("Done", onBack, footer)
        }
    }
}

@Composable
private fun DomainStatusCard(hostname: String, verified: Boolean) {
    val palette = LocalDashboardPalette.current
    val accent = if (verified) palette.ready else palette.warning
    val paper = if (verified) palette.domainVerifiedSurface else palette.domainUnverifiedSurface
    DashboardGroup {
        Row(
            Modifier.fillMaxWidth().heightIn(min = Tokens.DomainStatusRow).padding(horizontal = Tokens.Inset, vertical = Tokens.Space8),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space12), verticalAlignment = Alignment.CenterVertically,
        ) {
            DashboardIcon(Glyph.ProjectDomain, palette.secondary)
            Text(hostname, Modifier.weight(1f), fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectOptionsLine,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            DottedStatusPill(if (verified) "Verified" else "Unverified", accent, paper,
                Modifier.widthIn(min = if (verified) Tokens.DomainVerifiedWidth else Tokens.DomainStatusWidth))
        }
    }
}

@Composable
private fun GitHubSync(project: Project, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    DashboardHeader("GitHub sync", onBack)
    Column(Modifier.fillMaxWidth().dashboardScroll().padding(horizontal = Tokens.Inset, vertical = Tokens.Space24)) {
        DashboardGroup {
            DashboardRow("Status", trailing = { DottedStatusPill("Up to date", palette.ready, palette.domainVerifiedSurface) },
                minHeight = Tokens.Touch)
            ProjectInfoDivider()
            DashboardRow("Repository", project.repository, leading = { DashboardIcon(Glyph.GitHub) },
                trailing = { DashboardIcon(Glyph.Outbound, palette.secondary) })
            ProjectInfoDivider()
            ProjectInfoRow("Commit", DashboardFixtures.deployments(project.id).first().sha,
                Glyph.Commit, mono = true, trailing = Glyph.Outbound)
            ProjectInfoDivider()
            KeyValueRow("Last synced", "5 minutes ago")
        }
        Spacer(Modifier.height(Tokens.Space32))
    }
}

@Composable
private fun TransferSheet(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val project = navigation.project(route.target)
    val source = DashboardFixtures.workspace(navigation.workspaceOf(project))
    val destinations = DashboardFixtures.transferWorkspaces.filter { it.id != source.id }
    var choice by rememberSaveable(route.detail) { mutableStateOf(route.detail ?: destinations.first().id) }
    val destination = DashboardFixtures.workspace(choice)
    val name = navigation.projectName(project)
    val advance: (String) -> Unit = { phase ->
        navigation.replace(route.copy(variant = phase, detail = destination.id, frozen = false))
    }
    LaunchedEffect(route) {
        if (route.variant == "progress" && !route.frozen) {
            delay(1700)
            navigation.transfer(project, destination.id)
            advance("done")
        }
    }
    val bodyHeight = when (route.variant) {
        "review" -> Tokens.TransferReviewBody
        "progress" -> Tokens.TransferProgressBody
        "done" -> Tokens.TransferCompleteBody
        "failed" -> Tokens.TransferFailureBody
        else -> Tokens.TransferSelectionBody
    }
    val scope = listOf("Project" to name, "From" to source.name, "To" to destination.name)
    Column(Modifier.fillMaxWidth().height(bodyHeight)) {
        when (route.variant) {
            "review" -> DashboardHeader("Review transfer", { advance("content") })
            "progress" -> DashboardHeader("Transfer project")
            "done" -> DashboardHeader("Transfer complete", onBack, titleGlyph = Glyph.TransferSuccess, titleInk = palette.ready,
                titleGlyphTint = Color.Unspecified)
            "failed" -> DashboardHeader("Transfer couldn’t finish", onBack, titleGlyph = Glyph.TransferFailure, titleInk = palette.error,
                titleGlyphTint = Color.Unspecified)
            else -> DashboardHeader("Transfer project", onBack)
        }
        val contentInset = when (route.variant) {
            "done" -> Tokens.AlertMeasurePad
            "review", "progress", "failed" -> Tokens.Inset
            else -> Tokens.Space20
        }
        Column(Modifier.fillMaxWidth().weight(1f).dashboardScroll().padding(horizontal = Tokens.Inset)
            .padding(top = contentInset, bottom = Tokens.Space8)) {
            when (route.variant) {
                "review" -> {
                    TransferSummary("Project and destination", scope)
                    Spacer(Modifier.height(Tokens.FeedbackActionGap))
                    TransferSummary("Transfer impact", listOf(
                        "Deployments and domains" to "Move", "Environment variables" to "Copied",
                        "Integrations" to "Reconnect", "Project access" to "${destination.name} members",
                    ))
                }
                "progress" -> {
                    DashboardGroup {
                        DashboardRow("Moving to ${destination.name}", leading = {
                            CircularProgressIndicator(Modifier.size(Tokens.DockIcon), color = palette.link, strokeWidth = Tokens.Space2)
                        }, minHeight = Tokens.TransferProgressRow)
                    }
                    Spacer(Modifier.height(Tokens.Inset))
                    TransferSummary("Project and destination", scope)
                }
                "done" -> {
                    TransferSummary("Destination", listOf("Project" to name, "Team" to destination.name),
                        labelGap = Tokens.Space8)
                    Text("Actions", Modifier.padding(start = Tokens.Space8, top = Tokens.Space20, bottom = Tokens.PreviewStatusInset),
                        color = palette.secondary, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.MetadataLine)
                    DashboardGroup {
                        DashboardRow("Open project", onClick = {
                            if (!route.frozen) navigation.chooseWorkspace(destination.id)
                            navigation.show(Destination.Project, project.id, "transferred")
                        }, minHeight = Tokens.TransferResultAction)
                        ProjectInfoDivider()
                        DashboardRow("Reconnect integrations", onClick = {
                            navigation.show(Destination.Integrations, destination.id)
                        }, minHeight = Tokens.TransferResultAction)
                    }
                }
                "failed" -> {
                    TransferSummary("Project and destination", listOf("Project" to name,
                        "Current owner" to source.name, "Destination" to destination.name), labelGap = Tokens.Space8)
                }
                else -> {
                    Text("Current owner", Modifier.padding(start = Tokens.BodyInset, bottom = Tokens.Space8),
                        color = palette.secondary, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine, fontWeight = FontWeight.Medium)
                    DashboardGroup {
                        DashboardRow(source.name, leading = { DashboardIcon(Glyph.Team, palette.secondary) }, minHeight = Tokens.SearchRow,
                            trailing = { MutedText("Current team") })
                    }
                    Text("Destination team", Modifier.padding(start = Tokens.BodyInset, top = Tokens.Space24 + Tokens.Space2, bottom = Tokens.Space8),
                        color = palette.secondary, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine, fontWeight = FontWeight.Medium)
                    DashboardGroup {
                        destinations.forEachIndexed { index, workspace ->
                            if (index > 0) ProjectInfoDivider()
                            val selected = workspace.id == choice
                            Box {
                                if (selected) Box(Modifier.matchParentSize().padding(Tokens.Space4)
                                    .background(palette.border, RoundedCornerShape(Tokens.ControlRadius)))
                                DashboardRow(workspace.name, "Project transfers available",
                                    leading = { DashboardIcon(Glyph.Team, palette.secondary) },
                                    trailing = { if (selected) DashboardIcon(Glyph.Check, palette.secondary) },
                                    onClick = { choice = workspace.id }, minHeight = Tokens.TransferDestinationRow)
                            }
                        }
                    }
                }
            }
        }
        val footer = Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset)
        when (route.variant) {
            "review" -> Row(footer, horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                GlassAction("Cancel", { advance("content") }, Modifier.weight(.56f).heightIn(min = Tokens.Touch))
                GlassAction("Transfer project", { advance("progress") }, Modifier.weight(1f).heightIn(min = Tokens.Touch))
            }
            "failed" -> Row(footer, horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                GlassAction("Back to project", { navigation.replace(DashboardRoute(Destination.Project, project.id)) }, Modifier.weight(.64f))
                GlassAction("Review transfer", { advance("review") }, Modifier.weight(1f))
            }
            "done", "progress" -> Unit
            else -> GlassAction("Review transfer", { advance("review") }, footer.padding(bottom = Tokens.Space2))
        }
    }
}

@Composable
private fun TransferSummary(
    title: String, values: List<Pair<String, String>>, labelGap: Dp = Tokens.Space8 + Tokens.Space2,
) {
    Column {
        Text(title, Modifier.padding(start = Tokens.Space8, bottom = labelGap),
            color = LocalDashboardPalette.current.secondary, fontSize = Tokens.SearchDetailSize,
            lineHeight = Tokens.MetadataLine, fontWeight = FontWeight.Medium)
        DashboardGroup {
            values.forEachIndexed { index, (label, value) ->
                if (index > 0) ProjectInfoDivider()
                KeyValueRow(label, value, compactValue = true)
            }
        }
    }
}
