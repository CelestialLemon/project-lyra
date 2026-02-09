package com.projectlyra.app.core

private val YearPattern = Regex("""\b(\d{4})\b""")

fun yearOrBlank(rawDate: String?): String {
    val normalized = rawDate?.trim().orEmpty()
    if (normalized.isEmpty()) {
        return ""
    }

    if (normalized.length >= 4) {
        val prefix = normalized.substring(0, 4)
        if (prefix.all { it.isDigit() }) {
            return prefix
        }
    }

    return YearPattern.find(normalized)?.groupValues?.get(1).orEmpty()
}
