package com.octaviantocan.vercelnative.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.PopupPositionProvider
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

@Composable
internal fun ProjectsScreen(route: DashboardRoute, navigation: DashboardNavigation) {
    val projects = navigation.projects()
    var showFavorites by rememberSaveable(route.variant) {
        mutableStateOf(route.variant !in setOf("plain", "icons", "menu-add", "menu-earlier", "theme-plain"))
    }
    val useProjectTiles = !showFavorites && route.variant in setOf("plain", "icons")
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
    val listState = rememberLazyListState()
    val toggleFavorite: (String) -> Unit = { projectId ->
        navigation.changeFavorite(projectId)
        if (!showFavorites) {
            showFavorites = true
            listState.requestScrollToItem(0)
        }
    }

    Column(Modifier.fillMaxSize()) {
        DashboardRootHeader("Projects") { navigation.show(Destination.More) }
        if (route.variant == "loading") {
            ProjectLoadingList(Modifier.weight(1f))
            return@Column
        }
        if (route.variant in setOf("empty", "error")) {
            val empty = route.variant == "empty"
            DashboardEmpty(
                title = if (empty) "A clean slate." else "Couldn’t load projects",
                description = if (empty) "No projects in this workspace yet." else "Check your connection and try again.",
                glyph = Glyph.Project,
                modifier = Modifier.weight(1f).padding(bottom = if (empty) Tokens.BottomBar else Tokens.DockScrollClearance + Tokens.BodyInset),
                illustration = if (empty) R.drawable.dashboard_figma_projects_empty else R.drawable.dashboard_figma_projects_error,
                action = if (empty) null else ({ navigation.main(Destination.Projects) }), glassAction = true,
                prominentTitle = !empty,
                descriptionGap = if (empty) Tokens.Space4 else Tokens.FeedbackExplanationGap,
            )
            return@Column
        }
        Spacer(Modifier.height(Tokens.Inset))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            DashboardList(Modifier.fillMaxSize(), state = listState,
                contentPadding = PaddingValues(bottom = Tokens.DockScrollClearance),
            ) {
                if (favorite.isNotEmpty()) item { ProjectSectionLabel("Favorites") }
                items(favorite, key = Project::id) { project ->
                    ProjectListRow(
                        project, navigation, expanded == project.id,
                        { expanded = project.id }, { expanded = null }, { toggleFavorite(project.id) },
                    )
                }
                if (showFavorites) item { ProjectSectionLabel("Other projects") }
                itemsIndexed(other, key = { _, project -> project.id }) { index, project ->
                    ProjectListRow(
                        project, navigation, expanded == project.id,
                        { expanded = project.id }, { expanded = null }, { toggleFavorite(project.id) },
                        earlier = route.variant == "menu-earlier",
                        useTile = useProjectTiles,
                        topDivider = !showFavorites && index == 0,
                    )
                }
            }
            val attempted = other.firstOrNull()
            if (favoriteFailed && attempted != null) {
                DashboardGlassNotice(
                    "Couldn’t add to Favorites", detail = navigation.projectName(attempted), error = true,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(horizontal = Tokens.Inset).padding(bottom = Tokens.DockScrollClearance),
                    onRetry = { toggleFavorite(attempted.id); favoriteFailed = false },
                )
            }
        }
    }
}

