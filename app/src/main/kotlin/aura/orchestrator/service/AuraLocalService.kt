package aura.orchestrator.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import aura.orchestrator.di.ServiceLocator

/**
 * Local bound orchestrator service. This is the in-process face of the AURA
 * protocol used by the UI and background workers. Companion AURA apps will bind
 * through a versioned AIDL contract against sibling services in later milestones;
 * the same capabilities are invoked through [aura.core.protocol] requests.
 */
class AuraLocalService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun service(): AuraLocalService = this@AuraLocalService
    }

    private val engine get() = ServiceLocator.get().engine

    fun submit(input: String): String = engine.submit(input)

    fun capabilityCount(): Int = ServiceLocator.get().registry.providers.value.size

    override fun onBind(intent: Intent): IBinder = binder
}
