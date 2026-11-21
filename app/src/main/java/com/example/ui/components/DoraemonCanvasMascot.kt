package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.clipPath
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
    // State-driven transition between SALUTE / THINK / CELEBRATE / ADVISING / ALERT
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
            DoraemonGesture.SALUTE -> 20f
            DoraemonGesture.CELEBRATE -> 27f
            DoraemonGesture.THINK -> -88f
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
            DoraemonGesture.CELEBRATE -> 178f // raised arm up in victory
            DoraemonGesture.THINK -> 88f // hand up near chin thinking
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

    val happy by transition.animateFloat(transitionSpec = { tween(320) }, label = "happy") {
        if (it == DoraemonGesture.CELEBRATE) 1f else 0f
    }
    val alert by transition.animateFloat(transitionSpec = { tween(320) }, label = "alert") {
        if (it == DoraemonGesture.ALERT) 1f else 0f
    }
    val greeting by transition.animateFloat(transitionSpec = { tween(320) }, label = "greeting") {
        if (it == DoraemonGesture.SALUTE) 1f else 0f
    }
    val description = when (gesture) {
        DoraemonGesture.SALUTE -> "Doraemon saludando"
        DoraemonGesture.THINK -> "Doraemon pensando"
        DoraemonGesture.CELEBRATE -> "Doraemon celebrando"
        DoraemonGesture.ADVISING -> "Doraemon aconsejando"
        DoraemonGesture.ALERT -> "Doraemon en alerta"
    }
    val loop = rememberInfiniteTransition(label = "doraemon")
    val time by loop.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4800, easing = LinearEasing)),
        label = "motion_cycle"
    )
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        val h = minOf(size.height, size.width / 0.84f)
        val w = h * 0.84f
        val unit = w
        val stroke = unit * 0.015f
        val angle = time * (2 * PI).toFloat()
        val phase = angle * 2f
        val blinkCycle = time
        val sparklePulse = 0.9f + 0.25f * cos(phase * 2f)
        withTransform({ translate((size.width - w) / 2f, (size.height - h) / 2f) }) {
            val hopWave = (1f - cos(phase)) * 0.5f
            val floatY = -hopWave * (unit * 0.048f) * jumpFactor
            val breathScaleX = 1f + cos(phase * 2f) * (0.022f * jumpFactor + 0.01f)
            val breathScaleY = 1f - cos(phase * 2f) * (0.022f * jumpFactor + 0.01f)

            val leftArmAngle = leftArmBaseDeg + sin(phase * 2f) * leftArmWaveAmp
            val rightArmAngle = rightArmBaseDeg + cos(phase * 2f) * rightArmWaveAmp
            val tailWag = sin(phase * 2f) * (8f + 10f * jumpFactor)
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
                    rotationDeg = angle * 180f / PI.toFloat()
                )
                drawFourPointSparkle(
                    center = Offset(w * 0.88f, h * 0.18f),
                    radius = unit * 0.048f * (2f - sparklePulse),
                    color = BoingBlueLight.copy(alpha = celebrateFxAlpha),
                    rotationDeg = -angle * 180f / PI.toFloat()
                )
                drawFourPointSparkle(
                    center = Offset(w * 0.84f, h * 0.44f),
                    radius = unit * 0.036f * sparklePulse,
                    color = BoingGoldLight.copy(alpha = celebrateFxAlpha * 0.9f),
                    rotationDeg = angle * 180f / PI.toFloat()
                )
            }

            withTransform({
                translate(left = 0f, top = floatY)
                rotate(degrees = bodySway, pivot = Offset(w * 0.50f, h * 0.72f))
                scale(scaleX = breathScaleX, scaleY = breathScaleY, pivot = Offset(w * 0.50f, h * 0.88f))
            }) {
                // Wagging Tail (behind torso)
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

                // Chubby Cel-Shaded Blue Torso
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

                // Short stubby legs & Plump White Shoes
                drawCelFoot(Offset(w * 0.22f, h * 0.805f), Size(w * 0.25f, h * 0.09f), stroke)
                drawCelFoot(Offset(w * 0.53f, h * 0.805f), Size(w * 0.25f, h * 0.09f), stroke)

                // White Belly Patch & 4D Pocket
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

                // Big Expressive Modern Boing-Style Head
                val neckPivot = Offset(w * 0.50f, h * 0.525f)
                rotate(degrees = headTiltDeg, pivot = neckPivot) {
                    drawCharacterFace(w, h, stroke, phase, blinkCycle, thinkFxAlpha, happy, alert, greeting, blushAlpha)
                }

                // Red Collar with Highlight & 3D Swinging Golden Bell
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
                // Left Arm (articulated waving / saluting arm)
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

                // Right Arm (reaches pocket in SALUTE, touches chin in THINK, raises high in CELEBRATE)
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

            }
        }
    }
}

