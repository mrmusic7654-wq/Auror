package aura.core.memory

import aura.core.Ids
import kotlinx.serialization.Serializable

/**
 * Layered memory (spec section 18). [MemoryStore] is an interface so Room-backed
 * persistent memory and (optionally) a vector store can share one contract. The
 * core ships an in-memory implementation used by unit tests and as a cache.
 */
@Serializable
data class MemoryItem(
    val id: String = Ids.newId("mem"),
    val text: String,
    val importance: Double = 0.5,          // 0..1, user/engine assigned
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val projectId: String? = null,
    val source: String = "user",           // user | agent | task | tool | system
    val layer: String = "project"          // working | task | project | longterm
)

interface MemoryStore {
    suspend fun add(item: MemoryItem)
    suspend fun retrieve(projectId: String?, query: String, limit: Int = 20): List<MemoryItem>
    suspend fun update(item: MemoryItem)
    suspend fun delete(id: String)
    suspend fun all(projectId: String?): List<MemoryItem>
    suspend fun clear(projectId: String?)
}

/** In-memory store with simple keyword + recency + importance ranking. */
class InMemoryMemoryStore : MemoryStore {
    private val items = LinkedHashMap<String, MemoryItem>()

    override suspend fun add(item: MemoryItem) { items[item.id] = item }
    override suspend fun update(item: MemoryItem) { items[item.id] = item }
    override suspend fun delete(id: String) { items.remove(id) }
    override suspend fun clear(projectId: String?) {
        items.entries.removeIf { it.value.projectId == projectId }
    }
    override suspend fun all(projectId: String?): List<MemoryItem> =
        items.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAtEpochMs }

    override suspend fun retrieve(projectId: String?, query: String, limit: Int): List<MemoryItem> {
        val q = query.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        return items.values
            .asSequence()
            .filter { it.projectId == projectId }
            .map { item ->
                val hay = item.text.lowercase()
                val hits = q.count { hay.contains(it) }
                val score = hits * 2.0 + item.importance + item.createdAtEpochMs / 1e12
                item to score
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }
}

/**
 * Ranker that selects the most relevant memories to include in a bounded context.
 */
object MemoryRanker {
    fun rank(items: List<MemoryItem>, query: String, budgetChars: Int): List<MemoryItem> {
        var used = 0
        val chosen = ArrayList<MemoryItem>()
        for (it in items) {
            val cost = it.text.length + 40
            if (used + cost > budgetChars) break
            chosen.add(it); used += cost
        }
        return chosen
    }
}
