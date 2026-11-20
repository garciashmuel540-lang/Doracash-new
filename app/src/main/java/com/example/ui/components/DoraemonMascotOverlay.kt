package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.DoraemonChatMessage
import com.example.ai.DoraemonChatUiState
import com.example.ai.DoraemonTipUiState
import com.example.ui.theme.DoraBellGold
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue

@Composable
fun DoraemonImportantPopup(
    visible: Boolean,
    tipState: DoraemonTipUiState,
    defaultThoughtText: String,
    gestureOverride: DoraemonGesture?,
    onGestureSelect: (DoraemonGesture) -> Unit,
    onDismissNotice: () -> Unit,
    onRetryCheck: () -> Unit,
    onOpenAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetGesture = when {
        gestureOverride != null -> gestureOverride
        tipState is DoraemonTipUiState.Important -> tipState.gesture
        tipState is DoraemonTipUiState.Error -> DoraemonGesture.ALERT
        else -> DoraemonGesture.SALUTE
    }
    val noticeKey = when {
        gestureOverride != null -> "manual_${gestureOverride.name}"
        tipState is DoraemonTipUiState.Important -> tipState.alertKey
        tipState is DoraemonTipUiState.Error -> "error"
        else -> "manual_default"
    }

    // State-driven sequence: enter with SALUTE, then transition into target gesture unless overridden
    var activeGesture by remember { mutableStateOf(DoraemonGesture.SALUTE) }
    LaunchedEffect(visible, noticeKey, targetGesture, gestureOverride) {
        if (!visible) {
            activeGesture = DoraemonGesture.SALUTE
        } else if (gestureOverride != null) {
            activeGesture = gestureOverride
        } else {
            activeGesture = DoraemonGesture.SALUTE
            if (targetGesture != DoraemonGesture.SALUTE) {
                delay(if (targetGesture == DoraemonGesture.ALERT) 550L else 1000L)
                activeGesture = targetGesture
            }
        }
    }

    val bubbleText = when {
        gestureOverride == DoraemonGesture.THINK -> "Analizando tus cuentas, pagos y ahorros del mes…"
        gestureOverride == DoraemonGesture.CELEBRATE -> "¡Tu bolsillo avanza genial! Toca para ver ideas."
        tipState is DoraemonTipUiState.Important -> tipState.tip
        tipState is DoraemonTipUiState.Error -> tipState.message
        else -> defaultThoughtText
    }

    DoraemonAssistant(
        visible = visible,
        gesture = activeGesture,
        thoughtText = bubbleText,
        isAlert = tipState is DoraemonTipUiState.Important && tipState.gesture == DoraemonGesture.ALERT,
        isError = tipState is DoraemonTipUiState.Error,
        onGestureSelect = onGestureSelect,
        onBubbleClick = {
            if (tipState is DoraemonTipUiState.Error) onRetryCheck() else onOpenAssistant()
        },
        onCharacterClick = onOpenAssistant,
        onDismiss = onDismissNotice,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoraemonAssistantSheet(
    messages: List<DoraemonChatMessage>,
    chatState: DoraemonChatUiState,
    doraemonEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = true) { onDismiss() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    var currentSheetGesture by remember { mutableStateOf(DoraemonGesture.SALUTE) }
    var welcomePreviewGesture by remember { mutableStateOf(DoraemonGesture.SALUTE) }
    var hasPlayedIntro by remember { mutableStateOf(false) }

    LaunchedEffect(chatState, messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
        if (!hasPlayedIntro && chatState is DoraemonChatUiState.Idle && messages.isNotEmpty()) {
            currentSheetGesture = DoraemonGesture.SALUTE
            delay(850L)
        }
        hasPlayedIntro = true

        when (chatState) {
            is DoraemonChatUiState.Thinking -> {
                currentSheetGesture = DoraemonGesture.THINK
            }
            is DoraemonChatUiState.Error -> {
                currentSheetGesture = DoraemonGesture.ALERT
            }
            is DoraemonChatUiState.Idle -> {
                val lastBot = messages.lastOrNull { !it.isUser }
                if (lastBot == null) {
                    currentSheetGesture = DoraemonGesture.SALUTE
                } else {
                    val hasCompletedAction = lastBot.executedActions.any { !it.startsWith("No se pudo") }
                    if (hasCompletedAction && lastBot.gesture != DoraemonGesture.ALERT) {
                        currentSheetGesture = DoraemonGesture.CELEBRATE
                        if (lastBot.gesture != DoraemonGesture.CELEBRATE) {
                            delay(2200L)
                            currentSheetGesture = lastBot.gesture
                        }
                    } else {
                        currentSheetGesture = lastBot.gesture
                    }
                }
            }
        }
    }

    val quickPrompts = listOf(
        "¿Cómo van mis finanzas?",
        "Registra gasto de C$150 en comida",
        "¿Qué pagos vencen pronto?",
        "Crea una meta de ahorro de C$10,000",
        "Haz arqueo de efectivo"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Sheet Header with live animated Doraemon avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(DoraSkyBlue.copy(alpha = 0.14f))
                            .border(2.dp, DoraSkyBlue, CircleShape)
                    ) {
                        DoraemonAvdCharacter(
                            gesture = currentSheetGesture,
                            isAnimating = true,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.doraemon_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = DoraSkyBlue
                        )
                        AnimatedContent(
                            targetState = currentSheetGesture,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                            label = "gesture_status_label"
                        ) { g ->
                            val statusText = when (g) {
                                DoraemonGesture.SALUTE -> "Saludando · bolsillo listo"
                                DoraemonGesture.THINK -> "Pensando y analizando…"
                                DoraemonGesture.CELEBRATE -> "¡Celebrando contigo!"
                                DoraemonGesture.ALERT -> "¡Atención a tus finanzas!"
                                DoraemonGesture.ADVISING -> stringResource(R.string.doraemon_subtitle)
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (g) {
                                    DoraemonGesture.ALERT -> DoraCollarRed
                                    DoraemonGesture.CELEBRATE -> DoraMintGreen
                                    DoraemonGesture.THINK -> DoraSkyBlue
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onToggleEnabled(!doraemonEnabled) },
                        modifier = Modifier.testTag("sheet_toggle_mascot_button")
                    ) {
                        Text(
                            text = if (doraemonEnabled) "Avisos: ON" else "Avisos: OFF",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (doraemonEnabled) DoraMintGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = onClearChat,
                            modifier = Modifier.testTag("doraemon_clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Limpiar conversación",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Suggestion Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickPrompts) { prompt ->
                    AssistChip(
                        onClick = { onSendMessage(prompt) },
                        enabled = chatState !is DoraemonChatUiState.Thinking,
                        label = {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chat History
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, DoraSkyBlue.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                DoraemonAvdCharacter(
                                    gesture = welcomePreviewGesture,
                                    isAnimating = true,
                                    modifier = Modifier
                                        .size(132.dp)
                                        .clickable {
                                            welcomePreviewGesture = when (welcomePreviewGesture) {
                                                DoraemonGesture.SALUTE -> DoraemonGesture.THINK
                                                DoraemonGesture.THINK -> DoraemonGesture.CELEBRATE
                                                else -> DoraemonGesture.SALUTE
                                            }
                                        }
                                        .testTag("doraemon_welcome_mascot")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "¡Hola! Soy Doraemon, tu copiloto financiero",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Conozco tus cuentas, saldos, gastos, ahorros, arqueos y fechas de pago. Solo apareceré en tu pantalla cuando tenga un aviso importante, pero puedes hablarme aquí cuando quieras.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        DoraemonChatBubble(message = msg)
                    }
                }

                if (chatState is DoraemonChatUiState.Thinking) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = DoraSkyBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Buscando un gadget en el bolsillo…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (chatState is DoraemonChatUiState.Error) {
                    item {
                        Surface(
                            color = DoraCollarRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, DoraCollarRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = DoraCollarRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = chatState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DoraCollarRed
                                )
                            }
                        }
                    }
                }
            }

            // Input Composer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.doraemon_input_hint)) },
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (draft.isNotBlank() && chatState !is DoraemonChatUiState.Thinking) {
                                val toSend = draft
                                draft = ""
                                onSendMessage(toSend)
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("doraemon_chat_input")
                )
                FilledIconButton(
                    onClick = {
                        if (draft.isNotBlank() && chatState !is DoraemonChatUiState.Thinking) {
                            val toSend = draft
                            draft = ""
                            onSendMessage(toSend)
                        }
                    },
                    enabled = draft.isNotBlank() && chatState !is DoraemonChatUiState.Thinking,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = DoraSkyBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("doraemon_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.doraemon_send)
                    )
                }
            }
        }
    }
}

@Composable
private fun DoraemonChatBubble(message: DoraemonChatMessage) {
    Row(
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!message.isUser) {
            DoraemonMiniHeadIcon(
                active = true,
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Surface(
                color = if (message.isUser) DoraSkyBlue else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(
                    topStart = if (message.isUser) 20.dp else 6.dp,
                    topEnd = if (message.isUser) 6.dp else 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                ),
                tonalElevation = if (message.isUser) 0.dp else 2.dp
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            if (message.executedActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    message.executedActions.forEach { summary ->
                        Surface(
                            color = DoraMintGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DoraMintGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = DoraMintGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
