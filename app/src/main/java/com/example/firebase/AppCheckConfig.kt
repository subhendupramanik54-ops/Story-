package com.example.firebase

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
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

enum class AppCheckState {
    DEBUG_CONFIGURED,
    RELEASE_RECAPTCHA,
    NOT_CONFIGURED,
    FAILED
}

object AppCheckStatusRegistry {
    @Volatile
    var state: AppCheckState = AppCheckState.NOT_CONFIGURED
        internal set

    @Volatile
    var detail: String = "Pending initialization"
        internal set
}

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // Synchronous commit() is mandatory so DebugAppCheckProviderFactory reads the secret immediately.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
                AppCheckStatusRegistry.detail = "Debug App Check token active"
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
                AppCheckStatusRegistry.detail = "Debug provider active (runtime token)"
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            AppCheckStatusRegistry.state = AppCheckState.DEBUG_CONFIGURED
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                context.packageName
            )
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    AppCheckStatusRegistry.state = AppCheckState.RELEASE_RECAPTCHA
                    AppCheckStatusRegistry.detail = "reCAPTCHA Enterprise attestation active"
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    AppCheckStatusRegistry.state = AppCheckState.FAILED
                    AppCheckStatusRegistry.detail = "recaptcha_site_key is blank"
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                AppCheckStatusRegistry.state = AppCheckState.FAILED
                AppCheckStatusRegistry.detail = "recaptcha_site_key resource missing"
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        AppCheckStatusRegistry.state = AppCheckState.FAILED
        AppCheckStatusRegistry.detail = e.localizedMessage ?: "App Check init error"
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
