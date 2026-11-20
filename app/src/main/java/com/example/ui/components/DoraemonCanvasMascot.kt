package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class DoraemonGesture {
    SALUTE,
    THINK,
    CELEBRATE,
    ADVISING,
    ALERT;

    companion object {
        fun safeValueOf(raw: String?): DoraemonGesture = when (raw?.trim()?.lowercase()) {
            "salute", "greeting", "saludar" -> SALUTE
            "think", "thinking", "pensar" -> THINK
            "celebrate", "celebrating", "celebrar" -> CELEBRATE
            "alert", "warning", "alerta" -> ALERT
            else -> ADVISING
        }
    }
}

// Boing / 2005 Anime-inspired bright cel-shaded palette
private val BoingBlueLight = Color(0xFF5BC8FF)
private val BoingBlueMid = Color(0xFF169BF6)
private val BoingBlueDeep = Color(0xFF036EC4)
private val BoingWhiteTop = Color(0xFFFFFFFF)
private val BoingWhiteShade = Color(0xFFE6EEF8)
private val BoingRedLight = Color(0xFFFF4052)
private val BoingRedDeep = Color(0xFFD6081D)
private val BoingMouthDark = Color(0xFFB50B24)
private val BoingTongueLight = Color(0xFFFFB648)
private val BoingTongueDeep = Color(0xFFEA7B12)
private val BoingGoldLight = Color(0xFFFFF08A)
private val BoingGoldMid = Color(0xFFFBBF24)
private val BoingGoldDeep = Color(0xFFB45309)
private val BoingInk = Color(0xFF1E293B)
private val BoingBlush = Color(0xFFFB7185)

