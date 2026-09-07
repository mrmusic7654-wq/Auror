package aura.orchestrator.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges Hilt-injected singletons (held by [aura.orchestrator.di.ServiceLocator])
 * into WorkManager workers, which are instantiated by the OS rather than Dagger.
 */
@Singleton
class AuraWorkerFactory @Inject constructor() : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? = when (workerClassName) {
        DownloadWorker::class.java.name -> DownloadWorker(appContext, workerParameters)
        else -> null
    }
}
