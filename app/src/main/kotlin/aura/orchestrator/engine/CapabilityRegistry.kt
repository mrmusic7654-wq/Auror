package aura.orchestrator.engine

import aura.core.Capabilities
import aura.core.PermissionCatalog
import aura.core.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Metadata describing an installed capability provider (spec sections 13/39). */
data class CapabilityProvider(
    val appId: String,
    val capability: String,
    val name: String,
    val transport: String,
    val risk: RiskLevel,
    val healthy: Boolean = true,
    val version: String = "0.1.0"
) {
    val installed: Boolean = true
}

/**
 * Dynamic capability registry. The orchestrator resolves capability -> provider ->
 * transport at runtime; it never hardcodes knowledge of a provider into the UI.
 * Built-in providers register themselves; external apps/sandboxes register
 * themselves on connection and are removed on death.
 */
@Singleton
class CapabilityRegistry @Inject constructor() {

    private val _providers = MutableStateFlow<List<CapabilityProvider>>(emptyList())
    val providers: StateFlow<List<CapabilityProvider>> = _providers

    init {
        registerBuiltins()
    }

    private fun registerBuiltins() {
        val builtins = listOf(
            CapabilityProvider("aura.files", Capabilities.FILES_LIST, "Files · list", "internal", RiskLevel.LOW, healthy = true),
            CapabilityProvider("aura.files", Capabilities.FILES_READ, "Files · read", "internal", RiskLevel.LOW, healthy = true),
            CapabilityProvider("aura.files", Capabilities.FILES_WRITE, "Files · write", "internal", RiskLevel.MEDIUM, healthy = true),
            CapabilityProvider("aura.memory", Capabilities.MEMORY_NOTE, "Memory · note", "internal", RiskLevel.LOW, healthy = true),
            CapabilityProvider("aura.downloads", Capabilities.DOWNLOAD, "Downloads · HTTP(S)", "workmanager", RiskLevel.MEDIUM, healthy = true),
            CapabilityProvider("aura.calc", "aura.calc", "Calculator", "internal", RiskLevel.LOW, healthy = true),
            CapabilityProvider("aura.model", Capabilities.MODEL_CHAT, "Model gateway · chat", "https", RiskLevel.LOW, healthy = true)
        )
        _providers.value = builtins
    }

    fun markExternal(appId: String, capability: String, name: String, transport: String, version: String) {
        val p = CapabilityProvider(appId, capability, name, transport, PermissionCatalog.riskOf(capability), healthy = true, version = version)
        _providers.value = _providers.value.filterNot { it.appId == appId && it.capability == capability } + p
    }

    fun removeExternal(appId: String) {
        _providers.value = _providers.value.filterNot { it.appId == appId }
    }

    fun providersFor(appId: String): List<CapabilityProvider> = _providers.value.filter { it.appId == appId }
}
