package aura.orchestrator.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import aura.orchestrator.credential.CredentialBroker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.auraDataStore by preferencesDataStore(name = "aura_settings")

/**
 * User preferences + model-endpoint configuration persisted via Jetpack DataStore.
 * The raw API key is never stored here — only the alias under which it lives in
 * the encrypted [CredentialBroker].
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val broker: CredentialBroker
) {
    private object K {
        val DARK = booleanPreferencesKey("dark_theme")
        val PROVIDER = stringPreferencesKey("model_provider_id")
        val BASE_URL = stringPreferencesKey("model_base_url")
        val MODEL = stringPreferencesKey("model_name")
        val KEY_ALIAS = stringPreferencesKey("model_key_alias")
        val ENABLED = booleanPreferencesKey("model_enabled")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
    }

    val isDark: Flow<Boolean> = context.auraDataStore.data.map { it[K.DARK] ?: true }
    val memoryEnabled: Flow<Boolean> = context.auraDataStore.data.map { it[K.MEMORY_ENABLED] ?: true }

    data class ModelConfig(
        val providerId: String,
        val baseUrl: String,
        val modelName: String,
        val keyAlias: String?,
        val hasKey: Boolean,
        val enabled: Boolean
    ) {
        val configured: Boolean get() = enabled && baseUrl.isNotBlank() && modelName.isNotBlank()
    }

    val modelConfig: Flow<ModelConfig> = context.auraDataStore.data.map { p ->
        val alias = p[K.KEY_ALIAS]
        ModelConfig(
            providerId = p[K.PROVIDER] ?: "openai",
            baseUrl = p[K.BASE_URL] ?: "https://api.openai.com/v1",
            modelName = p[K.MODEL] ?: "",
            keyAlias = alias,
            hasKey = alias?.let { broker.has(it) } ?: false,
            enabled = p[K.ENABLED] ?: false
        )
    }

    suspend fun setDark(dark: Boolean) = context.auraDataStore.edit { it[K.DARK] = dark }
    suspend fun setMemoryEnabled(on: Boolean) = context.auraDataStore.edit { it[K.MEMORY_ENABLED] = on }

    suspend fun configureModel(baseUrl: String, modelName: String, providerId: String, apiKey: String) {
        val alias = "model_api_key_$providerId"
        if (apiKey.isNotBlank()) broker.put(alias, apiKey.trim())
        context.auraDataStore.edit {
            it[K.BASE_URL] = baseUrl.trim().trimEnd('/')
            it[K.MODEL] = modelName.trim()
            it[K.PROVIDER] = providerId
            it[K.KEY_ALIAS] = alias
            it[K.ENABLED] = true
        }
    }

    suspend fun disableModel() = context.auraDataStore.edit { it[K.ENABLED] = false }

    suspend fun clearAll(): Preferences = context.auraDataStore.edit { it.clear() }

    /** Convenience one-shot snapshot of the current model config. */
    suspend fun modelConfigValue(): ModelConfig = modelConfig.first()

    /** Blocking snapshot (used from synchronous worker/service contexts). */
    fun modelConfigValueBlocking(): ModelConfig = kotlinx.coroutines.runBlocking { modelConfig.first() }

    /** Fetch a stored secret from the encrypted credential broker by alias. */
    fun apiKey(alias: String): String? = broker.get(alias)
}
