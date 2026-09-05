package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * texts in the app's language, not the phone's.
 *
 * `AppLocale.wrap` hung on `BigLauActivity` alone. everything outside an activity - notices,
 * the alarm, the sos default text - fetched its texts from the raw context, so in the system
 * language.
 *
 * that hits exactly the case this setting exists for: a phone whose system language somebody
 * else set. changing it would mean working through system settings in a language one cannot
 * read.
 */
class AppLanguageTest {

    private fun files(): List<File> =
        Quelltext.files()

    /**
     * inside an activity the language already hangs on the context (`attachBaseContext`),
     * and composables read through `stringResource`. the rest is checked.
     */
    private fun outsideAnActivity(file: File): Boolean =
        !file.name.endsWith("Activity.kt") && !file.path.contains("/ui/")

    @Test
    fun `texts outside an activity go through AppLocale`() {
        val raw = mutableListOf<String>()
        files().filter(::outsideAnActivity).forEach { file ->
            val content = file.readText()
            // which names stand for a wrapped context in this file.
            val wrapped = Regex("""val (\w+) = AppLocale\.forApp\(""")
                .findAll(content)
                .map { it.groupValues[1] }
                .toSet()
            file.readLines().forEachIndexed { index, line ->
                // **both** ways to a text, not only one: `getQuantityString` fetches a text
                // just as much and came out of the raw context just as much.
                val hit = Regex("""(\w+)?\.?(?:resources\.)?get(?:String|QuantityString)\(R\.(?:string|plurals)""")
                    .find(line)
                    ?: return@forEachIndexed
                val receiver = hit.groupValues[1]
                val throughAppLocale = receiver in wrapped || "AppLocale.forApp" in line
                if (!throughAppLocale) raw += "${file.name}:${index + 1}: ${line.trim()}"
            }
        }
        assertTrue(
            "these texts would come in the phone's language instead of the app's:\n" +
                raw.joinToString("\n"),
            raw.isEmpty(),
        )
    }
}
