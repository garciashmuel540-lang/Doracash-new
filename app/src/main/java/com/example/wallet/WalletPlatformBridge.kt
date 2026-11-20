package com.example.wallet

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings

data class DeviceWalletStatus(
    val hasNfcHardware: Boolean,
    val isNfcEnabled: Boolean,
    val hasHceTapToPay: Boolean,
    val isGoogleWalletInstalled: Boolean,
    val isGooglePayInstalled: Boolean,
    val isDeviceSecure: Boolean
) {
    val readyForTapToPay: Boolean
        get() = hasNfcHardware && isNfcEnabled && hasHceTapToPay &&
            (isGoogleWalletInstalled || isGooglePayInstalled)
}

object WalletPlatformBridge {
    private const val GOOGLE_WALLET_PACKAGE = "com.google.android.apps.walletnfcrel"
    private const val GOOGLE_PAY_PACKAGE = "com.google.android.apps.nbu.paisa.user"
    private const val GOOGLE_PAY_WEB_METHODS = "https://pay.google.com/gp/w/u/0/home/paymentmethods"

    fun inspectDeviceWalletStatus(context: Context): DeviceWalletStatus {
        val pm = context.packageManager
        val nfcAdapter = runCatching { NfcAdapter.getDefaultAdapter(context) }.getOrNull()
        val hasNfc = nfcAdapter != null || pm.hasSystemFeature(PackageManager.FEATURE_NFC)
        val nfcEnabled = nfcAdapter?.isEnabled == true
        val hasHce = pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
        val walletInstalled = isPackageInstalled(pm, GOOGLE_WALLET_PACKAGE)
        val gpayInstalled = isPackageInstalled(pm, GOOGLE_PAY_PACKAGE)
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val deviceSecure = keyguard?.isDeviceSecure == true

        return DeviceWalletStatus(
            hasNfcHardware = hasNfc,
            isNfcEnabled = nfcEnabled,
            hasHceTapToPay = hasHce,
            isGoogleWalletInstalled = walletInstalled,
            isGooglePayInstalled = gpayInstalled,
            isDeviceSecure = deviceSecure
        )
    }

    fun openGoogleWallet(context: Context): Result<String> {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(GOOGLE_WALLET_PACKAGE)
            ?: pm.getLaunchIntentForPackage(GOOGLE_PAY_PACKAGE)

        if (launchIntent != null) {
            return runCatching {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                "Abriendo Google Wallet"
            }
        }

        val playStoreIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_WALLET_PACKAGE")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(playStoreIntent)
            "Google Wallet no instalado; abriendo Google Play"
        }
    }

    fun openGooglePayMethods(context: Context): Result<String> {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PAY_WEB_METHODS)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(webIntent)
            "Abriendo métodos de pago de Google Pay"
        }
    }

    fun openNfcPaymentSettings(context: Context): Result<String> {
        val primary = Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallback = Intent(Settings.ACTION_NFC_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val generalFallback = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            context.startActivity(primary)
            "Abriendo pagos sin contacto NFC"
        }.recoverCatching {
            context.startActivity(fallback)
            "Abriendo configuración NFC"
        }.recoverCatching {
            context.startActivity(generalFallback)
            "Abriendo ajustes del dispositivo"
        }
    }

    @Suppress("DEPRECATION")
    fun createDeviceUnlockIntent(context: Context, title: String, description: String): Intent? {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return null
        if (!keyguard.isDeviceSecure) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            keyguard.createConfirmDeviceCredentialIntent(title, description)
        } else {
            keyguard.createConfirmDeviceCredentialIntent(title, description)
        }
    }

    @Suppress("DEPRECATION")
    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
