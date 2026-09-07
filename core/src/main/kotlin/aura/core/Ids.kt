package aura.core

import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicLong

/**
 * Deterministic id helpers used across AURA. All ids are strings so they are
 * trivially persistable in Room and serializable over the AURA Protocol.
 */
object Ids {
    private val rand = SecureRandom()
    private val counter = AtomicLong(0)

    /** Generates an opaque, unpredictable id with an optional human prefix, e.g. task_ab3x… */
    fun newId(prefix: String = "id"): String {
        val n = counter.incrementAndGet()
        val rnd = java.lang.Integer.toHexString(rand.nextInt())
        return "${prefix}_${n}_${rnd}"
    }
}
