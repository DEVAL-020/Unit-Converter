package app

import converter.Catalog
import converter.Category
import converter.UnitDef
import converter.convert
import converter.formatNumber
import converter.parseNumber
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement

private const val STATE_KEY = "uc.state"
private const val THEME_KEY = "uc.theme"

private inline fun <reified T : Element> byId(id: String): T =
    document.getElementById(id) as? T ?: error("Missing element #$id in index.html")

fun main() {
    ConverterPage().start()
}

/** Connects the static markup in index.html to the conversion logic in the shared `converter` package. */
private class ConverterPage {
    private val tabs = byId<HTMLElement>("categories")
    private val fromInput = byId<HTMLInputElement>("from-value")
    private val fromSelect = byId<HTMLSelectElement>("from-unit")
    private val toOutput = byId<HTMLElement>("to-value")
    private val toSelect = byId<HTMLSelectElement>("to-unit")
    private val swapButton = byId<HTMLButtonElement>("swap")
    private val copyButton = byId<HTMLButtonElement>("copy")
    private val themeButton = byId<HTMLButtonElement>("theme")
    private val rateLine = byId<HTMLElement>("rate")
    private val hint = byId<HTMLElement>("hint")
    private val allTitle = byId<HTMLElement>("all-title")
    private val allList = byId<HTMLElement>("all-list")

    private val tabButtons = LinkedHashMap<String, HTMLButtonElement>()

    private var category: Category = Catalog.categories.first()
    private var from: UnitDef = category.unit(category.defaultFrom)
    private var to: UnitDef = category.unit(category.defaultTo)

    fun start() {
        buildTabs()
        restoreState()
        fillSelect(fromSelect, category.units, from)
        fillSelect(toSelect, category.units, to)
        updateTabs()
        updateThemeLabel()
        wireEvents()
        refresh()
    }

    // ---- Setup ---------------------------------------------------------------------------------

    private fun buildTabs() {
        for (c in Catalog.categories) {
            val button = document.createElement("button") as HTMLButtonElement
            button.type = "button"
            button.className = "tab"
            button.textContent = c.name
            button.addEventListener("click", { selectCategory(c) })
            tabs.appendChild(button)
            tabButtons[c.id] = button
        }
    }

    private fun wireEvents() {
        fromInput.addEventListener("input", { refresh() })
        fromSelect.addEventListener("change", {
            from = category.unit(fromSelect.value)
            refresh()
        })
        toSelect.addEventListener("change", {
            to = category.unit(toSelect.value)
            refresh()
        })
        swapButton.addEventListener("click", { swap() })
        copyButton.addEventListener("click", { copyResult() })
        themeButton.addEventListener("click", { toggleTheme() })
    }

    private fun fillSelect(select: HTMLSelectElement, units: List<UnitDef>, selected: UnitDef) {
        select.textContent = ""
        for (u in units) {
            val option = document.createElement("option") as HTMLOptionElement
            option.value = u.id
            option.textContent = "${u.name} (${u.symbol})"
            select.appendChild(option)
        }
        select.value = selected.id
    }

    // ---- Actions -------------------------------------------------------------------------------

    private fun selectCategory(c: Category) {
        category = c
        from = c.unit(c.defaultFrom)
        to = c.unit(c.defaultTo)
        fillSelect(fromSelect, c.units, from)
        fillSelect(toSelect, c.units, to)
        updateTabs()
        tabButtons[c.id]?.asDynamic()?.scrollIntoView(js("({ inline: 'nearest', block: 'nearest' })"))
        refresh()
    }

    /** Swaps the two units and moves the result into the input, so both sides trade places. */
    private fun swap() {
        val result = parseNumber(fromInput.value)?.let { convert(it, from, to) }
        val previousFrom = from
        from = to
        to = previousFrom
        fromSelect.value = from.id
        toSelect.value = to.id
        if (result != null) fromInput.value = formatNumber(result, group = false)
        swapButton.classList.toggle("turned")
        refresh()
    }

