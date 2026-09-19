package converter

/**
 * One unit inside a [Category].
 *
 * Every unit knows how to reach and leave the category's base unit (meter, kilogram, degree Celsius...).
 * Converting A to B is then always: A -> base -> B. Linear units only need a factor;
 * temperature scales bring their own formulas.
 */
class UnitDef(
    val id: String,
    val name: String,
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
)

class Category(
    val id: String,
    val name: String,
    val defaultFrom: String,
    val defaultTo: String,
    val units: List<UnitDef>,
) {
    /** Finds a unit by id, falling back to the first unit if the id is unknown. */
    fun unit(id: String): UnitDef = units.firstOrNull { it.id == id } ?: units.first()
}

/** A unit whose size is [factor] base units, for example 1 foot = 0.3048 meters. */
private fun linear(id: String, name: String, symbol: String, factor: Double) =
    UnitDef(id, name, symbol, toBase = { it * factor }, fromBase = { it / factor })

/**
 * Everything the converter knows. To add a unit, add one `linear(...)` line to a category.
 * To add a category, add a new `Category(...)` to the list.
 */
object Catalog {
    val categories: List<Category> = listOf(
        Category(
            id = "length", name = "Length", defaultFrom = "m", defaultTo = "ft",
            units = listOf(
                linear("mm", "Millimeter", "mm", 0.001),
                linear("cm", "Centimeter", "cm", 0.01),
                linear("m", "Meter", "m", 1.0),
                linear("km", "Kilometer", "km", 1000.0),
                linear("in", "Inch", "in", 0.0254),
                linear("ft", "Foot", "ft", 0.3048),
                linear("yd", "Yard", "yd", 0.9144),
                linear("mi", "Mile", "mi", 1609.344),
                linear("nmi", "Nautical mile", "nmi", 1852.0),
            ),
        ),
        Category(
            id = "mass", name = "Mass", defaultFrom = "kg", defaultTo = "lb",
            units = listOf(
                linear("mg", "Milligram", "mg", 0.000001),
                linear("g", "Gram", "g", 0.001),
                linear("kg", "Kilogram", "kg", 1.0),
                linear("t", "Metric ton", "t", 1000.0),
                linear("oz", "Ounce", "oz", 0.028349523125),
                linear("lb", "Pound", "lb", 0.45359237),
                linear("st", "Stone", "st", 6.35029318),
            ),
        ),
        Category(
            id = "temperature", name = "Temperature", defaultFrom = "c", defaultTo = "f",
            units = listOf(
                UnitDef("c", "Celsius", "°C", toBase = { it }, fromBase = { it }),
                UnitDef(
                    "f", "Fahrenheit", "°F",
                    toBase = { (it - 32.0) * 5.0 / 9.0 },
                    fromBase = { it * 9.0 / 5.0 + 32.0 },
                ),
                UnitDef("k", "Kelvin", "K", toBase = { it - 273.15 }, fromBase = { it + 273.15 }),
            ),
        ),
        Category(
            id = "volume", name = "Volume", defaultFrom = "l", defaultTo = "gal",
            units = listOf(
                linear("ml", "Milliliter", "mL", 0.001),
                linear("l", "Liter", "L", 1.0),
                linear("m3", "Cubic meter", "m³", 1000.0),
                linear("tsp", "Teaspoon (US)", "tsp", 0.00492892159375),
                linear("tbsp", "Tablespoon (US)", "tbsp", 0.01478676478125),
                linear("floz", "Fluid ounce (US)", "fl oz", 0.0295735295625),
                linear("cup", "Cup (US)", "cup", 0.2365882365),
                linear("pt", "Pint (US)", "pt", 0.473176473),
                linear("qt", "Quart (US)", "qt", 0.946352946),
                linear("gal", "Gallon (US)", "gal", 3.785411784),
            ),
        ),
        Category(
            id = "area", name = "Area", defaultFrom = "m2", defaultTo = "ft2",
            units = listOf(
                linear("cm2", "Square centimeter", "cm²", 0.0001),
                linear("m2", "Square meter", "m²", 1.0),
                linear("km2", "Square kilometer", "km²", 1_000_000.0),
                linear("ha", "Hectare", "ha", 10_000.0),
                linear("in2", "Square inch", "in²", 0.00064516),
                linear("ft2", "Square foot", "ft²", 0.09290304),
                linear("yd2", "Square yard", "yd²", 0.83612736),
                linear("ac", "Acre", "ac", 4046.8564224),
                linear("mi2", "Square mile", "mi²", 2_589_988.110336),
            ),
        ),
        Category(
            id = "speed", name = "Speed", defaultFrom = "kmh", defaultTo = "mph",
            units = listOf(
                linear("ms", "Meter per second", "m/s", 1.0),
                linear("kmh", "Kilometer per hour", "km/h", 1000.0 / 3600.0),
                linear("mph", "Mile per hour", "mph", 0.44704),
                linear("kn", "Knot", "kn", 1852.0 / 3600.0),
                linear("fts", "Foot per second", "ft/s", 0.3048),
            ),
        ),
        Category(
            id = "time", name = "Time", defaultFrom = "h", defaultTo = "min",
            units = listOf(
                linear("ms", "Millisecond", "ms", 0.001),
                linear("s", "Second", "s", 1.0),
                linear("min", "Minute", "min", 60.0),
                linear("h", "Hour", "h", 3600.0),
                linear("d", "Day", "d", 86_400.0),
                linear("wk", "Week", "wk", 604_800.0),
                linear("yr", "Year (365 days)", "yr", 31_536_000.0),
            ),
        ),
        Category(
            id = "data", name = "Data", defaultFrom = "gb", defaultTo = "mb",
            units = listOf(
                linear("bit", "Bit", "bit", 0.125),
                linear("b", "Byte", "B", 1.0),
                linear("kb", "Kilobyte", "kB", 1_000.0),
                linear("mb", "Megabyte", "MB", 1_000_000.0),
                linear("gb", "Gigabyte", "GB", 1_000_000_000.0),
                linear("tb", "Terabyte", "TB", 1_000_000_000_000.0),
                linear("kib", "Kibibyte", "KiB", 1_024.0),
                linear("mib", "Mebibyte", "MiB", 1_048_576.0),
                linear("gib", "Gibibyte", "GiB", 1_073_741_824.0),
                linear("tib", "Tebibyte", "TiB", 1_099_511_627_776.0),
            ),
        ),
    )

    /** Finds a category by id, falling back to the first one if the id is unknown. */
    fun category(id: String): Category = categories.firstOrNull { it.id == id } ?: categories.first()
}
