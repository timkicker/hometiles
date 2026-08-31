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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalContext
import org.biglau.data.LabelPosition
import org.biglau.notify.NotificationCounts
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.LocalTextScale
import org.biglau.ui.theme.tileBorder

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
    contentDescription: String = label,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val palette = LocalBigPalette.current
    val textScale = LocalTextScale.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")

    val labelSp = labelSizeSp(cellWidth.value, cellHeight.value, textScale)
    val labelZone = labelZoneDp(cellHeight.value, labelSp).dp
    val iconSize = iconSizeDp(cellWidth.value, cellHeight.value).dp
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
    val border = if (badgeCount > 0) palette.onTile else staticBorder
    val borderWidth = when {
        badgeCount > 0 -> pulse.dp
        borderOverride != null -> 2.dp
        else -> 3.dp
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(background)
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
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { this.contentDescription = contentDescription },
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
                    photoUri != null -> Unit
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

            if (labelPosition != LabelPosition.HIDDEN) {
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
                        fontSize = dpSp(labelSp),
                        lineHeight = dpSp(labelSp * 1.1f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

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
