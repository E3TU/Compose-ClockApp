package com.example.clockapp.ui

/**
 * HH:MM:SS digit-pad input, stock-clock style: digits fill the rightmost
 * unit first, each pair capped at 59 (hours at 99), max 6 digits.
 * The typed value is held as a digit string. Pure logic, unit-testable.
 */
object TimerInputLogic {
    fun append(current: String, digit: Int): String {
        val next = current + digit
        return if (isValid(next)) next else current
    }

    fun backspace(current: String): String = current.dropLast(1)

    fun clear(): String = ""

    fun isValid(digits: String): Boolean {
        if (digits.length > 6) return false
        return parts(digits).let { (hh, mm, ss) -> hh <= 99 && mm <= 59 && ss <= 59 }
    }

    fun totalSeconds(digits: String): Int {
        val (hh, mm, ss) = parts(digits)
        return hh * 3600 + mm * 60 + ss
    }

    fun format(digits: String): String = formatSeconds(totalSeconds(digits))

    fun formatSeconds(total: Int): String {
        val hh = total / 3600
        val mm = (total % 3600) / 60
        val ss = total % 60
        return "%02d:%02d:%02d".format(hh, mm, ss)
    }

    /** Digit string for a preset total, e.g. 60s -> "100" (1:00). */
    fun digitsFor(totalSeconds: Int): String {
        val raw = "%02d%02d%02d".format(
            totalSeconds / 3600,
            (totalSeconds % 3600) / 60,
            totalSeconds % 60
        )
        val trimmed = raw.trimStart('0')
        return trimmed.ifEmpty { "0" }
    }

    private fun parts(digits: String): Triple<Int, Int, Int> {
        val len = digits.length
        val ssStart = (len - 2).coerceAtLeast(0)
        val mmStart = (len - 4).coerceAtLeast(0)
        val ss = digits.substring(ssStart).padStart(2, '0').toInt()
        val mm = digits.substring(mmStart until ssStart).padStart(2, '0').toInt()
        val hh = digits.substring(0 until mmStart).padStart(2, '0').toInt()
        return Triple(hh, mm, ss)
    }
}
