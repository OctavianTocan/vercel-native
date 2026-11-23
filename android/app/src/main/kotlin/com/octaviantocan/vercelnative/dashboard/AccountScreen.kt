package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlinx.coroutines.delay

@Composable
internal fun AccountScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val paper = LocalDashboardPalette.current.accountCanvas
    Box(Modifier.fillMaxSize().background(paper)) {
        CompositionLocalProvider(LocalDashboardScrollPaper provides paper) {
            if (route.destination == Destination.Agent) AgentScreen(route, navigation, onBack)
            else ProfileScreen(route, navigation, onBack)
        }
    }
}

@Composable
internal fun ProfileScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val focus = LocalFocusManager.current
    val nameRequester = remember { FocusRequester() }
    val usernameRequester = remember { FocusRequester() }
    var name by rememberSaveable { mutableStateOf(if (route.variant == "editing") "Jamie Parker" else navigation.displayName) }
    var username by rememberSaveable { mutableStateOf(if (route.variant == "conflict") "jamie" else navigation.username) }
    var conflict by rememberSaveable { mutableStateOf(route.variant == "conflict") }
    var nameFocused by remember { mutableStateOf(false) }
    var usernameFocused by remember { mutableStateOf(false) }
    LaunchedEffect(route.variant) {
        if (route.variant == "editing") nameRequester.requestFocus()
        if (route.variant == "conflict") usernameRequester.requestFocus()
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Box {
            DashboardHeader("Profile Settings", onBack, leftIcon = Glyph.BackChevron, modifier = Modifier.padding(vertical = Tokens.Space8))
            DividerLine(Modifier.align(Alignment.BottomCenter).padding(horizontal = Tokens.Inset, vertical = Tokens.Stroke), palette.profileDivider)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            DashboardList(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Tokens.Inset, top = Tokens.AlertMeasurePad, end = Tokens.Inset, bottom = Tokens.Space24),
                verticalArrangement = Arrangement.spacedBy(Tokens.AlertMeasurePad),
            ) {
                item {
                    ProfileCard(avatar = true) {
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.Button) { navigation.show(Destination.Photo) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
                                ProfileTitle("Avatar")
                                ProfileDescription("Upload a photo from your device.")
                            }
                            Box(Modifier.size(Tokens.ProfileAvatarBlock), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(Tokens.ProfileAvatar).background(palette.selected, CircleShape), contentAlignment = Alignment.Center) {
                                    if (navigation.samplePhoto) SampleAvatar(Modifier.fillMaxSize().clip(CircleShape))
                                    else Text("JE", fontSize = Tokens.Heading, fontWeight = FontWeight.SemiBold)
                                }
                                Box(Modifier.align(Alignment.BottomEnd).size(Tokens.Icon).background(palette.group, CircleShape), contentAlignment = Alignment.Center) {
                                    DashboardIcon(Glyph.Edit, palette.secondary)
                                }
                            }
                        }
                    }
                }
                item {
                    ProfileCard {
                        ProfileTitle("Display name")
                        ProfileDescription("The name shown on your Vercel profile.")
                        DashboardField(
                            name, { name = it },
                            Modifier.fillMaxWidth().focusRequester(nameRequester).onFocusChanged { nameFocused = it.isFocused }
                                .semantics { contentDescription = "Display name" },
                            height = Tokens.ProfileFieldHeight, color = palette.profileField,
                            outline = if (nameFocused) palette.studyDeliveryInk else palette.profileFieldBorder,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (name.isNotBlank()) navigation.displayName = name.trim()
                                focus.clearFocus()
                            }),
                        )
                    }
                }
                item {
                    ProfileCard {
                        ProfileTitle("Username")
                        ProfileDescription("Your URL namespace within Vercel.")
                        DashboardField(
                            username, { username = it; conflict = false },
                            Modifier.fillMaxWidth().focusRequester(usernameRequester).onFocusChanged { usernameFocused = it.isFocused }
                                .semantics { contentDescription = "Username" },
                            height = Tokens.ProfileFieldHeight, color = palette.profileField,
                            outline = when {
                                conflict -> palette.profileConflictBorder
                                usernameFocused -> palette.studyDeliveryInk
                                else -> palette.profileFieldBorder
                            },
                            leading = {
                                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.BodyInset), verticalAlignment = Alignment.CenterVertically) {
                                    Text("vercel.com/", Modifier.widthIn(min = Tokens.ProfilePrefixWidth),
                                        color = palette.plotInk, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine)
                                    Box(Modifier.width(Tokens.Stroke).height(Tokens.Space24).background(palette.profileFieldBorder))
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                conflict = username.trim() == "jamie"
                                if (!conflict && username.isNotBlank()) {
                                    navigation.username = username.trim()
                                    focus.clearFocus()
                                }
                            }),
                        )
                    }
                }
                item {
                    ProfileCard {
                        ProfileTitle("Default scopes")
                        Column(Modifier.padding(top = Tokens.Space4), verticalArrangement = Arrangement.spacedBy(Tokens.Inset)) {
                            ProfilePicker("Default team", "Your API and CLI requests use this team by default.",
                                navigation.defaultTeam ?: "Choose a default team") {
                                navigation.show(Destination.DefaultScope)
                            }
                            ProfilePicker("Landing page", "Choose which team or organization opens when you visit the dashboard.",
                                navigation.landingPage ?: "Choose a team or organization") {
                                navigation.show(Destination.DefaultScope, variant = "landing")
                            }
                        }
                    }
                }
            }
            if (conflict) DashboardGlassNotice("Username already in use", error = true,
                modifier = Modifier.align(Alignment.BottomCenter).padding(Tokens.Inset))
        }
    }
}

