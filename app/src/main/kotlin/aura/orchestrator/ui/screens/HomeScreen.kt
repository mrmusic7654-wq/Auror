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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aura.orchestrator.data.TaskEntity
import aura.orchestrator.ui.AuraViewModel
import aura.orchestrator.ui.components.AuraCard
import aura.orchestrator.ui.components.DragonCore
import aura.orchestrator.ui.components.DragonState
import aura.orchestrator.ui.components.SectionTitle
import aura.orchestrator.ui.components.StatusPill
import aura.orchestrator.ui.theme.FrostCyan
import aura.orchestrator.ui.theme.MutedText
import aura.orchestrator.ui.theme.OnDark

@Composable
fun HomeScreen(vm: AuraViewModel) {
    val active by vm.activeTasks.collectAsState()
    val tasks by vm.allTasks.collectAsState()
    val modelConfigured by vm.modelConfigured.collectAsState()
    var input by remember { mutableStateOf("") }

    val busy = active.isNotEmpty()
    val state = when {
        active.any { it.status == "FAILED" } -> DragonState.ERROR
        busy -> DragonState.EXECUTING
        else -> DragonState.IDLE
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                DragonCore(state = state, size = 132)
                Text("Dragon Core", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp))
                Text("Intelligent orchestration workspace", color = MutedText, fontSize = 13.sp)
                Text(
                    "◉ ${state.label}",
                    color = if (busy) FrostCyan else MaterialTheme.colorScheme.onBackground,
                    fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            AuraCard {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What should we accomplish?") },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎙", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 6.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { vm.submit(input); input = "" }
                                    .padding(10.dp)
                            ) { Text("➤", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                )
                if (!modelConfigured) {
                    Text(
                        "No AI model configured. Offline commands now: calculate, list files, download <url>, note <text>.",
                        color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        if (busy) {
            item {
                AuraCard {
                    SectionTitle("ACTIVE TASKS")
                    active.forEach { TaskItem(it, vm, expandedDefault = true) }
                }
            }
        }

        item {
            Column {
                SectionTitle("ACTIVITY")
                if (tasks.isEmpty()) {
                    AuraCard { Text("No activity yet. Ask AURA to calculate, list files, download a URL, or save a note.", color = MutedText) }
                } else {
                    tasks.take(15).forEach { TaskItem(it, vm) }
                }
            }
        }
    }
}

@Composable
fun TaskItem(t: TaskEntity, vm: AuraViewModel, expandedDefault: Boolean = false) {
    var expanded by remember { mutableStateOf(expandedDefault) }
    AuraCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${t.complexity} · ${t.id}", color = MutedText, fontSize = 11.sp)
            }
            StatusPill(t.status)
        }
        if (t.progress > 0 && (t.status == "RUNNING" || t.status == "QUEUED")) {
            Text("${t.progress}%", color = FrostCyan, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
        if (expanded) {
            Text(t.detail.ifBlank { "No detail." }, color = OnDark, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                if (t.status == "FAILED" || t.status == "CANCELLED") {
                    Button(onClick = { vm.retry(t.id) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("↻  Retry", fontSize = 12.sp)
                    }
                }
                if (t.status == "RUNNING" || t.status == "QUEUED" || t.status == "PENDING") {
                    Button(onClick = { vm.cancel(t.id) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("✕  Cancel", fontSize = 12.sp)
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide" else "Details", color = FrostCyan, fontSize = 12.sp)
            }
        }
    }
}
