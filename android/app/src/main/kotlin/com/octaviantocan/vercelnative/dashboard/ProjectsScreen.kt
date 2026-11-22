package com.octaviantocan.vercelnative.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

@Composable
internal fun ProjectsScreen(route: DashboardRoute, navigation: DashboardNavigation) {
    val projects = navigation.projects()
    val showFavorites = route.variant !in setOf("plain", "icons", "menu-add", "menu-earlier")
    val useProjectTiles = route.variant in setOf("plain", "icons")
    val favorite = projects.filter { showFavorites && it.id in navigation.favorites }
    val other = projects.filterNot { it in favorite }
    var expanded by rememberSaveable {
        mutableStateOf(
            when (route.variant) {
                "menu-add", "menu-earlier" -> projects.firstOrNull { it.id !in navigation.favorites }?.id
                "menu-remove" -> favorite.firstOrNull()?.id
                else -> null
            },
        )
    }
    var favoriteFailed by rememberSaveable { mutableStateOf(route.variant == "favorite-failed") }

    Column(Modifier.fillMaxSize()) {
        DashboardRootHeader("Projects") { navigation.show(Destination.More) }
        if (route.variant in setOf("loading", "empty", "error")) {
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
                modifier = Modifier.weight(1f).padding(bottom = Tokens.DockScrollClearance),
                loading = route.variant == "loading",
                action = if (route.variant == "error") ({ navigation.main(Destination.Projects) }) else null,
            )
            return@Column
        }
        Spacer(Modifier.height(Tokens.Inset))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = Tokens.DockScrollClearance)) {
            if (favoriteFailed) item {
                DashboardNotice(
                    "Couldn’t add to Favorites",
                    Modifier.padding(horizontal = Tokens.Inset),
                    Tone.Error,
                    retry = { favoriteFailed = false },
                )
            }
            if (favorite.isNotEmpty()) item { ProjectSectionLabel("Favorites") }
            items(favorite, key = Project::id) { project ->
                ProjectListRow(project, navigation, true, expanded == project.id, { expanded = project.id }, { expanded = null })
            }
            if (showFavorites) item { ProjectSectionLabel("Other projects") }
            itemsIndexed(other, key = { _, project -> project.id }) { index, project ->
                ProjectListRow(
                    project, navigation, false, expanded == project.id,
                    { expanded = project.id }, { expanded = null },
                    earlier = route.variant == "menu-earlier",
                    useTile = useProjectTiles,
                    topDivider = !showFavorites && index == 0,
                )
            }
        }
    }
}

@Composable
private fun ProjectSectionLabel(title: String) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = Tokens.ProjectSectionHeight)
            .padding(start = Tokens.Space24, top = Tokens.Space6, end = Tokens.Space24, bottom = Tokens.Stroke),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            title, style = GeistText, color = LocalDashboardPalette.current.secondary,
            fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine, fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ProjectListRow(
    project: Project,
    navigation: DashboardNavigation,
    favorite: Boolean,
    expanded: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    earlier: Boolean = false,
    useTile: Boolean = false,
    topDivider: Boolean = false,
) {
    val palette = LocalDashboardPalette.current
    val name = navigation.projectName(project)
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).heightIn(min = Tokens.ProjectListRow)
                    .clickable(role = Role.Button) { navigation.show(Destination.Project, project.id) }
                    .padding(start = if (useTile) Tokens.Space8 else Tokens.Space6, end = Tokens.Space8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (useTile) Tokens.Inset else Tokens.ProjectBadgeGap),
            ) {
                if (useTile) {
                    Box(
                        Modifier.size(Tokens.IconSlot).background(palette.projectTile, RoundedCornerShape(Tokens.ProjectTileRadius)),
                        contentAlignment = Alignment.Center,
                    ) { ProjectArtwork(project, Tokens.Space24, Tokens.MetadataIcon) }
                } else ProjectMark(project)
                Column(Modifier.weight(1f)) {
                    Text(
                        name, color = palette.ink, style = GeistText,
                        fontSize = Tokens.ListTitle, lineHeight = Tokens.ListTitleLine, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        project.domain, color = palette.secondary, style = GeistText,
                        fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine,
                        maxLines = 1, overflow = TextOverflow.MiddleEllipsis,
                    )
                }
            }
            Box {
                Box(
                    Modifier.size(Tokens.Touch).clip(CircleShape).clickable(role = Role.Button, onClick = onOpen)
                        .semantics { contentDescription = "Options for $name" },
                    contentAlignment = Alignment.Center,
                ) {
                    if (useTile) Box(Modifier.offset(x = Tokens.Space8)) {
                        DashboardArtwork(R.drawable.dashboard_icon_chevrons, Tokens.Inset, palette.secondary)
                    } else Box(
                        Modifier.size(Tokens.IconSlot).then(
                            if (favorite) Modifier.background(palette.projectBadge, CircleShape)
                                .border(Tokens.Stroke, palette.insetSurface, CircleShape) else Modifier,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        DashboardArtwork(R.drawable.dashboard_icon_row_more, Tokens.IconSlot, palette.projectEllipsis)
                    }
                }
                if (expanded) ProjectOptions(project, navigation, earlier, onClose)
            }
        }
        val divider = if (useTile) palette.projectTileDivider else palette.projectDivider
        if (topDivider) DividerLine(Modifier.align(Alignment.TopCenter).padding(horizontal = Tokens.Space24), divider)
        DividerLine(Modifier.align(Alignment.BottomCenter).padding(horizontal = Tokens.Space24), divider)
    }
}

