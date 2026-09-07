package aura.core

/**
 * Core domain vocabulary shared by the whole system. Kept free of Android
 * dependencies so it is unit-testable on the JVM.
 */

/** Execution complexity tiers selected by the [ComplexityRouter]. */
enum class Complexity { SIMPLE, MEDIUM, COMPLEX }

/** Lifecycle states of a persisted task / task-graph node. */
enum class TaskStatus {
    PENDING,
    QUEUED,
    RUNNING,
    WAITING,
    RETRYING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    BLOCKED
}

/** Sensitivity levels used by the permission engine for capability classification. */
enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

/** Outcome of a permission evaluation. */
enum class PermissionDecision { ALLOW, DENY, ASK_USER }

/**
 * Canonical capability identifiers understood by the orchestrator. These are the
 * capability *classes* the orchestrator can reason about; concrete providers that
 * implement them are discovered dynamically and never hardcoded into the UI.
 */
object Capabilities {
    const val FILES_READ = "files.read"
    const val FILES_WRITE = "files.write"
    const val FILES_DELETE = "files.delete"
    const val FILES_LIST = "files.list"
    const val DOWNLOAD = "downloads.create"
    const val DOWNLOAD_MANAGE = "downloads.manage"
    const val BROWSER_SEARCH = "browser.search"
    const val BROWSER_OPEN = "browser.open"
    const val BROWSER_DOWNLOAD = "browser.download"
    const val TERMINAL_EXECUTE = "terminal.execute"
    const val PYTHON_EXECUTE = "python.execute"
    const val PYTHON_PACKAGE = "python.installPackage"
    const val RESEARCH = "research.query"
    const val PDF_GENERATE = "pdf.generate"
    const val GIT_READ = "git.read"
    const val GIT_WRITE = "git.write"
    const val GITHUB_PUSH = "github.push"
    const val ACCOUNT_ACCESS = "account.access"
    const val MCP_TOOL = "mcp.tool"
    const val MODEL_CHAT = "model.chat"
    const val DATA_ANALYZE = "data.analyze"
    const val NOTE = "memory.note"
    const val MEMORY_NOTE = "memory.note"
    const val MEMORY_READ = "memory.read"
    const val MEMORY_DELETE = "memory.delete"
    const val MINIAPP_INVOKE = "miniapp.invoke"
}

/**
 * Static classification of a capability's risk. The permission engine consults
 * this when deciding whether an operation may proceed without asking the user.
 */
object PermissionCatalog {
    val sensitivity: Map<String, RiskLevel> = mapOf<String, RiskLevel>(
        Capabilities.FILES_READ to RiskLevel.LOW,
        Capabilities.FILES_LIST to RiskLevel.LOW,
        Capabilities.FILES_WRITE to RiskLevel.MEDIUM,
        Capabilities.FILES_DELETE to RiskLevel.HIGH,
        Capabilities.BROWSER_OPEN to RiskLevel.LOW,
        Capabilities.BROWSER_SEARCH to RiskLevel.LOW,
        Capabilities.BROWSER_DOWNLOAD to RiskLevel.MEDIUM,
        Capabilities.DOWNLOAD to RiskLevel.MEDIUM,
        Capabilities.DOWNLOAD_MANAGE to RiskLevel.MEDIUM,
        Capabilities.MODEL_CHAT to RiskLevel.LOW,
        Capabilities.MEMORY_READ to RiskLevel.MEDIUM,
        Capabilities.MEMORY_NOTE to RiskLevel.MEDIUM,
        Capabilities.MEMORY_DELETE to RiskLevel.HIGH,
        Capabilities.NOTE to RiskLevel.LOW,
        Capabilities.RESEARCH to RiskLevel.LOW,
        Capabilities.GIT_READ to RiskLevel.LOW,
        Capabilities.GIT_WRITE to RiskLevel.HIGH,
        Capabilities.GITHUB_PUSH to RiskLevel.HIGH,
        Capabilities.TERMINAL_EXECUTE to RiskLevel.HIGH,
        Capabilities.PYTHON_EXECUTE to RiskLevel.HIGH,
        Capabilities.PYTHON_PACKAGE to RiskLevel.HIGH,
        Capabilities.PDF_GENERATE to RiskLevel.LOW,
        Capabilities.ACCOUNT_ACCESS to RiskLevel.CRITICAL,
        Capabilities.MCP_TOOL to RiskLevel.MEDIUM,
        Capabilities.DATA_ANALYZE to RiskLevel.LOW,
        Capabilities.MINIAPP_INVOKE to RiskLevel.MEDIUM
    )

    fun riskOf(capability: String): RiskLevel = sensitivity[capability] ?: RiskLevel.MEDIUM
}

/** A bound, persistent "task" that the orchestrator schedules and supervises. */
data class TaskSummary(
    val taskId: String = Ids.newId("task"),
    val title: String,
    val complexity: Complexity,
    val requiredCapabilities: Set<String> = emptySet(),
    val status: TaskStatus = TaskStatus.PENDING,
    val projectId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

/** Descriptor for a built-in or discovered agent. */
data class AgentDescriptor(
    val agentId: String,
    val name: String,
    val description: String = "",
    val capabilities: Set<String> = emptySet(),
    val enabled: Boolean = true,
    val maxIterations: Int = 20,
    val maxTokens: Long = 32_000,
    val timeBudgetMs: Long = 30 * 60 * 1000L
)
