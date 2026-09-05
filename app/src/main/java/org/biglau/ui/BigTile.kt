package org.biglau.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.material3.LocalTextStyle
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
 * width of the border that says there is something new.
 *
 * it used to pulse. on a key phone a pulsing rectangle reads as here is the focus rather
 * than here is something new, so the new gets a thin quiet border. nothing is lost: the
 * number in the corner still says how much waits, and more precisely than any movement.
 */
private const val BADGE_BORDER_DP = 2f

/**
 * width of the focus border, clearly more than [BADGE_BORDER_DP] so two borders on one
 * screen do not look alike while pointing at different things.
 */
private const val FOCUS_BORDER_DP = 8f

/**
 * a tile in the sign design (`PLAN.md` 3.0): full colour to the edge, a large icon top
 * left, the label in a zone of fixed height bottom left.
 *
 * the fixed label zone is the point: the labels of a grid row then share a baseline,
 * whether one line or two.
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
    /** contact photo, filling the tile behind the label. */
    photoUri: String? = null,
    labelPosition: LabelPosition = LabelPosition.BOTTOM_LEFT,
    cornerRadius: Dp = 12.dp,
    /** overrides the theme border; used by the empty tile. */
    borderOverride: Color? = null,
    /** waiting notifications; above zero the tile blinks. */
    badgeCount: Int = 0,
    /** replaces icon and label; used by the clock and the battery. */
    content: (@Composable () -> Unit)? = null,
    /**
     * own drawing *in place of the icon*, with the label zone as usual. unlike [content],
     * which takes the whole tile and swallows the label; a folder needs both.
     */
    iconContent: (@Composable () -> Unit)? = null,
    /**
     * initials instead of an icon (`PLAN.md` 3.4). here and not at the caller, because only
     * here is the icon size computed, and they follow the same rule: no room for an icon
     * means no room for letters.
     */
    initials: String? = null,
    contentDescription: String = label,
    /**
     * how the corner count is read out. the default fits an app tile; the missed calls tile
     * counts calls, not notices, and was still announced as new notices.
     */
    badgeSpeech: Int = R.plurals.a11y_badge,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    // the corner count is drawn and carries no text: without this a screen reader hears
    // messages and not that five of them wait. see TileSpeech and `PLAN.md` 3.6. here and
    // not at the callers, so no tile is forgotten.
    val spoken = TileSpeech.describe(
        label = contentDescription,
        badge = if (badgeCount > 0) {
            pluralStringResource(badgeSpeech, badgeCount, badgeCount)
        } else {
            null
        },
    )
    val palette = LocalBigPalette.current
    val haptics = LocalHapticFeedback.current
    val hapticStrength = LocalHaptics.current
    val textScale = LocalTextScale.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")

    val labelWish =
        labelSizeSp(cellWidth.value, cellHeight.value, textScale, LocalLabelScale.current)
    // `PLAN.md` 3.2: the label may go when it would be cut off anyway, and its room then
    // belongs to the icon.
    //
    // measured, not estimated: an average character width would have hidden "Nachrichten"
    // although it fits. the measurer runs before drawing, so nothing flashes up.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val zoneDp = labelZoneDp(cellHeight.value, labelWish)
    // shrink first, then cut: see labelLadder. the zone stays as tall as at the wish, or
    // the icon above it would hop with the word length. measured with the style that is
    // drawn, since Hyperlegible is wider than the default.
    val baseStyle = LocalTextStyle.current
    val steps = labelLadder(labelWish).map { size ->
        size to baseStyle.copy(
            fontSize = dpSp(size),
            lineHeight = dpSp(size * 1.1f),
            fontWeight = FontWeight.Bold,
        )
    }
    val (labelSp, labelStyle, fits) = remember(label, labelWish, cellWidth, cellHeight) {
        val widthPx = with(density) { labelWidthDp(cellWidth.value, cellHeight.value).dp.roundToPx() }
        // the zone height bounds it too: two lines often fit the width but not the zone.
        val heightPx = with(density) { zoneDp.dp.roundToPx() }
        fun measures(style: TextStyle) = !measurer.measure(
            text = AnnotatedString(label),
            style = style,
            maxLines = 2,
            constraints = Constraints(maxWidth = widthPx, maxHeight = heightPx),
        ).hasVisualOverflow
        val hit = steps.firstOrNull { measures(it.second) }
        Triple(
            hit?.first ?: steps.last().first,
            hit?.second ?: steps.last().second,
            hit != null,
        )
    }
    val showLabel = labelPosition != LabelPosition.HIDDEN &&
        (!LocalHideCutLabels.current || fits)
    val labelZone = if (showLabel) zoneDp.dp else 0.dp
    val iconWish = iconSizeDp(cellWidth.value, cellHeight.value, LocalIconPercent.current)
    val iconDp = iconSizeDp(
        cellWidth.value,
        cellHeight.value,
        LocalIconPercent.current,
        labelZone.value,
    )
    val iconSize = iconDp.dp
    // if the icon had to be squeezed for the label, the word gets the whole room.
    val showIcon = IconRoom.show(LocalIconVisibility.current, iconWish, iconDp)
    val pad = (cellHeight.value * 0.06f).coerceIn(6f, 16f).dp
    val staticBorder = borderOverride ?: palette.tileBorder()

    // `PLAN.md` 3.1, principle 3: press is colour plus haptics. shrinking by three percent
    // alone is covered by the finger at the moment of pressing. darker, never lighter:
    // lighter would mean less distance to the label, and the threshold holds during a press
    // too. in the contrast theme the surface is already black, so the border thickens there.
    val pressedColour = if (pressed) darken(background) else background
    // focus wins over blinking: both set the border, and the focused tile is the one about
    // to start. what has something new is still said by the number in the corner.
    val border = if (focused || badgeCount > 0) palette.onTile else staticBorder
    val borderWidth = when {
        focused -> FOCUS_BORDER_DP.dp
        badgeCount > 0 -> BADGE_BORDER_DP.dp
        borderOverride != null -> 2.dp
        else -> 3.dp
    } + if (pressed) 2.dp else 0.dp

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(pressedColour)
            .then(
                if (border != null) {
                    Modifier.border(borderWidth, border, RoundedCornerShape(cornerRadius))
                } else {
                    Modifier
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { haptics.tap(hapticStrength); onClick() },
                onLongClick = onLongClick?.let { handler -> { haptics.longPress(hapticStrength); handler() } },
            )
            .semantics { this.contentDescription = spoken },
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // the only gradient allowed in the whole app: it makes the label readable on an
            // arbitrary photo. a readability measure, not a look.
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
                    !showIcon -> Unit
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

            if (showLabel) {
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
                        style = labelStyle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * the tile colour under the finger: nearly a third darker.
 *
 * darker and not lighter, so the distance to the label does not shrink during a press. at
 * 0.68 the difference across all twelve tile hues is 1.5 to 1.7 to one and therefore
 * visible; the label never drops under 9 to 1.
 */
fun darken(color: Color): Color = Color(
    red = color.red * 0.68f,
    green = color.green * 0.68f,
    blue = color.blue * 0.68f,
    alpha = color.alpha,
)
