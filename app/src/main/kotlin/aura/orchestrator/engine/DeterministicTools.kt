package aura.orchestrator.engine

import java.io.File
import java.util.Locale

/**
 * Deterministic tools that never require an LLM (spec section 4 SIMPLE path).
 * Each is a real implementation — we never claim an operation ran when it did not.
 */
object Calculator {

    /** Pulls the longest run of arithmetic characters out of a phrase. */
    fun extractExpression(phrase: String): String? {
        val allowed = Regex("[0-9.()+\\-*/ ]")
        var best: String? = null
        var cur = StringBuilder()
        fun flush() {
            val s = cur.toString().trim()
            if (s.length >= 3 && s.any { it in "+-*/" } && best == null || (s.length > (best?.length ?: 0) && s.any { it in "+-*/" })) {
                best = s
            }
            cur = StringBuilder()
        }
        for (ch in phrase) {
            if (allowed.matches(ch.toString())) cur.append(ch) else flush()
        }
        flush()
        return best
    }

    fun evaluate(expr: String): Double? {
        val cleaned = expr.filterNot { it == ' ' }
        if (cleaned.isEmpty()) return null
        return try {
            val p = Parser(cleaned)
            val v = p.parseExpr()
            if (p.pos != cleaned.length) null else v
        } catch (t: Exception) {
            null
        }
    }

    private class Parser(val s: String) {
        var pos = 0
        fun parseExpr(): Double {
            var v = parseTerm()
            while (pos < s.length) {
                val c = s[pos]
                if (c == '+') { pos++; v += parseTerm() }
                else if (c == '-') { pos++; v -= parseTerm() }
                else break
            }
            return v
        }
        fun parseTerm(): Double {
            var v = parseFactor()
            while (pos < s.length) {
                val c = s[pos]
                if (c == '*') { pos++; v *= parseFactor() }
                else if (c == '/') { pos++; v /= parseFactor() }
                else break
            }
            return v
        }
        fun parseFactor(): Double {
            if (pos < s.length && s[pos] == '-') { pos++; return -parseFactor() }
            if (pos < s.length && s[pos] == '(') {
                pos++
                val v = parseExpr()
                if (pos < s.length && s[pos] == ')') pos++
                return v
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (start == pos) throw IllegalArgumentException()
            return s.substring(start, pos).toDouble()
        }
    }
}

/** Minimal workspace file tooling rooted in the app's internal files directory. */
object WorkspaceTools {
    fun list(dir: File): List<String> =
        dir.listFiles()?.sortedBy { it.name }?.map { if (it.isDirectory) it.name + "/" else it.name } ?: emptyList()

    fun rename(dir: File, from: String, to: String): Boolean {
        if (from.isBlank() || to.isBlank() || from == to) return false
        val f = File(dir, from)
        val t = File(dir, to)
        return f.exists() && !t.exists() && f.renameTo(t)
    }

    fun count(dir: File): Pair<Int, Long> {
        val files = dir.listFiles() ?: return 0 to 0L
        var size = 0L
        files.forEach { if (it.isFile) size += it.length() }
        return files.count { it.isFile } to size
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        return String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }
}
