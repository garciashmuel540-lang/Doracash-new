package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.DoraBellGold
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue
import kotlinx.coroutines.delay

/**
 * State-driven AnimatedVectorDrawable character renderer supporting
 * 'salute', 'think', and 'celebrate' (plus mapped advising/alert poses).
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun DoraemonAvdCharacter(
    gesture: DoraemonGesture,
    isAnimating: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tilt by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAnimating && gesture == DoraemonGesture.THINK) -6f else 0f,
        label = "Gesto de Doraemon"
    )
    Image(
        painter = androidx.compose.ui.res.painterResource(R.drawable.img_doraemon_mascot),
        contentDescription = "Doraemon",
        contentScale = ContentScale.Fit,
        modifier = modifier.rotate(tilt)
    )
}

/**
 * State-driven DoraemonAssistant overlay component with boolean visibility control,
 * animated vector gestures ('salute', 'think', 'celebrate'), thought bubble, and quick triggers.
 */
@Composable
fun DoraemonAssistant(
    visible: Boolean,
    gesture: DoraemonGesture,
    thoughtText: String? = null,
    isAlert: Boolean = gesture == DoraemonGesture.ALERT,
    isError: Boolean = false,
    onGestureSelect: ((DoraemonGesture) -> Unit)? = null,
    onBubbleClick: () -> Unit = {},
    onCharacterClick: () -> Unit = onBubbleClick,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(tween(260)) + scaleIn(
            initialScale = 0.78f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) +
            fadeOut(tween(220)) +
            scaleOut(targetScale = 0.82f),
        modifier = modifier
    ) {
        val borderColor by animateColorAsState(
            targetValue = when {
                isAlert || isError -> DoraCollarRed.copy(alpha = 0.72f)
                gesture == DoraemonGesture.CELEBRATE -> DoraBellGold.copy(alpha = 0.85f)
                else -> DoraSkyBlue.copy(alpha = 0.62f)
            },
            animationSpec = tween(300),
            label = "doraemon_assistant_border"
        )

        Column(horizontalAlignment = Alignment.Start) {
            if (!thoughtText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.8.dp, borderColor),
                    modifier = Modifier
                        .widthIn(min = 224.dp, max = 278.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        .clickable { onBubbleClick() }
                        .testTag("doraemon_thought_bubble")
                ) {
                    Column(
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isAlert || isError) {
                                        Icons.Filled.NotificationsActive
                                    } else {
                                        Icons.Filled.AutoAwesome
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isAlert || isError -> DoraCollarRed
                                        gesture == DoraemonGesture.CELEBRATE -> DoraBellGold
                                        else -> DoraSkyBlue
                                    },
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = when {
                                        isAlert || isError -> "Aviso importante"
                                        gesture == DoraemonGesture.CELEBRATE -> "¡Logro Dorayaki!"
                                        gesture == DoraemonGesture.THINK -> "Doraemon pensando…"
                                        gesture == DoraemonGesture.SALUTE -> "¡Hola desde el bolsillo!"
                                        else -> "Doraemon dice:"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAlert || isError) DoraCollarRed else DoraSkyBlue,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("doraemon_close_bubble_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Ocultar asistente Doraemon",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = thoughtText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) DoraCollarRed else MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (onGestureSelect != null && !isError) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                            ) {
                                GestureStatePill(
                                    label = "Saludar",
                                    selected = gesture == DoraemonGesture.SALUTE,
                                    accent = DoraSkyBlue,
                                    onClick = { onGestureSelect(DoraemonGesture.SALUTE) },
                                    modifier = Modifier.testTag("gesture_salute_chip")
                                )
                                GestureStatePill(
                                    label = "Pensar",
                                    selected = gesture == DoraemonGesture.THINK,
                                    accent = DoraSkyBlue,
                                    onClick = { onGestureSelect(DoraemonGesture.THINK) },
                                    modifier = Modifier.testTag("gesture_think_chip")
                                )
                                GestureStatePill(
                                    label = "Celebrar",
                                    selected = gesture == DoraemonGesture.CELEBRATE,
                                    accent = DoraMintGreen,
                                    onClick = { onGestureSelect(DoraemonGesture.CELEBRATE) },
                                    modifier = Modifier.testTag("gesture_celebrate_chip")
                                )
                            }
                        }

                        Text(
                            text = if (isError) "Toca para reintentar" else "Toca para hablar con Doraemon →",
                            style = MaterialTheme.typography.labelSmall,
                            color = DoraSkyBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Comic Thought Bubble trailing dots pointing down to Doraemon
                Box(
                    modifier = Modifier
                        .padding(start = 46.dp, top = 4.dp)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.4.dp, borderColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .padding(start = 38.dp, top = 3.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.2.dp, borderColor, CircleShape)
                )
            }

            // State-driven AnimatedVectorDrawable Doraemon Mascot
            DoraemonAvdCharacter(
                gesture = gesture,
                isAnimating = visible,
                modifier = Modifier
                    .size(106.dp)
                    .clickable { onCharacterClick() }
                    .testTag("doraemon_mascot_button")
            )
        }
    }
}

@Composable
private fun GestureStatePill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accent else Color.Transparent
        ),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