@Composable
fun DoraemonAnimatedMascot(
    gesture: DoraemonGesture,
    modifier: Modifier = Modifier
) {
    // 1. State-driven transition between SALUTE / THINK / CELEBRATE / ADVISING / ALERT
    val transition = updateTransition(targetState = gesture, label = "doraemon_state_transition")
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val headTiltDeg by transition.animateFloat(
        transitionSpec = { springSpec },
        label = "head_tilt"
    ) { state ->
        when (state) {
            DoraemonGesture.SALUTE -> 4.5f
            DoraemonGesture.THINK -> -10f
            DoraemonGesture.CELEBRATE -> 0f
            DoraemonGesture.ADVISING -> 2.5f
            DoraemonGesture.ALERT -> -2f
        }
    }

    val leftArmBaseDeg by transition.animateFloat(
        transitionSpec = { springSpec },
        label = "left_arm_base"
    ) { state ->
        when (state) {
            DoraemonGesture.SALUTE -> -34f
            DoraemonGesture.CELEBRATE -> -46f
            DoraemonGesture.THINK -> 38f
            DoraemonGesture.ADVISING -> -16f
            DoraemonGesture.ALERT -> -28f
        }
    }

    val leftArmWaveAmp by transition.animateFloat(
        transitionSpec = { tween(320) },
        label = "left_arm_amp"
    ) { state ->
        when (state) {
            DoraemonGesture.SALUTE -> 22f
            DoraemonGesture.CELEBRATE -> 14f
            DoraemonGesture.THINK -> 3f
            DoraemonGesture.ADVISING -> 12f
            DoraemonGesture.ALERT -> 24f
        }
    }

    val rightArmBaseDeg by transition.animateFloat(
        transitionSpec = { springSpec },
        label = "right_arm_base"
    ) { state ->
        when (state) {
            DoraemonGesture.CELEBRATE -> -124f // raised arm up in victory
            DoraemonGesture.THINK -> -72f // hand up near chin thinking
            DoraemonGesture.SALUTE -> 8f // relaxed near hip/pocket
            DoraemonGesture.ADVISING -> 0f
            DoraemonGesture.ALERT -> -18f
        }
    }

    val rightArmWaveAmp by transition.animateFloat(
        transitionSpec = { tween(320) },
        label = "right_arm_amp"
    ) { state ->
        when (state) {
            DoraemonGesture.CELEBRATE -> 14f
            DoraemonGesture.THINK -> 5f
            DoraemonGesture.SALUTE -> 6f
            DoraemonGesture.ADVISING -> 5f
            DoraemonGesture.ALERT -> 14f
        }
    }

    val jumpFactor by transition.animateFloat(
        transitionSpec = { springSpec },
        label = "jump_factor"
    ) { state ->
        when (state) {
            DoraemonGesture.CELEBRATE -> 1.0f
            DoraemonGesture.SALUTE -> 0.30f
            DoraemonGesture.ALERT -> 0.35f
            DoraemonGesture.ADVISING -> 0.18f
            DoraemonGesture.THINK -> 0.06f
        }
    }

    val bellSwingAmp by transition.animateFloat(
        transitionSpec = { tween(280) },
        label = "bell_amp"
    ) { state ->
        when (state) {
            DoraemonGesture.ALERT -> 18f
            DoraemonGesture.CELEBRATE -> 14f
            DoraemonGesture.SALUTE -> 9f
            DoraemonGesture.ADVISING -> 5f
            DoraemonGesture.THINK -> 2f
        }
    }

    val celebrateFxAlpha by transition.animateFloat(
        transitionSpec = { tween(360) },
        label = "celebrate_fx"
    ) { state ->
        when (state) {
            DoraemonGesture.CELEBRATE -> 1f
            DoraemonGesture.SALUTE -> 0.35f
            else -> 0f
        }
    }

    val thinkFxAlpha by transition.animateFloat(
        transitionSpec = { tween(360) },
        label = "think_fx"
    ) { state ->
        if (state == DoraemonGesture.THINK) 1f else 0f
    }

    val blushAlpha by transition.animateFloat(
        transitionSpec = { tween(320) },
        label = "blush_alpha"
    ) { state ->
        when (state) {
            DoraemonGesture.CELEBRATE -> 0.42f
            DoraemonGesture.SALUTE -> 0.28f
            DoraemonGesture.ADVISING -> 0.20f
            DoraemonGesture.THINK -> 0.12f
            DoraemonGesture.ALERT -> 0.10f
        }
    }

    // 2. Continuous rhythmic drivers
    val infinite = rememberInfiniteTransition(label = "doraemon_loop")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loop_phase"
    )

    val blinkCycle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blink_cycle"
    )

    val sparklePulse by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val stroke = (unit * 0.024f).coerceAtLeast(1.8f)

        val hopWave = abs(sin(phase))
        val floatY = -hopWave * (unit * 0.065f) * jumpFactor
        val breathScaleX = 1f + cos(phase * 2f) * (0.022f * jumpFactor + 0.01f)
        val breathScaleY = 1f - cos(phase * 2f) * (0.022f * jumpFactor + 0.01f)

        val leftArmAngle = leftArmBaseDeg + sin(phase * 2f) * leftArmWaveAmp
        val rightArmAngle = rightArmBaseDeg + cos(phase * 2f) * rightArmWaveAmp
        val tailWag = sin(phase * 2.5f) * (8f + 10f * jumpFactor)
        val bellSwing = sin(phase * 3f) * bellSwingAmp
        val bodySway = sin(phase) * (2f + 2f * jumpFactor) + headTiltDeg * 0.25f

        // Ground shadow shrinks smoothly during hops
        val shadowShrink = (1f - (hopWave * 0.18f * jumpFactor)).coerceIn(0.68f, 1f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(w * 0.50f, h * 0.92f),
                radius = w * 0.30f * shadowShrink
            ),
            topLeft = Offset(w * (0.50f - 0.27f * shadowShrink), h * 0.885f),
            size = Size(w * 0.54f * shadowShrink, h * 0.07f)
        )

        // Thought dots floating smoothly near temple in THINK state
        if (thinkFxAlpha > 0.02f) {
            val drift = sin(phase) * (unit * 0.016f)
            drawCircle(
                color = Color.White.copy(alpha = 0.92f * thinkFxAlpha),
                radius = unit * 0.024f,
                center = Offset(w * 0.86f, h * 0.16f + drift)
            )
            drawCircle(
                color = BoingBlueLight.copy(alpha = 0.85f * thinkFxAlpha),
                radius = unit * 0.024f,
                center = Offset(w * 0.86f, h * 0.16f + drift),
                style = Stroke(width = stroke * 0.65f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.95f * thinkFxAlpha),
                radius = unit * 0.036f,
                center = Offset(w * 0.92f, h * 0.09f - drift)
            )
            drawCircle(
                color = BoingBlueLight.copy(alpha = 0.9f * thinkFxAlpha),
                radius = unit * 0.036f,
                center = Offset(w * 0.92f, h * 0.09f - drift),
                style = Stroke(width = stroke * 0.65f)
            )
        }

        // Anime celebration sparkles in CELEBRATE / SALUTE states
        if (celebrateFxAlpha > 0.02f) {
            drawFourPointSparkle(
                center = Offset(w * 0.12f, h * 0.16f),
                radius = unit * 0.055f * sparklePulse,
                color = BoingGoldMid.copy(alpha = celebrateFxAlpha),
                rotationDeg = phase * 30f
            )
            drawFourPointSparkle(
                center = Offset(w * 0.88f, h * 0.18f),
                radius = unit * 0.048f * (2f - sparklePulse),
                color = BoingBlueLight.copy(alpha = celebrateFxAlpha),
                rotationDeg = -phase * 35f
            )
            drawFourPointSparkle(
                center = Offset(w * 0.84f, h * 0.44f),
                radius = unit * 0.036f * sparklePulse,
                color = BoingGoldLight.copy(alpha = celebrateFxAlpha * 0.9f),
                rotationDeg = phase * 20f
            )
        }

        withTransform({
            translate(left = 0f, top = floatY)
            rotate(degrees = bodySway, pivot = Offset(w * 0.50f, h * 0.72f))
            scale(scaleX = breathScaleX, scaleY = breathScaleY, pivot = Offset(w * 0.50f, h * 0.88f))
        }) {
            // 1. Wagging Tail (behind torso)
            val tailBase = Offset(w * 0.71f, h * 0.76f)
            rotate(degrees = tailWag, pivot = tailBase) {
                drawLine(
                    color = BoingInk,
                    start = tailBase,
                    end = Offset(w * 0.86f, h * 0.72f),
                    strokeWidth = stroke * 1.1f,
                    cap = StrokeCap.Round
                )
                val tailBall = Offset(w * 0.875f, h * 0.715f)
                val tailR = unit * 0.048f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BoingRedLight, BoingRedDeep),
                        center = Offset(tailBall.x - tailR * 0.3f, tailBall.y - tailR * 0.3f),
                        radius = tailR * 1.3f
                    ),
                    radius = tailR,
                    center = tailBall
                )
                drawCircle(color = BoingInk, radius = tailR, center = tailBall, style = Stroke(width = stroke))
            }

            // 2. Left Arm (articulated waving / saluting arm)
            val leftShoulder = Offset(w * 0.28f, h * 0.565f)
            rotate(degrees = leftArmAngle, pivot = leftShoulder) {
                drawBlueLimbCapsule(
                    start = leftShoulder,
                    end = Offset(w * 0.135f, h * 0.46f),
                    width = unit * 0.095f,
                    stroke = stroke
                )
                drawRoundWhitePaw(
                    center = Offset(w * 0.125f, h * 0.45f),
                    radius = unit * 0.068f,
                    stroke = stroke
                )
            }

            // 3. Chubby Cel-Shaded Blue Torso
            val torsoRect = Rect(w * 0.245f, h * 0.50f, w * 0.755f, h * 0.835f)
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(BoingBlueLight, BoingBlueMid, BoingBlueDeep),
                    center = Offset(w * 0.40f, h * 0.58f),
                    radius = unit * 0.45f
                ),
                topLeft = torsoRect.topLeft,
                size = torsoRect.size,
                cornerRadius = CornerRadius(unit * 0.20f, unit * 0.20f)
            )
            drawRoundRect(
                color = BoingInk,
                topLeft = torsoRect.topLeft,
                size = torsoRect.size,
                cornerRadius = CornerRadius(unit * 0.20f, unit * 0.20f),
                style = Stroke(width = stroke)
            )

            // 4. Short stubby legs & Plump White Shoes
            drawCelFoot(Offset(w * 0.22f, h * 0.805f), Size(w * 0.25f, h * 0.09f), stroke)
            drawCelFoot(Offset(w * 0.53f, h * 0.805f), Size(w * 0.25f, h * 0.09f), stroke)

            // 5. White Belly Patch & 4D Pocket
            val bellyTopLeft = Offset(w * 0.305f, h * 0.535f)
            val bellySize = Size(w * 0.39f, h * 0.245f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(BoingWhiteTop, BoingWhiteShade),
                    center = Offset(w * 0.48f, h * 0.61f),
                    radius = unit * 0.25f
                ),
                topLeft = bellyTopLeft,
                size = bellySize
            )
            drawOval(color = BoingInk, topLeft = bellyTopLeft, size = bellySize, style = Stroke(width = stroke))

            val pocketPath = Path().apply {
                moveTo(w * 0.355f, h * 0.655f)
                lineTo(w * 0.645f, h * 0.655f)
                arcTo(
                    rect = Rect(w * 0.355f, h * 0.56f, w * 0.645f, h * 0.755f),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(
                pocketPath,
                brush = Brush.verticalGradient(
                    colors = listOf(BoingWhiteShade, BoingWhiteTop),
                    startY = h * 0.65f,
                    endY = h * 0.76f
                )
            )
            drawPath(pocketPath, color = BoingInk, style = Stroke(width = stroke, join = StrokeJoin.Round))

            // 6. Right Arm (reaches pocket in SALUTE, touches chin in THINK, raises high in CELEBRATE)
            val rightShoulder = Offset(w * 0.72f, h * 0.565f)
            rotate(degrees = rightArmAngle, pivot = rightShoulder) {
                val pawEnd = Offset(w * 0.62f, h * 0.665f)
                drawBlueLimbCapsule(
                    start = rightShoulder,
                    end = pawEnd,
                    width = unit * 0.092f,
                    stroke = stroke
                )
                drawRoundWhitePaw(
                    center = pawEnd,
                    radius = unit * 0.064f,
                    stroke = stroke
                )
            }

            // 7. Big Expressive Modern Boing-Style Head
            val neckPivot = Offset(w * 0.50f, h * 0.525f)
            rotate(degrees = headTiltDeg, pivot = neckPivot) {
                val headCenter = Offset(w * 0.50f, h * 0.295f)
                val headRx = w * 0.385f
                val headRy = h * 0.26f

                // Volumetric 3D blue head gradient
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(BoingBlueLight, BoingBlueMid, BoingBlueDeep),
                        center = Offset(headCenter.x - headRx * 0.34f, headCenter.y - headRy * 0.38f),
                        radius = headRx * 1.65f
                    ),
                    topLeft = Offset(headCenter.x - headRx, headCenter.y - headRy),
                    size = Size(headRx * 2f, headRy * 2f)
                )
                // Glossy anime forehead highlight arc
                drawArc(
                    color = Color.White.copy(alpha = 0.26f),
                    startAngle = 210f,
                    sweepAngle = 55f,
                    useCenter = false,
                    topLeft = Offset(headCenter.x - headRx * 0.86f, headCenter.y - headRy * 0.86f),
                    size = Size(headRx * 1.72f, headRy * 1.72f),
                    style = Stroke(width = unit * 0.025f, cap = StrokeCap.Round)
                )
                drawOval(
                    color = BoingInk,
                    topLeft = Offset(headCenter.x - headRx, headCenter.y - headRy),
                    size = Size(headRx * 2f, headRy * 2f),
                    style = Stroke(width = stroke)
                )

                // White Face Mask
                val faceRx = headRx * 0.84f
                val faceRy = headRy * 0.78f
                val faceCenter = Offset(headCenter.x, headCenter.y + h * 0.044f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(BoingWhiteTop, BoingWhiteTop, BoingWhiteShade),
                        center = Offset(faceCenter.x, faceCenter.y - faceRy * 0.2f),
                        radius = faceRx * 1.3f
                    ),
                    topLeft = Offset(faceCenter.x - faceRx, faceCenter.y - faceRy),
                    size = Size(faceRx * 2f, faceRy * 2f)
                )
                drawOval(
                    color = BoingInk,
                    topLeft = Offset(faceCenter.x - faceRx, faceCenter.y - faceRy),
                    size = Size(faceRx * 2f, faceRy * 2f),
                    style = Stroke(width = stroke)
                )

                // Soft Rosy Anime Cheek Blush
                if (blushAlpha > 0.02f) {
                    drawOval(
                        color = BoingBlush.copy(alpha = blushAlpha),
                        topLeft = Offset(w * 0.23f, h * 0.29f),
                        size = Size(w * 0.11f, h * 0.045f)
                    )
                    drawOval(
                        color = BoingBlush.copy(alpha = blushAlpha),
                        topLeft = Offset(w * 0.66f, h * 0.29f),
                        size = Size(w * 0.11f, h * 0.045f)
                    )
                }

                // Expressive Mouth & Tongue
                drawBoingMouth(gesture = gesture, phase = phase, w = w, h = h, stroke = stroke)

                // Philtrum line
                drawLine(
                    color = BoingInk,
                    start = Offset(w * 0.50f, h * 0.26f),
                    end = Offset(w * 0.50f, h * 0.33f),
                    strokeWidth = stroke * 0.85f
                )

                // Crisp anime whiskers (3 per side)
                val whiskerFlex = sin(phase * 2f) * (h * 0.006f)
                drawLine(BoingInk, Offset(w * 0.36f, h * 0.255f), Offset(w * 0.17f, h * 0.215f + whiskerFlex), stroke * 0.78f, StrokeCap.Round)
                drawLine(BoingInk, Offset(w * 0.35f, h * 0.285f), Offset(w * 0.15f, h * 0.282f), stroke * 0.78f, StrokeCap.Round)
                drawLine(BoingInk, Offset(w * 0.36f, h * 0.315f), Offset(w * 0.17f, h * 0.342f - whiskerFlex), stroke * 0.78f, StrokeCap.Round)

                drawLine(BoingInk, Offset(w * 0.64f, h * 0.255f), Offset(w * 0.83f, h * 0.215f + whiskerFlex), stroke * 0.78f, StrokeCap.Round)
                drawLine(BoingInk, Offset(w * 0.65f, h * 0.285f), Offset(w * 0.85f, h * 0.282f), stroke * 0.78f, StrokeCap.Round)
                drawLine(BoingInk, Offset(w * 0.64f, h * 0.315f), Offset(w * 0.83f, h * 0.342f - whiskerFlex), stroke * 0.78f, StrokeCap.Round)

                // Expressive Eyes (blink, wink for salute, double happy arcs for celebrate, look-up for think)
                drawBoingEyes(
                    gesture = gesture,
                    blinkCycle = blinkCycle,
                    phase = phase,
                    w = w,
                    h = h,
                    unit = unit,
                    stroke = stroke
                )

                // Glossy 3D Red Nose
                val noseCenter = Offset(w * 0.50f, h * 0.228f)
                val noseR = unit * 0.048f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BoingRedLight, BoingRedDeep),
                        center = Offset(noseCenter.x - noseR * 0.32f, noseCenter.y - noseR * 0.32f),
                        radius = noseR * 1.4f
                    ),
                    radius = noseR,
                    center = noseCenter
                )
                drawCircle(color = BoingInk, radius = noseR, center = noseCenter, style = Stroke(width = stroke))
                drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    radius = noseR * 0.33f,
                    center = Offset(noseCenter.x - noseR * 0.28f, noseCenter.y - noseR * 0.28f)
                )
            }

            // 8. Red Collar with Highlight & 3D Swinging Golden Bell
            val collarRect = Rect(w * 0.27f, h * 0.50f, w * 0.73f, h * 0.546f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(BoingRedLight, BoingRedDeep),
                    startY = collarRect.top,
                    endY = collarRect.bottom
                ),
                topLeft = collarRect.topLeft,
                size = collarRect.size,
                cornerRadius = CornerRadius(unit * 0.04f, unit * 0.04f)
            )
            drawRoundRect(
                color = BoingInk,
                topLeft = collarRect.topLeft,
                size = collarRect.size,
                cornerRadius = CornerRadius(unit * 0.04f, unit * 0.04f),
                style = Stroke(width = stroke)
            )

            val bellCenter = Offset(w * 0.50f, h * 0.56f)
            val bellR = unit * 0.068f
            if (gesture == DoraemonGesture.ALERT || gesture == DoraemonGesture.CELEBRATE) {
                drawCircle(
                    color = BoingGoldMid.copy(alpha = 0.32f * (sparklePulse - 0.4f)),
                    radius = bellR * 1.55f,
                    center = bellCenter
                )
            }
            rotate(degrees = bellSwing, pivot = Offset(bellCenter.x, h * 0.525f)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BoingGoldLight, BoingGoldMid, BoingGoldDeep),
                        center = Offset(bellCenter.x - bellR * 0.3f, bellCenter.y - bellR * 0.3f),
                        radius = bellR * 1.4f
                    ),
                    radius = bellR,
                    center = bellCenter
                )
                drawCircle(color = BoingInk, radius = bellR, center = bellCenter, style = Stroke(width = stroke))
                drawLine(
                    color = BoingInk,
                    start = Offset(bellCenter.x - bellR * 0.86f, bellCenter.y - bellR * 0.14f),
                    end = Offset(bellCenter.x + bellR * 0.86f, bellCenter.y - bellR * 0.14f),
                    strokeWidth = stroke * 0.75f
                )
                drawCircle(
                    color = BoingInk,
                    radius = bellR * 0.22f,
                    center = Offset(bellCenter.x, bellCenter.y + bellR * 0.28f)
                )
                drawLine(
                    color = BoingInk,
                    start = Offset(bellCenter.x, bellCenter.y + bellR * 0.3f),
                    end = Offset(bellCenter.x, bellCenter.y + bellR * 0.84f),
                    strokeWidth = stroke * 0.72f
                )
            }
        }
    }
}

