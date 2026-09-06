package dev.kicker.hometiles.ui

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
import dev.kicker.hometiles.ui.theme.LocalCornerRadius
import dev.kicker.hometiles.ui.theme.LocalBigPalette

/** contact picture; without a photo the initials on a palette colour derived from the name. */
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
 * up to two initials, from the first and last part of the name.
 *
 * letters and digits only: a real phone book holds names like "? (Wien) (Sus)".
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
 * initials for a tile, or `null` when the name holds no letter: a tile for the bare number
 * "055 501 00" produced the initials "00". empty is better than wrong here, unlike in
 * [ContactAvatar] where an empty box would be worse.
 */
fun tileInitials(name: String): String? =
    if (name.any { it.isLetter() }) initialsOf(name) else null

/** stable per name; a random one would jump on every redraw. */
fun colorIndexFor(name: String): Int {
    var hash = 0
    name.trim().lowercase().forEach { hash = hash * 31 + it.code }
    return hash
}
