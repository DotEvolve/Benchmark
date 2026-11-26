package net.dotevolve.benchmark.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import net.dotevolve.benchmark.BuildConfig

object WebLinks {

    fun buildRunUrl(runId: String, baseUrl: String = BuildConfig.BENCHMARK_API_BASE_URL): String {
        val cleanBase = baseUrl.trimEnd('/')
        return "$cleanBase/run/${Uri.encode(runId.trim())}"
    }

    fun buildMyRunsUrl(clientDeviceId: String, baseUrl: String = BuildConfig.BENCHMARK_API_BASE_URL): String {
        val cleanBase = baseUrl.trimEnd('/')
        return "$cleanBase/my-runs?id=${Uri.encode(clientDeviceId.trim())}"
    }

    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No browser found to open $url", Toast.LENGTH_SHORT).show()
        }
    }
}
