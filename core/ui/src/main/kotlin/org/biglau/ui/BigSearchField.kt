package org.biglau.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.biglau.ui.theme.LocalCornerRadius
import org.biglau.core.ui.R
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.LocalTextScale
import org.biglau.ui.theme.tileBorder

/**
 * Suchfeld in der Formsprache der Kacheln: flaechig, ohne Schatten, grosse Schrift.
 * Bekommt bewusst keinen Fokus beim Oeffnen - auf drei Zoll frisst die Tastatur
 * sonst sofort die halbe Liste, bevor der Nutzer ueberhaupt geschaut hat.
 */
@Composable
fun BigSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    /** Zweite Zeile im Feld, etwa die Zahl der Treffer. */
    secondary: String? = null,
    /** Die Lupentaste der Tastatur. */
    onSearch: (() -> Unit)? = null,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val border = palette.tileBorder()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocalCornerRadius.current))
            .background(palette.emptyTile)
            .then(if (border != null) Modifier.border(3.dp, border, RoundedCornerShape(LocalCornerRadius.current)) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = palette.onBackground,
            modifier = Modifier.size(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Box(contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = hint,
                    color = palette.onBackground,
                    fontSize = (20f * scale).sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.onBackground,
                    fontSize = (20f * scale).sp,
                    fontWeight = FontWeight.Bold,
                ),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                // Ohne diesen Namen meldet ein Screenreader nur "Eingabefeld" - der
                // aufgemalte Platzhalter ist fuer ihn nicht das Gleiche wie eine Beschriftung.
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = hint },
            )
            }
            // Auf drei Zoll verdeckt die Tastatur die Trefferliste vollstaendig. Die Zahl
            // der Treffer steht deshalb im Feld selbst - der einzigen Zeile, die sichtbar
            // bleibt, waehrend man tippt.
            if (secondary != null) {
                Text(
                    text = secondary,
                    color = palette.onBackground,
                    fontSize = (14f * scale).sp,
                    // Zwei Zeilen: bei 1,35-facher Systemschrift passt der Hinweis zur
                    // Lupentaste sonst nicht, und abgeschnitten erklaert er nichts.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value.isNotEmpty()) {
            // Eigene Flaeche statt eines nackten Symbols: 48 dp ist das Mindestmass fuer
            // einen Fingertipp, und ein Knopf ohne Namen bleibt fuer TalkBack stumm.
            val clear = stringResource(R.string.search_clear)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(LocalCornerRadius.current))
                    .clickable { onValueChange("") }
                    .semantics { contentDescription = clear },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = palette.onBackground,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
