package aura.orchestrator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import aura.orchestrator.data.SettingsRepository
import aura.orchestrator.data.TaskEntity
import aura.orchestrator.engine.CapabilityRegistry
import aura.orchestrator.engine.OrchestratorEngine
import aura.orchestrator.engine.OrchestratorEngine.HealthItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AuraViewModel @Inject constructor(
    private val engine: OrchestratorEngine,
    val settings: SettingsRepository,
    private val registry: CapabilityRegistry
) : ViewModel() {

    val isDark: StateFlow<Boolean> = settings.isDark
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val memoryEnabled: StateFlow<Boolean> = settings.memoryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val modelConfig: StateFlow<SettingsRepository.ModelConfig> = settings.modelConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.ModelConfig("openai", "", "", null, false, false))

    val modelConfigured: StateFlow<Boolean> = settings.modelConfig
        .map { it.configured }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeTasks: StateFlow<List<TaskEntity>> = engine.observeActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = engine.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<aura.orchestrator.data.ProjectEntity>> = engine.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providers: StateFlow<List<aura.orchestrator.engine.CapabilityProvider>> = registry.providers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun submit(text: String) {
        val input = text.trim()
        if (input.isNotEmpty()) engine.submit(input)
    }

    fun retry(taskId: String) = engine.retry(taskId)
    fun cancel(taskId: String) = engine.cancel(taskId)
    fun deleteTask(taskId: String) = engine.deleteTask(taskId)

    fun setDark(dark: Boolean) = viewModelScopeLaunch { settings.setDark(dark) }
    fun setMemoryEnabled(on: Boolean) = viewModelScopeLaunch { settings.setMemoryEnabled(on) }
    fun configureModel(baseUrl: String, model: String, provider: String, key: String) =
        viewModelScopeLaunch { settings.configureModel(baseUrl, model, provider, key) }
    fun disableModel() = viewModelScopeLaunch { settings.disableModel() }
    fun createProject(name: String) = engine.createProjectAsync(name)

    fun diagnostics(): List<HealthItem> = engine.diagnostics()

    private fun viewModelScopeLaunch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
