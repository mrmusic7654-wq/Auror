package aura.core.orchestrator

import kotlin.math.min
import kotlin.random.Random

/**
 * Exponential backoff with jitter (spec section 41). Pure so it is easy to test the
 * bounded, jittered range.
 */
object Backoff {
    fun delayMs(attempt: Int, baseMs: Long = 1_000, maxMs: Long = 60_000, random: Random = Random.Default): Long {
        val exp = baseMs shl min(attempt, 6).toInt()
        val cap = min(exp, maxMs)
        // full jitter between 0 and cap
        return if (cap <= 1) 1 else random.nextLong(0, cap + 1)
    }

    fun maxDelayMs(attempt: Int, baseMs: Long = 1_000, maxMs: Long = 60_000): Long {
        val exp = baseMs shl min(attempt, 6).toInt()
        return min(exp, maxMs)
    }
}
