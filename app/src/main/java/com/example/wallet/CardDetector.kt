package com.example.wallet

import com.example.data.BankCard
import com.example.data.CardColorTheme
import com.example.data.CardKind
import com.example.data.CardNetwork
import com.example.data.CurrencyCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DetectedCardMetadata(
    val network: CardNetwork? = null,
    val cardKind: CardKind? = null,
    val bankName: String? = null,
    val suggestedAlias: String? = null,
    val last4: String? = null,
    val suggestedCurrency: CurrencyCode? = null,
    val suggestedTheme: CardColorTheme? = null,
    val countryName: String? = null,
    val sourceLabel: String = ""
)

object CardDetector {

    private data class BankHint(
        val keywords: List<String>,
        val bankName: String,
        val defaultTheme: CardColorTheme,
        val defaultKind: CardKind? = null,
        val defaultNetwork: CardNetwork? = null
    )

    private val knownBanks = listOf(
        BankHint(listOf("bac", "credomatic"), "BAC Credomatic", CardColorTheme.CRIMSON),
        BankHint(listOf("lafise", "bancentro"), "Banco LAFISE", CardColorTheme.EMERALD),
        BankHint(listOf("banpro", "promerica"), "Banpro Promerica", CardColorTheme.EMERALD),
        BankHint(listOf("ficohsa"), "Banco Ficohsa", CardColorTheme.DORA_BLUE),
        BankHint(listOf("avanz"), "Banco Avanz", CardColorTheme.PURPLE),
        BankHint(listOf("bdf", "banco de finanzas"), "Banco de Finanzas (BDF)", CardColorTheme.GOLD),
        BankHint(listOf("nubank", "nu bank"), "Nubank", CardColorTheme.PURPLE),
        BankHint(listOf("bbva"), "BBVA", CardColorTheme.DORA_BLUE),
        BankHint(listOf("santander"), "Santander", CardColorTheme.CRIMSON),
        BankHint(listOf("scotiabank"), "Scotiabank", CardColorTheme.CRIMSON),
        BankHint(listOf("citibank", "citi"), "Citibank", CardColorTheme.OBSIDIAN),
        BankHint(listOf("chase", "jpmorgan"), "Chase", CardColorTheme.DORA_BLUE),
        BankHint(listOf("bank of america", "bofa"), "Bank of America", CardColorTheme.CRIMSON),
        BankHint(listOf("capital one"), "Capital One", CardColorTheme.OBSIDIAN),
        BankHint(listOf("wells fargo"), "Wells Fargo", CardColorTheme.CRIMSON),
        BankHint(listOf("hsbc"), "HSBC", CardColorTheme.CRIMSON),
        BankHint(listOf("mercado pago", "mercadopago"), "Mercado Pago", CardColorTheme.DORA_BLUE, defaultKind = CardKind.PREPAID),
        BankHint(listOf("zinli"), "Zinli", CardColorTheme.PURPLE, defaultKind = CardKind.PREPAID, defaultNetwork = CardNetwork.VISA),
        BankHint(listOf("wally"), "Wally Tech", CardColorTheme.DORA_BLUE, defaultKind = CardKind.PREPAID),
        BankHint(listOf("paypal"), "PayPal", CardColorTheme.DORA_BLUE, defaultKind = CardKind.VIRTUAL),
        BankHint(listOf("wise"), "Wise", CardColorTheme.EMERALD, defaultKind = CardKind.DEBIT, defaultNetwork = CardNetwork.VISA),
        BankHint(listOf("american express", "amex"), "American Express", CardColorTheme.GOLD, defaultNetwork = CardNetwork.AMEX)
    )

    fun detectNetworkFromDigits(rawDigits: String): CardNetwork? {
        val digits = rawDigits.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        if (digits.startsWith("4")) return CardNetwork.VISA
        if (digits.startsWith("34") || digits.startsWith("37")) return CardNetwork.AMEX

        val p2 = digits.take(2).toIntOrNull()
        val p4 = digits.take(4).toIntOrNull()
        if ((p2 != null && p2 in 51..55) || (p4 != null && p4 in 2221..2720)) {
            return CardNetwork.MASTERCARD
        }
        val p3 = digits.take(3).toIntOrNull()
        if (digits.startsWith("6011") || digits.startsWith("65") || (p3 != null && p3 in 644..649)) {
            return CardNetwork.OTHER
        }
        return null
    }

    fun detectFromText(text: String): DetectedCardMetadata {
        val lower = text.trim().lowercase()
        if (lower.isBlank()) return DetectedCardMetadata()

        val bankMatch = knownBanks.firstOrNull { hint ->
            hint.keywords.any { kw ->
                Regex("(^|\\b)${Regex.escape(kw)}(\\b|$)").containsMatchIn(lower)
            }
        }

        val kind = when {
            listOf("débito", "debito", "debit", "ahorro", "nómina", "nomina", "cuenta corriente")
                .any { lower.contains(it) } -> CardKind.DEBIT
            listOf("prepago", "prepaid", "recargable", "regalo", "gift")
                .any { lower.contains(it) } -> CardKind.PREPAID
            listOf("virtual", "digital", "desechable", "online")
                .any { lower.contains(it) } -> CardKind.VIRTUAL
            listOf("crédito", "credito", "credit", "cashback", "platinum", "infinite", "signature", "oro", "gold", "black", "millas")
                .any { lower.contains(it) } -> CardKind.CREDIT
            else -> bankMatch?.defaultKind
        }

        val net = when {
            lower.contains("visa") -> CardNetwork.VISA
            lower.contains("mastercard") || lower.contains("master card") -> CardNetwork.MASTERCARD
            lower.contains("amex") || lower.contains("american express") -> CardNetwork.AMEX
            else -> bankMatch?.defaultNetwork
        }

        val theme = when {
            lower.contains("black") || lower.contains("titanio") || lower.contains("obsidian") -> CardColorTheme.OBSIDIAN
            lower.contains("oro") || lower.contains("gold") -> CardColorTheme.GOLD
            lower.contains("purple") || lower.contains("morad") || lower.contains("nu") -> CardColorTheme.PURPLE
            kind == CardKind.DEBIT && bankMatch == null -> CardColorTheme.DORA_BLUE
            else -> bankMatch?.defaultTheme
        }

        return DetectedCardMetadata(
            network = net,
            cardKind = kind,
            bankName = bankMatch?.bankName,
            suggestedTheme = theme,
            sourceLabel = if (bankMatch != null || kind != null || net != null) "Detección inteligente" else ""
        )
    }

