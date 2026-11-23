package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

@Composable
internal fun DeploymentRow(
    deployment: Deployment,
    project: Project?,
    onOpen: () -> Unit,
    options: (@Composable () -> Unit)? = null,
    first: Boolean = false,
    last: Boolean = false,
) {
    if (project != null) {
        RecentDeploymentRow(deployment, project, onOpen, options)
        return
    }
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(
        topStart = if (first) Tokens.CardRadius else Dp.Hairline,
        topEnd = if (first) Tokens.CardRadius else Dp.Hairline,
        bottomStart = if (last) Tokens.CardRadius else Dp.Hairline,
        bottomEnd = if (last) Tokens.CardRadius else Dp.Hairline,
    )
    Box(Modifier.fillMaxWidth().clip(shape).background(palette.projectSheetGroup)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen)
                .heightIn(min = Tokens.ProjectDeploymentRow)
                .padding(start = Tokens.Inset, end = Tokens.Space8 + Tokens.Space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.RecentTitleGap),
        ) {
            Image(
                painterResource(R.drawable.dashboard_sample_avatar),
                contentDescription = deployment.author,
                modifier = Modifier.size(Tokens.Icon).clip(CircleShape),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
                Text(
                    deployment.title,
                    fontSize = Tokens.ListTitle, lineHeight = Tokens.ListSubtitleLine,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(.34f), horizontalArrangement = Arrangement.spacedBy(Tokens.Space4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painterResource(R.drawable.dashboard_github_mark), contentDescription = "GitHub",
                            modifier = Modifier.size(Tokens.MetadataIcon), colorFilter = ColorFilter.tint(palette.secondary),
                        )
                        Text(deployment.pullRequest?.let { "#$it" } ?: "Source", color = palette.secondary,
                            fontSize = Tokens.ListTitle, lineHeight = Tokens.ProjectSectionLine, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    Text(deployment.status.label.takeIf { deployment.pullRequest == null } ?: deployment.sha,
                        modifier = Modifier.weight(.32f), color = palette.secondary,
                        fontSize = Tokens.ListTitle, lineHeight = Tokens.ProjectSectionLine,
                        fontFamily = GeistMono.takeIf { deployment.pullRequest != null }, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(deployment.elapsed, Modifier.weight(.34f), color = palette.secondary, fontSize = Tokens.ListTitle,
                        lineHeight = Tokens.ProjectSectionLine, textAlign = TextAlign.End, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            options?.invoke()
        }
        if (!last) DividerLine(Modifier.align(Alignment.BottomCenter).padding(horizontal = Tokens.Inset))
    }
}

@Composable
private fun RecentDeploymentRow(
    deployment: Deployment,
    project: Project,
    onOpen: (() -> Unit)?,
    options: (@Composable () -> Unit)?,
) {
    val palette = LocalDashboardPalette.current
    val metadataStyle = GeistText.copy(fontSize = Tokens.RecentMetadata, lineHeight = Tokens.RecentMetadataLine)
    val pullRequest = deployment.pullRequest
    Box(Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).heightIn(min = Tokens.RecentDeploymentRow)
                    .then(if (onOpen == null) Modifier else Modifier.clickable(onClick = onOpen))
                    .padding(top = Tokens.RecentContentTop,
                        end = if (onOpen == null) Tokens.Touch + Tokens.Space8 else Tokens.Space8, bottom = Tokens.Space8),
                horizontalArrangement = Arrangement.spacedBy(Tokens.RecentTitleGap),
            ) {
                Box(Modifier.width(Tokens.RecentAvatarSlot).padding(top = Tokens.Stroke)) {
                    Image(
                        painterResource(R.drawable.dashboard_sample_avatar), contentDescription = deployment.author,
                        modifier = Modifier.padding(start = Tokens.Space2).size(Tokens.Icon).clip(CircleShape),
                    )
                    Box(
                        Modifier.align(Alignment.CenterEnd).size(Tokens.Icon)
                            .background(palette.projectTile, RoundedCornerShape(Tokens.RecentProjectRadius)),
                        contentAlignment = Alignment.Center,
                    ) { ProjectArtwork(project, Tokens.Icon, Tokens.MetadataIcon) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.RecentMetadataGap)) {
                    Text(
                        deployment.title, style = GeistText, color = palette.ink,
                        fontSize = Tokens.ListTitle, lineHeight = Tokens.ListTitleLine, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.MiddleEllipsis,
                    )
                    Row(
                        Modifier.heightIn(min = Tokens.RecentMetadataHeight),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.RecentMetadataSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Space4),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painterResource(R.drawable.dashboard_github_mark), contentDescription = "GitHub",
                                modifier = Modifier.size(Tokens.MetadataIcon), colorFilter = ColorFilter.tint(palette.ink),
                            )
                            Text(
                                pullRequest?.let { "#$it" } ?: "Source", color = palette.ink,
                                style = metadataStyle, fontWeight = FontWeight.Medium,
                            )
                        }
                        Text(
                            if (pullRequest == null) deployment.status.label else deployment.sha, color = palette.ink,
                            style = metadataStyle, fontFamily = if (pullRequest != null) GeistMono else null,
                            fontWeight = if (pullRequest == null) FontWeight.Medium else FontWeight.Normal,
                        )
                        Text(deployment.elapsed, color = palette.secondary, style = metadataStyle)
                    }
                }
            }
            options?.invoke()
        }
        if (onOpen != null) DividerLine(Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
internal fun DeploymentPreviewCard(
    project: Project, deployment: Deployment, outlined: Boolean = true, onClick: () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    val previewPaper = palette.previewPaper
    val previewInk = palette.previewInk
    val previewShape = RoundedCornerShape(Tokens.ProjectPreviewRadius)
    val backdrop = rememberGraphicsLayer()
    var previewOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier.fillMaxWidth().height(Tokens.ProjectPreviewHeight).clip(previewShape).background(previewPaper)
            .onGloballyPositioned { previewOrigin = it.positionInRoot() }.clickable(onClick = onClick),
    ) {
        Box(Modifier.matchParentSize().drawWithContent {
            backdrop.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop)
        }) {
            Box(Modifier.fillMaxWidth().height(Tokens.Touch).background(
                Brush.horizontalGradient(listOf(previewPaper, Color.White, previewPaper)),
            ))
            Row(
                Modifier.fillMaxSize().padding(start = Tokens.Inset, top = Tokens.Space24, end = Tokens.Inset),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
                    ProjectMark(project, Modifier.size(Tokens.IconSlot))
                    Text(project.name, fontSize = Tokens.Caption, lineHeight = Tokens.AlertPillLine,
                        fontWeight = FontWeight.SemiBold, color = previewInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Project overview", color = previewInk, fontSize = Tokens.ChartCaption, lineHeight = Tokens.AlertPillLine)
                    Box(Modifier.fillMaxWidth(.82f).height(Tokens.Stroke).background(previewInk.copy(alpha = .25f)))
                }
                Column(Modifier.weight(2f).padding(top = Tokens.Space24), verticalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
                    Text("Latest changes", color = previewInk, fontSize = Tokens.ChartCaption, lineHeight = Tokens.AlertPillLine,
                        fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
                        for ((title, fill) in listOf("Overview" to palette.badge, "Activity" to previewInk)) {
                            Column(
                                Modifier.weight(1f).height(Tokens.BottomBar).background(fill, RoundedCornerShape(Tokens.AlertLoadingRadius))
                                    .padding(Tokens.Space6),
                                verticalArrangement = Arrangement.spacedBy(Tokens.Space4),
                            ) {
                                Text(title, color = Color.White, fontSize = Tokens.ChartCaption, lineHeight = Tokens.AlertPillLine)
                                Box(Modifier.fillMaxWidth(.7f).height(Tokens.Stroke).background(Color.White.copy(alpha = .48f)))
                                Box(Modifier.fillMaxWidth(.5f).height(Tokens.Stroke).background(Color.White.copy(alpha = .26f)))
                            }
                        }
                    }
                }
            }
        }
        val (ink, paper) = when (deployment.status) {
            DeploymentStatus.Ready -> palette.ready to palette.readySurface
            DeploymentStatus.Error -> palette.error to palette.errorSurface
            DeploymentStatus.Building -> palette.warning to palette.warningSurface
            DeploymentStatus.Canceled -> palette.secondary to palette.control
        }
        val ready = deployment.status == DeploymentStatus.Ready
        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = Tokens.PreviewStatusInset, bottom = Tokens.Space8)
                .widthIn(min = Tokens.PreviewStatusWidth + if (outlined) Tokens.Space2 else Dp.Hairline)
                .heightIn(min = Tokens.PreviewStatusHeight)
                .then(if (ready) Modifier.dashboardReadyGlass(backdrop, previewOrigin, outlined) else Modifier.background(paper, CircleShape))
                .padding(horizontal = Tokens.PreviewStatusInset, vertical = Tokens.DockInset),
            horizontalArrangement = Arrangement.spacedBy(Tokens.DockInset, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(Tokens.ProjectDetailGap).background(if (ready) palette.previewReadyDot else ink, CircleShape))
            Text(deployment.status.label, color = ink, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListTitle,
                fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}


