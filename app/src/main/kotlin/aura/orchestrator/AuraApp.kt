package aura.orchestrator

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import aura.orchestrator.di.ServiceLocator
import aura.orchestrator.worker.AuraWorkerFactory
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hilt-injected application. Fields marked @Inject are populated from the Dagger
 * graph after onCreate and mirrored into [ServiceLocator] so plain Android
 * components (services, workers) can reach the same singletons without a separate
 * DI path.
 */
@HiltAndroidApp
class AuraApp : Application(), Configuration.Provider {

    @Inject lateinit var serviceLocator: ServiceLocator

    @Inject lateinit var workerFactory: AuraWorkerFactory

    private lateinit var workManager: WorkManager

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(serviceLocator)
        workManager = WorkManager.getInstance(this)
        // Task recovery: on any process restart, persist-free states are reconciled
        // from Room so nothing depends on RAM-resident data (spec section 8).
        serviceLocator.appScope.launch { serviceLocator.engine.recoverInterrupted() }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()
}
