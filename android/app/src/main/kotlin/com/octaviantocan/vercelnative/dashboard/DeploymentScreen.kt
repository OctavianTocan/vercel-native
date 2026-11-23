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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

@Composable
internal fun DeploymentRow(
    deployment: Deployment,
    project: Project?,
    onOpen: () -> Unit,
    options: (@Composable () -> Unit)? = null,
) {
    if (project != null) {
        RecentDeploymentRow(deployment, project, onOpen, options)
        return
    }
    val palette = LocalDashboardPalette.current
    Box(Modifier.fillMaxWidth().background(palette.group)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen)
                .heightIn(min = Tokens.ProjectDeploymentRow)
                .padding(start = Tokens.Inset, end = Tokens.Space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.RecentTitleGap),
        ) {
            Image(
                painterResource(R.drawable.dashboard_sample_avatar),
                contentDescription = deployment.author,
                modifier = Modifier.size(Tokens.Icon).clip(CircleShape),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
                Text(
                    deployment.title,
                    fontSize = Tokens.ListTitle, lineHeight = Tokens.ListSubtitleLine,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painterResource(R.drawable.dashboard_github_mark), contentDescription = "GitHub",
                        modifier = Modifier.size(Tokens.MetadataIcon), colorFilter = ColorFilter.tint(palette.secondary),
                    )
                    MutedText(deployment.pullRequest?.let { "#$it" } ?: "Source")
                    MutedText(deployment.status.label.takeIf { deployment.pullRequest == null } ?: deployment.sha, monospace = true)
                    MutedText(deployment.elapsed)
                }
            }
            options?.invoke()
        }
        DividerLine(Modifier.align(Alignment.BottomCenter).padding(horizontal = Tokens.Inset))
    }
}

@Composable
private fun RecentDeploymentRow(
    deployment: Deployment,
    project: Project,
    onOpen: () -> Unit,
    options: (@Composable () -> Unit)?,
) {
    val palette = LocalDashboardPalette.current
    val metadataStyle = GeistText.copy(fontSize = Tokens.RecentMetadata, lineHeight = Tokens.RecentMetadataLine)
    val pullRequest = deployment.pullRequest
    Box(Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).heightIn(min = Tokens.RecentDeploymentRow).clickable(onClick = onOpen)
                    .padding(top = Tokens.RecentContentTop, end = Tokens.Space8, bottom = Tokens.Space8),
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
        DividerLine(Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
internal fun DeploymentPreviewCard(
    project: Project, deployment: Deployment, tinted: Boolean = true, onClick: () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    val previewPaper = palette.previewPaper
    val previewInk = palette.previewInk
    val previewShape = RoundedCornerShape(Tokens.ProjectPreviewRadius)
    Box(
        Modifier.fillMaxWidth().height(Tokens.ProjectPreviewHeight).clip(previewShape).background(previewPaper)
            .clickable(onClick = onClick).padding(Tokens.Space12),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
                ProjectMark(project, Modifier.size(Tokens.IconSlot))
                Text(project.name, fontSize = Tokens.ListSubtitle, fontWeight = FontWeight.SemiBold, color = previewInk,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Preview", color = previewInk, fontSize = Tokens.Metadata, fontWeight = FontWeight.Medium)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
                repeat(3) { index ->
                    val card = RoundedCornerShape(Tokens.SmallRadius)
                    Column(
                        Modifier.weight(1f).fillMaxHeight().background(Color.White, card)
                            .border(Tokens.Stroke, Color.Black.copy(alpha = .09f), card).padding(Tokens.Space4),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Space4),
                    ) {
                        Box(Modifier.fillMaxWidth().weight(1f).background(
                            if (index == 1) palette.chart.copy(alpha = .40f) else previewPaper,
                            RoundedCornerShape(Tokens.AlertLoadingRadius),
                        ))
                        Box(Modifier.fillMaxWidth(.64f).height(Tokens.Space2).background(previewInk.copy(alpha = .3f), CircleShape))
                    }
                }
            }
        }
        val tone = when (deployment.status) {
            DeploymentStatus.Ready -> Tone.Success
            DeploymentStatus.Error -> Tone.Error
            DeploymentStatus.Building -> Tone.Warning
            DeploymentStatus.Canceled -> Tone.Neutral
        }
        StatusPill(deployment.status.label, if (tinted) tone else Tone.Neutral, Modifier.align(Alignment.BottomEnd))
    }
}