private fun DrawScope.drawCharacterFace(
    w: Float, h: Float, stroke: Float, phase: Float, blinkCycle: Float,
    thinking: Float, happy: Float, alert: Float, greeting: Float, blushAlpha: Float
) {
    val unit = w
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
    drawMouth(w, h, stroke, phase, thinking, happy, alert)

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
        thinking = thinking, happy = happy, greeting = greeting,
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
    thinking: Float, happy: Float, greeting: Float, blinkCycle: Float, phase: Float,
    w: Float, h: Float, unit: Float, stroke: Float
) {
    val blink = (1f - abs(blinkCycle - 0.72f) / 0.025f).coerceIn(0f, 1f)
    val wink = greeting * (1f - abs(blinkCycle - 0.28f) / 0.055f).coerceIn(0f, 1f)
    for (index in 0..1) {
        val side = if (index == 0) -1f else 1f
        val eye = Offset(w * (0.5f + side * 0.065f), h * 0.151f)
        val radius = Size(w * 0.066f, h * 0.073f)
        val top = Offset(eye.x - radius.width, eye.y - radius.height)
        val bounds = Size(radius.width * 2, radius.height * 2)
        drawOval(BoingWhiteTop, top, bounds)
        drawOval(BoingInk, top, bounds, style = Stroke(stroke))
        val closure = maxOf(happy, blink, if (side > 0f) wink else 0f)
        val pupil = Offset(eye.x - side * w * 0.015f + w * (0.017f * thinking + 0.005f * sin(phase)),
            eye.y + h * (0.015f - 0.036f * thinking))
        scale(1f, maxOf(0.02f, 1f - closure), pivot = pupil) {
            drawCircle(BoingInk.copy(alpha = 1f - closure), unit * 0.025f, pupil)
            drawCircle(Color.White.copy(alpha = 1f - closure), unit * 0.008f, pupil - Offset(unit * 0.007f, unit * 0.008f))
        }
        if (closure > 0f) drawArc(BoingInk.copy(alpha = closure), 195f, 150f, false,
            Offset(eye.x - w * 0.041f, eye.y), Size(w * 0.082f, h * 0.038f),
            style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawMouth(w: Float, h: Float, stroke: Float, phase: Float, thinking: Float, happy: Float, alert: Float) {
    val cx = w * (0.5f + 0.025f * thinking)
    val half = w * (0.18f - 0.14f * thinking - 0.095f * alert + 0.018f * happy)
    val lip = h * (0.327f + 0.024f * thinking)
    val depth = h * (0.119f - 0.102f * thinking - 0.009f * alert + 0.025f * happy +
        0.006f * sin(phase * 2f) * (1f - thinking))
    val mouth = Path().apply {
        moveTo(cx - half, lip)
        quadraticTo(cx, lip + h * (0.039f - 0.064f * alert), cx + half, lip)
        cubicTo(cx + half * 0.86f, lip + depth * 1.3f, cx - half * 0.86f, lip + depth * 1.3f, cx - half, lip)
        close()
    }
    drawPath(mouth, BoingMouthDark)
    clipPath(mouth) {
        drawOval(BoingTongueLight, Offset(cx - half * 0.72f, lip + depth * 0.56f), Size(half * 1.48f, depth * 0.63f))
    }
    drawPath(mouth, BoingInk, style = Stroke(stroke, join = StrokeJoin.Round))
}

@Composable
fun DoraemonMiniHeadIcon(active: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.alpha(if (active) 1f else 0.65f)) {
        // Chat history uses a still portrait from the same character, without starting a frame clock per row.
        val w = size.minDimension / 0.81f
        val h = w / 0.84f
        withTransform({ translate(size.width * 0.5f - w * 0.5f, size.height * 0.5f - h * 0.295f) }) {
            drawCharacterFace(w, h, w * 0.017f, 0f, 0f, 0f, 0f, 0f, 0f, 0.18f)
        }
    }
}
