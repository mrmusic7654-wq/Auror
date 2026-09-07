package aura.core.agents

import aura.core.AgentDescriptor
import aura.core.Capabilities

/**
 * Catalogue of built-in agents (spec section 5). Each agent is described by its
 * capabilities and limits — none of them holds unrestricted access. The runtime
 * executes steps through the capability registry + permission engine; an agent is
 * just a bounded executor role, not an autonomous loop.
 */
object AgentIds {
    const val SUPERVISOR = "supervisor"
    const val PLANNER = "planner"
    const val RESEARCH = "research"
    const val BROWSER = "browser"
    const val CODING = "coding"
    const val PYTHON = "python"
    const val DATA = "data"
    const val GIT = "git"
    const val GITHUB = "github"
    const val FILE = "file"
    const val TERMINAL = "terminal"
    const val ANDROID = "android"
    const val PDF = "pdf"
    const val WRITING = "writing"
    const val QA = "qa"
    const val SECURITY = "security"
    const val AUTOMATION = "automation"
}

object BuiltinAgents {
    val all: List<AgentDescriptor> = listOf(
        AgentDescriptor(AgentIds.SUPERVISOR, "Supervisor", "Coordinates decomposition, assignment, dependencies and final verification.",
            capabilities = emptySet()),
        AgentDescriptor(AgentIds.PLANNER, "Planner", "Breaks a complex goal into a task graph.",
            capabilities = setOf(Capabilities.MODEL_CHAT)),
        AgentDescriptor(AgentIds.RESEARCH, "Research", "Collects and validates sources.",
            capabilities = setOf(Capabilities.RESEARCH, Capabilities.BROWSER_SEARCH, Capabilities.MODEL_CHAT)),
        AgentDescriptor(AgentIds.BROWSER, "Browser", "Searches, opens pages and reads web content.",
            capabilities = setOf(Capabilities.BROWSER_SEARCH, Capabilities.BROWSER_OPEN)),
        AgentDescriptor(AgentIds.CODING, "Coding", "Writes and modifies code in the workspace.",
            capabilities = setOf(Capabilities.FILES_READ, Capabilities.FILES_WRITE, Capabilities.GIT_READ, Capabilities.MODEL_CHAT)),
        AgentDescriptor(AgentIds.PYTHON, "Python", "Runs Python scripts and packages via local or remote compute.",
            capabilities = setOf(Capabilities.PYTHON_EXECUTE, Capabilities.PYTHON_PACKAGE)),
        AgentDescriptor(AgentIds.DATA, "Data", "Analyzes datasets and produces statistics/charts.",
            capabilities = setOf(Capabilities.DATA_ANALYZE, Capabilities.PYTHON_EXECUTE, Capabilities.PDF_GENERATE)),
        AgentDescriptor(AgentIds.GIT, "Git", "Inspects repositories and diffs.",
            capabilities = setOf(Capabilities.GIT_READ)),
        AgentDescriptor(AgentIds.GITHUB, "GitHub", "Works with repositories, issues and pull requests.",
            capabilities = setOf(Capabilities.GIT_READ, Capabilities.GIT_WRITE, Capabilities.GITHUB_PUSH)),
        AgentDescriptor(AgentIds.FILE, "File", "Manages workspace files and artifacts.",
            capabilities = setOf(Capabilities.FILES_READ, Capabilities.FILES_WRITE, Capabilities.FILES_LIST)),
        AgentDescriptor(AgentIds.TERMINAL, "Terminal", "Runs approved shell commands locally or on a sandbox.",
            capabilities = setOf(Capabilities.TERMINAL_EXECUTE)),
        AgentDescriptor(AgentIds.ANDROID, "Android", "Performs on-device operations.",
            capabilities = setOf(Capabilities.FILES_READ, Capabilities.DOWNLOAD, Capabilities.MODEL_CHAT)),
        AgentDescriptor(AgentIds.PDF, "PDF", "Generates and reads PDF documents.",
            capabilities = setOf(Capabilities.PDF_GENERATE)),
        AgentDescriptor(AgentIds.WRITING, "Writing", "Drafts and refines prose and reports.",
            capabilities = setOf(Capabilities.FILES_WRITE, Capabilities.MODEL_CHAT)),
        AgentDescriptor(AgentIds.QA, "QA", "Verifies results against requirements.",
            capabilities = setOf(Capabilities.FILES_READ, Capabilities.MODEL_CHAT)),
        AgentDescriptor(AgentIds.SECURITY, "Security", "Audits actions and config before they run.",
            capabilities = setOf(Capabilities.GIT_READ, Capabilities.FILES_READ)),
        AgentDescriptor(AgentIds.AUTOMATION, "Automation", "Runs scheduled triggers and workflows.",
            capabilities = setOf(Capabilities.FILES_WRITE, Capabilities.TERMINAL_EXECUTE))
    )

    val byId: Map<String, AgentDescriptor> = all.associateBy { it.agentId }
}
