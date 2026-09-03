package org.biglau.ui

import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.biglau.ui.theme.LocalCornerRadius
import org.biglau.ui.theme.BigSurface
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.LocalTextScale
import org.biglau.ui.theme.tileBorder

/**
 * Eine Zeile in einer Auswahlliste. Gleiche Sprache wie die Kachel - flaechig, ohne Schatten,
 * linksbuendig - nur waagerecht statt hochkant. Listen duerfen scrollen, der Homescreen nicht.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BigRow(
    label: String,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    icon: ImageVector? = null,
    iconBitmap: ImageBitmap? = null,
    /** Freier Platz vorn - genutzt fuer Kontaktfotos und Initialen. */
    leading: (@Composable () -> Unit)? = null,
    surface: org.biglau.ui.theme.BigSurface? = null,
    /**
     * Nur fuer die Schriftauswahl: dort steht jede Zeile in ihrer eigenen Schrift, damit
     * man den Unterschied sieht statt ihn zu lesen. Sonst gilt die Schrift des Themas.
     */
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    /** Nur fuer die Radius-Auswahl: dort zeigt jede Zeile ihre eigene Ecke. */
    cornerRadius: androidx.compose.ui.unit.Dp? = null,
    /**
     * Was beim Antippen geschieht - oder `null` fuer eine Zeile, die **nur etwas sagt**.
     *
     * Der Unterschied ist nicht nur Zierde: eine Zeile mit leerer Handlung (`onClick = {}`)
     * sieht aus wie ein Knopf, schluckt den Tipp still, und die Vorlesefunktion sagt sie
     * als „Schaltflaeche" an. Wer sich darauf verlaesst, tippt und wartet auf etwas, das
     * nie kommt. Ohne Handlung ist die Zeile weder anklickbar noch ein Knopf.
     */
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    /**
     * Zwei Zeilen als Vorgabe: erklaerende Zweitzeilen sind fast immer laenger als eine
     * Zeile bei 1,35-facher Systemschrift, und ein abgeschnittener Satz erklaert nichts.
     * Listen, in denen die Zweitzeile Daten traegt - eine Rufnummer, eine Vorschau -,
     * bleiben bei einer Zeile, damit die Zeilenhoehe gleich bleibt.
     */
    secondaryMaxLines: Int = 2,
    /** Eigener Rahmen statt des Themenrahmens - fuer Zeilen, deren Flaeche schon etwas sagt. */
    borderColor: Color? = null,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val haptik = LocalHapticFeedback.current
    val haptikStaerke = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.98f else 1f, label = "press")
    val paint = surface ?: palette.surfaceDefault
    val border = borderColor ?: palette.tileBorder()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(press)
            .clip(RoundedCornerShape(cornerRadius ?: LocalCornerRadius.current))
            .background(paint.fill)
            .then(if (border != null) Modifier.border(3.dp, border, RoundedCornerShape(cornerRadius ?: LocalCornerRadius.current)) else Modifier)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { haptik.tap(haptikStaerke); onClick() },
                        onLongClick = onLongClick?.let { echt ->
                            { haptik.longPress(haptikStaerke); echt() }
                        },
                    )
                },
            )
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            when {
                leading != null -> leading()
                iconBitmap != null -> Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(36.dp))
                icon != null -> Icon(icon, contentDescription = null, tint = paint.ink, modifier = Modifier.size(36.dp))
            }
        }
        // Die Randbedingungen gelten fuer beide Zeilen: Beschriftung und Zweitzeile teilen
        // sich die Hoehe, also muss beides an derselben Stelle gemessen werden.
        BoxWithConstraints(Modifier.weight(1f)) {
          val breite = constraints.maxWidth
          val maxHoehe = constraints.maxHeight
          Column {
            // Erst kleiner werden, dann trennen oder abschneiden. Bei 200 % stand in der
            // Liste "Nachrichte / n" und "Alles zurückset…" - beides an Zeilen, die man
            // antippt, um irgendwohin zu kommen. Gemessen wird das laengste Wort; daran
            // bricht die Zeile. Siehe BigHeading, dort dasselbe.
            val messer = rememberTextMeasurer()
            val grundstil = LocalTextStyle.current
            val stufen = labelLadder(22f * scale).map { groesse ->
                    // `fontFamily` ist meist null und heisst dann "die des Themas". Als
                    // Feld einer Kopie gesetzt heisst dasselbe null aber "keine" - dann
                    // waere in einer anderen Schrift gemessen worden als gezeichnet wird.
                    val stufe = grundstil.copy(
                        fontSize = groesse.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (fontFamily != null) stufe.copy(fontFamily = fontFamily) else stufe
                }
                val stil = remember(label, scale, breite, fontFamily) {
                    val wort = AnnotatedString(longestWord(label))
                    stufen.firstOrNull { messer.measure(wort, it).size.width <= breite }
                        ?: stufen.last()
                }
                // Drei Zeilen, wenn drei Zeilen Platz haben - gemessen, nicht geraten.
                // In Listen ist die Hoehe offen; auf dem Notrufbildschirm ist sie begrenzt,
                // aber reichlich (dort stand "Kontakte jetzt eintr…"); im Gespraech steht
                // sie fest bei 72 dp, und eine dritte Zeile waere abgeschnitten statt
                // gekuerzt - also schlechter als das Kuerzen.
                val darfWachsen = remember(label, stil, breite, maxHoehe) {
                    val hoch = messer.measure(
                        text = AnnotatedString(label),
                        style = stil,
                        maxLines = 3,
                        constraints = Constraints(maxWidth = breite),
                    ).size.height
                    hoch <= maxHoehe
                }
                // Dieselben Angaben wie vor der Stufenleiter, nur die Groesse kommt aus
                // ihr: ein ganzer TextStyle ersetzt die geerbte Schrift und veraendert
                // dabei Kleinigkeiten wie den Zeichenabstand - bei 100 % war die Zeile
                // danach ein paar Bildpunkte schmaler als vorher.
                Text(
                    text = label,
                    color = paint.ink,
                    fontSize = stil.fontSize,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (darfWachsen) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            if (secondary != null) {
                // Auch die Zweitzeile darf eine dritte bekommen, wenn Platz ist: die
                // Warnung vor einem verwaisten Ordner endete sonst mit "Das laesst sich
                // nicht rueck…", und ausgerechnet dieser Halbsatz ist der Grund, warum man
                // vorher nachdenkt. Zeilen, die Daten tragen (secondaryMaxLines = 1),
                // bleiben einzeilig - dort haelt die gleiche Zeilenhoehe die Liste ruhig.
                // Vom gezeichneten Stil aus, nicht frisch gebaut: sonst misst die Zeile
                // in der Standardschrift und zeichnet in der des Nutzers.
                val zweitStil = grundstil.copy(fontSize = (15f * scale).sp)
                val zweitZeilen = remember(secondary, scale, breite, maxHoehe) {
                    if (secondaryMaxLines < 2) {
                        secondaryMaxLines
                    } else {
                        val hoch = messer.measure(
                            text = AnnotatedString(secondary),
                            style = zweitStil,
                            maxLines = 3,
                            constraints = Constraints(maxWidth = breite),
                        ).size.height
                        // Die Beschriftung darueber braucht ihren Platz auch noch.
                        val labelHoch = messer.measure(
                            text = AnnotatedString(label),
                            style = stil,
                            maxLines = 3,
                            constraints = Constraints(maxWidth = breite),
                        ).size.height
                        if (hoch + labelHoch <= maxHoehe) 3 else secondaryMaxLines
                    }
                }
                Text(
                    text = secondary,
                    color = paint.ink,
                    fontSize = (15f * scale).sp,
                    maxLines = zweitZeilen,
                    overflow = TextOverflow.Ellipsis,
                )
            }
          }
        }
    }
}

