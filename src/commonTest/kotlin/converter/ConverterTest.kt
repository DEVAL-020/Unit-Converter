package converter

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConverterTest {

    private fun conv(category: String, from: String, to: String, value: Double): Double {
        val c = Catalog.category(category)
        return convert(value, c.unit(from), c.unit(to))
    }

    private fun assertClose(expected: Double, actual: Double, tolerance: Double = 1e-9) {
        val allowed = tolerance * maxOf(1.0, abs(expected))
        assertTrue(abs(expected - actual) <= allowed, "expected $expected but was $actual")
    }

    @Test
    fun lengthConversions() {
        assertClose(3.280839895013123, conv("length", "m", "ft", 1.0))
        assertClose(12.0, conv("length", "ft", "in", 1.0))
        assertClose(0.6213711922373339, conv("length", "km", "mi", 1.0))
        assertClose(1852.0, conv("length", "nmi", "m", 1.0))
    }

    @Test
    fun massVolumeAndArea() {
        assertClose(2.2046226218487757, conv("mass", "kg", "lb", 1.0))
        assertClose(3.785411784, conv("volume", "gal", "l", 1.0))
        assertClose(4.0, conv("volume", "qt", "cup", 1.0))
        assertClose(43560.0, conv("area", "ac", "ft2", 1.0), 1e-7)
    }

    @Test
    fun temperatureUsesFormulasNotFactors() {
        assertClose(212.0, conv("temperature", "c", "f", 100.0))
        assertClose(0.0, conv("temperature", "f", "c", 32.0))
        assertClose(273.15, conv("temperature", "c", "k", 0.0))
        assertClose(-459.67, conv("temperature", "k", "f", 0.0))
        assertClose(-40.0, conv("temperature", "c", "f", -40.0))
    }

    @Test
    fun speedTimeAndData() {
        assertClose(62.13711922373339, conv("speed", "kmh", "mph", 100.0))
        assertClose(86_400.0, conv("time", "d", "s", 1.0))
        assertClose(1024.0, conv("data", "gib", "mib", 1.0))
        assertClose(1000.0, conv("data", "gb", "mb", 1.0))
        assertClose(1.0, conv("data", "bit", "b", 8.0))
    }

    @Test
    fun convertingToTheSameUnitChangesNothing() {
        for (c in Catalog.categories) {
            for (u in c.units) assertEquals(42.5, convert(42.5, u, u))
        }
    }

    @Test
    fun everyPairOfUnitsRoundTrips() {
        for (c in Catalog.categories) {
            for (a in c.units) {
                for (b in c.units) {
                    val there = convert(25.0, a, b)
                    val back = convert(there, b, a)
                    assertClose(25.0, back, 1e-9)
                }
            }
        }
    }

    @Test
    fun catalogIsConsistent() {
        for (c in Catalog.categories) {
            val ids = c.units.map { it.id }
            assertEquals(ids.size, ids.toSet().size, "duplicate unit id in ${c.id}")
            assertTrue(c.defaultFrom in ids, "${c.id}: unknown default 'from' ${c.defaultFrom}")
            assertTrue(c.defaultTo in ids, "${c.id}: unknown default 'to' ${c.defaultTo}")
        }
        assertEquals(Catalog.categories.size, Catalog.categories.map { it.id }.toSet().size)
    }

    @Test
    fun parsesWhatPeopleType() {
        assertEquals(12.5, parseNumber("12.5"))
        assertEquals(1.5, parseNumber("1,5"))
        assertEquals(1000.0, parseNumber("1,000"))
        assertEquals(1234.5, parseNumber("1,234.5"))
        assertEquals(-3.0, parseNumber("-3"))
        assertEquals(100000.0, parseNumber("1e5"))
        assertEquals(2500.0, parseNumber(" 2 500 "))
    }

    @Test
    fun rejectsWhatIsNotANumber() {
        assertNull(parseNumber(""))
        assertNull(parseNumber("   "))
        assertNull(parseNumber("abc"))
        assertNull(parseNumber("-"))
        assertNull(parseNumber("12 apples"))
    }

    @Test
    fun formatsNumbersForDisplay() {
        assertEquals("3.2808399", formatNumber(3.280839895013123))
        assertEquals("1,234.5678", formatNumber(1234.5678))
        assertEquals("12", formatNumber(12.000000000000002))
        assertEquals("0.3", formatNumber(0.1 + 0.2))
        assertEquals("0", formatNumber(0.0))
        assertEquals("-2.5", formatNumber(-2.5))
        assertEquals("0.00001234", formatNumber(0.00001234))
        assertEquals("1,000,000", formatNumber(1_000_000.0))
        assertEquals("150,000,000,000", formatNumber(1.5e11))
    }

    @Test
    fun formatsExtremeNumbersInScientificNotation() {
        assertEquals("1e+15", formatNumber(1e15))
        assertEquals("1.234e-7", formatNumber(1.234e-7))
        assertEquals("-2.5e+20", formatNumber(-2.5e20))
    }

    @Test
    fun canSkipThousandsSeparators() {
        assertEquals("1234567.9", formatNumber(1234567.89, group = false))
    }

    @Test
    fun handlesNonFiniteNumbers() {
        assertEquals("—", formatNumber(Double.NaN))
        assertEquals("∞", formatNumber(Double.POSITIVE_INFINITY))
        assertEquals("-∞", formatNumber(Double.NEGATIVE_INFINITY))
    }
}
