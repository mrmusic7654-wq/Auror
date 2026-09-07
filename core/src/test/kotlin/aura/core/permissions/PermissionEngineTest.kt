package aura.core.permissions

import aura.core.Capabilities
import aura.core.PermissionDecision
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionEngineTest {

    private fun engine() = PermissionEngine(PermissionPolicy())

    @Test
    fun lowRisk_isAllowed() {
        assertThat(engine().evaluate(Capabilities.FILES_LIST))
            .isEqualTo(PermissionDecision.ALLOW)
    }

    @Test
    fun highRisk_asksUserByDefault() {
        assertThat(engine().evaluate(Capabilities.TERMINAL_EXECUTE))
            .isEqualTo(PermissionDecision.ASK_USER)
    }

    @Test
    fun alwaysGrant_allowsWithoutAsk() {
        val e = engine()
        e.grant(Grant.Always(Capabilities.GITHUB_PUSH))
        assertThat(e.evaluate(Capabilities.GITHUB_PUSH)).isEqualTo(PermissionDecision.ALLOW)
    }

    @Test
    fun onceGrant_isConsumedAfterOneUse() {
        val e = engine()
        val task = "task_1"
        e.grant(Grant.ForTask(Capabilities.TERMINAL_EXECUTE, task))
        assertThat(e.evaluate(Capabilities.TERMINAL_EXECUTE, taskId = task))
            .isEqualTo(PermissionDecision.ALLOW)
    }

    @Test
    fun unknownCapability_defaultsToMediumAsk() {
        assertThat(engine().evaluate("unknown.tool")).isEqualTo(PermissionDecision.ASK_USER)
    }

    @Test
    fun revokeClearsTaskGrants() {
        val e = engine()
        val task = "task_x"
        e.grant(Grant.ForTask(Capabilities.FILES_WRITE, task))
        e.revokeAllForTask(task)
        assertThat(e.evaluate(Capabilities.FILES_WRITE, taskId = task))
            .isEqualTo(PermissionDecision.ASK_USER)
    }
}