@Composable
private fun ProfileCard(avatar: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.ProfileCardRadius)
    Column(
        Modifier.fillMaxWidth().background(palette.sheet, shape).border(Tokens.Stroke, palette.insetSurface, shape)
            .padding(start = Tokens.Inset, end = if (avatar) Tokens.AlertMeasurePad else Tokens.Inset,
                top = if (avatar) Tokens.AlertMeasurePad else Tokens.BodyInset,
                bottom = if (avatar) Tokens.AlertMeasurePad else Tokens.BodyInset),
        verticalArrangement = Arrangement.spacedBy(Tokens.ProjectDetailGap),
        content = content,
    )
}

@Composable
private fun ProfileTitle(label: String) {
    Text(label, fontSize = Tokens.RootTitle, lineHeight = Tokens.ProfileHeadingLine, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ProfileDescription(label: String) {
    Text(label, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine, color = LocalDashboardPalette.current.profileCaption)
}

@Composable
private fun ProfilePicker(label: String, description: String, selected: String, onClick: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.ControlRadius)
    Column {
        Text(label, fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium)
        Text(description, Modifier.padding(top = Tokens.AlertTextGap),
            fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine, color = palette.profileCaption)
        Row(
            Modifier.fillMaxWidth().padding(top = Tokens.Space8).heightIn(min = Tokens.ProfilePickerHeight).background(palette.profileField, shape)
                .border(Tokens.Stroke, palette.profileFieldBorder, shape).clickable(role = Role.Button, onClick = onClick).padding(horizontal = Tokens.Space12),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(selected, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine, color = palette.secondary)
            DashboardIcon(Glyph.Chevron, palette.secondary, Modifier.rotate(90f))
        }
    }
}

@Composable
internal fun AccountSheetContent(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val theme = route.destination == Destination.Theme
    val lightTheme = theme && palette == DashboardPalette.Light
    val landing = route.variant == "landing"
    DashboardHeader(if (landing) "Landing page" else route.destination.label, onBack,
        leftIcon = when {
            lightTheme -> Glyph.LightThemeBack
            theme -> Glyph.Back
            else -> Glyph.AccountBack
        },
        modifier = if (theme && !lightTheme) Modifier else Modifier.padding(top = Tokens.Space4),
        titleLine = Tokens.AccountSheetTitleLine, backGlass = !lightTheme)
    if (route.destination == Destination.Photo) {
        Column(
            Modifier.fillMaxWidth().dashboardScroll().padding(horizontal = Tokens.Inset)
                .heightIn(min = Tokens.ProfilePhotoPreviewBody),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.fillMaxWidth().padding(top = Tokens.ProfilePhotoTop),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.ProfilePhotoCaptionGap)) {
                SampleAvatar(Modifier.offset(x = -Tokens.ProjectDetailGap).size(Tokens.ProfilePhotoPreviewSize)
                    .clip(CircleShape).border(Tokens.Stroke, palette.border, CircleShape))
                Text("This photo will appear on your Vercel profile.", fontSize = Tokens.Body, lineHeight = Tokens.ProjectOptionsLine,
                    color = palette.secondary)
            }
            Row(Modifier.fillMaxWidth().padding(bottom = Tokens.Space2), horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                GlassAction("Choose another", {}, Modifier.weight(156f), enabled = false, labelWeight = FontWeight.Medium)
                GlassAction("Use photo", { navigation.samplePhoto = true; onBack() }, Modifier.weight(197f))
            }
        }
        return
    }
    if (!theme) {
        val selectedName = when {
            route.frozen && landing -> DashboardFixtures.workspace(DashboardFixtures.studioWorkspace).name
            route.frozen -> DashboardFixtures.workspace(DashboardFixtures.primaryWorkspace).name
            landing -> navigation.landingPage
            else -> navigation.defaultTeam
        }
        Column(Modifier.fillMaxWidth().dashboardScroll().padding(horizontal = Tokens.Inset)
            .padding(top = Tokens.ScopeDescriptionTop, bottom = if (landing) Tokens.ScopeLandingBottom else Tokens.ScopeTeamBottom),
            verticalArrangement = Arrangement.spacedBy(Tokens.ProjectOptionsInset),
        ) {
            Text(if (landing) "Choose the workspace that opens first in your dashboard." else "API and CLI requests use this team by default.",
                Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset), fontSize = Tokens.ListSubtitle,
                lineHeight = Tokens.ListSubtitleLine, color = palette.projectOptionsGlyph)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.ScopeGroupRadius))
                .background(palette.projectSheetGroup)) {
                for (scope in DashboardFixtures.transferWorkspaces) {
                    val selected = scope.name == selectedName
                    AccountChoice(scope.name, R.drawable.dashboard_figma_scope_team, Tokens.Touch, selected,
                        palette.insetSurface, Tokens.Space6, Tokens.ProjectOptionsInset,
                        check = true, checkInk = palette.studyDeliveryInk) {
                        if (landing) navigation.landingPage = scope.name else navigation.defaultTeam = scope.name
                        onBack()
                    }
                }
            }
        }
        return
    }
    val applied = when {
        route.frozen && lightTheme -> Appearance.Light
        route.frozen -> Appearance.Dark
        else -> navigation.appearance
    }
    Column(Modifier.fillMaxWidth().dashboardScroll().padding(horizontal = Tokens.Inset)
        .padding(top = if (lightTheme) Tokens.LightThemeTop else Tokens.Inset,
            bottom = if (lightTheme) Tokens.LightThemeBottom else Tokens.Space8),
    ) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.Space20))
            .background(if (lightTheme) palette.sheet else palette.group)) {
            Appearance.entries.forEachIndexed { index, choice ->
                if (!lightTheme && index > 0) DividerLine(Modifier.padding(horizontal = Tokens.Inset), palette.border)
                val artwork = when (choice) {
                    Appearance.System -> if (lightTheme) R.drawable.dashboard_figma_theme_system_light else Glyph.ThemeSystem.artwork
                    Appearance.Light -> if (lightTheme) R.drawable.dashboard_figma_theme_sun_light else Glyph.ThemeSun.artwork
                    Appearance.Dark -> if (lightTheme) R.drawable.dashboard_figma_theme_moon_light else Glyph.ThemeMoon.artwork
                }
                AccountChoice(choice.name, artwork, if (lightTheme) Tokens.LightThemeChoiceHeight else Tokens.ThemeChoiceHeight,
                    choice == applied, if (lightTheme) palette.insetSurface else palette.selected,
                    if (lightTheme) Tokens.Space6 else Tokens.Space4,
                    if (lightTheme) Tokens.Space20 - Tokens.Space6 else Tokens.ProjectOptionsLightRadius,
                    check = lightTheme, checkInk = palette.ink, darkTheme = !lightTheme) {
                    navigation.chooseAppearance(choice)
                    if (route.variant == "light") navigation.replace(DashboardRoute(Destination.Theme))
                }
            }
        }
    }
}

