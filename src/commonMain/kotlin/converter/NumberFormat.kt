package converter

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.round

/**
 * Formats a number for display: at most [significant] significant digits, no trailing zeros,
 * optional thousands separators, and scientific notation for very large or very small values.
 *
 * Floating point noise like 12.000000000000002 disappears because of the rounding.
 * It is written by hand (no String.format, no toLocaleString) so the output is identical on the JVM
 * and in the browser, which keeps the unit tests honest.
 */
fun formatNumber(value: Double, significant: Int = 8, group: Boolean = true): String {
    if (value.isNaN()) return "—"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"

    val a = abs(value)
    if (a < 1e-300) return "0"

    val exp = magnitude(a)
    val body = if (exp >= 12 || exp < -6) {
        scientific(a, exp, significant)
    } else {
        plain(a, exp, significant, group)
    }
    return if (value < 0) "-$body" else body
}

private fun plain(a: Double, exp: Int, significant: Int, group: Boolean): String {
    val decimals = significant - 1 - exp
    val integerPart: String
    var fraction = ""

    if (decimals > 0) {
        // Scale up so the digits we keep become a whole number, then split it around the decimal point.
        val digits = round(a * pow10(decimals)).toLong().toString().padStart(decimals + 1, '0')
        integerPart = digits.dropLast(decimals)
        fraction = digits.takeLast(decimals).trimEnd('0')
    } else {
        // Big numbers: round to the nearest 10, 100, 1000... so only `significant` digits survive.
        val step = pow10(-decimals)
        integerPart = (round(a / step) * step).toLong().toString()
    }

    val whole = if (group) groupThousands(integerPart) else integerPart
    return if (fraction.isEmpty()) whole else "$whole.$fraction"
}

private fun scientific(a: Double, exp: Int, significant: Int): String {
    val mantissa = scaleToUnit(a, exp)
    val sign = if (exp >= 0) "+" else "-"
    return "${plain(mantissa, 0, significant, false)}e$sign${abs(exp)}"
}

/** The exponent e such that 10^e <= a < 10^(e+1), for a > 0. */
private fun magnitude(a: Double): Int {
    var e = floor(log10(a)).toInt()
    // log10 can be off by one right next to a power of ten, so check and nudge once.
    val scaled = scaleToUnit(a, e)
    if (scaled >= 10.0) e++ else if (scaled < 1.0) e--
    return e
}

private fun scaleToUnit(a: Double, e: Int): Double = if (e >= 0) a / pow10(e) else a * pow10(-e)

/** 10^n by repeated multiplication: exact for n up to 22 and identical on every platform. */
private fun pow10(n: Int): Double {
    var result = 1.0
    repeat(n) { result *= 10.0 }
    return result
}

private fun groupThousands(digits: String): String =
    digits.reversed().chunked(3).joinToString(",").reversed()
