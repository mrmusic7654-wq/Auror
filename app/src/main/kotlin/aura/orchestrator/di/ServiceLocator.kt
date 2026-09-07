package aura.orchestrator.di

import aura.orchestrator.credential.CredentialBroker
import aura.orchestrator.data.AppDatabase
import aura.orchestrator.data.SettingsRepository
import aura.orchestrator.engine.CapabilityRegistry
import aura.orchestrator.engine.ModelGatewayClient
import aura.orchestrator.engine.OrchestratorEngine
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

/**
 * Static access to the Hilt singleton graph for components the OS instantiates
 * outside Dagger's control (WorkManager workers and the bound service).
 */
@Singleton
class ServiceLocator @Inject constructor(
    val okHttp: OkHttpClient,
    val database: AppDatabase,
    val settings: SettingsRepository,
    val broker: CredentialBroker,
    val gateway: ModelGatewayClient,
    val engine: OrchestratorEngine,
    val registry: CapabilityRegistry,
    val appScope: CoroutineScope
) {
    companion object {
        @Volatile private var instance: ServiceLocator? = null
        fun init(locator: ServiceLocator) { instance = locator }
        fun get(): ServiceLocator = checkNotNull(instance) { "ServiceLocator not initialized" }
        fun reset() { instance = null }
    }
}