    private fun copyResult() {
        val value = parseNumber(fromInput.value) ?: return
        val text = formatNumber(convert(value, from, to), group = false)
        try {
            val promise = window.navigator.asDynamic().clipboard.writeText(text)
            promise.then({ flashCopyLabel("Copied") }, { flashCopyLabel("Copy failed") })
        } catch (e: Throwable) {
            flashCopyLabel("Copy failed")
        }
    }

    private fun flashCopyLabel(label: String) {
        copyButton.textContent = label
        window.setTimeout({ copyButton.textContent = "Copy result" }, 1600)
    }

    private fun toggleTheme() {
        val root = document.documentElement ?: return
        val next = if (root.getAttribute("data-theme") == "dark") "light" else "dark"
        root.setAttribute("data-theme", next)
        try {
            localStorage.setItem(THEME_KEY, next)
        } catch (e: Throwable) {
            // Storage can be blocked (private mode); the theme still switches for this visit.
        }
        updateThemeLabel()
    }

    // ---- Rendering -----------------------------------------------------------------------------

    private fun refresh() {
        val raw = fromInput.value
        val value = parseNumber(raw)
        val invalid = raw.isNotBlank() && value == null
        fromInput.setAttribute("aria-invalid", invalid.toString())
        hint.hidden = !invalid

        val result = if (value != null) convert(value, from, to) else null
        val shown = if (result != null) formatNumber(result) else "—"
        toOutput.textContent = shown
        setSize(toOutput, shown)
        setSize(fromInput, raw)

        rateLine.textContent = "1 ${from.symbol} = ${formatNumber(convert(1.0, from, to))} ${to.symbol}"
        copyButton.disabled = result == null

        renderAllUnits(value)
        saveState()
    }

    private fun renderAllUnits(value: Double?) {
        allTitle.textContent =
            if (value != null) "${formatNumber(value)} ${from.symbol} in other units"
            else "Other ${category.name.lowercase()} units"

        allList.textContent = ""
        for (u in category.units) {
            if (u === from) continue
            val item = document.createElement("li") as HTMLElement
            if (u === to) item.setAttribute("aria-current", "true")

            val name = document.createElement("span") as HTMLElement
            name.className = "unit-name"
            name.textContent = u.name

            val amount = if (value != null) formatNumber(convert(value, from, u)) else "—"
            val number = document.createElement("span") as HTMLElement
            number.className = "unit-value"
            number.textContent = "$amount ${u.symbol}"

            item.appendChild(name)
            item.appendChild(number)
            allList.appendChild(item)
        }
    }

    private fun updateTabs() {
        tabButtons.forEach { (id, button) ->
            button.setAttribute("aria-pressed", (id == category.id).toString())
        }
    }

    private fun updateThemeLabel() {
        val dark = document.documentElement?.getAttribute("data-theme") == "dark"
        themeButton.setAttribute("aria-label", if (dark) "Switch to light theme" else "Switch to dark theme")
    }

    /** Long numbers get a smaller type size (see style.css) so they never overflow the panel. */
    private fun setSize(target: Element, text: String) {
        val size = when {
            text.length > 16 -> "xs"
            text.length > 12 -> "s"
            text.length > 9 -> "m"
            else -> "l"
        }
        target.setAttribute("data-size", size)
    }

    // ---- Remembering the last conversion --------------------------------------------------------

    private fun saveState() {
        try {
            localStorage.setItem(STATE_KEY, listOf(category.id, from.id, to.id, fromInput.value).joinToString("|"))
        } catch (e: Throwable) {
            // Not being able to remember is fine.
        }
    }

    private fun restoreState() {
        val raw = try {
            localStorage.getItem(STATE_KEY)
        } catch (e: Throwable) {
            null
        }
        if (raw == null) return

        val parts = raw.split("|", limit = 4)
        if (parts.size < 4) return
        val saved = Catalog.categories.firstOrNull { it.id == parts[0] } ?: return

        category = saved
        from = saved.unit(parts[1])
        to = saved.unit(parts[2])
        fromInput.value = parts[3]
    }
}