private fun DrawScope.drawBlueLimbCapsule(
    start: Offset,
    end: Offset,
    width: Float,
    stroke: Float
) {
    drawLine(
        color = BoingInk,
        start = start,
        end = end,
        strokeWidth = width + stroke * 1.9f,
        cap = StrokeCap.Round
    )
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(BoingBlueLight, BoingBlueMid),
            start = start,
            end = end
        ),
        start = start,
        end = end,
        strokeWidth = width,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawRoundWhitePaw(
    center: Offset,
    radius: Float,
    stroke: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BoingWhiteTop, BoingWhiteShade),
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
            radius = radius * 1.3f
        ),
        radius = radius,
        center = center
    )
    drawCircle(color = BoingInk, radius = radius, center = center, style = Stroke(width = stroke))
}

private fun DrawScope.drawCelFoot(topLeft: Offset, size: Size, stroke: Float) {
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(BoingWhiteTop, BoingWhiteShade),
            startY = topLeft.y,
            endY = topLeft.y + size.height
        ),
        topLeft = topLeft,
        size = size
    )
    drawOval(color = BoingInk, topLeft = topLeft, size = size, style = Stroke(width = stroke))
}

private fun DrawScope.drawFourPointSparkle(
    center: Offset,
    radius: Float,
    color: Color,
    rotationDeg: Float
) {
    rotate(degrees = rotationDeg, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - radius)
            quadraticTo(center.x + radius * 0.22f, center.y - radius * 0.22f, center.x + radius, center.y)
            quadraticTo(center.x + radius * 0.22f, center.y + radius * 0.22f, center.x, center.y + radius)
            quadraticTo(center.x - radius * 0.22f, center.y + radius * 0.22f, center.x - radius, center.y)
            quadraticTo(center.x - radius * 0.22f, center.y - radius * 0.22f, center.x, center.y - radius)
            close()
        }
        drawPath(path, color = color)
    }
}

