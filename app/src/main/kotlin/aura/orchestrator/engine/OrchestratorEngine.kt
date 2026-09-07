package aura.orchestrator.engine

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import aura.core.Ids
import aura.core.TaskStatus
import aura.core.models.ModelMessage
import aura.core.orchestrator.Backoff
import aura.core.orchestrator.IntentRouter
import aura.core.permissions.PermissionEngine
import aura.orchestrator.data.AppDatabase
import aura.orchestrator.data.ExecutionLogEntity
import aura.orchestrator.data.MessageEntity
import aura.orchestrator.data.SettingsRepository
import aura.orchestrator.data.TaskEntity
import aura.orchestrator.data.ProjectEntity
import aura.orchestrator.data.MemoryEntity
import aura.orchestrator.worker.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * AuraOrchestrator core (spec section 3). Routes a request through classification,
 * capability resolution and execution, persisting every state transition to Room so
 * nothing depends on RAM-resident state. Deterministic SIMPLE work runs offline;
 * agent/LLM work runs only when a model endpoint is configured and honestly reports
 * when it is not.
 */
@Singleton
class OrchestratorEngine @Inject constructor(
    private val database: AppDatabase,
    private val settings: SettingsRepository,
    private val gateway: ModelGatewayClient,
    private val capabilityRegistry: CapabilityRegistry,
    private val permissions: PermissionEngine,
    private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context
) {
    private val taskDao = database.taskDao()
    private val logDao = database.executionLogDao()
    private val messageDao = database.messageDao()
    private val projectDao = database.projectDao()
    private val memoryDao = database.memoryDao()
    private val concurrencyGuard = AtomicInteger(0)

    fun workspaceDir(): File {
        val dir = File(context.filesDir, "workspace")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun log(taskId: String?, agentId: String?, pluginId: String?, level: String, msg: String) {
        appScope.launch(Dispatchers.IO) {
            logDao.insert(
                ExecutionLogEntity(
                    id = Ids.newId("log"), taskId = taskId, agentId = agentId,
                    pluginId = pluginId, level = level, message = msg
                )
            )
        }
    }

    private suspend fun updateTask(t: TaskEntity, status: String? = null, detail: String? = null,
                                   progress: Int? = null) {
        val merged = t.copy(
            status = status ?: t.status,
            detail = detail ?: t.detail,
            progress = progress ?: t.progress,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        taskDao.upsert(merged)
    }

    // ---- Public observation API -------------------------------------------------

    fun observeActiveTasks(): Flow<List<TaskEntity>> =
        taskDao.observeInStatuses(listOf("PENDING", "QUEUED", "RUNNING", "WAITING", "RETRYING"))

    fun observeAllTasks(): Flow<List<TaskEntity>> = taskDao.observeAll()
    fun observeTask(id: String): Flow<TaskEntity?> = taskDao.observeById(id)
    fun observeLogs(taskId: String): Flow<List<ExecutionLogEntity>> = logDao.observeForTask(taskId)
    suspend fun workspaceFiles(): List<String> = withContext(Dispatchers.IO) { WorkspaceTools.list(workspaceDir()) }

    // ---- Task lifecycle ---------------------------------------------------------

    /** Creates a task and dispatches execution on the application scope. */
    fun submit(input: String, projectId: String? = null, conversationId: String? = null): String {
        val taskId = Ids.newId("task")
        appScope.launch(Dispatchers.IO) { runTask(taskId, input.trim(), projectId, conversationId) }
        return taskId
    }

    private suspend fun runTask(taskId: String, input: String, projectId: String?, conversationId: String?) {
        val classification = IntentRouter.classify(input)
        val initial = TaskEntity(
            id = taskId,
            title = input.take(80),
            complexity = classification.complexity.name,
            status = TaskStatus.PENDING.name,
            projectId = projectId,
            source = input,
            detail = "Routing request…",
            progress = 5
        )
        taskDao.upsert(initial)
        log(taskId, null, "aura.orchestrator", "info",
            "Task created · complexity=${classification.complexity} · path=${classification.simpleIntent ?: "model/agent"}")

        try {
            when (classification.simpleIntent) {
                IntentRouter.SimpleIntent.CALCULATE -> runCalculate(taskId, input)
                IntentRouter.SimpleIntent.LIST_FILES -> runListFiles(taskId)
                IntentRouter.SimpleIntent.NOTE -> runNote(taskId, input, projectId)
                IntentRouter.SimpleIntent.DOWNLOAD -> runDownload(taskId, input)
                else -> runModelPath(taskId, input, conversationId, forceChat = true)
            }
        } catch (e: Throwable) {
            val t = taskDao.byId(taskId) ?: return
            log(taskId, null, "aura.orchestrator", "error", "Execution failed: ${e.message}")
            updateTask(t, status = TaskStatus.FAILED.name, detail = "Execution failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private suspend fun setRunning(t: TaskEntity, detail: String) =
        updateTask(t, status = TaskStatus.RUNNING.name, detail = detail, progress = 15)

    private suspend fun setSuccess(t: TaskEntity, detail: String, progress: Int = 100) {
        updateTask(t, status = TaskStatus.SUCCEEDED.name, detail = detail, progress = progress)
        log(t.id, null, "aura.orchestrator", "info", "Task succeeded")
    }

    private suspend fun setFailed(t: TaskEntity, detail: String) {
        updateTask(t, status = TaskStatus.FAILED.name, detail = detail)
        log(t.id, null, "aura.orchestrator", "error", detail)
    }

    private suspend fun runCalculate(taskId: String, input: String) {
        val t = taskDao.byId(taskId) ?: return
        setRunning(t, "Computing…")
        val expr = Calculator.extractExpression(input)
        val value = expr?.let { Calculator.evaluate(it) }
        if (value == null) {
            setFailed(t, "I couldn't parse a valid arithmetic expression from that request.")
        } else {
            val pretty = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
            val detail = "${expr?.trim()} = $pretty"
            setSuccess(t, detail)
        }
    }

    private suspend fun runListFiles(taskId: String) {
        val t = taskDao.byId(taskId) ?: return
        setRunning(t, "Scanning workspace…")
        val (fileCount, size) = WorkspaceTools.count(workspaceDir())
        val files = WorkspaceTools.list(workspaceDir())
        val detail = buildString {
            append("Workspace · ").append(fileCount).append(" files · ").append(WorkspaceTools.formatBytes(size)).append("\n")
            if (files.isEmpty()) append("(empty workspace)")
            else files.forEach { append("• ").append(it).append("\n") }
        }
        setSuccess(t, detail.trimEnd())
    }

    private suspend fun runNote(taskId: String, input: String, projectId: String?) {
        val t = taskDao.byId(taskId) ?: return
        setRunning(t, "Saving to memory…")
        val text = input.removePrefix("note").trim()
        val saved = if (text.isNotBlank()) {
            memoryDao.upsert(MemoryEntity(id = Ids.newId("mem"), text = text, projectId = projectId, source = "user"))
            text
        } else null
        if (saved == null) setFailed(t, "Nothing to save. Try: note <your reminder>")
        else setSuccess(t, "Saved to memory: “${saved.take(120)}”")
    }

    private suspend fun runDownload(taskId: String, input: String) {
        val t = taskDao.byId(taskId) ?: return
        setRunning(t, "Queuing download…")
        val url = Regex("https?://\\S+").find(input)?.value?.trimEnd(')', ',', '.')
        if (url == null) {
            setFailed(t, "No http(s) URL found in that request. Try: download https://example.com/file.zip")
            return
        }
        val data = Data.Builder()
            .putString(DownloadWorker.KEY_URL, url)
            .putString(DownloadWorker.KEY_TASK_ID, taskId)
            .putString(DownloadWorker.KEY_DEST, workspaceDir().absolutePath)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("download_$taskId", ExistingWorkPolicy.REPLACE, request)
        updateTask(t, status = TaskStatus.RUNNING.name, detail = "Downloading $url…", progress = 20)
        log(taskId, null, "aura.downloads", "info", "Enqueued download worker")
    }

    private suspend fun runModelPath(taskId: String, input: String, conversationId: String?, forceChat: Boolean) {
        val t = taskDao.byId(taskId) ?: return
        val cfg = settings.modelConfigValue()
        if (!cfg.configured) {
            setFailed(
                t,
                "This request needs an AI model, but none is configured yet.\n" +
                    "Open Settings → AI Models and connect an OpenAI-compatible endpoint " +
                    "(OpenAI, OpenRouter, Ollama, LM Studio…). Offline actions you can run right now:\n" +
                    "• “calculate 6 * 7”\n• “list files”\n• “download <url>”\n• “note <text>”"
            )
            return
        }

        updateTask(t, status = TaskStatus.RUNNING.name, detail = "Reasoning with ${cfg.providerId}/${cfg.modelName}…", progress = 30)
        val system = "You are AURA Orchestrator, an AI reasoning and orchestration engine inside the AURA workspace. " +
            "Answer the user's request helpfully and concisely. If the request needs tools that are not installed " +
            "(Python, web browser, PDF generation, GitHub push, remote sandbox), say so clearly and provide the best " +
            "offline answer or an actionable plan instead. Never claim an action was executed unless it truly was."

        try {
            val response = gateway.complete(listOf(ModelMessage("system", system), ModelMessage("user", input)))
            if (conversationId != null) {
                messageDao.insert(
                    MessageEntity(id = Ids.newId("msg"), conversationId = conversationId, role = "user", content = input)
                )
                messageDao.insert(
                    MessageEntity(id = Ids.newId("msg"), conversationId = conversationId, role = "assistant", content = response.content)
                )
            }
            setSuccess(t, response.content)
        } catch (e: Throwable) {
            log(taskId, null, "aura.model", "error", "Model call failed: ${e.message}")
            setFailed(t, "Model call failed: ${e.message ?: e.javaClass.simpleName}. Check Settings → AI Models and try again.")
        }
    }

    fun retry(taskId: String) {
        appScope.launch(Dispatchers.IO) {
            val t = taskDao.byId(taskId) ?: return@launch
            if (t.source.isBlank()) {
                updateTask(t, status = TaskStatus.FAILED.name, detail = "Cannot retry — original request was not stored.")
                return@launch
            }
            taskDao.upsert(t.copy(status = TaskStatus.CANCELLED.name, updatedAtEpochMs = System.currentTimeMillis()))
            runTask(Ids.newId("task"), t.source, t.projectId, conversationId = null)
        }
    }

    fun cancel(taskId: String) {
        appScope.launch(Dispatchers.IO) {
            val t = taskDao.byId(taskId) ?: return@launch
            updateTask(t, status = TaskStatus.CANCELLED.name, detail = "Cancelled by user.")
            log(taskId, null, "aura.orchestrator", "info", "Cancelled by user")
        }
    }

    fun deleteTask(taskId: String) {
        appScope.launch(Dispatchers.IO) { taskDao.delete(taskId) }
    }

    fun createProjectAsync(name: String) {
        appScope.launch(Dispatchers.IO) { createProject(name) }
    }

    fun observeProjects(): Flow<List<ProjectEntity>> = projectDao.observeAll()

    fun observeMemories(projectId: String?): Flow<List<MemoryEntity>> = memoryDao.observeAll(projectId)

    fun addMemory(text: String, projectId: String? = null) {
        appScope.launch(Dispatchers.IO) {
            memoryDao.upsert(MemoryEntity(id = Ids.newId("mem"), text = text, projectId = projectId, source = "user"))
        }
    }

    // ---- Projects --------------------------------------------------------------

    suspend fun createProject(name: String): ProjectEntity {
        val p = ProjectEntity(id = Ids.newId("project"), name = name.trim().ifBlank { "Project" })
        projectDao.insert(p)
        return p
    }

    // ---- Recovery (spec section 8) ----------------------------------------------

    suspend fun recoverInterrupted(): RecoveryReport {
        val interrupted = listOf("RUNNING", "QUEUED", "PENDING", "WAITING", "RETRYING")
        val stale = taskDao.listInStatuses(interrupted)
        var reset = 0
        stale.forEach { task ->
            val updated = task.copy(
                status = TaskStatus.FAILED.name,
                detail = "Interrupted by AURA restart — tap Retry to resume safely.",
                updatedAtEpochMs = System.currentTimeMillis()
            )
            taskDao.upsert(updated)
            reset++
        }
        log(null, null, "aura.orchestrator", "info", "Recovery: reset $reset interrupted task(s)")
        return RecoveryReport(resetTasks = reset)
    }

    // ---- Diagnostics -------------------------------------------------------------

    data class HealthItem(val label: String, val ok: Boolean, val detail: String)

    fun diagnostics(): List<HealthItem> {
        val items = mutableListOf<HealthItem>()
        items += HealthItem("AURA Core", true, "engine v0.1")
        items += HealthItem("Database", true, "Room v1")
        items += HealthItem("Protocol", true, "aura/1.0")
        val model = settings.modelConfigValueBlocking()
        items += HealthItem(
            "Model Gateway",
            model.configured,
            if (model.configured) "${model.providerId} · ${model.modelName}" else "not configured"
        )
        items += HealthItem("Files workspace", true, workspaceDir().absolutePath)
        items += HealthItem("Downloads", true, "WorkManager")
        items += HealthItem("Network", hasNetwork(), if (hasNetwork()) "online" else "offline (offline mode ok)")
        items += HealthItem("Python / Remote compute", false, "not configured")
        items += HealthItem("Browser automation", false, "not configured")
        items += HealthItem("GitHub", false, "not configured")
        return items
    }

    private fun hasNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    data class RecoveryReport(val resetTasks: Int)

    /** Bound-service entry used to enqueue work from other components. */
    fun executionCoroutineContext(): CoroutineContext = appScope.coroutineContext
}
