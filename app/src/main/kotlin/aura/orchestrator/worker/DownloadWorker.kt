package aura.orchestrator.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import aura.core.TaskStatus
import aura.orchestrator.data.TaskEntity
import aura.orchestrator.di.ServiceLocator
import aura.orchestrator.engine.WorkspaceTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Background-safe download executor driven by WorkManager (spec section 22). Writes
 * directly to the chosen destination directory (never through JSON), reports
 * progress into the task row, and resolves the task to SUCCEEDED/FAILED.
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URL = "url"
        const val KEY_TASK_ID = "taskId"
        const val KEY_DEST = "dest"
        const val UNIQUE = "aura_download"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val locator = ServiceLocator.get()
        val taskDao = locator.database.taskDao()
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val taskId = inputData.getString(KEY_TASK_ID)
        val destDir = File(inputData.getString(KEY_DEST) ?: applicationContext.filesDir.absolutePath)
        if (!destDir.exists()) destDir.mkdirs()

        try {
            val request = Request.Builder().url(url).header("User-Agent", "AuraOrchestrator/0.1").build()
            locator.okHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    taskId?.let {
                        val t = taskDao.byId(it)
                        if (t != null) taskDao.upsert(t.copy(status = TaskStatus.FAILED.name,
                            detail = "Download failed: HTTP ${response.code}", updatedAtEpochMs = System.currentTimeMillis()))
                    }
                    return@withContext if (runAttemptCount < 2) Result.retry() else Result.failure()
                }
                val body = response.body ?: run { return@withContext Result.failure() }
                val contentLength = body.contentLength()
                val fileName = pickFileName(response.header("Content-Disposition"), url)
                val out = File(destDir, fileName)
                val input = body.byteStream()
                val chunk = ByteArray(8192)
                val downloaded = AtomicInteger(0)
                val outStream = out.outputStream()
                outStream.use { os ->
                    var read = input.read(chunk)
                    while (read != -1) {
                        if (read > 0) {
                            os.write(chunk, 0, read)
                            downloaded.addAndGet(read)
                        }
                        val total = downloaded.get().toLong()
                        if (contentLength > 0) {
                            val percent = (total * 100L / contentLength).toInt().coerceIn(0, 100)
                            taskId?.let {
                                val t = taskDao.byId(it)
                                if (t != null) taskDao.upsert(t.copy(progress = 20 + (percent * 70 / 100),
                                    updatedAtEpochMs = System.currentTimeMillis()))
                            }
                        }
                        read = input.read(chunk)
                    }
                }
                val size = out.length()
                taskId?.let {
                    val t = taskDao.byId(it)
                    if (t != null) taskDao.upsert(t.copy(
                        status = TaskStatus.SUCCEEDED.name,
                        detail = "Downloaded ${fileName} · ${WorkspaceTools.formatBytes(size)}\n${out.absolutePath}",
                        progress = 100,
                        updatedAtEpochMs = System.currentTimeMillis()
                    ))
                }
                Result.success()
            }
        } catch (e: Exception) {
            taskId?.let {
                val t = taskDao.byId(it)
                if (t != null) taskDao.upsert(t.copy(
                    status = TaskStatus.FAILED.name,
                    detail = "Download failed: ${e.message ?: e.javaClass.simpleName}",
                    updatedAtEpochMs = System.currentTimeMillis()
                ))
            }
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun pickFileName(contentDisposition: String?, url: String): String {
        val cd = contentDisposition ?: ""
        val m = Regex("filename=\"?([^\";]+)\"?").find(cd)
        m?.let { return sanitize(it.groupValues[1]) }
        val base = url.substringBefore('?')
        val last = base.substringAfterLast('/')
        return if (last.isNotBlank()) sanitize(last) else "download_${System.currentTimeMillis()}"
    }

    private fun sanitize(name: String): String {
        val cleaned = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return cleaned.ifBlank { "download" }
    }
}