@Composable
private fun ProjectLoadingList(modifier: Modifier) {
    val palette = LocalDashboardPalette.current
    DashboardList(modifier.fillMaxWidth().semantics { contentDescription = "Loading projects" },
        contentPadding = PaddingValues(start = Tokens.Space24, top = Tokens.Inset, end = Tokens.Space24, bottom = Tokens.DockScrollClearance),
    ) {
        items(Tokens.AlertLoadingRows + 3) { index ->
            if (index == 0 || index == 1) {
                Box(Modifier.height(Tokens.ProjectSectionHeight), contentAlignment = Alignment.CenterStart) {
                    Box(Modifier.fillMaxWidth(if (index == 0) .16f else .24f).height(Tokens.Space8).background(palette.selected, CircleShape))
                }
            }
            Box(Modifier.fillMaxWidth().height(Tokens.ProjectListRow)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.ProjectBadgeGap)) {
                    Box(Modifier.size(Tokens.RowIcon).background(palette.selected, CircleShape))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
                        Box(Modifier.fillMaxWidth(listOf(.65f, .76f, .35f, .27f, .59f)[index % 5])
                            .height(Tokens.Space8).background(palette.selected, CircleShape))
                        Box(Modifier.fillMaxWidth(listOf(.82f, .44f, .37f, .72f)[index % 4])
                            .height(Tokens.Space8).background(palette.selected, CircleShape))
                    }
                    DashboardIcon(Glyph.More, palette.selected)
                }
                DividerLine(Modifier.align(Alignment.BottomCenter))
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
    expanded: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onToggleFavorite: () -> Unit,
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
                            if (expanded) Modifier.background(palette.projectBadge, CircleShape)
                                .border(Tokens.Stroke, palette.insetSurface, CircleShape) else Modifier,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        DashboardArtwork(R.drawable.dashboard_icon_row_more, Tokens.IconSlot, palette.projectEllipsis)
                    }
                }
                ProjectOptions(project, navigation, earlier, expanded, onClose, onToggleFavorite)
            }
        }
        val divider = if (useTile) palette.projectTileDivider else palette.projectDivider
        if (topDivider) DividerLine(Modifier.align(Alignment.TopCenter).padding(horizontal = Tokens.Space24), divider)
        DividerLine(Modifier.align(Alignment.BottomCenter).padding(horizontal = Tokens.Space24), divider)
    }
}

private data class ProjectOption(val title: String, @DrawableRes val icon: Int, val onClick: () -> Unit)

@Composable
private fun ProjectOptions(
    project: Project,
    navigation: DashboardNavigation,
    earlier: Boolean,
    expanded: Boolean,
    onClose: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    val light = palette == DashboardPalette.Light
    val id = project.id
    val deployment = DashboardFixtures.deployments(id).first()
    val options = listOf(
        ProjectOption(
            if (id in navigation.favorites) "Remove Favorite" else "Add Favorite",
            R.drawable.dashboard_icon_favorite, onToggleFavorite,
        ),
        ProjectOption("Visit with Toolbar", R.drawable.dashboard_icon_toolbar) { navigation.show(Destination.Toolbar, deployment.id) },
        ProjectOption("View Logs", R.drawable.dashboard_icon_view_logs) { navigation.show(Destination.Logs, id) },
        ProjectOption("Manage Domains", R.drawable.dashboard_icon_manage_domains) { navigation.show(Destination.ProjectDomains, id) },
        ProjectOption("Transfer Project", R.drawable.dashboard_icon_transfer) { navigation.show(Destination.Transfer, id) },
        ProjectOption("Settings", R.drawable.dashboard_icon_settings) { navigation.show(Destination.ProjectSettings, id) },
    )
    val cardRadius = if (light) Tokens.ProjectOptionsLightRadius else Tokens.ProjectOptionsRadius
    val shape = RoundedCornerShape(cardRadius)
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
    DashboardMenu(expanded, position, onClose) {
        Column(
            Modifier.dismissOutsideRoundedCard(cardRadius, shadowMargin, onClose).padding(shadowMargin).width(if (light) Tokens.ProjectOptionsLightWidth else Tokens.ProjectOptionsWidth)
                .then(if (light) Modifier.shadow(Tokens.Space12, shape) else Modifier)
                .clip(shape).background(palette.projectOptionsSurface).border(Tokens.Stroke, palette.projectOptionsBorder, shape)
                .padding(Tokens.Stroke).dashboardScroll(),
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

internal class ProjectOptionsPosition(
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
