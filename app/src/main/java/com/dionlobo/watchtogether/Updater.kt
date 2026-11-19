package com.dionlobo.watchtogether

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GithubReleaseAsset(
    val name: String = "",
    @SerializedName("browser_download_url") val browserDownloadUrl: String = "",
    val size: Long = 0
)

data class GithubRelease(
    @SerializedName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubReleaseAsset> = emptyList()
)

data class AppUpdate(
    val versionName: String,
    val releaseName: String,
    val notes: String,
    val assetName: String,
    val downloadUrl: String,
    val size: Long
)

private val versionSeparator = Regex("[.\\-+]")

private interface GithubReleaseService {
    @GET("repos/dionjoshualobo/TwinWatch/releases/latest")
    suspend fun latest(): GithubRelease
}

object AppUpdater {
    private const val PREFS_NAME = "watch_together_prefs"
    private const val LAST_CHECK_KEY = "last_update_check_ms"
    private const val CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000
    private const val PROVIDER_SUFFIX = ".fileprovider"

    private val service: GithubReleaseService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GithubReleaseService::class.java)

    fun currentVersion(context: Context): String = try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName ?: ""
    } catch (e: PackageManager.NameNotFoundException) {
        ""
    }

    fun shouldCheck(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return now - prefs.getLong(LAST_CHECK_KEY, 0L) >= CHECK_INTERVAL_MS
    }

    fun markChecked(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_CHECK_KEY, System.currentTimeMillis())
            .apply()
    }

    suspend fun latestUpdate(context: Context): AppUpdate? = withContext(Dispatchers.IO) {
        val release = service.latest()
        if (release.draft || release.prerelease) return@withContext null
        val latest = release.tagName.trim().removePrefix("v").removePrefix("V")
        if (!isNewerVersion(latest, currentVersion(context))) return@withContext null
        val asset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return@withContext null
        AppUpdate(
            versionName = latest,
            releaseName = release.name.ifBlank { "TwinWatch ${release.tagName}" },
            notes = release.body.trim(),
            assetName = asset.name,
            downloadUrl = asset.browserDownloadUrl,
            size = asset.size
        )
    }

    suspend fun download(
        context: Context,
        update: AppUpdate,
        onProgress: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(directory, "twinwatch-update.apk")
        if (file.exists()) file.delete()

        val connection = URL(update.downloadUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = (20L * 1000).toInt()
        connection.readTimeout = (60L * 1000).toInt()
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "TwinWatch-Updater")
        connection.useCaches = false

        try {
            val response = connection.responseCode
            if (response !in 200..299) throw IllegalStateException("Download failed ($response)")

            val expected = if (connection.contentLength > 0) connection.contentLength.toLong() else update.size
            var lastPercent = -1

            FileOutputStream(file).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var total = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        total += read
                        if (expected > 0) {
                            val percent = (total * 100 / expected).toInt().coerceIn(0, 99)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                withContext(Dispatchers.Main) { onProgress(percent) }
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) { onProgress(100) }
            file
        } finally {
            connection.disconnect()
        }
    }

    fun installIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, context.packageName + PROVIDER_SUFFIX, file)
        return Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
        }
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
}

internal fun versionParts(value: String): List<Int> =
    value.trim().removePrefix("v").removePrefix("V").split(versionSeparator)
        .mapNotNull { it.toIntOrNull() }

internal fun isNewerVersion(latest: String, current: String): Boolean {
    val latestParts = versionParts(latest)
    val currentParts = versionParts(current)
    val length = maxOf(latestParts.size, currentParts.size)
    for (index in 0 until length) {
        val newer = latestParts.getOrElse(index) { 0 }
        val older = currentParts.getOrElse(index) { 0 }
        if (newer != older) return newer > older
    }
    return false
}
