package aura.orchestrator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aura.orchestrator.ui.AuraViewModel
import aura.orchestrator.ui.components.AuraCard
import aura.orchestrator.ui.components.SectionTitle
import aura.orchestrator.ui.components.StatusPill
import aura.orchestrator.ui.theme.FrostCyan
import aura.orchestrator.ui.theme.MutedText
import aura.orchestrator.ui.theme.OnDark

data class ProviderPreset(val name: String, val baseUrl: String, val model: String)

private val presets = listOf(
    ProviderPreset("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
    ProviderPreset("OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini"),
    ProviderPreset("Ollama", "http://10.0.2.2:11434/v1", "llama3.2"),
    ProviderPreset("LM Studio", "http://localhost:1234/v1", "local-model")
)

@Composable
fun SettingsScreen(vm: AuraViewModel, onOpenDiagnostics: () -> Unit) {
    val dark by vm.isDark.collectAsState()
    val memoryOn by vm.memoryEnabled.collectAsState()
    val cfg by vm.modelConfig.collectAsState()

    var provider by remember { mutableStateOf("openai") }
    var baseUrl by remember { mutableStateOf(cfg.baseUrl) }
    var model by remember { mutableStateOf(cfg.modelName) }
    var apiKey by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle("APPEARANCE")
        AuraCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Dark theme", color = MaterialTheme.colorScheme.onSurface)
                    Text("Luxury cyber-dragon dark surface", color = MutedText, fontSize = 11.sp)
                }
                Switch(checked = dark, onCheckedChange = { vm.setDark(it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Memory", color = MaterialTheme.colorScheme.onSurface)
                    Text("Persist layered working/project memory", color = MutedText, fontSize = 11.sp)
                }
                Switch(checked = memoryOn, onCheckedChange = { vm.setMemoryEnabled(it) })
            }
        }

        SectionTitle("AI MODELS")
        AuraCard {
            Text("Connect an OpenAI-compatible endpoint. Secrets are stored in the encrypted credential vault and never enter model context or logs.", color = MutedText, fontSize = 12.sp)
            Row(
                modifier = Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { p ->
                    Box(
                        modifier = Modifier
                            .background(if (baseUrl == p.baseUrl) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                            .clickable { provider = p.name.lowercase().replace(" ", ""); baseUrl = p.baseUrl; model = p.model }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text(p.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) }
                }
            }
            OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Base URL") }, singleLine = true)
            OutlinedTextField(value = model, onValueChange = { model = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Model") }, singleLine = true)
            OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("API key (leave blank to keep existing)") }, singleLine = true)
            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { vm.configureModel(baseUrl, model, provider, apiKey) }) { Text("Save & connect") }
                Button(onClick = { vm.disableModel() }) { Text("Disconnect") }
            }
            if (cfg.configured) {
                StatusPill("CONNECTED · ${cfg.providerId}")
            } else {
                Text("Status: no model connected.", color = FrostCyan, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        SectionTitle("INTEGRATIONS")
        AuraCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Remote compute (Python / Linux sandbox)", color = MaterialTheme.colorScheme.onSurface)
                    Text("Not configured — add a remote WebSocket/sandbox endpoint in a future build.", color = MutedText, fontSize = 11.sp)
                }
                StatusPill("N/A")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("GitHub", color = MaterialTheme.colorScheme.onSurface)
                    Text("Push/PR actions require configuration and explicit permission.", color = MutedText, fontSize = 11.sp)
                }
                StatusPill("N/A")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("MCP tools", color = MaterialTheme.colorScheme.onSurface)
                    Text("External MCP servers can be adapted into AURA capabilities.", color = MutedText, fontSize = 11.sp)
                }
                StatusPill("N/A")
            }
        }

        SectionTitle("SYSTEM")
        AuraCard {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Diagnostics", color = MaterialTheme.colorScheme.onSurface)
                    Text("Core health, providers, storage, network", color = MutedText, fontSize = 11.sp)
                }
                Button(onClick = onOpenDiagnostics) { Text("Open") }
            }
            Text(
                "AURA Orchestrator v0.1.0 — protocol aura/1.0 · offline-first · Android 9+",
                color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
fun DiagnosticsScreen(vm: AuraViewModel, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<aura.orchestrator.engine.OrchestratorEngine.HealthItem>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        items = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { vm.diagnostics() }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("‹ Back", color = MaterialTheme.colorScheme.onSurface) }
            Text("System Health", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 14.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 18.dp)) {
            items.forEach {
                AuraCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (it.ok) "✓" else "—", color = if (it.ok) Color(0xFF7BE09B) else MutedText, fontWeight = FontWeight.Bold)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(it.label, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                            Text(it.detail, color = MutedText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        AuraCard {
            Text("Safe diagnostics only. Structured logs (requestId, taskId, agentId, pluginId) are recorded in the execution log; secrets are never logged.", color = MutedText, fontSize = 11.sp)
        }
    }
}
