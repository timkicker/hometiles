package org.biglau.ui

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onClick: () -> Unit,
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.98f else 1f, label = "press")
    val paint = surface ?: palette.surfaceDefault
    val border = borderColor ?: palette.tileBorder()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(press)
            .clip(RoundedCornerShape(12.dp))
            .background(paint.fill)
            .then(if (border != null) Modifier.border(3.dp, border, RoundedCornerShape(12.dp)) else Modifier)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
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
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                color = paint.ink,
                fontSize = (22f * scale).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    color = paint.ink,
                    fontSize = (15f * scale).sp,
                    maxLines = secondaryMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
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
            .clip(RoundedCornerShape(12.dp))
            .background(paint.fill)
            .then(if (border != null) Modifier.border(3.dp, border, RoundedCornerShape(12.dp)) else Modifier)
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
    Text(
        text = text,
        color = palette.onBackground,
        fontSize = (26f * scale).sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}
