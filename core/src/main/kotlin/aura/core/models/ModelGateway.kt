package aura.core.models

import aura.core.Ids

/** A chat message in a neutral, provider-agnostic form. */
data class ModelMessage(
    val role: String,      // system | user | assistant | tool
    val content: String,
    val toolCallId: String? = null
)

/** Capabilities of a provider/model. */
data class ModelSpec(
    val id: String,
    val name: String,
    val contextWindow: Int,
    val maxOutput: Int,
    val vision: Boolean = false,
    val tools: Boolean = false,
    val reasoning: Boolean = false,
    val streaming: Boolean = true,
    val structuredOutput: Boolean = false,
    val costPer1kIn: Double = 0.0,
    val costPer1kOut: Double = 0.0
)

/** A configured model endpoint. */
data class ModelProvider(
    val providerId: String,     // gemini | openai | anthropic | openrouter | local | custom
    val name: String,
    val spec: ModelSpec,
    val enabled: Boolean = true
)

/** User-facing routing decision between available providers. */
data class RoutingDecision(
    val provider: ModelProvider,
    val taskId: String,
    val estimatedTokens: Long = 0
)

/**
 * Unified model gateway contract (spec section 16). Implementations translate to a
 * concrete SDK over the wire; this core keeps only the routing/budget decisions
 * provider-agnostic.
 */
interface ModelGateway {
    suspend fun chat(messages: List<ModelMessage>, providerId: String?): ModelMessage
    suspend fun stream(messages: List<ModelMessage>, providerId: String?): kotlinx.coroutines.flow.Flow<String>
    suspend fun availableProviders(): List<ModelProvider>
    suspend fun health(providerId: String): Boolean
}

/**
 * Deterministic provider selection given a task's token estimate and budget. Falls
 * back through an ordered candidate list, skipping disabled/unavailable providers.
 */
class ModelRouter(private val providers: List<ModelProvider>) {
    fun route(taskId: String, estimatedTokens: Long, prefersReasoning: Boolean): RoutingDecision? {
        val candidates = providers.filter { it.enabled }
            .sortedWith(compareBy<ModelProvider> { if (it.spec.streaming) 0 else 1 }
                .thenBy { it.spec.costPer1kIn })
        if (candidates.isEmpty()) return null
        val chosen = candidates.firstOrNull { it.spec.contextWindow >= estimatedTokens }
            ?: candidates.first()
        return RoutingDecision(chosen, taskId, estimatedTokens)
    }

    fun fallbackAfter(providerId: String): ModelProvider? =
        providers.filter { it.enabled && it.providerId != providerId }
            .minByOrNull { it.spec.costPer1kIn }
}

/** Simple token estimate used to pick context/budget before hitting a provider. */
object TokenEstimator {
    fun estimate(text: String): Long = (text.length / 4).toLong() + 1
}
