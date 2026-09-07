package aura.core.models

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelRouterTest {

    private val providers = listOf(
        ModelProvider("local", "Local", ModelSpec("local", "local", 8000, 2000, streaming = true, costPer1kIn = 0.0)),
        ModelProvider("openai", "GPT", ModelSpec("gpt", "gpt", 128000, 4000, streaming = true, costPer1kIn = 0.002))
    )

    @Test
    fun routesToStreamingProviderWhenFit() {
        val router = ModelRouter(providers)
        val d = router.route("task_1", estimatedTokens = 1000, prefersReasoning = false)
        assertThat(d).isNotNull()
        assertThat(d!!.provider.providerId).isEqualTo("local")
    }

    @Test
    fun fallsBackToLargerContextWhenEstimateTooBig() {
        val router = ModelRouter(providers)
        val d = router.route("task_2", estimatedTokens = 100_000, prefersReasoning = false)
        assertThat(d!!.provider.providerId).isEqualTo("openai")
    }

    @Test
    fun returnsNullWhenNoProviderEnabled() {
        val router = ModelRouter(providers.map { it.copy(enabled = false) })
        assertThat(router.route("t", 100, false)).isNull()
    }
}
