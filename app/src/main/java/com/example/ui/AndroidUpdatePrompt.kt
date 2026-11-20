package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal const val RELEASES_URL = "https://github.com/garciashmuel540-lang/Doracash-new/releases"
private const val LATEST_API = "https://api.github.com/repos/garciashmuel540-lang/Doracash-new/releases/latest"
private const val CHECK_INTERVAL = 6 * 60 * 60 * 1000L
private val assetName = Regex("""Doracash-([1-9][0-9]{0,6})\.apk""")

internal data class AndroidRelease(val code: Int, val version: String, val url: String)

internal fun parseAndroidRelease(metadata: JSONObject): AndroidRelease? {
    if (metadata.optBoolean("prerelease") || metadata.optBoolean("draft")) return null
    val tag = metadata.optString("tag_name")
    if (!tag.matches(Regex("""android-v[0-9]+(\.[0-9]+){1,3}"""))) return null
    val assets = metadata.optJSONArray("assets") ?: return null
    for (index in 0 until assets.length()) {
        val asset = assets.getJSONObject(index)
        val name = asset.optString("name")
        val code = assetName.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull() ?: continue
        val url = "$RELEASES_URL/download/$tag/$name"
        if (asset.optString("browser_download_url") == url && asset.optLong("size") > 0) {
            return AndroidRelease(code, tag.removePrefix("android-v"), url)
        }
    }
    return null
}

private suspend fun latestAndroidRelease(): AndroidRelease? = withContext(Dispatchers.IO) {
    val connection = URL(LATEST_API).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        if (connection.responseCode == 404) return@withContext null
        if (connection.responseCode != 200) throw IOException("HTTP ${connection.responseCode}")
        connection.inputStream.bufferedReader().use { parseAndroidRelease(JSONObject(it.readText())) }
    } finally {
        connection.disconnect()
    }
}

@Composable
fun AndroidUpdatePrompt() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val prefs = remember(context) { context.getSharedPreferences("android_updates", Context.MODE_PRIVATE) }
    var release by remember { mutableStateOf<AndroidRelease?>(null) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                val nextCheck = prefs.getLong("nextCheck", 0)
                if (nextCheck > now && nextCheck - now <= CHECK_INTERVAL) {
                    delay(nextCheck - now)
                    continue
                }
                try {
                    val latest = latestAndroidRelease()
                    if (latest != null && latest.code > BuildConfig.VERSION_CODE) {
                        release = latest
                    }
                    // Keep an actionable update visible across a process restart.
                    if (release == null) prefs.edit().putLong("nextCheck", now + CHECK_INTERVAL).apply()
                    delay(CHECK_INTERVAL)
                } catch (e: IOException) {
                    delay(15 * 60 * 1000L)
                } catch (e: org.json.JSONException) {
                    delay(CHECK_INTERVAL)
                }
            }
        }
    }
    release?.let { latest ->
        val later = {
            prefs.edit().putLong("nextCheck", System.currentTimeMillis() + CHECK_INTERVAL).apply()
            release = null
        }
        AlertDialog(
            onDismissRequest = later,
            title = { Text("Doracash ${latest.version} disponible") },
            text = { Text("Descargá el APK y abrilo en este teléfono para confirmar la actualización.") },
            confirmButton = {
                Button(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latest.url)))
                        later()
                    } catch (e: android.content.ActivityNotFoundException) {
                        Toast.makeText(context, "Abrí GitHub Releases en tu navegador para descargar Doracash.", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Descargar actualización") }
            },
            dismissButton = { TextButton(onClick = later) { Text("Más tarde") } }
        )
    }
}
