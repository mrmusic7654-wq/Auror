package aura.core.taskgraph

import aura.core.TaskStatus

/**
 * A persisted, dependency-aware task graph (spec section 7). Nodes represent
 * steps; edges express "must run before". Every node carries [state] so the graph
 * can be reconstructed after process death and scheduling decisions can be made
 * purely from persisted state.
 */
class TaskGraph {

    data class Node(
        val nodeId: String,
        val label: String,
        val agentId: String? = null,
        val capability: String? = null,
        var state: TaskStatus = TaskStatus.PENDING,
        var result: String? = null,
        var error: String? = null,
        var attempts: Int = 0,
        val maxAttempts: Int = 3
    )

    private val nodes = LinkedHashMap<String, Node>()
    private val dependencies = HashMap<String, MutableSet<String>>() // nodeId -> required nodeIds

    fun add(label: String, agentId: String? = null, capability: String? = null, maxAttempts: Int = 3): Node {
        val node = Node(nodeId = "n" + nodes.size, label = label, agentId = agentId,
            capability = capability, maxAttempts = maxAttempts)
        nodes[node.nodeId] = node
        dependencies[node.nodeId] = mutableSetOf()
        return node
    }

    /** Adds an edge: [from] must complete before [to] may start. */
    fun dependsOn(from: Node, to: Node) {
        dependencies[to.nodeId]?.add(from.nodeId)
    }

    fun allNodes(): List<Node> = nodes.values.toList()
    fun node(id: String): Node? = nodes[id]

    fun requiredBy(id: String): Set<String> = dependencies[id] ?: emptySet()

    /** True when every required dependency of [id] has succeeded. */
    fun dependenciesSatisfied(id: String): Boolean =
        (dependencies[id] ?: emptySet()).all { nodes[it]?.state == TaskStatus.SUCCEEDED }

    /** Returns nodes that are currently runnable (pending + all deps satisfied). */
    fun runnable(): List<Node> =
        nodes.values.filter { it.state == TaskStatus.PENDING && dependenciesSatisfied(it.nodeId) }

    /**
     * Full topological order for planning/visualization. Throws if the graph has a
     * cycle (a configuration error we must never let agents introduce).
     */
    fun topologicalOrder(): List<String> {
        val order = ArrayList<String>()
        val visited = HashMap<String, Int>() // 0 = visiting, 1 = done
        fun visit(id: String) {
            when (visited[id]) {
                1 -> return
                0 -> throw IllegalStateException("Task graph contains a cycle at node $id")
            }
            visited[id] = 0
            nodes[id]?.let { node ->
                for (dep in dependencies[id].orEmpty()) visit(dep)
            }
            visited[id] = 1
            order.add(id)
        }
        for (id in nodes.keys) visit(id)
        return order
    }

    val isCyclic: Boolean
        get() = runCatching { topologicalOrder() }.isFailure

    fun stateOf(id: String): TaskStatus = nodes[id]?.state ?: TaskStatus.BLOCKED

    /** Full persisted snapshot as plain data. */
    fun snapshot(): List<Node> = nodes.values.toList()
}
