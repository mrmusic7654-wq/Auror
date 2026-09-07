package aura.core.protocol

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class ProtocolTest {

    @Test
    fun requestSerializesToSpecShape() {
        val req = AuraRequest(
            id = "req_123",
            taskId = "task_456",
            plugin = "aura.python",
            method = "python.execute",
            arguments = mapOf("code" to "print(2 + 2)")
        )
        val json = AuraJson.json.encodeToString(AuraRequest.serializer(), req)
        assertThat(json).contains("\"protocol\":\"aura\"")
        assertThat(json).contains("\"version\":\"1.0\"")
        assertThat(json).contains("\"method\":\"python.execute\"")
    }

    @Test
    fun specExampleRoundTrips() {
        val raw = """
        {
          "protocol": "aura",
          "version": "1.0",
          "type": "request",
          "id": "req_123",
          "taskId": "task_456",
          "plugin": "aura.python",
          "method": "python.execute",
          "arguments": { "code": "print(2 + 2)" }
        }
        """.trimIndent()
        val req = Json { ignoreUnknownKeys = true }.decodeFromString(AuraRequest.serializer(), raw)
        assertThat(req.protocol).isEqualTo("aura")
        assertThat(req.plugin).isEqualTo("aura.python")
        assertThat(req.method).isEqualTo("python.execute")
        assertThat(req.arguments["code"]).isEqualTo("print(2 + 2)")
        assertThat(req.taskId).isEqualTo("task_456")
    }

    @Test
    fun standardErrorCodesAreStable() {
        assertThat(AuraErrorCode.PERMISSION_DENIED.code).isEqualTo("PERMISSION_DENIED")
        assertThat(AuraErrorCode.NETWORK_ERROR.code).isEqualTo("NETWORK_ERROR")
        assertThat(AuraError.of(AuraErrorCode.TIMEOUT).code).isEqualTo("TIMEOUT")
    }
}
