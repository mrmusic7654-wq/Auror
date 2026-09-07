package aura.core.memory

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MemoryStoreTest {

    @Test
    fun addAndRetrieveByProjectScope() = runBlocking {
        val store = InMemoryMemoryStore()
        store.add(MemoryItem(text = "python notes about pandas", projectId = "p1"))
        store.add(MemoryItem(text = "kotlin for android", projectId = "p1"))
        store.add(MemoryItem(text = "other project notes", projectId = "p2"))

        val p1 = store.all("p1")
        assertThat(p1).hasSize(2)

        val hits = store.retrieve("p1", "python", limit = 5)
        assertThat(hits.map { it.text }).contains("python notes about pandas")
        assertThat(hits.map { it.text }).doesNotContain("other project notes")
    }

    @Test
    fun deleteAndClear() = runBlocking {
        val store = InMemoryMemoryStore()
        val m = MemoryItem(text = "to delete", projectId = "p1")
        store.add(m)
        store.delete(m.id)
        assertThat(store.all("p1")).isEmpty()

        store.add(MemoryItem(text = "a", projectId = "p1"))
        store.add(MemoryItem(text = "b", projectId = "p1"))
        store.clear("p1")
        assertThat(store.all("p1")).isEmpty()
    }

    @Test
    fun ranker_respectsBudget() = runBlocking {
        val items = listOf(
            MemoryItem(text = "x".repeat(200), importance = 1.0),
            MemoryItem(text = "y".repeat(200), importance = 0.5)
        )
        val chosen = MemoryRanker.rank(items, "", 260)
        assertThat(chosen).hasSize(1)
    }
}
