package aura.orchestrator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import aura.core.agents.AgentIds
import aura.core.agents.BuiltinAgents
import aura.orchestrator.ui.AuraViewModel
import aura.orchestrator.ui.components.AuraCard
import aura.orchestrator.ui.components.SectionTitle
import aura.orchestrator.ui.components.StatusPill
import aura.orchestrator.ui.theme.FrostCyan
import aura.orchestrator.ui.theme.MutedText
import aura.orchestrator.ui.theme.OnDark

@Composable
fun AgentsScreen() {
    var disabled by remember { mutableStateOf<Set<String>>(emptySet()) }
    val agents = BuiltinAgents.all
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("AGENT SWARM") }
        item {
            Text("Bounded, permissioned executor roles. No agent holds unrestricted access; each routes through the capability registry + permission engine.", color = MutedText, fontSize = 12.sp)
        }
        items(agents.size) { idx ->
            val a = agents[idx]
            val isOn = a.agentId !in disabled
            val supervisor = a.agentId == AgentIds.SUPERVISOR
            AuraCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(if (supervisor) FrostCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(a.name.take(1), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(a.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(a.description.ifBlank { "Agent" }, color = MutedText, fontSize = 11.sp)
                        Text("capabilities: ${a.capabilities.size} · iter ${a.maxIterations} · tokens ${a.maxTokens / 1000}k",
                            color = FrostCyan, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                    Switch(checked = isOn, onCheckedChange = { on ->
                        disabled = if (on) disabled - a.agentId else disabled + a.agentId
                    })
                }
                if (!isOn) StatusPill("DISABLED")
            }
        }
    }
}

@Composable
fun ProjectsScreen(vm: AuraViewModel) {
    val projects by vm.projects.collectAsState()
    var name by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("PROJECTS")
            AuraCard {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(), placeholder = { Text("New project name") },
                    singleLine = true,
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { if (name.isNotBlank()) { vm.createProject(name); name = "" } }
                                .padding(10.dp)
                        ) { Text("+", color = Color.Black, fontWeight = FontWeight.Bold) }
                    })
            }
        }
        item { SectionTitle("WORKSPACES") }
        if (projects.isEmpty()) {
            item { AuraCard { Text("No projects yet. Create one to scope agents, tasks and memory.", color = MutedText) } }
        } else {
            items(projects.size) { idx ->
                val p = projects[idx]
                AuraCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("◉", color = FrostCyan)
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(p.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(p.id, color = MutedText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsScreen(vm: AuraViewModel) {
    val providers by vm.providers.collectAsState()
    val grouped = providers.groupBy { it.appId }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("CAPABILITY REGISTRY") }
        item {
            Text("Capabilities are resolved dynamically: capability → provider → transport → execution. The orchestrator never hardcodes a provider into the UI.", color = MutedText, fontSize = 12.sp)
        }
        grouped.forEach { (appId, caps) ->
            item { SectionTitle(appId) }
            items(caps.size) { ci ->
                val c = caps[ci]
                AuraCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(if (c.healthy) "✓" else "✕", color = if (c.healthy) Color(0xFF7BE09B) else MutedText)
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(c.capability, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(c.name, color = MutedText, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(c.transport, color = FrostCyan, fontSize = 10.sp)
                            Text("risk ${c.risk}", color = MutedText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        item { AuraCard { Text("External capabilities (AURA companion apps, MCP, remote compute, Python, Browser, GitHub) appear here when discovered on the network or configured.", color = MutedText, fontSize = 12.sp) } }
    }
}
