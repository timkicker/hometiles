package dev.kicker.hometiles.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kicker.hometiles.ui.theme.LocalBigPalette
import dev.kicker.hometiles.ui.theme.LocalCornerRadius
import dev.kicker.hometiles.ui.theme.LocalTextScale
import dev.kicker.hometiles.ui.theme.tileBorder

/** the name's wished size at text scale 1, before it shrinks to fit. */
private const val NAME_SP = 17f

/**
 * one app in the grid of the app list (issue #7): the icon large and the name under it.
 *
 * the same surface, border, focus frame and feedback as [BigRow], only standing up, so a
 * grid and a list read as the same app. the name shrinks until it fits two lines and its
 * longest word fits one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BigGridCell(
    label: String,
    modifier: Modifier = Modifier,
    iconBitmap: ImageBitmap? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val haptics = LocalHapticFeedback.current
    val hapticStrength = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.98f else 1f, label = "press")
    val paint = palette.surfaceDefault
    val border = palette.tileBorder()
    val shape = RoundedCornerShape(LocalCornerRadius.current)
    val nameLine = with(LocalDensity.current) { (NAME_SP * scale).sp.toDp() }

    Column(
        modifier = modifier
            .scale(press)
            .clip(shape)
            .background(paint.fill)
            .then(if (border != null) Modifier.border(3.dp, border, shape) else Modifier)
            .focusBorder(shape, paint.fill)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { haptics.tap(hapticStrength); onClick() },
                onLongClick = onLongClick?.let { handler -> { haptics.longPress(hapticStrength); handler() } },
            )
            // one height for every cell, or a row of names with one and two lines stands
            // ragged: the icon, the gap and two lines at the largest size the name may take.
            .height(56.dp + 8.dp + nameLine * 2.6f + 24.dp)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            when {
                iconBitmap != null -> Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(52.dp))
                icon != null -> Icon(icon, contentDescription = null, tint = paint.ink, modifier = Modifier.size(48.dp))
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val widthPx = constraints.maxWidth
            val measurer = rememberTextMeasurer()
            val baseStyle = LocalTextStyle.current
            val steps = labelLadder(NAME_SP * scale).map { size -> baseStyle.copy(fontSize = size.sp, fontWeight = FontWeight.Bold) }
            // the largest size at which the longest word fits a line and the whole name two:
            // "Play Movies & TV" lost its end at the first size that only held its words.
            val style = remember(label, scale, widthPx) {
                val word = AnnotatedString(longestWord(label))
                steps.firstOrNull { step ->
                    measurer.measure(word, step).size.width <= widthPx &&
                        !measurer.measure(
                            AnnotatedString(label),
                            step,
                            maxLines = 2,
                            constraints = Constraints(maxWidth = widthPx),
                        ).hasVisualOverflow
                } ?: steps.last()
            }
            Text(
                text = label,
                color = paint.ink,
                fontSize = style.fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
