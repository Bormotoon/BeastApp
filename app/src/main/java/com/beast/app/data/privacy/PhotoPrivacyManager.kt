package com.beast.app.data.privacy

import android.content.Context
import java.security.MessageDigest

class PhotoPrivacyManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isPasscodeSet(): Boolean = prefs.contains(KEY_HASH)

    fun setPasscode(passcode: String) {
        prefs.edit()
            .putString(KEY_HASH, hash(passcode))
            .apply()
    }

    fun verifyPasscode(passcode: String): Boolean {
        val stored = prefs.getString(KEY_HASH, null) ?: return false
        return stored == hash(passcode)
    }

    fun clearPasscode() {
        prefs.edit()
            .remove(KEY_HASH)
            .remove(KEY_BIOMETRIC)
            .apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    private fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val encoded = digest.digest(input.toByteArray(Charsets.UTF_8))
        return encoded.joinToString(separator = "") { byte ->
            ((byte.toInt() and 0xFF) + 0x100).toString(16).substring(1)
        }
    }

    companion object {
        private const val PREF_NAME = "photo_privacy_prefs"
        private const val KEY_HASH = "passcode_hash"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
