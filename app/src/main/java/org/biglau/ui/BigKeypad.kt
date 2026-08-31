package org.biglau.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.biglau.R
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.tileBorder

/**
 * Zifferntastatur in der Formsprache der Kacheln. Die Tasten fuellen die verfuegbare Flaeche
 * aus, statt eine feste Groesse zu haben - auf drei Zoll ist jede ungenutzte Flaeche eine
 * verpasste Trefferflaeche.
 *
 * Wird spaeter die Grundlage der Waehltastatur.
 */
@Composable
fun BigKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Box(Modifier.weight(1f))
                        "⌫" -> KeypadKey(
                            Modifier.weight(1f),
                            onClick = onBackspace,
                            // Ohne Beschreibung ist die Taste fuer TalkBack stumm - und
                            // im Baum ueberhaupt nicht auffindbar.
                            description = stringResource(R.string.keypad_backspace),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = null,
                                tint = LocalBigPalette.current.onBackground,
                            )
                        }
                        else -> KeypadKey(
                            Modifier.weight(1f),
                            onClick = { onDigit(key[0]) },
                            description = key,
                        ) {
                            Text(
                                text = key,
                                color = LocalBigPalette.current.onBackground,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    modifier: Modifier,
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    val palette = LocalBigPalette.current
    val border = palette.tileBorder()
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.emptyTile)
            .then(if (border != null) Modifier.border(3.dp, border, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** Punktreihe fuer eine verdeckte Eingabe. */
@Composable
fun PinDots(length: Int, total: Int = 8, modifier: Modifier = Modifier) {
    val palette = LocalBigPalette.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (index < length) palette.accent else palette.emptyTile)
                    .border(2.dp, palette.onBackground.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .size(18.dp)
            )
        }
    }
}
