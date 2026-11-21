package com.example.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"

private var providerInstalled = false

@Synchronized
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        // AI Studio can explicitly opt into its registered debug token. APKs shared
        // between phones always use the Android provider configured for this app.
        val debugToken = if (BuildConfig.DEBUG) intent?.getStringExtra(DEBUG_TOKEN_EXTRA) else null
        if (providerInstalled && debugToken.isNullOrBlank()) return
        if (!debugToken.isNullOrBlank()) {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val prefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            // Must commit synchronously before installing DebugAppCheckProviderFactory.
            prefs.edit()
                .putString(DEBUG_SECRET_KEY, debugToken)
                .commit()
            Log.d(TAG, "App Check debug secret configured successfully.")
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val siteKey = context.getString(com.example.R.string.recaptcha_site_key)
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                RecaptchaAppCheckProviderFactory.getInstance(siteKey), true
            )
        }
        providerInstalled = true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
