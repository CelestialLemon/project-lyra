package com.projectlyra.app.data.settings

data class AppSettings(
    val apiKey: String = "",
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val includeApiKeyInBackup: Boolean = false,
)