@Composable
internal fun DeploymentScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val deployment = DashboardFixtures.deployment(route.target ?: DashboardFixtures.defaultDeployment)
    val project = navigation.project(deployment.projectId)
    val palette = LocalDashboardPalette.current
    Column(Modifier.fillMaxSize()) {
        DashboardHeader(Destination.Deployment.label, onBack,
            modifier = Modifier.padding(top = Tokens.AlertDetailHeaderInset), titleSize = Tokens.ListSubtitle)
        DashboardList(
            Modifier.fillMaxSize(), contentPadding = PaddingValues(start = Tokens.Inset, end = Tokens.Inset, top = Tokens.Space8, bottom = Tokens.DockScrollClearance),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space12),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
                    MutedText(navigation.projectName(project))
                    Text(deployment.title, fontSize = Tokens.Heading, fontWeight = FontWeight.SemiBold)
                    MutedText("${deployment.environment} deployment")
                    DeploymentPreviewCard(project, deployment, tinted = route.variant != "heading") {
                        navigation.show(Destination.Preview, deployment.id)
                    }
                }
            }
            item {
                val rows = listOf(
                    "Project" to navigation.projectName(project), "Status" to deployment.status.label,
                    "Created" to deployment.elapsed, "Environment" to deployment.environment, "Domain" to project.domain,
                )
                Column(Modifier.clip(RoundedCornerShape(Tokens.ProjectOptionsRadius)).background(palette.group)) {
                    rows.forEachIndexed { index, (label, value) ->
                        Box(Modifier.then(if (index == 0) Modifier.clickable { navigation.show(Destination.Project, project.id) } else Modifier)) {
                            KeyValueRow(label, value)
                        }
                        if (index < rows.lastIndex) DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                    }
                }
            }
            item {
                SectionLabel("GitHub")
                Column(Modifier.clip(RoundedCornerShape(Tokens.ProjectOptionsRadius)).background(palette.group)) {
                    KeyValueRow("Branch", deployment.branch)
                    DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                    KeyValueRow("Commit", deployment.sha)
                    deployment.pullRequest?.let {
                        DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                        KeyValueRow("Pull request", "#$it")
                    }
                }
            }
        }
    }
}

@Composable
internal fun DeploymentSheetContent(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val deployment = DashboardFixtures.deployment(route.target ?: DashboardFixtures.defaultDeployment)
    val project = navigation.project(deployment.projectId)
    DashboardHeader(route.destination.label, onBack)
    Column(Modifier.dashboardScroll().padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
        when (route.destination) {
            Destination.RemoveRecent -> {
                Text("Remove this entry from the Recents list?", fontSize = Tokens.Body)
                DashboardRow(deployment.title, "${navigation.projectName(project)} · ${deployment.sha}", leading = { ProjectMark(project) })
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                    androidx.compose.material3.OutlinedButton(onBack, Modifier.weight(1f)) { Text("Cancel") }
                    DashboardButton("Remove", {
                        navigation.removeRecent(deployment.id)
                        onBack()
                    }, Modifier.weight(1f))
                }
            }
            Destination.Toolbar -> {
                DashboardNotice("Toolbar unavailable", tone = Tone.Warning)
                MutedText("Toolbar is disabled for this deployment.")
                SectionLabel("Actions")
                DashboardRow("Open deployment preview", glyph = Glyph.Globe, onClick = {
                    navigation.replace(DashboardRoute(Destination.Preview, deployment.id))
                })
                DashboardRow("Toolbar setup", glyph = Glyph.Settings)
            }
            else -> {
                when {
                    route.variant == "protected" -> DashboardNotice("Deployment is protected", tone = Tone.Warning)
                    route.variant == "unavailable" -> DashboardNotice("Preview unavailable", tone = Tone.Warning)
                    else -> DashboardRow(navigation.projectName(project), "${deployment.environment} · ${deployment.branch} · ${deployment.sha}", leading = { ProjectMark(project) })
                }
                if (route.variant != "unavailable") {
                    KeyValueRow("Deployment URL", deployment.url, Glyph.Globe)
                    DashboardButton("Open in browser", {}, Modifier.fillMaxWidth(), enabled = false)
                } else DashboardButton("Back to deployment", onBack, Modifier.fillMaxWidth())
            }
        }
    }
}
