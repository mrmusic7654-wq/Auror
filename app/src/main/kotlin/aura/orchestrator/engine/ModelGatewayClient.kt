package aura.orchestrator.engine

import aura.core.models.ModelMessage
import aura.orchestrator.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSource
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow

/**
 * OpenAI-compatible model gateway (covers OpenAI, OpenRouter, Ollama, LM Studio and
 * many local servers). The API key is fetched from the encrypted Credential Broker
 * at request time and is never placed into logs or model context.
 */
@Singleton
class ModelGatewayClient @Inject constructor(
    private val client: OkHttpClient,
    private val settings: SettingsRepository
) {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val bearer = "Authorization"

    private fun buildBody(messages: List<ModelMessage>, stream: Boolean, model: String): String {
        val sb = StringBuilder()
        sb.append("{\"model\":\"").append(escape(model)).append("\",\"stream\":").append(stream).append(",\"messages\":[")
        messages.forEachIndexed { i, m ->
            if (i > 0) sb.append(',')
            sb.append("{\"role\":\"").append(m.role).append("\",\"content\":\"").append(escape(m.content)).append("\"}")
        }
        sb.append("]}")
        return sb.toString()
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")

    suspend fun complete(messages: List<ModelMessage>): ModelMessage = withContext(Dispatchers.IO) {
        val cfg = settings.modelConfigValue()
        if (!cfg.configured) {
            throw IllegalStateException("AI model is not configured. Open Settings -> AI Models.")
        }
        val key = cfg.keyAlias?.let { settings.apiKey(it) }
        val req = Request.Builder()
            .url("${cfg.baseUrl}/chat/completions")
            .addHeader(bearer, "Bearer ${key ?: ""}")
            .post(buildBody(messages, stream = false, model = cfg.modelName).toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                throw IOException("Model error ${resp.code}: ${body.take(300)}")
            }
            val text = resp.body?.string().orEmpty()
            val content = parseContent(text)
            ModelMessage(role = "assistant", content = content)
        }
    }

    /** Server-Sent-Events style streaming of the assistant reply. */
    fun stream(messages: List<ModelMessage>): Flow<String> = callbackFlow {
        val cfg = settings.modelConfigValueBlocking()
        if (!cfg.configured) {
            close(IllegalStateException("AI model is not configured."))
            return@callbackFlow
        }
        val key = cfg.keyAlias?.let { settings.apiKey(it) }
        val req = Request.Builder()
            .url("${cfg.baseUrl}/chat/completions")
            .addHeader(bearer, "Bearer ${key ?: ""}")
            .post(buildBody(messages, stream = true, model = cfg.modelName).toRequestBody(jsonMedia))
            .build()
        val call: Call = client.newCall(req)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { close(e) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        if (!it.isSuccessful) {
                            close(IOException("Model error ${it.code}"))
                            return
                        }
                        val body: BufferedSource = it.body!!.source()
                        while (true) {
                            val line = body.readUtf8Line() ?: break
                            if (line.isBlank()) continue
                            val data = parseSseData(line) ?: continue
                            if (data == "[DONE]") break
                            val delta = parseDelta(data)
                            if (delta.isNotEmpty()) trySend(delta)
                        }
                        close()
                    }
                } catch (e: Exception) {
                    close(e)
                }
            }
        })
        awaitClose { call.cancel() }
    }

    private fun parseContent(json: String): String {
        // Non-streamed: { "choices": [ { "message": { "content": "..." } } ] }
        val msgIdx = json.indexOf("\"message\"")
        val ctIdx = json.indexOf("\"content\"", msgIdx)
        if (msgIdx < 0 || ctIdx < 0) return json
        return parseStringValue(json, ctIdx)
    }

    private fun parseSseData(line: String): String? {
        if (!line.startsWith("data:")) return null
        return line.substring(5).trim()
    }

    private fun parseDelta(json: String): String {
        val idx = json.indexOf("\"content\":\"")
        if (idx < 0) return ""
        return parseStringValue(json, idx)
    }

    private fun parseStringValue(json: String, afterIdx: Int): String {
        val start = json.indexOf('"', afterIdx) // open quote (after colon)
        val open = json.indexOf('"', start + 1)
        val sb = StringBuilder()
        var i = open + 1
        while (i < json.length) {
            val c = json[i]
            if (c == '\\' && i + 1 < json.length) {
                val n = json[i + 1]
                when (n) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    else -> sb.append(n)
                }
                i += 2
            } else if (c == '"') {
                return sb.toString()
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }
}
