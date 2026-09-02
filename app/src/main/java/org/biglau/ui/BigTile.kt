package org.biglau.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalContext
import org.biglau.data.LabelPosition
import org.biglau.notify.NotificationCounts
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.data.IconVisibility
import org.biglau.ui.theme.LocalIconPercent
import org.biglau.ui.theme.LocalHideCutLabels
import org.biglau.ui.theme.LocalIconVisibility
import org.biglau.ui.theme.LocalLabelScale
import org.biglau.ui.theme.LocalTextScale
import org.biglau.ui.theme.tileBorder
import org.biglau.R
import org.biglau.a11y.TileSpeech

/**
 * Eine Kachel im Schild-Entwurf (PLAN.md 3.0): vollflaechige Farbe bis an die Kante,
 * Icon gross oben links, Beschriftung in einer Zone fester Hoehe unten links.
 *
 * Die feste Labelzone ist der Punkt: dadurch stehen die Beschriftungen einer Rasterzeile
 * auf einer gemeinsamen Grundlinie, egal ob ein- oder zweizeilig.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BigTile(
    label: String,
    background: Color,
    modifier: Modifier = Modifier,
    cellHeight: Dp,
    cellWidth: Dp = cellHeight,
    icon: ImageVector? = null,
    iconBitmap: ImageBitmap? = null,
    /** Kontaktfoto, formatfuellend hinter der Beschriftung. */
    photoUri: String? = null,
    labelPosition: LabelPosition = LabelPosition.BOTTOM_LEFT,
    cornerRadius: Dp = 12.dp,
    /** Ueberschreibt den Themenrahmen - genutzt fuer die leere Kachel. */
    borderOverride: Color? = null,
    /** Anzahl wartender Benachrichtigungen; groesser null laesst die Kachel blinken. */
    badgeCount: Int = 0,
    /** Ersetzt Icon und Beschriftung - genutzt von Uhr und Batterie. */
    content: (@Composable () -> Unit)? = null,
    /**
     * Eigene Zeichnung **an der Stelle des Symbols**, mit Beschriftungszone wie sonst auch.
     * Anders als [content], das die ganze Kachel uebernimmt und damit auch die Beschriftung
     * verschluckt - der Ordner braucht beides, sein Inhalt oben und sein Name unten.
     */
    iconContent: (@Composable () -> Unit)? = null,
    /**
     * Initialen statt eines Symbols. `PLAN.md` 3.4 sagt sie fuer Kontaktkacheln ohne Foto
     * zu: "ohne Foto die Initialen auf der Kachelfarbe". Sie stehen hier und nicht beim
     * Aufrufer, weil nur hier die Symbolgroesse ausgerechnet ist - und weil sie derselben
     * Regel folgen sollen: ist kein Platz fuer ein Symbol, ist auch keiner fuer Buchstaben.
     */
    initials: String? = null,
    contentDescription: String = label,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    // Der Zaehler an der Ecke ist gezeichnet und traegt keinen Text. Ohne diese Zeile
    // hoert ein Screenreader "Nachrichten" und nicht, dass fuenf davon warten - siehe
    // TileSpeech und PLAN.md 3.6. Hier und nicht bei den Aufrufern, damit keine Kachel
    // vergessen wird.
    val gesprochen = TileSpeech.describe(
        label = contentDescription,
        badge = if (badgeCount > 0) {
            pluralStringResource(R.plurals.a11y_badge, badgeCount, badgeCount)
        } else {
            null
        },
    )
    val palette = LocalBigPalette.current
    val haptik = LocalHapticFeedback.current
    val haptikStaerke = LocalHaptics.current
    val textScale = LocalTextScale.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")

    val labelSp = labelSizeSp(cellWidth.value, cellHeight.value, textScale, LocalLabelScale.current)
    // PLAN.md 3.2: die Beschriftung kann weichen, wenn sie ohnehin abgeschnitten wuerde.
    // Faellt sie weg, gehoert ihr Platz dem Symbol - sonst bliebe ein Streifen Nichts.
    //
    // Gemessen, nicht geschaetzt: eine Rechnung mit mittlerer Zeichenbreite lag daneben
    // ("Nachrichten" waere ausgeblendet worden, obwohl es passt). Der TextMeasurer misst
    // vor dem Zeichnen, es blitzt also nichts auf.
    val messer = rememberTextMeasurer()
    val dichte = LocalDensity.current
    val labelStil = TextStyle(
        fontSize = dpSp(labelSp),
        lineHeight = dpSp(labelSp * 1.1f),
        fontWeight = FontWeight.Bold,
    )
    val zoneDp = labelZoneDp(cellHeight.value, labelSp)
    val passt = remember(label, labelSp, cellWidth, cellHeight, labelStil) {
        val breite = with(dichte) { labelWidthDp(cellWidth.value, cellHeight.value).dp.roundToPx() }
        // Auch die Hoehe der Beschriftungszone begrenzt: zwei Zeilen passen der Breite nach
        // oft, aber nicht in die Zone. Am Bildschirm gesehen - "Nachrichten" stand auf vier
        // Spalten weiter als "Nachrich..." da, obwohl die reine Breitenmessung "passt" sagte.
        val hoehe = with(dichte) { zoneDp.dp.roundToPx() }
        !messer.measure(
            text = AnnotatedString(label),
            style = labelStil,
            maxLines = 2,
            constraints = Constraints(maxWidth = breite, maxHeight = hoehe),
        ).hasVisualOverflow
    }
    val zeigeLabel = labelPosition != LabelPosition.HIDDEN &&
        (!LocalHideCutLabels.current || passt)
    val labelZone = if (zeigeLabel) zoneDp.dp else 0.dp
    val iconGewuenscht = iconSizeDp(cellWidth.value, cellHeight.value, LocalIconPercent.current)
    val iconDp = iconSizeDp(
        cellWidth.value,
        cellHeight.value,
        LocalIconPercent.current,
        labelZone.value,
    )
    val iconSize = iconDp.dp
    // Musste das Symbol fuer die Beschriftung gequetscht werden, bekommt das Wort den
    // ganzen Platz - siehe IconRoom.
    val zeigeIcon = IconRoom.show(LocalIconVisibility.current, iconGewuenscht, iconDp)
    val pad = (cellHeight.value * 0.06f).coerceIn(6f, 16f).dp
    val staticBorder = borderOverride ?: palette.tileBorder()

    // PLAN.md 3.5: der Rand pulst, die Flaeche nicht. Ein aufblitzender Hintergrund
    // ist auf drei Zoll direkt vor dem Gesicht unertraeglich, und Farbe allein
    // erreicht niemanden mit Rot-Gruen-Schwaeche - deshalb zusaetzlich der Punkt.
    val pulse = if (badgeCount > 0 && animationsOn()) {
        rememberInfiniteTransition(label = "blink").animateFloat(
            initialValue = 2f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "borderWidth",
        ).value
    } else {
        if (badgeCount > 0) 4f else 0f
    }
    // PLAN.md 3.1, Leitsatz 3: "Druck = Farbe + Haptik". Da war nur das Schrumpfen um drei
    // Prozent - und das verdeckt im Moment des Druecken der Finger. Die Flaeche wird
    // dunkler, nie heller: heller hiesse weniger Abstand zur Beschriftung, und die
    // Kontrastschwelle gilt auch waehrend eines Drucks. Im Hochkontrast-Thema ist die
    // Flaeche schon schwarz, deshalb wird dort zusaetzlich der Rand dicker.
    val gedrueckt = if (pressed) darken(background) else background
    val border = if (badgeCount > 0) palette.onTile else staticBorder
    val borderWidth = when {
        badgeCount > 0 -> pulse.dp
        borderOverride != null -> 2.dp
        else -> 3.dp
    } + if (pressed) 2.dp else 0.dp

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(gedrueckt)
            .then(
                if (border != null) {
                    Modifier.border(borderWidth, border, RoundedCornerShape(cornerRadius))
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { haptik.tap(haptikStaerke); onClick() },
                onLongClick = onLongClick?.let { echt -> { haptik.longPress(haptikStaerke); echt() } },
            )
            .semantics { this.contentDescription = gesprochen },
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Der einzige erlaubte Verlauf der ganzen App: er macht die Beschriftung auf
            // einem beliebigen Foto lesbar. Das ist eine Lesbarkeitsmassnahme, keine Optik.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f),
                        )
                    )
            )
        }

        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(pad)
                    .size((iconSize.value * 0.55f).coerceAtLeast(20f).dp)
                    .background(palette.onTile, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = NotificationCounts.badgeText(badgeCount).orEmpty(),
                    color = background,
                    fontSize = dpSp((iconSize.value * 0.30f).coerceAtLeast(11f)),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (content != null) {
            content()
            return@Box
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(pad),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.fillMaxWidth()) {
                when {
                    iconContent != null -> iconContent()
                    !zeigeIcon -> Unit
                    photoUri != null -> Unit
                    initials != null -> Text(
                        text = initials,
                        color = palette.onTile,
                        fontSize = dpSp(iconDp * 0.62f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    iconBitmap != null -> Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        contentScale = ContentScale.Fit,
                    )
                    icon != null -> Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.onTile,
                        modifier = Modifier.size(iconSize),
                    )
                }
            }

            if (zeigeLabel) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(labelZone),
                    contentAlignment = if (labelPosition == LabelPosition.BOTTOM_CENTER) {
                        Alignment.BottomCenter
                    } else {
                        Alignment.BottomStart
                    },
                ) {
                    Text(
                        text = label,
                        color = palette.onTile,
                        style = labelStil,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Die Kachelfarbe unter dem Finger: knapp ein Drittel dunkler.
 *
 * Dunkler und nicht heller, damit der Abstand zur Beschriftung waehrend des Drucks nicht
 * kleiner wird - die Schwelle aus `PLAN.md` 3.3 gilt auch in diesem Moment. Nachgerechnet:
 * mit 0,68 liegt der Unterschied zur ungedrueckten Kachel ueber allen zwoelf Kacheltoenen
 * bei 1,5 bis 1,7 zu 1 und ist damit zu sehen; die Beschriftung kommt dabei nie unter 9 zu 1.
 */
fun darken(color: Color): Color = Color(
    red = color.red * 0.68f,
    green = color.green * 0.68f,
    blue = color.blue * 0.68f,
    alpha = color.alpha,
)

/**
 * Respektiert die Systemeinstellung fuer Animationsdauer. Wer sie auf null stellt, will
 * keine Bewegung - dann steht der Rand still und der Punkt allein traegt den Hinweis.
 */
@Composable
private fun animationsOn(): Boolean {
    val context = LocalContext.current
    return remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
}
