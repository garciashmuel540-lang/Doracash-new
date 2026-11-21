package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.DoraemonAnimatedMascot
import com.example.ui.components.DoraemonGesture
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "mdpi")
class DoraemonMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun gesturesAnimateAndChangeTheRenderedCharacter() {
        val pose = mutableStateOf(DoraemonGesture.SALUTE)
        compose.mainClock.autoAdvance = false
        lateinit var host: View
        compose.setContent {
            host = LocalView.current
            DoraemonAnimatedMascot(pose.value, Modifier.size(180.dp).background(Color(0xFF0C1728)).testTag("mascot"))
        }
        val node = compose.onNodeWithTag("mascot")
        val bounds = node.fetchSemanticsNode().boundsInRoot
        fun frame(): Bitmap = compose.runOnIdle {
            val result = Bitmap.createBitmap(bounds.width.toInt(), bounds.height.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.translate(-bounds.left, -bounds.top)
            host.draw(canvas)
            result
        }
        val preview = System.getenv("DORACASH_MOTION_FRAMES")?.let { File(it).apply { mkdirs() } }
        var index = 0
        var previousPose: Bitmap? = null
        for (gesture in listOf(DoraemonGesture.SALUTE, DoraemonGesture.THINK, DoraemonGesture.CELEBRATE, DoraemonGesture.ADVISING)) {
            compose.runOnIdle { pose.value = gesture }
            val frames = if (preview == null) 3 else 32
            var first: Bitmap? = null
            repeat(frames) {
                compose.mainClock.advanceTimeBy(if (preview == null) 208L else 64L)
                val image = frame()
                if (it == 0) first = image
                if (it == frames - 1) {
                    assertFalse("Movement within $gesture", image.sameAs(first))
                    previousPose?.let { prior -> assertFalse("Transition to $gesture", image.sameAs(prior)) }
                    previousPose = image
                }
                preview?.let { directory ->
                    File(directory, "frame-%04d.png".format(index++)).outputStream().use { stream ->
                        image.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                }
            }
        }
    }
}
