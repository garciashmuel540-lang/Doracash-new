package com.example

import com.example.ui.parseAndroidRelease
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [36])
class AndroidReleaseTest {
    @Test fun onlyOffersTheVersionedDistributionApk() {
        val url = "https://github.com/garciashmuel540-lang/Doracash-new/releases/download/android-v1.2/Doracash-3.apk"
        val asset = JSONObject().put("name", "Doracash-3.apk").put("size", 1234L).put("browser_download_url", url)
        val metadata = JSONObject().put("tag_name", "android-v1.2").put("assets", JSONArray().put(asset))
        assertEquals(3, parseAndroidRelease(metadata)?.code)
        assertNull(parseAndroidRelease(metadata.put("prerelease", true)))
        metadata.put("prerelease", false)
        asset.put("browser_download_url", url.replace("android-v1.2/", "base-debito-1/"))
        assertNull(parseAndroidRelease(metadata))
    }
}
