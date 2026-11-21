package com.example.ai

import kotlin.coroutines.cancellation.CancellationException

internal class ChatVerificationException(cause: Exception) : Exception("Falló la verificación de Doracash", cause)

internal fun isAppCheckRejection(error: Throwable): Boolean =
    generateSequence(error) { it.cause }.any {
        val message = it.message.orEmpty()
        message.contains("app check", ignoreCase = true) || message.contains("appcheck", ignoreCase = true)
    }

internal suspend fun <T> withVerifiedAppCheck(
    getToken: suspend (forceRefresh: Boolean) -> Unit,
    request: suspend () -> T
): T {
    suspend fun verify(force: Boolean) {
        try {
            getToken(force)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ChatVerificationException(e)
        }
    }

    verify(false)
    try {
        return request()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (!isAppCheckRejection(e)) throw e
    }
    verify(true)
    return request()
}
