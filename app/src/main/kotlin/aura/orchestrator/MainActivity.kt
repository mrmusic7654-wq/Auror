package aura.orchestrator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import aura.orchestrator.ui.AuraViewModel
import aura.orchestrator.ui.screens.AgentsScreen
import aura.orchestrator.ui.screens.DiagnosticsScreen
import aura.orchestrator.ui.screens.HomeScreen
import aura.orchestrator.ui.screens.ProjectsScreen
import aura.orchestrator.ui.screens.SettingsScreen
import aura.orchestrator.ui.screens.ToolsScreen
import aura.orchestrator.ui.theme.AuraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AuraViewModel = hiltViewModel()
            val dark by vm.isDark.collectAsState()
            AuraTheme(darkTheme = dark) {
                AuraShell(vm)
            }
        }
    }
}

private data class Tab(val label: String, val glyph: String)

private val tabs = listOf(
    Tab("Home", "◈"),
    Tab("Agents", "◉"),
    Tab("Projects", "▤"),
    Tab("Tools", "⌘"),
    Tab("Settings", "⚙")
)

@Composable
fun AuraShell(vm: AuraViewModel) {
    var selected by rememberSaveable { mutableStateOf(0) }
    var showDiag by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!showDiag) {
                NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { i, tab ->
                        NavigationBarItem(
                            selected = selected == i,
                            onClick = { selected = i },
                            icon = { Text(tab.glyph, fontSize = 18.sp) },
                            label = { Text(tab.label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            when {
                showDiag -> DiagnosticsScreen(vm, onBack = { showDiag = false })
                else -> when (selected) {
                    0 -> HomeScreen(vm)
                    1 -> AgentsScreen()
                    2 -> ProjectsScreen(vm)
                    3 -> ToolsScreen(vm)
                    else -> SettingsScreen(vm, onOpenDiagnostics = { showDiag = true })
                }
            }
        }
    }
}
