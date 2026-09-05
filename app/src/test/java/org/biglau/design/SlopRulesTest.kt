package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the rules of PLAN.md 3.7, as far as they can be checked against the source.
 *
 * the plan puts the bans here as checkable rules rather than a matter of taste, because the
 * building goes on by machine: an unchecked rule is no rule twenty commits later.
 */
class SlopRulesTest {

    private val sources: List<File> = Quelltext.files()

    private fun withoutTheme(): List<File> = sources.filterNot { it.path.contains("ui/theme") }

    private fun violations(pattern: Regex, files: List<File> = sources): List<String> =
        files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if (pattern.containsMatchIn(line)) "${file.name}:${index + 1}: ${line.trim()}" else null
            }
        }

    @Test
    fun `colours come from the token system`() {
        // the emergency screen is the exception: it has to work without configuration and
        // without the palette, which may be exactly what is broken.
        val allowed = sources.filterNot {
            it.path.contains("ui/theme") || it.name == "EmergencyScreen.kt"
        }
        // the darkener under the contact photo is black in every theme: a token would
        // swallow the white label on it in the light theme.
        val darkenerLine = Regex("""1f to Color\.Black\.copy""")
        assertEquals(
            emptyList<String>(),
            violations(Regex("""Color\(0x|Color\.(Black|White|Red|Green|Blue|Gray|Yellow|Magenta|Cyan)"""), allowed)
                .filterNot { darkenerLine.containsMatchIn(it) },
        )
    }

    @Test
    fun `the app cannot reach the network at all`() {
        // without the INTERNET permission "BigLau sends nothing" is a fact, not a promise,
        // and the sim in the phone has a limited data allowance.
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertEquals(false, manifest.contains("android.permission.INTERNET"))
        assertEquals(false, manifest.contains("android.permission.ACCESS_NETWORK_STATE"))
    }

    @Test
    fun `no drop shadows`() {
        assertEquals(emptyList<String>(), violations(Regex("""\.shadow\(|shadowElevation|defaultElevation""")))
    }

    @Test
    fun `one single gradient, and it makes text readable`() {
        val found = violations(Regex("""Brush\.\w+Gradient"""))
        assertEquals("only the darkener under the contact photo", 1, found.size)
        assertEquals(true, found.single().startsWith("BigTile.kt"))
    }

    @Test
    fun `surfaces have exactly one corner radius`() {
        // round shapes stay allowed for indicators - dots, badges, bars, handles - which
        // write themselves as the percentage form `RoundedCornerShape(50)`. surfaces have
        // one value, and since PLAN.md 4.1 made it settable it lives in `LocalCornerRadius`:
        // a squared tile beside a rounded row was one edit away.
        val radii = sources.flatMap { file ->
            Regex("""RoundedCornerShape\((\d+)\.dp\)""").findAll(file.readText())
                .map { it.groupValues[1].toInt() }
        }.toSet()
        assertEquals(emptySet<Int>(), radii)
    }

    @Test
    fun `the one radius comes from one source`() {
        // the rule above would also hold if nobody rounded anything any more.
        val users = sources.count { it.readText().contains("LocalCornerRadius.current") }
        assertEquals(true, users >= 5)
    }

    @Test
    fun `spacings come from the scale`() {
        // spacings only, not sizes: an icon size of 36 dp is no spacing.
        val scale = setOf(0, 4, 8, 12, 16, 24, 32, 48)
        val pattern = Regex("""(?:padding|spacedBy)\(([^)]*)\)""")
        val number = Regex("""(\d+)\.dp""")
        val bad = sources.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val hits = pattern.findAll(line)
                    .flatMap { number.findAll(it.groupValues[1]) }
                    .map { it.groupValues[1].toInt() }
                    .filterNot { it in scale }
                    .toList()
                if (hits.isEmpty()) null else "${file.name}:${index + 1}: $hits"
            }
        }
        assertEquals(emptyList<String>(), bad)
    }

    @Test
    fun `no emoji as icons`() {
        // an emoji comes from the system's emoji font: a second typeface in a foreign colour
        // in the middle of our own surface.
        val emoji = Regex("""[\uD83C-\uDBFF][\uDC00-\uDFFF]|[\u26A0-\u27BF\u2B00-\u2BFF\uFE0F]""")
        assertEquals(emptyList<String>(), violations(emoji))
    }

    @Test
    fun `the texts do without emoji too`() {
        val emoji = Regex("""[\uD83C-\uDBFF][\uDC00-\uDFFF]|[\u26A0-\u27BF\u2B00-\u2BFF\uFE0F]""")
        val hits = listOf("values", "values-de").flatMap { directory ->
            Quelltext.texts(directory).flatMap { it.readLines() }
                .mapIndexedNotNull { index, line ->
                    if (emoji.containsMatchIn(line)) "$directory:${index + 1}" else null
                }
        }
        assertEquals(emptyList<String>(), hits)
    }

    @Test
    fun `no icon appears twice in one list`() {
        // an icon carries knowledge or it goes: the same house twice carries none and
        // invites a mix-up. checked per composable, since only there do the lines stand
        // under each other and get compared.
        val duplicates = mutableListOf<String>()
        sources.forEach { file ->
            var function = file.name
            val seen = mutableMapOf<String, Int>()
            file.readLines().forEach { line ->
                Regex("""(?:private )?fun (\w+)\(""").find(line)?.let {
                    function = "${file.name}#${it.groupValues[1]}"
                    seen.clear()
                }
                Regex("""icon = Icons\.(?:Filled|AutoMirrored\.Filled)\.(\w+)""").find(line)?.let {
                    val name = it.groupValues[1]
                    seen[name] = (seen[name] ?: 0) + 1
                    if (seen.getValue(name) == 2) duplicates += "$function: $name"
                }
            }
        }
        assertEquals(emptyList<String>(), duplicates)
    }

    @Test
    fun `only regular and bold`() {
        val weights = sources.flatMap { file ->
            Regex("""FontWeight\.(\w+)""").findAll(file.readText()).map { it.groupValues[1] }
        }.toSet()
        assertEquals(emptySet<String>(), weights - setOf("Normal", "Bold"))
    }
}

