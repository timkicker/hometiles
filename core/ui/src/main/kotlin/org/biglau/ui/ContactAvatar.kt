package org.biglau.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.biglau.ui.theme.LocalCornerRadius
import org.biglau.ui.theme.LocalBigPalette

/**
 * Kontaktbild. Ohne Foto die Initialen auf einer Palettenfarbe - abgeleitet aus dem Namen,
 * damit derselbe Kontakt immer dieselbe Farbe bekommt und wiedererkennbar bleibt.
 */
@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBigPalette.current
    val shape = RoundedCornerShape(LocalCornerRadius.current)

    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape),
        )
        return
    }

    val color = palette.tiles[colorIndexFor(name).mod(palette.tiles.size)]
    Box(
        modifier = modifier.size(size).clip(shape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            color = palette.onTile,
            fontSize = dpSp(size.value * 0.40f),
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Bis zu zwei Initialen aus dem ersten und letzten Namensteil.
 *
 * Gezaehlt werden nur Buchstaben und Ziffern: im echten Telefonbuch stehen Namen wie
 * "? (Wien) (Sus)", und daraus Initialen wie "?(" zu machen sieht nach Fehler aus.
 */
fun initialsOf(name: String): String {
    val parts = name.trim()
        .split(Regex("\\s+"))
        .mapNotNull { part -> part.firstOrNull { it.isLetterOrDigit() } }
    return when (parts.size) {
        0 -> "?"
        1 -> parts[0].uppercase()
        else -> "${parts.first()}${parts.last()}".uppercase()
    }
}

/**
 * Initialen fuer eine Kachel - oder `null`, wenn im Namen kein Buchstabe steht.
 *
 * Am Emulator gesehen: eine Kachel fuer eine Nummer ohne Kontakt heisst "055 501 00", und
 * daraus wurden die Initialen "00". Zwei Nullen sagen nichts und sehen nach Fehler aus. Auf
 * der Kachel ist die Alternative leerer Platz, und leer ist hier besser als falsch - anders
 * als im [ContactAvatar], wo ein Kaestchen ohne Inhalt schlimmer waere.
 */
fun tileInitials(name: String): String? =
    if (name.any { it.isLetter() }) initialsOf(name) else null

/** Stabile Farbe pro Name - nicht zufaellig, sonst springt sie bei jedem Neuzeichnen. */
fun colorIndexFor(name: String): Int {
    var hash = 0
    name.trim().lowercase().forEach { hash = hash * 31 + it.code }
    return hash
}