private data class ProjectOption(val title: String, @DrawableRes val icon: Int, val onClick: () -> Unit)

@Composable
private fun ProjectOptions(project: Project, navigation: DashboardNavigation, earlier: Boolean, onClose: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val light = palette == DashboardPalette.Light
    val id = project.id
    val deployment = DashboardFixtures.deployments(id).first()
    val options = listOf(
        ProjectOption(
            if (id in navigation.favorites) "Remove Favorite" else "Add Favorite", R.drawable.dashboard_icon_favorite,
        ) { navigation.changeFavorite(id) },
        ProjectOption("Visit with Toolbar", R.drawable.dashboard_icon_toolbar) { navigation.show(Destination.Toolbar, deployment.id) },
        ProjectOption("View Logs", R.drawable.dashboard_icon_view_logs) { navigation.show(Destination.Logs, id) },
        ProjectOption("Manage Domains", R.drawable.dashboard_icon_manage_domains) { navigation.show(Destination.ProjectDomains, id) },
        ProjectOption("Transfer Project", R.drawable.dashboard_icon_transfer) { navigation.show(Destination.Transfer, id) },
        ProjectOption("Settings", R.drawable.dashboard_icon_settings) { navigation.show(Destination.ProjectSettings, id) },
    )
    val shape = RoundedCornerShape(if (light) Tokens.ProjectOptionsLightRadius else Tokens.ProjectOptionsRadius)
    val shadowMargin = if (light) Tokens.Space20 else Dp.Hairline
    val position = with(LocalDensity.current) {
        val edgeGap = (if (light) Tokens.Space8 else Tokens.Space2).roundToPx()
        val anchorGap = (Tokens.IconSlot / 2 + Tokens.Space2).roundToPx()
        val openingOffset = if (light) (-(Tokens.IconSlot / 2)).roundToPx() else anchorGap
        val viewportGap = Tokens.Space8.roundToPx()
        val margin = shadowMargin.roundToPx()
        remember(edgeGap, anchorGap, openingOffset, viewportGap, margin) {
            ProjectOptionsPosition(edgeGap, anchorGap, openingOffset, viewportGap, margin)
        }
    }
    Popup(position, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        Column(
            Modifier.padding(shadowMargin).width(if (light) Tokens.ProjectOptionsLightWidth else Tokens.ProjectOptionsWidth)
                .then(if (light) Modifier.shadow(Tokens.Space12, shape) else Modifier)
                .clip(shape).background(palette.projectOptionsSurface).border(Tokens.Stroke, palette.projectOptionsBorder, shape)
                .padding(Tokens.Stroke).verticalScroll(rememberScrollState()),
        ) {
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    if (earlier) DividerLine(Modifier.padding(horizontal = Tokens.ProjectOptionsInset))
                    else if (light) Spacer(Modifier.height(Tokens.Stroke))
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = if (light) Tokens.ProjectOptionsLightRow else Tokens.MenuRow)
                        .clickable(role = Role.Button) {
                            onClose()
                            option.onClick()
                        }
                        .padding(
                            start = if (light) Tokens.Inset else Tokens.ProjectOptionsInset,
                            end = if (light) Tokens.Inset - Tokens.Space2 else Tokens.ProjectOptionsInset,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space12),
                ) {
                    if (!light) {
                        Box(
                            Modifier.size(Tokens.ProjectOptionsIcon).offset(x = -Tokens.Stroke, y = -Tokens.Stroke),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (option.icon == R.drawable.dashboard_icon_settings) {
                                Icon(painterResource(option.icon), null, Modifier.requiredSize(Tokens.ProjectOptionsGear), tint = Color.Unspecified)
                            } else DashboardArtwork(option.icon, Tokens.ProjectOptionsIcon, palette.projectOptionsGlyph)
                        }
                    }
                    Text(
                        option.title, Modifier.weight(1f), color = palette.projectOptionsInk, style = GeistText,
                        fontSize = if (light) Tokens.ListTitle else Tokens.Body,
                        lineHeight = if (light) Tokens.ListSubtitleLine else Tokens.ProjectOptionsLine,
                    )
                    if (light && index == 0) {
                        DashboardArtwork(R.drawable.dashboard_icon_light_star, Tokens.ProjectOptionsIcon, palette.projectOptionsGlyph)
                    }
                }
            }
        }
    }
}

private class ProjectOptionsPosition(
    private val edgeGap: Int,
    private val anchorGap: Int,
    private val openingOffset: Int,
    private val viewportGap: Int,
    private val shadowMargin: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = when (layoutDirection) {
            LayoutDirection.Ltr -> anchorBounds.right - popupContentSize.width - edgeGap + shadowMargin
            LayoutDirection.Rtl -> anchorBounds.left + edgeGap - shadowMargin
        }
        val below = anchorBounds.center.y + openingOffset - shadowMargin
        val above = anchorBounds.center.y - anchorGap - popupContentSize.height + shadowMargin
        val maxY = (windowSize.height - popupContentSize.height - viewportGap).coerceAtLeast(0)
        val y = if (below <= maxY) below else above
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceIn(viewportGap.coerceAtMost(maxY), maxY),
        )
    }
}
