package com.example.ui.util

object PhoneUtil {

    /**
     * Normalizes a phone string to digits:
     * - Strips non-digit characters.
     * - Strips leading 0 if followed by 10 digits (e.g. 09876543210 -> 919876543210).
     * - 10-digit Indian numbers get "91" prepended.
     * - Preserves existing country codes (e.g. 919876543210 or other international formats).
     * - Returns empty string for blank input.
     */
    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""

        return when {
            digits.length == 11 && digits.startsWith("0") -> "91" + digits.substring(1)
            digits.length == 10 -> "91$digits"
            digits.length == 12 && digits.startsWith("91") -> digits
            else -> digits
        }
    }

    /**
     * Returns normalized digits ready for wa.me/<number> without any leading + or whitespace.
     */
    fun toWhatsAppNumber(raw: String?): String {
        return normalize(raw)
    }

    /**
     * Formats normalized number for display in Indian format: +91 98765 43210
     */
    fun toDisplay(raw: String?): String {
        val norm = normalize(raw)
        if (norm.length == 12 && norm.startsWith("91")) {
            val cc = norm.substring(0, 2)
            val p1 = norm.substring(2, 7)
            val p2 = norm.substring(7)
            return "+$cc $p1 $p2"
        }
        if (norm.isNotBlank()) {
            return norm
        }
        return ""
    }
}
