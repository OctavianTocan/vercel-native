package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        DashboardHeader("Profile Settings", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space12)) {
            item {
                Surface(shape = RoundedCornerShape(Tokens.CardRadius), color = palette.group) {
                    DashboardRow("Avatar", "Upload a photo from your device.", onClick = { navigation.show(Destination.Photo) }, trailing = {
                        Box(Modifier.size(Tokens.TopBar).background(palette.badge, CircleShape), contentAlignment = Alignment.Center) {
                            if (navigation.samplePhoto) SampleAvatar(Modifier.fillMaxSize()) else Text("JE", fontSize = Tokens.Title)
                            Box(Modifier.align(Alignment.BottomEnd).background(palette.control, CircleShape).padding(Tokens.Space2)) {
                                DashboardIcon(Glyph.Settings, palette.muted)
                            }
                        }
                    })
                }
            }
            item {
                SectionLabel("Display name")
                MutedText("The name shown on your Vercel profile.")
                OutlinedTextField(
                    name, { name = it }, Modifier.fillMaxWidth().focusRequester(nameRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (name.isNotBlank()) navigation.displayName = name.trim()
                        focus.clearFocus()
                    }),
                )
            }
            item {
                SectionLabel("Username")
                MutedText("Your URL namespace within Vercel.")
                OutlinedTextField(
                    username, { username = it; conflict = false },
                    Modifier.fillMaxWidth().focusRequester(usernameRequester), singleLine = true,
                    isError = conflict, prefix = { Text("vercel.com/") },
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
            item {
                SectionLabel("Default scopes")
                Surface(shape = RoundedCornerShape(Tokens.CardRadius), color = palette.group) {
                    Column {
                        DashboardRow("Default team", navigation.defaultTeam, onClick = { navigation.show(Destination.DefaultScope) })
                        DividerLine(Modifier.padding(horizontal = Tokens.Inset))
                        DashboardRow("Landing page", navigation.landingPage, onClick = { navigation.show(Destination.DefaultScope, variant = "landing") })
                    }
                }
            }
        }
    }
}

@Composable
internal fun AccountSheetContent(route: DashboardRoute, navigation: DashboardNavigation, onBack: () -> Unit) {
    val palette = LocalDashboardPalette.current
    val landing = route.variant == "landing"
    DashboardHeader(if (landing) "Landing page" else route.destination.label, onBack)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(Tokens.Inset), verticalArrangement = Arrangement.spacedBy(Tokens.Space8)) {
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
    var phase by rememberSaveable { mutableStateOf(if (route.variant == "content" && navigation.agentEnabled) "conversation" else route.variant) }
    var input by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf("Is the latest production deployment ready?") }
    val project = navigation.project(null)
    val deployment = DashboardFixtures.deployments(project.id).first()
    val send = {
        if (input.isNotBlank() && phase == "conversation") {
            sent = input
            input = ""
            focus.clearFocus()
        }
    }

    LaunchedEffect(phase, route.frozen) {
        if (phase == "enabling" && !route.frozen) {
            delay(1300)
            navigation.agentEnabled = true
            phase = "conversation"
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        DashboardHeader("Agent", onBack, Glyph.Close, subtitle = navigation.currentWorkspace.name, trailing = {
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
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Tokens.Inset),
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