@Composable
internal fun DeploymentScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val deployment = DashboardFixtures.deployment(route.target ?: DashboardFixtures.defaultDeployment)
    val project = navigation.project(deployment.projectId)
    val palette = LocalDashboardPalette.current
    Column(Modifier.fillMaxSize()) {
        DashboardHeader(Destination.Deployment.label, onBack, modifier = Modifier.padding(top = Tokens.Space8 + Tokens.Space2),
            titleSize = Tokens.Body, titleWeight = FontWeight.Medium, titleLine = Tokens.ListSubtitleLine)
        DashboardList(
            Modifier.fillMaxSize(), contentPadding = PaddingValues(start = Tokens.Inset, end = Tokens.Inset, top = Tokens.Space8, bottom = Tokens.DockScrollClearance),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space24 + Tokens.Space2),
        ) {
            item {
                Column {
                    Text(navigation.projectName(project), Modifier.padding(start = Tokens.Space4), color = palette.secondary,
                        fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.ProjectSectionLine)
                    Text(deployment.title, Modifier.padding(start = Tokens.Space4, top = Tokens.Space4),
                        fontSize = Tokens.DeploymentDetailTitle, lineHeight = Tokens.DeploymentDetailTitleLine,
                        fontWeight = FontWeight.SemiBold)
                    Text("${deployment.environment} deployment", Modifier.padding(start = Tokens.Space6,
                        top = Tokens.DeploymentEnvironmentGap, bottom = Tokens.DeploymentPreviewGap), color = palette.secondary,
                        fontWeight = FontWeight.Medium, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine)
                    DeploymentPreviewCard(project, deployment, outlined = route.variant != "heading") {
                        navigation.show(Destination.Preview, deployment.id)
                    }
                }
            }
            item {
                val rows = listOf(
                    "Project" to navigation.projectName(project), "Status" to deployment.status.label,
                    "Created" to deployment.elapsed, "Environment" to deployment.environment, "Domain" to project.domain,
                )
                Column(Modifier.clip(RoundedCornerShape(Tokens.ProjectOptionsRadius)).background(palette.projectSheetGroup)) {
                    rows.forEachIndexed { index, (label, value) ->
                        Box(Modifier.then(if (index == 0) Modifier.clickable { navigation.show(Destination.Project, project.id) } else Modifier)) {
                            DeploymentDetailRow(label, value)
                        }
                        if (index < rows.lastIndex) DividerLine(Modifier.padding(horizontal = Tokens.ProjectOptionsInset), palette.insetSurface)
                    }
                }
            }
            item {
                Text("GitHub", Modifier.padding(start = Tokens.Space8, bottom = Tokens.Space6), color = palette.secondary,
                    fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine, fontWeight = FontWeight.Medium)
                val outbound = route.variant != "heading"
                Column(Modifier.clip(RoundedCornerShape(Tokens.ProjectOptionsRadius)).background(palette.projectSheetGroup)) {
                    DeploymentDetailRow("Branch", deployment.branch, outbound)
                    DividerLine(Modifier.padding(horizontal = Tokens.ProjectOptionsInset), palette.insetSurface)
                    DeploymentDetailRow("Commit", deployment.sha, outbound)
                    deployment.pullRequest?.let {
                        DividerLine(Modifier.padding(horizontal = Tokens.ProjectOptionsInset), palette.insetSurface)
                        DeploymentDetailRow("Pull request", "#$it", outbound)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeploymentDetailRow(label: String, value: String, outbound: Boolean = false) {
    val palette = LocalDashboardPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = Tokens.Touch).padding(start = Tokens.AlertMeasurePad, end = Tokens.Inset),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space6), verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine)
        Text(value, Modifier.weight(2f), textAlign = TextAlign.End, color = palette.secondary,
            fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine, fontWeight = FontWeight.Medium,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (outbound) DashboardIcon(Glyph.Outbound, palette.secondary)
    }
}

@Composable
internal fun DeploymentSheetContent(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val deployment = DashboardFixtures.deployment(route.target ?: DashboardFixtures.defaultDeployment)
    val project = navigation.project(deployment.projectId)
    val palette = LocalDashboardPalette.current
    val copy = rememberCopyText()
    val projectName = navigation.projectName(project)
    if (route.destination == Destination.RemoveRecent) {
        Column(Modifier.fillMaxWidth().height(Tokens.RecentsRemoveBody)) {
            DashboardHeader(route.destination.label, onBack, modifier = Modifier.padding(top = Tokens.Space2))
            Column(Modifier.fillMaxWidth().weight(1f).dashboardScroll().padding(top = Tokens.Space12)) {
                RecentDeploymentRow(deployment, project, onOpen = null, options = null)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset).padding(bottom = Tokens.Stroke),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                GlassAction("Cancel", onBack, Modifier.weight(148f).heightIn(min = Tokens.Touch), labelWeight = FontWeight.Medium)
                GlassAction("Remove", {
                    navigation.removeRecent(deployment.id)
                    onBack()
                }, Modifier.weight(205f).heightIn(min = Tokens.Touch), destructive = true, labelWeight = FontWeight.Medium)
            }
        }
        return
    }
    val toolbar = route.destination == Destination.Toolbar
    val unavailable = route.variant == "unavailable"
    val protected = route.variant == "protected"
    val fromOverview = route.variant == "project-overview" && !toolbar
    val height = when {
        toolbar || unavailable -> Tokens.CompactPreviewBody
        protected -> Tokens.ProtectedPreviewBody
        fromOverview -> Tokens.BrowserPreviewBody + Tokens.PreviewActionRow
        else -> Tokens.BrowserPreviewBody
    }
    val bodyInset = when {
        unavailable || toolbar -> Tokens.DockIcon
        protected -> Tokens.Space24 + Tokens.Space2
        else -> Tokens.Space24 + Tokens.Space4
    }
    Column(Modifier.fillMaxWidth().height(height)) {
        DashboardHeader(route.destination.label, onBack, titleLine = Tokens.DeploymentDetailTitleLine)
        Column(Modifier.weight(1f).fillMaxWidth().dashboardScroll().padding(horizontal = Tokens.Inset).padding(top = bodyInset)) {
            when {
                toolbar -> {
                    DeploymentPreviewNotice("Toolbar unavailable", "Toolbar is disabled for this deployment.")
                    Text("Actions", Modifier.padding(start = Tokens.Space8, top = Tokens.BodyInset, bottom = Tokens.PreviewStatusInset),
                        fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.MetadataLine, color = palette.secondary,
                        fontWeight = FontWeight.Medium)
                    DashboardGroup {
                        DashboardRow("Open deployment preview", leading = { DashboardIcon(Glyph.Eye, palette.secondary) }, onClick = {
                            navigation.replace(route.copy(destination = Destination.Preview, variant = "content"))
                        }, trailing = { DashboardIcon(Glyph.Outbound, palette.secondary) }, minHeight = Tokens.PreviewActionRow)
                        DividerLine(Modifier.padding(horizontal = Tokens.Inset), palette.border)
                        DashboardRow("Toolbar setup", leading = { DashboardIcon(Glyph.ToolbarDocs, palette.secondary, Modifier.width(Tokens.DockIcon)) },
                            trailing = { DashboardIcon(Glyph.Outbound, palette.secondary) }, minHeight = Tokens.PreviewActionRow)
                    }
                }
                unavailable -> {
                    DeploymentPreviewNotice("Preview unavailable", "This deployment has no preview URL.")
                    Text("Actions", Modifier.padding(start = Tokens.Space8, top = Tokens.Space24 + Tokens.DockInset, bottom = Tokens.DockInset),
                        fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine, color = palette.secondary,
                        fontWeight = FontWeight.Medium)
                    DashboardGroup {
                        DashboardRow("Back to deployment", leading = { DashboardIcon(Glyph.PreviewDeployment, palette.secondary) },
                            onClick = onBack, minHeight = Tokens.PreviewActionRow)
                    }
                }
                protected -> DashboardGroup {
                    Row(Modifier.padding(start = Tokens.Inset, top = Tokens.DockIcon, end = Tokens.Inset + Tokens.Space20),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
                        DashboardIcon(Glyph.Lock, palette.secondary, Modifier.size(Tokens.Icon))
                        Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
                            Text("Deployment is protected", fontSize = Tokens.RootTitle, lineHeight = Tokens.DeploymentDetailTitleLine,
                                fontWeight = FontWeight.SemiBold)
                            Text("Continue in your browser to verify access with Vercel.", fontSize = Tokens.ListSubtitle,
                                lineHeight = Tokens.ProjectOptionsLine, color = palette.secondary)
                        }
                    }
                    DividerLine(Modifier.padding(horizontal = Tokens.Inset).padding(top = Tokens.Space6), palette.border)
                    Text(projectName, Modifier.padding(horizontal = Tokens.Inset).padding(top = Tokens.Space8, bottom = Tokens.Space8 + Tokens.Space2),
                        color = palette.secondary, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.ProjectSectionLine)
                }
                else -> DashboardGroup {
                    DashboardRow(projectName, "${deployment.environment} · ${deployment.branch} · ${deployment.sha}",
                        leading = { DashboardIcon(Glyph.Eye, palette.secondary) }, minHeight = Tokens.PreviewRecordRow)
                    DividerLine(Modifier.padding(horizontal = Tokens.Inset), palette.border)
                    DashboardRow("Deployment URL", deployment.url,
                        leading = { DashboardIcon(Glyph.ProjectDomain, palette.secondary, Modifier.size(Tokens.DockIcon)) }, trailing = {
                            IconControl(Glyph.Copy, "Copy deployment URL", { copy(deployment.url) }, visualSize = Tokens.Space20, glass = false)
                        }, minHeight = Tokens.PreviewRecordRow)
                    if (fromOverview) {
                        DividerLine(Modifier.padding(horizontal = Tokens.Inset), palette.border)
                        DashboardRow("Visit with Toolbar", leading = { DashboardIcon(Glyph.Toolbar, palette.secondary) }, onClick = {
                            navigation.replace(route.copy(destination = Destination.Toolbar))
                        }, minHeight = Tokens.PreviewActionRow)
                    }
                }
            }
        }
        if (!toolbar && !unavailable) {
            GlassAction(if (protected) "Continue in browser" else "Open in browser", {},
                Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset, vertical = Tokens.Space4),
                enabled = false, glyph = Glyph.BrowserAction)
        }
    }
}

@Composable
private fun DeploymentPreviewNotice(title: String, description: String) {
    val palette = LocalDashboardPalette.current
    Row(Modifier.padding(horizontal = Tokens.AlertMeasurePad), horizontalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
        DashboardIcon(Glyph.PreviewInfo, palette.secondary)
        Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
            Text(title, fontSize = Tokens.ListTitle, lineHeight = Tokens.SheetTitleLine, fontWeight = FontWeight.Medium)
            Text(description, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine, color = palette.secondary)
        }
    }
}
