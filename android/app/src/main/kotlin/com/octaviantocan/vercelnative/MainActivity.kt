package com.octaviantocan.vercelnative

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octaviantocan.vercelnative.data.SessionState
import com.octaviantocan.vercelnative.update.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app get() = application as VercelApp
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val session by app.account.session.state.collectAsStateWithLifecycle()
                val state by app.updater.state.collectAsStateWithLifecycle()
                val offer by app.updater.offer.collectAsStateWithLifecycle()
                val prompt by app.updater.prompt.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                var error by remember { mutableStateOf<String?>(null) }
                var busy by remember { mutableStateOf(false) }
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                val prefs = remember { getSharedPreferences("local", MODE_PRIVATE) }
                var sample by remember { mutableStateOf(prefs.getBoolean("sample", false)) }
                val run = { call: suspend () -> Unit ->
                    scope.launch {
                        busy = true; error = null
                        try { call() } catch (failure: Exception) { error = failure.message ?: "Please try again" }
                        finally { busy = false }
                    }
                    Unit
                }
                LaunchedEffect(session) {
                    if (session == SessionState.SignedIn && !BuildConfig.DEBUG) app.updater.checkIfStale()
                }
                LaunchedEffect(prompt) { prompt?.let { startActivity(it) } }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(if (offer != null) "Update Vercel Native" else "Vercel Native", style = MaterialTheme.typography.headlineLarge)
                        Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall)
                        if (offer == null) {
                            Text("The native workspace is ready.", style = MaterialTheme.typography.titleLarge)
                            Text("Dashboard screens will be built here. Downloads, sign-in and the release channel are connected independently.")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Switch(checked = sample, onCheckedChange = { sample = it; prefs.edit().putBoolean("sample", it).apply() })
                                Text("Keep this preference across updates")
                            }
                            HorizontalDivider()
                            Text("Downloads and updates", style = MaterialTheme.typography.titleLarge)
                        } else {
                            Text("Version ${offer!!.versionName} is ready. Update to keep using Vercel Native.")
                            offer!!.notes.forEach { Text("• $it") }
                        }
                        if (session != SessionState.SignedIn) {
                            Text("Sign in with the same account you use for the downloads page.")
                            Button(enabled = !busy, onClick = { run { app.account.google(this@MainActivity) } }) { Text(if (busy) "Signing in…" else "Continue with Google") }
                            OutlinedTextField(username, { username = it }, label = { Text("Password account username") }, singleLine = true)
                            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                            TextButton(enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                                onClick = { run { app.account.password(username, password); password = "" } }) { Text("Sign in with password") }
                        } else {
                            Text("Signed in for downloads")
                            if (offer == null) {
                                Button(enabled = !busy && !BuildConfig.DEBUG, onClick = { run { app.updater.check() } }) { Text("Check for updates") }
                                TextButton(onClick = { run { app.account.session.clear() } }) { Text("Sign out of downloads") }
                            }
                        }
                        when (val value = state) {
                            is UpdateState.Downloading -> { LinearProgressIndicator(progress = { value.progress }, modifier = Modifier.fillMaxWidth()); Text("Downloading…") }
                            is UpdateState.Installing -> Text("Installing…")
                            is UpdateState.Failed -> Text(value.reason, color = MaterialTheme.colorScheme.error)
                            UpdateState.UpToDate -> Text("You have the latest version")
                            UpdateState.Checking -> Text("Checking for updates…")
                            else -> Unit
                        }
                        if (offer != null && session == SessionState.SignedIn) {
                            Button(enabled = !busy && state !is UpdateState.Installing && state !is UpdateState.Downloading, onClick = {
                                if (!packageManager.canRequestPackageInstalls()) {
                                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                                } else run {
                                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, UpdateService::class.java))
                                    try {
                                        if (app.updater.state.value !is UpdateState.Ready) app.updater.check()
                                        app.updater.install()
                                    } finally { stopService(Intent(this@MainActivity, UpdateService::class.java)) }
                                }
                            }) { Text(if (prompt != null) "Continue update" else "Update now") }
                            if (prompt != null) TextButton(onClick = { startActivity(prompt) }) { Text("Open Android confirmation") }
                        }
                        if (BuildConfig.DEBUG) Text("Development build: production self-updating is disabled.")
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    override fun onStart() { super.onStart(); app.updater.foreground = true }
    override fun onStop() { app.updater.foreground = false; super.onStop() }
    override fun onResume() {
        super.onResume()
        if (app.account.session.token != null && !BuildConfig.DEBUG) app.scope.launch { app.updater.checkIfStale() }
    }
}
