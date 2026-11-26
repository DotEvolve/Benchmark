package net.dotevolve.benchmark.core

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/**
 * Provides a privacy-preserving, installation-scoped UUID for grouping runs
 * uploaded from the same app installation on the web analytics dashboard.
 */
object DeviceIdentityProvider {
    private const val PREFS_NAME = "benchmark_prefs"
    private const val KEY_CLIENT_DEVICE_ID = "client_device_id"

    @JvmStatic
    @Synchronized
    fun getOrCreateClientDeviceId(context: Context): String {
        val appContext = context.applicationContext ?: context
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_CLIENT_DEVICE_ID, null)?.trim()
        if (!existing.isNullOrEmpty()) {
            return existing
        }
        val generated = UUID.randomUUID().toString()
        prefs.edit { putString(KEY_CLIENT_DEVICE_ID, generated) }
        return generated
    }
}
