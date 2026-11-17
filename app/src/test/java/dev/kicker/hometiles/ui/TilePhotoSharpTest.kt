package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a contact tile draws the large photo.
 *
 * tiles were drawn from the 96 px thumbnail and came out blurred (issue #14, emulator
 * 17.11.2026 with a 720 px probe photo).
 */
class TilePhotoSharpTest {

    private val tile = Quelltext.withoutComments("dev/kicker/hometiles/ui/BigTile.kt")

    @Test
    fun `the tile asks for the large copy`() {
        assertTrue("the tile draws the thumbnail as it was stored.", "ContactPhotoUri.large(photoUri)" in tile)
    }

    @Test
    fun `a contact without a large photo keeps its small one`() {
        val image = Quelltext.cut(tile, "AsyncImage(", "contentDescription")
        assertTrue(
            "without the fallback a contact that has only a thumbnail loses its photo.",
            "onError" in image && "else photoUri" in image,
        )
    }
}