/**
 * the orientation stands in the configuration, not in the manifest.
 *
 * twelve activities carried `screenOrientation="portrait"` while PLAN.md 4.2 promises one
 * setting: a wired value is a dead field the other way round, a promised choice that is none.
 */
class ManifestOrientationTest {

    @Test
    fun `no activity clamps the orientation`() {
        val manifest = java.io.File("src/main/AndroidManifest.xml").readText()
        val hits = Regex("""screenOrientation="[^"]*"""").findAll(manifest).map { it.value }.toList()
        assertEquals(emptyList<String>(), hits)
    }
}

/**
 * a phone number looks the same everywhere.
 *
 * it stood in four places in three spellings, so comparing two lists meant comparing two
 * spellings instead of two numbers - and a number is exactly what nobody knows by heart.
 */
class PhoneNumberFormattingTest {

    private val sources: List<File> = Quelltext.files()

    @Test
    fun `no display shows a raw number`() {
        // the assignment itself, not what follows: line by line is too little because it
        // often spans two lines, a fixed window too much, since
        // `onClick = { onPick(number.number) }` is the value being passed on and belongs
        // unformatted.
        val hits = sources
            .filter { it.name.endsWith("Activity.kt") }
            .flatMap { file ->
                val lines = file.readLines()
                val nextAssignment = Regex("""^\s*(?:\w+ = |\)|\})""")
                lines.indices.mapNotNull { i ->
                    if (!Regex("""^\s*(?:secondary|label) = """).containsMatchIn(lines[i])) {
                        return@mapNotNull null
                    }
                    val block = StringBuilder(lines[i])
                    var j = i + 1
                    while (j < lines.size && !nextAssignment.containsMatchIn(lines[j])) {
                        block.append(' ').append(lines[j])
                        j++
                    }
                    val text = block.toString()
                    val showsNumber = Regex("""\.number\b""").containsMatchIn(text)
                    if (showsNumber && !text.contains("forDisplay")) {
                        "${file.name}:${i + 1}: ${lines[i].trim()}"
                    } else {
                        null
                    }
                }
            }
        assertEquals(emptyList<String>(), hits)
    }
}
