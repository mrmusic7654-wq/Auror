package aura.core.orchestrator

import aura.core.Complexity

/**
 * Intent + complexity classification. Deterministic heuristics so simple requests
 * do not require an LLM round trip (spec section 4). These are intentionally
 * transparent and easily testable.
 */
object IntentRouter {

    /** Deterministic tool intents that never need an LLM call. */
    enum class SimpleIntent { CALCULATE, OPEN_APP, RENAME_FILE, LIST_FILES, CONVERT_UNITS, DOWNLOAD, NOTE }

    data class Classification(
        val complexity: Complexity,
        val simpleIntent: SimpleIntent? = null,
        val keywords: List<String> = emptyList(),
        val agentHint: String? = null
    )

    private val simplePhrases: List<Pair<Regex, SimpleIntent>> = listOf(
        Regex("\\b(calculate|compute|what is \\d+|\\d+[+\\-*/]\\d+)\\b", RegexOption.IGNORE_CASE) to SimpleIntent.CALCULATE,
        Regex("\\b(list|show|find) files\\b", RegexOption.IGNORE_CASE) to SimpleIntent.LIST_FILES,
        Regex("\\b(rename|rename file)\\b", RegexOption.IGNORE_CASE) to SimpleIntent.RENAME_FILE,
        Regex("\\b(convert .* to .*|units)\\b", RegexOption.IGNORE_CASE) to SimpleIntent.CONVERT_UNITS,
        Regex("\\b(download )", RegexOption.IGNORE_CASE) to SimpleIntent.DOWNLOAD
    )

    private val mediumMarkers = listOf(
        "summar", "summari", "analyze csv", "research", "explain", "write code", "refactor",
        "review code", "translate"
    )
    private val complexMarkers = listOf(
        "build ", "create an app", "build an app", "application", "extensive research",
        "analyze dataset", "generate a report", "report", "debug", "automation", "project",
        "deploy", "multi-step", "pdf"
    )

    fun classify(raw: String): Classification {
        val text = raw.trim()
        if (text.isEmpty()) return Classification(Complexity.SIMPLE)

        val keywords = text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

        for ((re, intent) in simplePhrases) {
            if (re.containsMatchIn(text)) {
                return Classification(Complexity.SIMPLE, intent, keywords)
            }
        }

        if (keywords.size <= 12) {
            val lower = text.lowercase()
            val medium = mediumMarkers.count { lower.contains(it) }
            val complex = complexMarkers.count { lower.contains(it) }
            return when {
                complex >= 1 -> Classification(Complexity.COMPLEX, null, keywords)
                medium >= 1 -> Classification(Complexity.MEDIUM, null, keywords)
                else -> Classification(Complexity.SIMPLE, null, keywords)
            }
        }
        return Classification(Complexity.COMPLEX, null, keywords)
    }
}

/** Chooses which execution path a classification maps to. */
object ComplexityRouter {
    fun pathFor(c: Complexity): ExecutionPath = when (c) {
        Complexity.SIMPLE -> ExecutionPath.DETERMINISTIC
        Complexity.MEDIUM -> ExecutionPath.SINGLE_AGENT
        Complexity.COMPLEX -> ExecutionPath.MULTI_AGENT
    }

    enum class ExecutionPath { DETERMINISTIC, SINGLE_AGENT, MULTI_AGENT }
}