@Composable
private fun AccountChoice(
    label: String, artwork: Int, height: Dp, selected: Boolean, selectedSurface: Color, selectionInset: Dp,
    selectionRadius: Dp, check: Boolean, checkInk: Color, darkTheme: Boolean = false, onClick: () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    Box(Modifier.fillMaxWidth().heightIn(min = height).clickable(onClick = onClick)) {
        if (selected) Box(Modifier.matchParentSize().padding(horizontal = selectionInset,
            vertical = if (darkTheme) Tokens.Space4 else Tokens.Space6)
            .background(selectedSurface, RoundedCornerShape(selectionRadius)))
        Row(Modifier.fillMaxWidth().heightIn(min = height).padding(horizontal = Tokens.Inset), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(Tokens.DockIcon), contentAlignment = Alignment.Center) {
                val size = if (darkTheme && artwork == Glyph.ThemeSun.artwork) Tokens.Space20 else Tokens.DockIcon
                DashboardArtwork(artwork, size, if (darkTheme && selected) palette.ink else palette.secondary)
            }
            Text(label, Modifier.weight(1f).padding(start = if (darkTheme) Tokens.DockIcon else Tokens.ProjectOptionsInset),
                fontSize = Tokens.Body, lineHeight = if (darkTheme) Tokens.ListSubtitleLine else Tokens.ProjectOptionsLine,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (check && selected) DashboardArtwork(R.drawable.dashboard_figma_scope_check, Tokens.Inset, checkInk)
        }
    }
}

