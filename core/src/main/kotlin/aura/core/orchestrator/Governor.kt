package aura.core.orchestrator

/**
 * Per-task resource governor (spec section 42). Enforces hard caps that prevent
 * runaway agent loops and unbounded resource consumption. These values are owned
 * by the orchestrator, never by agent output.
 */
data class ResourceGovernor(
    val maxTimeMs: Long = 20 * 60 * 1000L,
    val maxIterations: Int = 50,
    val maxModelCalls: Int = 200,
    val maxAgents: Int = 8,
    val maxConcurrentAgents: Int = 3,
    val maxSandboxRuntimeMs: Long = 10 * 60 * 1000L,
    val maxFileSizeBytes: Long = 200 * 1024 * 1024L,
    val maxDownloadBytes: Long = 512 * 1024 * 1024L
)

/**
 * Idempotency helper: an operation key records whether a piece of work already
 * completed so it can be resumed without duplication after process death.
 */
data class IdempotencyRecord(
    val key: String,
    val completed: Boolean = false,
    val resultDigest: String? = null
)
