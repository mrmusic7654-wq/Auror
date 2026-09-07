package aura.core.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AURA Protocol SDK (versioned).
 *
 * This is the wire contract shared between the orchestrator, companion AURA apps,
 * MCP adapters and remote compute endpoints. Messages are transport-agnostic: the
 * same AuraMessage can travel over Android Binder/AIDL, a local HTTP/WebSocket
 * transport, or a remote WebSocket.
 */
object AuraProtocol {
    const val NAME = "aura"
    const val VERSION = "1.0"
}

/** Standard, stable error codes (spec section 9). */
enum class AuraErrorCode(val code: String) {
    INVALID_REQUEST("INVALID_REQUEST"),
    UNAUTHORIZED("UNAUTHORIZED"),
    FORBIDDEN("FORBIDDEN"),
    NOT_FOUND("NOT_FOUND"),
    TIMEOUT("TIMEOUT"),
    BUSY("BUSY"),
    RATE_LIMITED("RATE_LIMITED"),
    EXECUTION_FAILED("EXECUTION_FAILED"),
    NETWORK_ERROR("NETWORK_ERROR"),
    PLUGIN_ERROR("PLUGIN_ERROR"),
    UNSUPPORTED("UNSUPPORTED"),
    CANCELLED("CANCELLED"),
    RESOURCE_LIMIT("RESOURCE_LIMIT"),
    PERMISSION_DENIED("PERMISSION_DENIED"),
    NOT_CONFIGURED("NOT_CONFIGURED")
}

enum class AuraMessageType { DISCOVERY, REQUEST, RESPONSE, EVENT, PROGRESS, ERROR, CANCEL, HELLO }

@Serializable
data class AuraError(
    val code: String,
    val message: String = "",
    val retryable: Boolean = false,
    val details: Map<String, String> = emptyMap()
) {
    companion object {
        fun of(e: AuraErrorCode, message: String = "", retryable: Boolean = false) =
            AuraError(e.code, message, retryable)
    }
}

/** Identifies a concrete provider/transport/plugin implementing a capability. */
@Serializable
data class AuraCapability(
    val capability: String,
    val appId: String = "",
    val transport: String = "internal",
    val healthy: Boolean = true,
    val version: String = "1.0"
)

@Serializable
data class AuraRequest(
    val protocol: String = AuraProtocol.NAME,
    val version: String = AuraProtocol.VERSION,
    val type: String = "request",
    val id: String,
    val taskId: String = "",
    val plugin: String,
    val method: String,
    val arguments: Map<String, String> = emptyMap()
)

@Serializable
data class AuraResponse(
    val protocol: String = AuraProtocol.NAME,
    val version: String = AuraProtocol.VERSION,
    val type: String = "response",
    val id: String,
    val requestId: String = "",
    val ok: Boolean = true,
    val result: String = "",
    val artifactUri: String? = null,
    val error: AuraError? = null,
    val latencyMs: Long = 0
)

@Serializable
data class AuraEvent(
    val protocol: String = AuraProtocol.NAME,
    val version: String = AuraProtocol.VERSION,
    val type: String = "event",
    val id: String = "",
    val event: String,
    val payload: Map<String, String> = emptyMap()
)

/** Manifest an app/plugin advertises during discovery (spec section 13). */
@Serializable
data class AuraManifest(
    val appId: String,
    val version: String = "1.0.0",
    val protocolVersion: String = AuraProtocol.VERSION,
    val name: String = "",
    val description: String = "",
    val capabilities: List<String> = emptyList()
)

@Serializable
data class AuraSession(
    val sessionId: String,
    val appId: String,
    val capabilities: List<String> = emptyList(),
    val connectedAtEpochMs: Long = 0,
    val transport: String = ""
)

/** Message envelope used by stream-based transports (WebSocket / HTTP). */
@Serializable
data class AuraEnvelope(
    val protocol: String = AuraProtocol.NAME,
    val version: String = AuraProtocol.VERSION,
    val id: String,
    val kind: String,
    val body: String
)

object AuraJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
    val pretty: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
}