private fun DrawScope.drawBoingEyes(
    gesture: DoraemonGesture,
    blinkCycle: Float,
    phase: Float,
    w: Float,
    h: Float,
    unit: Float,
    stroke: Float
) {
    val leftEyeRect = Rect(w * 0.375f, h * 0.085f, w * 0.50f, h * 0.215f)
    val rightEyeRect = Rect(w * 0.50f, h * 0.085f, w * 0.625f, h * 0.215f)

    // Left eye outer white
    drawOval(color = BoingWhiteTop, topLeft = leftEyeRect.topLeft, size = leftEyeRect.size)
    drawOval(color = BoingInk, topLeft = leftEyeRect.topLeft, size = leftEyeRect.size, style = Stroke(width = stroke))

    // Right eye outer white
    drawOval(color = BoingWhiteTop, topLeft = rightEyeRect.topLeft, size = rightEyeRect.size)
    drawOval(color = BoingInk, topLeft = rightEyeRect.topLeft, size = rightEyeRect.size, style = Stroke(width = stroke))

    val isFullBlink = blinkCycle in 0.48f..0.52f
    val leftHappyArc = gesture == DoraemonGesture.CELEBRATE || isFullBlink
    val rightHappyArc = gesture == DoraemonGesture.CELEBRATE ||
        gesture == DoraemonGesture.SALUTE ||
        (gesture == DoraemonGesture.ADVISING && blinkCycle in 0.12f..0.44f) ||
        isFullBlink

    val lookX = when (gesture) {
        DoraemonGesture.THINK -> unit * 0.012f
        DoraemonGesture.SALUTE, DoraemonGesture.ADVISING -> sin(phase) * (unit * 0.006f)
        else -> 0f
    }
    val lookY = when (gesture) {
        DoraemonGesture.THINK -> -unit * 0.022f
        else -> 0f
    }

    if (leftHappyArc) {
        drawArc(
            color = BoingInk,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(leftEyeRect.left + w * 0.02f, leftEyeRect.top + h * 0.052f),
            size = Size(leftEyeRect.width - w * 0.04f, h * 0.042f),
            style = Stroke(width = stroke * 1.15f, cap = StrokeCap.Round)
        )
    } else {
        val pupil = Offset(leftEyeRect.center.x + w * 0.014f + lookX, leftEyeRect.center.y + lookY)
        drawCircle(color = BoingInk, radius = unit * 0.024f, center = pupil)
        drawCircle(
            color = Color.White,
            radius = unit * 0.009f,
            center = Offset(pupil.x - unit * 0.006f, pupil.y - unit * 0.007f)
        )
    }

    if (rightHappyArc) {
        drawArc(
            color = BoingInk,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(rightEyeRect.left + w * 0.02f, rightEyeRect.top + h * 0.052f),
            size = Size(rightEyeRect.width - w * 0.04f, h * 0.042f),
            style = Stroke(width = stroke * 1.15f, cap = StrokeCap.Round)
        )
    } else {
        val pupil = Offset(rightEyeRect.center.x - w * 0.014f + lookX, rightEyeRect.center.y + lookY)
        drawCircle(color = BoingInk, radius = unit * 0.024f, center = pupil)
        drawCircle(
            color = Color.White,
            radius = unit * 0.009f,
            center = Offset(pupil.x - unit * 0.006f, pupil.y - unit * 0.007f)
        )
    }
}

