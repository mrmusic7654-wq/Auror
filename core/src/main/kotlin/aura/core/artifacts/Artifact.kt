package aura.core.artifacts

import kotlinx.serialization.Serializable

/**
 * Metadata for a workspace artifact (spec section 19). Files themselves are never
 * transferred through JSON; only this descriptor plus a [uri] (e.g. an
 * aura:// or content:// or file:// reference) travels.
 */
@Serializable
data class Artifact(
    val artifactId: String,
    val name: String,
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0,
    val uri: String = "",
    val checksumSha256: String? = null,
    val projectId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val source: String = "tool"
)

object ArtifactChecksum {
    private fun hex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }

    fun sha256(bytes: ByteArray): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return hex(md.digest(bytes))
    }
}
