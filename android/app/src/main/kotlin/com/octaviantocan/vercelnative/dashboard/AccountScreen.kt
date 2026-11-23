package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlinx.coroutines.delay

@Composable
internal fun AccountScreen(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    if (route.destination == Destination.Agent) AgentScreen(route, navigation, onBack)
    else ProfileScreen(route, navigation, onBack)
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
    LaunchedEffect(route.variant) {
        if (route.variant == "editing") nameRequester.requestFocus()
        if (route.variant == "conflict") usernameRequester.requestFocus()
    }
    Column(Modifier.fillMaxSize()) {
        DashboardHeader("Profile Settings", onBack, leftIcon = Glyph.BackChevron, modifier = Modifier.padding(vertical = Tokens.Space8))
        DashboardList(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Tokens.Inset, top = Tokens.Inset, end = Tokens.Inset, bottom = Tokens.Space24),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space12),
        ) {
            item {
                ProfileCard {
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
                                if (navigation.samplePhoto) SampleAvatar(Modifier.fillMaxSize())
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
                        Modifier.fillMaxWidth().focusRequester(nameRequester).semantics { contentDescription = "Display name" },
                        height = Tokens.ProfileFieldHeight, color = palette.event,
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
                        Modifier.fillMaxWidth().focusRequester(usernameRequester).semantics { contentDescription = "Username" },
                        height = Tokens.ProfileFieldHeight, error = conflict, color = palette.event,
                        leading = { ProfileDescription("vercel.com/") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            conflict = username.trim() == "jamie"
                            if (!conflict && username.isNotBlank()) {
                                navigation.username = username.trim()
                                focus.clearFocus()
                            }
                        }),
                    )
                    if (conflict) DashboardNotice("Username already in use", tone = Tone.Error, retry = { usernameRequester.requestFocus() })
                }
            }
            item {
                ProfileCard {
                    ProfileTitle("Default scopes")
                    ProfilePicker("Default team", "Your API and CLI requests use this team by default.", navigation.defaultTeam) {
                        navigation.show(Destination.DefaultScope)
                    }
                    ProfilePicker("Landing page", "Choose which team or organization opens when you visit the dashboard.", navigation.landingPage) {
                        navigation.show(Destination.DefaultScope, variant = "landing")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.ProfileCardRadius)
    Column(
        Modifier.fillMaxWidth().background(palette.sheet, shape).border(Tokens.Stroke, palette.quietBorder, shape)
            .padding(Tokens.Inset),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space6),
        content = content,
    )
}

