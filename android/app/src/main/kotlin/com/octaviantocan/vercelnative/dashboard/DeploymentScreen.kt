package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val palette = LocalDashboardPalette.current
    val recent = project != null
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen)
            .heightIn(min = if (recent) Tokens.RecentDeploymentRow else Tokens.ProjectDeploymentRow)
            .padding(start = Tokens.Inset, end = Tokens.Space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
    ) {
        Box(Modifier.width(if (recent) Tokens.RowIcon else Tokens.Icon)) {
            Image(
                painterResource(R.drawable.dashboard_sample_avatar),
                contentDescription = deployment.author,
                modifier = Modifier.size(Tokens.Icon).clip(CircleShape),
            )
            if (project != null) {
                Box(
                    Modifier.align(Alignment.CenterEnd).size(Tokens.Icon).background(palette.control, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(project.name.take(1).uppercase(), color = palette.secondary, fontSize = Tokens.Caption)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
            Text(
                deployment.title,
                fontSize = Tokens.Body,
                fontWeight = if (recent) FontWeight.Medium else FontWeight.Normal,
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
    DividerLine(Modifier.padding(horizontal = Tokens.Inset))
}

@Composable
internal fun DeploymentPreviewCard(
    project: Project, deployment: Deployment, tinted: Boolean = true, onClick: () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(Tokens.CardRadius),
        color = palette.group,
    ) {
        Box(Modifier.fillMaxWidth().height(Tokens.PreviewHeight).padding(Tokens.Inset)) {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                DashboardIcon(Glyph.Project, palette.muted)
                Text(project.domain, fontSize = Tokens.Title, fontWeight = FontWeight.SemiBold)
                MutedText(deployment.environment)
                Box(Modifier.fillMaxWidth(0.72f).height(Tokens.Space4).background(palette.border, RoundedCornerShape(Tokens.SmallRadius)))
                Box(Modifier.fillMaxWidth(0.42f).height(Tokens.Space4).background(palette.border, RoundedCornerShape(Tokens.SmallRadius)))
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
}


@Composable
internal fun DeploymentScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val deployment = DashboardFixtures.deployment(route.target ?: DashboardFixtures.defaultDeployment)
    val project = navigation.project(deployment.projectId)
    Column(Modifier.fillMaxSize()) {
        DashboardHeader(Destination.Deployment.label, onBack)
        LazyColumn(
            Modifier.fillMaxSize(), contentPadding = PaddingValues(Tokens.Inset),
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
                DashboardRow("Project", navigation.projectName(project), glyph = Glyph.Project, onClick = { navigation.show(Destination.Project, project.id) })
                KeyValueRow("Status", deployment.status.label)
                KeyValueRow("Created", deployment.elapsed)
                KeyValueRow("Environment", deployment.environment)
                KeyValueRow("Domain", project.domain)
                SectionLabel("GitHub")
                KeyValueRow("Branch", deployment.branch, Glyph.Branch)
                KeyValueRow("Commit", deployment.sha, Glyph.Branch)
                deployment.pullRequest?.let { KeyValueRow("Pull request", "#$it") }
            }
        }
    }
}

@Composable
internal fun DeploymentSheetContent(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val deployment = DashboardFixtures.deployment(route.target ?: DashboardFixtures.defaultDeployment)
    val project = navigation.project(deployment.projectId)
    DashboardHeader(route.destination.label, onBack)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
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
