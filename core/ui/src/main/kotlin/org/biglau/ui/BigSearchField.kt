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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
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

    // Ein Tipp irgendwo in die Zeile setzt die Schreibmarke.
    //
    // Am 04.09.2026 am Jelly 2 gemessen: die gezeichnete Zeile ist 64 dp hoch, das
    // Eingabefeld darin nur 48 - und mit der Trefferzahl darunter sogar 38. Ein Tipp auf
    // die oberen vierzehn Bildpunkte der Zeile tat gar nichts (`mInputShown` blieb
    // `false`), obwohl dort ein Feld gezeichnet ist. Die Hand, fuer die BigLau gebaut ist,
    // trifft den Rand regelmaessig.
    //
    // `pointerInput` und nicht `clickable`: eine anklickbare Zeile waere fuer die
    // Vorlesefunktion eine Schaltflaeche - siehe BigRow. Hier ist sie ein Eingabefeld.
    val schreibmarke = remember { FocusRequester() }
    val fokus = LocalFocusManager.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocalCornerRadius.current))
            .background(palette.emptyTile)
            .pointerInput(Unit) {
                detectTapGestures { schreibmarke.requestFocus() }
            }
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
                    .focusRequester(schreibmarke)
                    // Die Lupentaste, die der Text `search_matches` nennt. `ImeAction.Search`
                    // deckt nur die Eingabetaste ab; auf einem Telefon mit Tastatur gibt es
                    // die Lupe wirklich, und ein Hinweis auf eine Taste, die nichts tut, ist
                    // schlimmer als gar keiner. Am 04.09.2026 am Emulator gemessen.
                    .onPreviewKeyEvent { taste ->
                        if (taste.type != KeyEventType.KeyDown) {
                            false
                        } else {
                            when (taste.key) {
                                // Die Lupentaste, die der Text `search_matches` nennt.
                                // `ImeAction.Search` deckt nur die Eingabetaste ab; auf einem
                                // Telefon mit Tastatur gibt es die Lupe wirklich, und ein
                                // Hinweis auf eine Taste, die nichts tut, ist schlimmer als
                                // gar keiner.
                                Key.Search -> {
                                    onSearch?.invoke()
                                    true
                                }
                                // Nach unten gehoert der Liste. Ein `EditText` nimmt den
                                // Fokus und gibt ihn von selbst nicht weiter; am 04.09.2026
                                // in App-Liste und Kontakten gemessen, vier Druecke ohne
                                // Bewegung, und damit war die ganze Liste mit Tasten
                                // unerreichbar. Das Feld ist einzeilig, ein Druck nach unten
                                // kann darin keinen Text erreichen.
                                Key.DirectionDown -> {
                                    fokus.moveFocus(FocusDirection.Down)
                                    true
                                }
                                else -> false
                            }
                        }
                    }
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