@Composable
private fun ProfileTitle(label: String) {
    Text(label, fontSize = Tokens.RootTitle, lineHeight = Tokens.ProfileHeadingLine, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ProfileDescription(label: String) {
    Text(label, fontSize = Tokens.SearchDetailSize, lineHeight = Tokens.SearchDetailLine, color = LocalDashboardPalette.current.secondary)
}

@Composable
private fun ProfilePicker(label: String, description: String, selected: String, onClick: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val shape = RoundedCornerShape(Tokens.ControlRadius)
    Column(Modifier.padding(top = Tokens.Space4), verticalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
        Text(label, fontSize = Tokens.Body, lineHeight = Tokens.ListSubtitleLine, fontWeight = FontWeight.Medium)
        ProfileDescription(description)
        Row(
            Modifier.fillMaxWidth().heightIn(min = Tokens.ProfilePickerHeight).background(palette.event, shape)
                .border(Tokens.Stroke, palette.quietBorder, shape).clickable(role = Role.Button, onClick = onClick).padding(horizontal = Tokens.Space12),
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
    val landing = route.variant == "landing"
    DashboardHeader(if (landing) "Landing page" else route.destination.label, onBack)
    Column(Modifier.dashboardScroll().padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
        when (route.destination) {
            Destination.Photo -> {
                Surface(shape = RoundedCornerShape(Tokens.CardRadius), color = palette.group) {
                    Box(Modifier.fillMaxWidth().height(Tokens.PreviewHeight), contentAlignment = Alignment.Center) {
                        SampleAvatar(Modifier.size(Tokens.AvatarSize))
                    }
                }
                MutedText("This photo will appear on your Vercel profile.")
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
                    OutlinedButton({}, Modifier.weight(1f), enabled = false) { Text("Choose another") }
                    DashboardButton("Use photo", { navigation.samplePhoto = true; onBack() }, Modifier.weight(1f))
                }
            }
            Destination.DefaultScope -> {
                MutedText(if (landing) "Choose the workspace that opens first." else "API and CLI requests use this team by default.")
                for (scope in DashboardFixtures.workspaces) {
                    val selected = scope.name == if (landing) navigation.landingPage else navigation.defaultTeam
                    DashboardRow(scope.name, glyph = Glyph.Account, trailing = { if (selected) DashboardIcon(Glyph.Check) }, onClick = {
                        if (landing) navigation.landingPage = scope.name else navigation.defaultTeam = scope.name
                        onBack()
                    })
                }
            }
            else -> {
                val applied = if (route.variant == "light") Appearance.Light else navigation.appearance
                for (choice in Appearance.entries) {
                    val selected = choice == applied
                    val glyph = when (choice) {
                        Appearance.System -> Glyph.Settings
                        Appearance.Light -> Glyph.Theme
                        Appearance.Dark -> Glyph.Theme
                    }
                    DashboardRow(
                        choice.name,
                        modifier = Modifier.background(if (selected) palette.selected else palette.sheet, RoundedCornerShape(Tokens.ControlRadius)),
                        glyph = glyph, trailing = { if (selected) DashboardIcon(Glyph.Check) },
                        onClick = {
                            navigation.appearance = choice
                            if (route.variant == "light") navigation.replace(DashboardRoute(Destination.Theme))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleAvatar(modifier: Modifier) {
    Image(painterResource(R.drawable.dashboard_sample_avatar), contentDescription = "Sample profile photo", modifier = modifier)
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
        DashboardHeader("Agent", onBack, Glyph.Close, subtitle = workspace.name, trailing = {
            IconControl(Glyph.Settings, "Agent settings", {}, enabled = false)
        })
        when (phase) {
            "content" -> DashboardEmpty("Agent", "Ask about your projects and deployments.", Glyph.Agent,
                Modifier.weight(1f), action = { phase = "enabling" }, actionLabel = "Enable Agent")
            "enabling" -> DashboardEmpty("Enabling Agent…", "Setting up Agent for this workspace.", Glyph.Agent,
                Modifier.weight(1f), loading = true)
            "failed" -> DashboardEmpty("Couldn’t enable Agent", "Try again to enable Agent for this workspace.", Glyph.Agent,
                Modifier.weight(1f), action = { phase = "enabling" })
            else -> {
                Column(
                    Modifier.weight(1f).fillMaxWidth().dashboardScroll().padding(Tokens.Inset),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space12),
                ) {
                    Surface(Modifier.align(Alignment.End), color = palette.group, shape = RoundedCornerShape(Tokens.CardRadius)) {
                        Text(sent, Modifier.padding(Tokens.Space12), fontSize = Tokens.Body)
                    }
                    DashboardRow("The latest production deployment finished successfully.", glyph = Glyph.Agent)
                    Surface(color = palette.group, shape = RoundedCornerShape(Tokens.CardRadius)) {
                        Column {
                            DashboardRow(navigation.projectName(project), deployment.sha,
                                trailing = { StatusPill(deployment.status.label, Tone.Success) })
                            DashboardRow("Open deployment", glyph = Glyph.Project, onClick = { navigation.show(Destination.Deployment, deployment.id) })
                        }
                    }
                    if (phase == "limit") {
                        DashboardNotice("Included Agent usage reached", tone = Tone.Warning)
                        DashboardRow("View Agent usage", glyph = Glyph.Usage, onClick = { navigation.main(Destination.Usage) })
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(Tokens.Inset),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space8),
                ) {
                    OutlinedTextField(input, { input = it }, Modifier.weight(1f), singleLine = true, enabled = phase != "limit",
                        placeholder = { Text(if (phase == "limit") "Agent usage limit reached" else "Ask about your projects…") },
                        shape = CircleShape)
                    IconControl(Glyph.Send, "Send message", send, enabled = phase != "limit" && input.isNotBlank())
                }
            }
        }
    }
}