private fun DrawScope.drawBoingMouth(
    gesture: DoraemonGesture,
    phase: Float,
    w: Float,
    h: Float,
    stroke: Float
) {
    when (gesture) {
        DoraemonGesture.THINK -> {
            // Cute small contemplative "o"
            val c = Offset(w * 0.50f, h * 0.375f)
            val r = minOf(w, h) * 0.032f
            drawCircle(color = BoingRedDeep, radius = r, center = c)
            drawCircle(color = BoingInk, radius = r, center = c, style = Stroke(width = stroke))
        }
        DoraemonGesture.ALERT -> {
            val ovalTopLeft = Offset(w * 0.405f, h * 0.34f)
            val ovalSize = Size(w * 0.19f, h * 0.11f)
            drawOval(color = BoingMouthDark, topLeft = ovalTopLeft, size = ovalSize)
            drawOval(
                brush = Brush.verticalGradient(listOf(BoingTongueLight, BoingTongueDeep)),
                topLeft = Offset(w * 0.44f, h * 0.395f),
                size = Size(w * 0.12f, h * 0.05f)
            )
            drawOval(color = BoingInk, topLeft = ovalTopLeft, size = ovalSize, style = Stroke(width = stroke))
        }
        DoraemonGesture.SALUTE,
        DoraemonGesture.CELEBRATE,
        DoraemonGesture.ADVISING -> {
            val amp = if (gesture == DoraemonGesture.CELEBRATE) 0.020f else 0.012f
            val talkDrop = (sin(phase * 3f) * 0.5f + 0.5f) * (h * amp)
            val smilePath = Path().apply {
                moveTo(w * 0.30f, h * 0.33f)
                quadraticTo(w * 0.50f, h * 0.355f, w * 0.70f, h * 0.31f)
                cubicTo(
                    w * 0.72f, h * (0.455f + talkDrop / h),
                    w * 0.58f, h * (0.495f + talkDrop / h),
                    w * 0.50f, h * (0.49f + talkDrop / h)
                )
                cubicTo(
                    w * 0.40f, h * (0.485f + talkDrop / h),
                    w * 0.29f, h * 0.415f,
                    w * 0.30f, h * 0.33f
                )
                close()
            }
            drawPath(
                smilePath,
                brush = Brush.verticalGradient(
                    colors = listOf(BoingMouthDark, BoingRedDeep),
                    startY = h * 0.32f,
                    endY = h * 0.50f
                )
            )
            drawOval(
                brush = Brush.verticalGradient(listOf(BoingTongueLight, BoingTongueDeep)),
                topLeft = Offset(w * 0.385f, h * (0.395f + talkDrop * 0.35f / h)),
                size = Size(w * 0.23f, h * 0.082f)
            )
            drawOval(
                color = BoingInk,
                topLeft = Offset(w * 0.385f, h * (0.395f + talkDrop * 0.35f / h)),
                size = Size(w * 0.23f, h * 0.082f),
                style = Stroke(width = stroke * 0.78f)
            )
            drawPath(smilePath, color = BoingInk, style = Stroke(width = stroke, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun DoraemonMiniHeadIcon(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val mascot = androidx.compose.ui.graphics.ImageBitmap.imageResource(com.example.R.drawable.img_doraemon_mascot)
    Canvas(modifier = modifier) {
        // Frame the face from the same illustration shown in the assistant.
        val x = (mascot.width * 0.14f).toInt()
        val width = mascot.width - x
        val height = (mascot.height * 0.62f).toInt()
        drawImage(
            image = mascot,
            srcOffset = androidx.compose.ui.unit.IntOffset(x, 0),
            srcSize = androidx.compose.ui.unit.IntSize(width, height),
            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
            alpha = if (active) 1f else 0.65f
        )
    }
}