@Composable
private fun SampleAvatar(modifier: Modifier) {
    Image(painterResource(R.drawable.dashboard_figma_profile_photo), contentDescription = "Sample profile photo",
        modifier = modifier, contentScale = ContentScale.Crop)
}

@Composable
internal fun AgentScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val focus = LocalFocusManager.current
    val workspace = navigation.currentWorkspace
    val frozenEnabling = route.frozen && route.variant == "enabling"
    var phase by rememberSaveable(workspace.id) {
        mutableStateOf(if (route.variant == "content" && navigation.isAgentEnabled(workspace.id)) "conversation" else route.variant)
    }
    var input by rememberSaveable(workspace.id) { mutableStateOf("") }
    var sent by rememberSaveable(workspace.id) { mutableStateOf("Is the latest production deployment ready?") }
    val project = navigation.project(null)
    val deployment = DashboardFixtures.deployments(project.id).first()
    val send = {
        if (input.isNotBlank() && phase == "conversation") {
            sent = input
            input = ""
            focus.clearFocus()
        }
    }

    LaunchedEffect(workspace.id, phase, frozenEnabling) {
        if (phase == "enabling" && !frozenEnabling) {
            delay(1300)
            navigation.enableAgent(workspace.id)
            phase = "conversation"
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        DashboardHeader("Agent", onBack, Glyph.AgentClose, subtitle = workspace.name,
            modifier = Modifier.padding(top = Tokens.AlertDetailHeaderInset), trailing = {
            if (phase != "enabling" && phase != "failed") {
                IconControl(Glyph.ProjectSettings, "Agent settings", {}, enabled = false)
            }
        })
        when (phase) {
            "content" -> DashboardEmpty("Agent", "Ask about your projects and deployments.", Glyph.Agent,
                Modifier.weight(1f), action = { phase = "enabling" }, actionLabel = "Enable Agent")
            "enabling" -> DashboardEmpty("Enabling Agent…", "Agent is being enabled for ${workspace.name}.", Glyph.Agent,
                Modifier.dashboardScroll().padding(top = Tokens.AgentEnablingTop), illustration = R.drawable.dashboard_figma_agent_enabling,
                prominentTitle = true,
                footer = {
                    LinearProgressIndicator(
                        Modifier.width(Tokens.AgentProgressWidth).padding(top = Tokens.Inset).height(Tokens.AgentProgressHeight),
                        color = palette.chart, trackColor = palette.group,
                    )
                })
            "failed" -> DashboardEmpty("Couldn’t enable Agent", "Try again to enable Agent for\n${workspace.name}.", Glyph.Agent,
                Modifier.dashboardScroll().padding(top = Tokens.AgentFailureTop), action = { phase = "enabling" },
                illustration = R.drawable.dashboard_figma_agent_failure, glassAction = true, prominentTitle = true)
            else -> {
                val atLimit = phase == "limit"
                DashboardList(Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = Tokens.Inset, end = Tokens.Inset, top = Tokens.TopBar, bottom = Tokens.Inset),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space32 + Tokens.Space4),
                ) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Surface(Modifier.fillMaxWidth(.76f), color = palette.selected, contentColor = palette.ink,
                                shape = RoundedCornerShape(Tokens.Space20)) {
                                Text(sent, Modifier.padding(Tokens.Inset), fontSize = Tokens.Body, lineHeight = Tokens.ProjectOptionsLine)
                            }
                        }
                    }
                    item {
                        if (atLimit) {
                            Text("The latest production deployment is ready.", Modifier.padding(horizontal = Tokens.Space8),
                                color = palette.secondary, fontSize = Tokens.Body, lineHeight = Tokens.ProjectOptionsLine)
                            Column(Modifier.padding(top = Tokens.Space12).fillMaxWidth().background(palette.popover, RoundedCornerShape(Tokens.CardRadius))
                                .border(Tokens.Stroke, palette.border, RoundedCornerShape(Tokens.CardRadius)).padding(Tokens.Inset),
                                verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space12), verticalAlignment = Alignment.CenterVertically) {
                                    DashboardIcon(Glyph.Info, palette.link)
                                    Text("Included Agent usage reached", fontSize = Tokens.ListTitle, fontWeight = FontWeight.SemiBold)
                                }
                                Text("Check usage for this workspace before sending another message.", Modifier.padding(start = Tokens.Space32),
                                    fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine, color = palette.secondary)
                                GlassAction("View Agent usage", { navigation.main(Destination.Usage) }, Modifier.fillMaxWidth())
                            }
                        } else {
                            val shape = RoundedCornerShape(Tokens.Space20)
                            Column(Modifier.fillMaxWidth().background(palette.popover, shape).border(Tokens.Stroke, palette.border, shape)
                                .padding(Tokens.Space12)) {
                                Row(Modifier.padding(Tokens.Space4), horizontalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
                                    DashboardIcon(Glyph.Agent, palette.link)
                                    Text("Yes. The latest production deployment finished successfully.", Modifier.weight(1f),
                                        fontSize = Tokens.Body, lineHeight = Tokens.ListTitleLine)
                                }
                                Column(Modifier.padding(top = Tokens.Space32 + Tokens.Space6).fillMaxWidth()
                                    .background(palette.control, RoundedCornerShape(Tokens.ControlRadius))
                                    .border(Tokens.Stroke, palette.border, RoundedCornerShape(Tokens.ControlRadius)).padding(Tokens.Space12),
                                    verticalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
                                    Text(navigation.projectName(project), fontSize = Tokens.Body, fontWeight = FontWeight.Medium)
                                    StatusPill(deployment.status.label, Tone.Success)
                                    GlassAction("Open deployment", { navigation.show(Destination.Deployment, deployment.id) }, Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
                AgentComposer(input, { input = it }, send, atLimit)
            }
        }
    }
}

