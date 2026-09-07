package aura.core.permissions

import aura.core.PermissionCatalog
import aura.core.PermissionDecision
import aura.core.RiskLevel

/**
 * Pure permission decision logic (spec section 14). A request is evaluated against:
 *  1. the user's policy preference for its risk level,
 *  2. any previously granted allow-once / allow-for-task / allow-for-project grants.
 *
 * The AI can never modify these rules: they live here (pure) and in persisted user
 * policy, not in model context.
 */
data class PermissionPolicy(
    val defaultForRisk: Map<RiskLevel, PermissionDecision> = mapOf(
        RiskLevel.LOW to PermissionDecision.ALLOW,
        RiskLevel.MEDIUM to PermissionDecision.ASK_USER,
        RiskLevel.HIGH to PermissionDecision.ASK_USER,
        RiskLevel.CRITICAL to PermissionDecision.ASK_USER
    )
) {
    fun decisionFor(risk: RiskLevel): PermissionDecision =
        defaultForRisk[risk] ?: PermissionDecision.ASK_USER
}

sealed interface Grant {
    val capability: String
    data class Once(override val capability: String, val scopeId: String? = null) : Grant
    data class ForTask(override val capability: String, val scopeId: String) : Grant
    data class ForProject(override val capability: String, val scopeId: String) : Grant
    data class Always(override val capability: String, val scopeId: String? = null) : Grant
}

class PermissionEngine(private val policy: PermissionPolicy) {

    private val onceGrants = mutableSetOf<String>()
    private val taskGrants = mutableSetOf<String>()
    private val projectGrants = mutableSetOf<String>()
    private val alwaysGrants = mutableSetOf<String>()

    /**
     * @param taskId    present when the operation belongs to an orchestrator task.
     * @param projectId present when the operation belongs to a project.
     * @param allowConsumeOnce when true and an allow-once grant applies, consume it.
     */
    fun evaluate(
        capability: String,
        taskId: String? = null,
        projectId: String? = null,
        allowConsumeOnce: Boolean = true
    ): PermissionDecision {
        val risk = PermissionCatalog.riskOf(capability)
        // Explicit blanket grants take precedence over the policy default.
        if (alwaysGrants.contains(capability)) return PermissionDecision.ALLOW
        if (projectId != null && "$capability|$projectId" in projectGrants) return PermissionDecision.ALLOW
        if (taskId != null && "$capability|$taskId" in taskGrants) return PermissionDecision.ALLOW
        val onceKey = if (taskId != null) "$capability|$taskId" else capability
        if (onceKey in onceGrants) {
            if (allowConsumeOnce) onceGrants.remove(onceKey)
            return PermissionDecision.ALLOW
        }
        return policy.decisionFor(risk)
    }

    fun grant(g: Grant) {
        when (g) {
            is Grant.Once -> onceGrants.add(if (g.scopeId != null) "${g.capability}|${g.scopeId}" else g.capability)
            is Grant.ForTask -> taskGrants.add("${g.capability}|${g.scopeId}")
            is Grant.ForProject -> projectGrants.add("${g.capability}|${g.scopeId}")
            is Grant.Always -> alwaysGrants.add(g.capability)
        }
    }

    fun revokeAllForTask(taskId: String) {
        taskGrants.removeIf { it.endsWith("|$taskId") }
        onceGrants.removeIf { it.endsWith("|$taskId") }
    }

    fun clearAlways() = alwaysGrants.clear()

    fun summarize(): Map<RiskLevel, PermissionDecision> = policy.defaultForRisk
}
