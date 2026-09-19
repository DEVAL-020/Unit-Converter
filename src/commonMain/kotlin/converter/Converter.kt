package converter

/** Converts [value] from one unit to another of the same category. */
fun convert(value: Double, from: UnitDef, to: UnitDef): Double =
    if (from === to) value else to.fromBase(from.toBase(value))

private val thousandsGrouped = Regex("^-?\\d{1,3}(,\\d{3})+$")

/**
 * Turns what a person typed into a number, or null if it isn't one.
 *
 * Accepts "12.5", "12,5", "1,234.5", "1,000", "-3" and "1e5".
 * A lone comma is read as a thousands separator when it groups exactly three digits
 * ("1,000" is one thousand) and as a decimal comma otherwise ("1,5" is one and a half).
 */
fun parseNumber(text: String): Double? {
    var s = text.trim().replace(" ", "").replace("\u00A0", "")
    if (s.isEmpty()) return null

    if (s.contains(',')) {
        s = when {
            s.contains('.') -> s.replace(",", "")
            thousandsGrouped.matches(s) -> s.replace(",", "")
            else -> s.replace(',', '.')
        }
    }

    val number = s.toDoubleOrNull() ?: return null
    return if (number.isNaN() || number.isInfinite()) null else number
}
