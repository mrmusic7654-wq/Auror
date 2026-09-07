package aura.core.orchestrator

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackoffTest {

    @Test
    fun maxDelay_growsExponentiallyAndCaps() {
        assertThat(Backoff.maxDelayMs(0, baseMs = 1000)).isEqualTo(1000)
        assertThat(Backoff.maxDelayMs(1, baseMs = 1000)).isEqualTo(2000)
        assertThat(Backoff.maxDelayMs(2, baseMs = 1000)).isEqualTo(4000)
        // capped
        assertThat(Backoff.maxDelayMs(20, baseMs = 1000, maxMs = 60000)).isEqualTo(60000)
    }

    @Test
    fun jitteredDelayStaysInBounds() {
        var attempt = 0
        repeat(200) {
            val d = Backoff.delayMs(attempt, baseMs = 1000, maxMs = 60000)
            assertThat(d).isAtLeast(0)
            assertThat(d).isLessThan(Backoff.maxDelayMs(attempt, maxMs = 60000) + 1)
            attempt++
        }
    }
}
