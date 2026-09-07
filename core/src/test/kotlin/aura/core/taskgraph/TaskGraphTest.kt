package aura.core.taskgraph

import aura.core.TaskStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TaskGraphTest {

    @Test
    fun topologicalOrder_respectsDependencies() {
        val g = TaskGraph()
        val a = g.add("download")
        val b = g.add("validate")
        val c = g.add("analyze")
        g.dependsOn(a, b)
        g.dependsOn(b, c)

        val order = g.topologicalOrder()
        assertThat(order.indexOf(a.nodeId)).isLessThan(order.indexOf(b.nodeId))
        assertThat(order.indexOf(b.nodeId)).isLessThan(order.indexOf(c.nodeId))
    }

    @Test
    fun runnable_returnsOnlySatisfiedPendingNodes() {
        val g = TaskGraph()
        val a = g.add("a")
        val b = g.add("b")
        g.dependsOn(a, b)
        assertThat(g.runnable().map { it.nodeId }).containsExactly(a.nodeId)
        a.state = TaskStatus.SUCCEEDED
        assertThat(g.runnable().map { it.nodeId }).containsExactly(b.nodeId)
    }

    @Test
    fun cycle_isDetected() {
        val g = TaskGraph()
        val a = g.add("a")
        val b = g.add("b")
        g.dependsOn(a, b)
        g.dependsOn(b, a)
        assertThat(g.isCyclic).isTrue()
    }

    @Test
    fun parallelBranches_bothProceedAfterCommonDep() {
        val g = TaskGraph()
        val dl = g.add("download")
        val chart = g.add("charts")
        val stats = g.add("statistics")
        val report = g.add("report")
        g.dependsOn(dl, chart)
        g.dependsOn(dl, stats)
        g.dependsOn(chart, report)
        g.dependsOn(stats, report)

        dl.state = TaskStatus.SUCCEEDED
        assertThat(g.runnable().map { it.nodeId }).containsExactly(chart.nodeId, stats.nodeId)
    }
}
