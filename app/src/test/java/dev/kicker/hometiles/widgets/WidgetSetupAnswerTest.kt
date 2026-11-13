package dev.kicker.hometiles.widgets

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a widget's setup screen answers through onActivityResult, nowhere else (issue #9).
 *
 * the host starts it with an IntentSender of the system and the request code it was given.
 * a compose result launcher has its own codes and never hears that answer: until 0.1.13 the
 * editor waited for it there, and every widget with a setup step (mail, door entry) stayed
 * in the picker while widgets without one worked.
 */
class WidgetSetupAnswerTest {

    @Test
    fun `whoever starts a widget setup also takes its answer`() {
        val starters = Quelltext.files().filter { "startConfiguration(" in it.readText() }
            .filterNot { it.name == "WidgetHostController.kt" }
        assertTrue(Quelltext.atLeast(starters, 1, "callers of startConfiguration").isNotEmpty())
        val deaf = starters.filterNot { file ->
            val text = file.readText()
            Regex("""override fun onActivityResult\(""").containsMatchIn(text) &&
                Regex("""requestCode == WidgetHostController\.REQUEST_CONFIGURE""").containsMatchIn(text)
        }.map { it.name }
        assertEquals(
            "starts a widget setup and never reads its answer in onActivityResult; the widget " +
                "would stay in the picker.",
            emptyList<String>(),
            deaf,
        )
    }
}
