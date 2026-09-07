package aura.orchestrator.credential

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credential Broker (spec section 15). Secrets are held in an encrypted
 * preferences store whose keys live in the Android Keystore. The model/agents
 * never receive the raw secret — only a boolean "authenticated" signal via the
 * gateway. If hardware-backed crypto is unavailable on an unusual device we fall
 * back to a process-memory vault rather than crashing, and never persist plaintext.
 */
@Singleton
class CredentialBroker @Inject constructor(@androidx.annotation.ApplicationContext context: Context) {

    private val logTag = "AuraCredentialBroker"
    private val memoryVault = HashMap<String, String>()

    private val prefs: SharedPreferences? = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "aura_credential_vault",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        Log.w(logTag, "Encrypted prefs unavailable; using memory vault", it)
        null
    }

    @Synchronized
    fun put(alias: String, secret: String) {
        val p = prefs
        if (p != null) p.edit().putString(alias, secret).apply() else memoryVault[alias] = secret
    }

    @Synchronized
    fun get(alias: String): String? {
        val p = prefs
        return if (p != null) p.getString(alias, null) else memoryVault[alias]
    }

    @Synchronized
    fun has(alias: String): Boolean = (prefs?.contains(alias)) ?: memoryVault.containsKey(alias)

    @Synchronized
    fun delete(alias: String) {
        val p = prefs
        if (p != null) p.edit().remove(alias).apply() else memoryVault.remove(alias)
    }
}
