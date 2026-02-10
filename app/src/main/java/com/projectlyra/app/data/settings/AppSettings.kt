package com.projectlyra.app.data.settings

import android.os.Build

data class AppSettings(
    val apiKey: String = "",
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val includeApiKeyInBackup: Boolean = false,
    val dynamicAccentEnabled: Boolean = dynamicAccentSupported,
) {
    companion object {
        val dynamicAccentSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}
