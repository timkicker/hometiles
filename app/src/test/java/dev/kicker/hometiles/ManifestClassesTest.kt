package dev.kicker.hometiles

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * every class named in the manifest exists.
 *
 * the manifest names activities, services and receivers as **strings**. moving or renaming a
 * class shows nothing while compiling: the source is fine, the manifest points at nothing,
 * and it would have shown up only on the phone - on tapping a tile, or not at all, because a
 * receiver simply stops doing anything.
 *
 * that nearly happened when `MessageReminderReceiver` moved from `notify` to `sms`: the alarm
 * for the repeated reminder would have pointed at a class no longer there under that name.
 */
class ManifestClassesTest {

    // the manifest exists once and only in :app; it moves with no module cut.
    private val manifest = java.io.File("src/main/AndroidManifest.xml")

    @Test
    fun `every class named in the manifest stands in the source`() {
        val named = Regex("""android:name="(\.[\w.]+)"""")
            .findAll(manifest.readText())
            .map { it.groupValues[1].removePrefix(".") }
            .toList()
        assertEquals("the manifest names not one class of ours any more - rule broken?", true, named.size >= 10)

        val present = Quelltext.files().map { it.nameWithoutExtension }.toSet()
        val sources = Quelltext.files().associate { it.nameWithoutExtension to it.readText() }
        val missing = named.filter { name ->
            val simple = name.substringAfterLast('.')
            // the file is usually named like the class; otherwise the class has to be
            // declared somewhere at least.
            simple !in present &&
                sources.values.none { Regex("""(class|object) $simple\b""").containsMatchIn(it) }
        }
        assertEquals("named in the manifest, not found in the source", emptyList<String>(), missing)
    }

    @Test
    fun `every class named in the manifest lies in the package given`() {
        val wrong = Regex("""android:name="\.([\w.]+)"""")
            .findAll(manifest.readText())
            .map { it.groupValues[1] }
            .filter { it.contains('.') }
            .mapNotNull { full ->
                val pkg = "dev.kicker.hometiles." + full.substringBeforeLast('.')
                val className = full.substringAfterLast('.')
                val file = Quelltext.files().firstOrNull { f ->
                    Regex("""(class|object) $className\b""").containsMatchIn(f.readText())
                } ?: return@mapNotNull "$full (not found)"
                val standsIn = Regex("""^package ([\w.]+)""", RegexOption.MULTILINE)
                    .find(file.readText())?.groupValues?.get(1)
                if (standsIn == pkg) null else "$full in truth stands in $standsIn"
            }.toList()
        assertEquals("manifest and package have drifted apart", emptyList<String>(), wrong)
    }
}