/**
 * Quadratischer Knopf mit Symbol statt Wort.
 *
 * Nur für Nebensachen, die neben einer Überschrift Platz finden müssen. Auf drei Zoll und
 * bei 1,35-facher Systemschrift passen zwei beschriftete Knöpfe schlicht nicht nebeneinander;
 * dann ist ein ehrliches Symbol mit Vorlese-Beschreibung besser als ein Wort, das mitten im
 * Buchstaben abbricht.
 */
@Composable
fun BigIconButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    surface: BigSurface? = null,
    onClick: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val paint = surface ?: palette.surfaceDefault
    val border = palette.tileBorder()
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(LocalCornerRadius.current))
            .background(paint.fill)
            .then(if (border != null) Modifier.border(3.dp, border, RoundedCornerShape(LocalCornerRadius.current)) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = paint.ink, modifier = Modifier.size(32.dp))
    }
}

/** Ueberschrift ueber einem Abschnitt einer Liste. */
@Composable
fun BigHeading(text: String, modifier: Modifier = Modifier) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val messer = rememberTextMeasurer()
    // Erst kleiner werden, dann trennen: bei 200 % stand ueber der Ruecksetzen-Seite
    // „Alles zuruecksetze / n". Compose trennt ein Wort mitten hindurch, sobald es allein
    // nicht in die Zeile passt - und eine mitten im Wort getrennte Ueberschrift liest sich
    // wie ein Fehler. Gemessen wird das **laengste Wort**; daran bricht die Zeile.
    BoxWithConstraints(modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        val breite = constraints.maxWidth
        // Auf der Schrift des Themas aufgebaut, nicht auf der Vorgabe: mit der falschen
        // Schrift gemessen fiel die Ueberschrift eine Stufe zu klein aus - und sie wurde
        // auch in der falschen Schrift gezeichnet, weil `style` die geerbte ersetzt.
        val grundstil = LocalTextStyle.current
        val stufen = labelLadder(26f * scale).map { groesse ->
            groesse to grundstil.copy(fontSize = groesse.sp, fontWeight = FontWeight.Bold)
        }
        val stil = remember(text, scale, breite) {
            val wort = AnnotatedString(longestWord(text))
            stufen.firstOrNull { messer.measure(wort, it.second).size.width <= breite }?.second
                ?: stufen.last().second
        }
        Text(text = text, color = palette.onBackground, style = stil)
    }
}