    fun fromPhysicalCard(pan: String): DetectedCardMetadata? {
        val digits = pan.filterNot { it == ' ' || it == '-' }
        if (digits.length !in 12..19 || digits.any { it !in '0'..'9' }) return null
        return DetectedCardMetadata(
            network = detectNetworkFromDigits(digits),
            last4 = digits.takeLast(4),
            sourceLabel = "Tarjeta física escaneada"
        )
    }

    fun parseBinlistJson(rawJson: String, binDigits: String): DetectedCardMetadata {
        val obj = JSONObject(rawJson)
        val scheme = obj.optString("scheme").trim().lowercase()
        val type = obj.optString("type").trim().lowercase()
        val brand = obj.optString("brand").trim()
        val bankObj = obj.optJSONObject("bank")
        val bankNameRaw = bankObj?.optString("name")?.trim().orEmpty()
        val countryObj = obj.optJSONObject("country")
        val countryName = countryObj?.optString("name")?.trim()?.takeIf { it.isNotEmpty() }
        val countryCurrency = countryObj?.optString("currency")?.trim()?.uppercase()

        val network = when {
            scheme.contains("visa") -> CardNetwork.VISA
            scheme.contains("master") -> CardNetwork.MASTERCARD
            scheme.contains("amex") || scheme.contains("american") -> CardNetwork.AMEX
            scheme.isNotBlank() -> CardNetwork.OTHER
            else -> detectNetworkFromDigits(binDigits)
        }

        val kind = when (type) {
            "credit" -> CardKind.CREDIT
            "debit" -> CardKind.DEBIT
            "prepaid" -> CardKind.PREPAID
            else -> if (obj.optBoolean("prepaid", false)) CardKind.PREPAID else null
        }

        val bankHint = detectFromText("$bankNameRaw $brand")
        val resolvedBank = bankHint.bankName ?: bankNameRaw.take(40).takeIf { it.isNotBlank() }
        val currency = when (countryCurrency) {
            "NIO" -> CurrencyCode.NIO
            "USD" -> CurrencyCode.USD
            else -> null
        }

        val aliasParts = listOfNotNull(
            resolvedBank?.take(25),
            brand.takeIf { it.isNotBlank() } ?: network?.label,
            kind?.label
        )

        return DetectedCardMetadata(
            network = network,
            cardKind = kind,
            bankName = resolvedBank,
            suggestedAlias = aliasParts.joinToString(" ").take(60).takeIf { it.isNotBlank() },
            suggestedCurrency = currency,
            suggestedTheme = bankHint.suggestedTheme ?: when (kind) {
                CardKind.DEBIT -> CardColorTheme.DORA_BLUE
                CardKind.PREPAID -> CardColorTheme.PURPLE
                CardKind.CREDIT -> CardColorTheme.GOLD
                else -> CardColorTheme.DORA_BLUE
            },
            countryName = countryName,
            sourceLabel = if (resolvedBank != null) "BIN verificado ($resolvedBank)" else "Red detectada por BIN"
        )
    }

    suspend fun lookupBin(rawDigitsOrPan: String): DetectedCardMetadata = withContext(Dispatchers.IO) {
        val digits = rawDigitsOrPan.filter { it.isDigit() }
        val last4 = if (digits.length >= 12) digits.takeLast(4) else null
        val bin = digits.take(8)
        val offlineNet = detectNetworkFromDigits(digits)

        if (bin.length < 6) {
            return@withContext DetectedCardMetadata(
                network = offlineNet,
                last4 = last4,
                sourceLabel = if (offlineNet != null) "Red detectada (${offlineNet.label})" else ""
            )
        }

        val onlineResult = queryBinlistApi(bin) ?: if (bin.length > 6) queryBinlistApi(bin.take(6)) else null
        if (onlineResult != null) {
            return@withContext onlineResult.copy(last4 = last4 ?: onlineResult.last4)
        }

        return@withContext DetectedCardMetadata(
            network = offlineNet,
            last4 = last4,
            sourceLabel = if (offlineNet != null) "Red detectada (${offlineNet.label}) · sin conexión BIN" else ""
        )
    }

    private fun queryBinlistApi(bin: String): DetectedCardMetadata? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://lookup.binlist.net/$bin")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept-Version", "3")
                setRequestProperty("User-Agent", "Doracash-Android/1.0")
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseBinlistJson(body, bin)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun isDuplicateCard(existing: List<BankCard>, candidate: DetectedCardMetadata): BankCard? {
        val l4 = candidate.last4 ?: return null
        return existing.firstOrNull {
            it.last4 == l4 && (candidate.network == null || it.networkEnum == candidate.network)
        }
    }
}
