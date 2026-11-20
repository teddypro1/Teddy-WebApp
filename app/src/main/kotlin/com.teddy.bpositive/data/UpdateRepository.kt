package com.teddy.bpositive.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.teddy.bpositive.model.UpdateMetadata
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object UpdateRepository {
    private const val UPDATE_URL =
        "https://raw.githubusercontent.com/teddypro1/Teddy-WebApp/feature/in-app-update-build15/update.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    data class CheckResult(
        val metadata: UpdateMetadata?,
        val installedVersionName: String,
        val installedVersionCode: Long,
        val message: String
    )

    fun check(context: Context, callback: (CheckResult) -> Unit) {
        val appContext = context.applicationContext
        Thread {
            val installed = getInstalledVersion(appContext)
            val result = try {
                val request = Request.Builder()
                    .url(UPDATE_URL)
                    .header("Cache-Control", "no-cache")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Update information is unavailable (HTTP ${response.code}).")
                    }
                    val body = response.body?.string()
                        ?: throw IllegalStateException("Update information is empty.")
                    val json = JSONObject(body)
                    val metadata = UpdateMetadata(
                        enabled = json.optBoolean("enabled", false),
                        latestVersionName = json.optString("latestVersionName", ""),
                        latestVersionCode = json.optLong("latestVersionCode", 0L),
                        minimumSupportedVersionCode = json.optLong("minimumSupportedVersionCode", 0L),
                        downloadUrl = json.optString("downloadUrl", ""),
                        sha256 = json.optString("sha256", ""),
                        releaseNotes = json.optString("releaseNotes", "")
                    )
                    if (metadata.latestVersionCode <= 0L ||
                        metadata.minimumSupportedVersionCode < 0L ||
                        metadata.latestVersionName.isBlank()
                    ) {
                        throw IllegalStateException("Update information has invalid version details.")
                    }
                    if (metadata.enabled) {
                        val message = when {
                            installed.second < metadata.minimumSupportedVersionCode ->
                                "A required update is available: ${metadata.latestVersionName}."
                            installed.second < metadata.latestVersionCode ->
                                "A new version is available: ${metadata.latestVersionName}."
                            else -> "You are using the latest version."
                        }
                        CheckResult(metadata, installed.first, installed.second, message)
                    } else {
                        CheckResult(metadata, installed.first, installed.second, "Update service is temporarily disabled.")
                    }
                }
            } catch (error: Exception) {
                CheckResult(
                    null,
                    installed.first,
                    installed.second,
                    error.message ?: "Could not check for updates. The app can be used normally."
                )
            }
            Handler(Looper.getMainLooper()).post { callback(result) }
        }.start()
    }

    private fun getInstalledVersion(context: Context): Pair<String, Long> {
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            (info.versionName ?: "Unknown") to code
        } catch (_: Exception) {
            "Unknown" to 0L
        }
    }
}
