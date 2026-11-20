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
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object UpdateRepository {
    private const val UPDATE_URL =
        "https://raw.githubusercontent.com/teddypro1/Teddy-WebApp/main/update.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    data class CheckResult(
        val metadata: UpdateMetadata?,
        val installedVersionName: String,
        val installedVersionCode: Long,
        val message: String
    )

    data class DownloadProgress(
        val downloadedBytes: Long,
        val totalBytes: Long
    ) {
        val fraction: Float
            get() = if (totalBytes > 0L) {
                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            } else 0f
    }

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

    fun downloadAndVerify(
        context: Context,
        metadata: UpdateMetadata,
        onProgress: (DownloadProgress) -> Unit,
        onComplete: (Result<File>) -> Unit
    ) {
        val appContext = context.applicationContext
        Thread {
            val result = runCatching {
                require(metadata.downloadUrl.startsWith("https://")) {
                    "The update download URL must use HTTPS."
                }
                require(metadata.sha256.matches(Regex("(?i)[a-f0-9]{64}"))) {
                    "The update SHA-256 value is invalid."
                }

                val request = Request.Builder()
                    .url(metadata.downloadUrl)
                    .header("Accept", "application/vnd.android.package-archive")
                    .get()
                    .build()

                downloadClient.newCall(request).execute().use { response ->
                    check(response.isSuccessful) {
                        "APK download failed (HTTP ${response.code})."
                    }
                    val body = response.body ?: error("The download response was empty.")
                    val totalBytes = body.contentLength()
                    val temporaryFile = File(appContext.cacheDir, "teddy-update.apk.part")
                    val verifiedFile = File(appContext.cacheDir, "teddy-update.apk")
                    temporaryFile.delete()

                    val digest = MessageDigest.getInstance("SHA-256")
                    var downloadedBytes = 0L
                    body.byteStream().use { input ->
                        FileOutputStream(temporaryFile).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val count = input.read(buffer)
                                if (count == -1) break
                                output.write(buffer, 0, count)
                                digest.update(buffer, 0, count)
                                downloadedBytes += count
                                val progress = DownloadProgress(downloadedBytes, totalBytes)
                                Handler(Looper.getMainLooper()).post { onProgress(progress) }
                            }
                            output.fd.sync()
                        }
                    }

                    check(temporaryFile.length() > 0L) { "The downloaded APK is empty." }
                    val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
                    check(actualHash.equals(metadata.sha256, ignoreCase = true)) {
                        "SHA-256 verification failed. The downloaded file was rejected."
                    }
                    verifiedFile.delete()
                    check(temporaryFile.renameTo(verifiedFile)) {
                        "Could not prepare the verified APK for installation."
                    }
                    verifiedFile
                }
            }
            Handler(Looper.getMainLooper()).post { onComplete(result) }
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