@Composable
private fun AgentComposer(value: String, onValueChange: (String) -> Unit, send: () -> Unit, atLimit: Boolean) {
    val palette = LocalDashboardPalette.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Tokens.Inset).padding(bottom = Tokens.AlertMeasurePad)
            .dashboardGlass(shape = RoundedCornerShape(Tokens.SearchPanelRadius))
            .heightIn(min = Tokens.AgentComposerHeight).padding(horizontal = Tokens.AlertMeasurePad, vertical = Tokens.Space8),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicTextField(
            value, onValueChange, Modifier.fillMaxWidth().padding(top = Tokens.Space6),
            enabled = !atLimit, singleLine = true, cursorBrush = SolidColor(palette.ink),
            textStyle = GeistText.copy(color = palette.ink, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine),
            decorationBox = { field ->
                Box {
                    if (value.isEmpty()) Text(if (atLimit) "Agent usage limit reached" else "Ask about your projects…",
                        color = palette.secondary, fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ListSubtitleLine)
                    field()
                }
            },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            DashboardIcon(Glyph.AgentAttach, if (atLimit) palette.disabled else palette.secondary)
            IconControl(Glyph.AgentSend, "Send message", send, enabled = !atLimit && value.isNotBlank(), visualSize = Tokens.ProjectSheetMark)
        }
    }
}
