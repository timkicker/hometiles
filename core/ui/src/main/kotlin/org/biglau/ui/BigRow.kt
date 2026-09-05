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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.res.stringResource
import org.biglau.core.ui.R
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
 * a row in a list. same language as the tile, flat and left-aligned, only lying down.
 * lists may scroll, the home screen may not.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BigRow(
    label: String,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    icon: ImageVector? = null,
    iconBitmap: ImageBitmap? = null,
    /** free space in front, for contact photos and initials. */
    leading: (@Composable () -> Unit)? = null,
    surface: org.biglau.ui.theme.BigSurface? = null,
    /** only for the font picker, where each row stands in its own face. */
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    /** only for the radius picker, where each row shows its own corner. */
    cornerRadius: androidx.compose.ui.unit.Dp? = null,
    /**
     * `null` for a row that only says something. an empty action (`onClick = {}`) looks
     * like a button, swallows the tap, and is announced as one.
     */
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    /**
     * two lines by default: an explaining second line rarely fits one at 1.35 system scale,
     * and a cut sentence explains nothing. rows whose second line carries data stay at one,
     * so the row height stays even.
     */
    secondaryMaxLines: Int = 2,
    /** own border instead of the theme's, for rows whose surface already says something. */
    borderColor: Color? = null,
    /**
     * this row is the chosen one of its list. colours the surface and says so: the bare
     * `selected` property does not reach the accessibility interface, the state has to go
     * into the name as well.
     *
     * only for one out of many. a list where several may hold at once is not a choice; each
     * row there is a switch of its own and takes [checked]. the difference shows when a row
     * does *not* hold: an unchosen one says nothing, an off switch says off.
     */
    selected: Boolean = false,
    /**
     * this row is a switch, and this is how it stands. a switch is not chosen, it is *on*.
     *
     * only where the off label is an invitation rather than a statement. most switch labels
     * here change with the state, and then adding on or off is doubled and reads as the
     * opposite; those rows pass nothing and let the caller colour the surface.
     */
    checked: Boolean? = null,
    /**
     * what else the row says about itself: two are unread, missed.
     *
     * for states that are neither a choice nor a switch. they used to sit in the surface
     * colour and in an unnamed icon, which read aloud as nothing. colours nothing itself:
     * what such a state colours differs case by case.
     */
    state: String? = null,
    /**
     * what is read out instead of the label, for labels that carry something drawn: the
     * message list appends a count in brackets that [state] already says as a sentence.
     */
    labelSpeech: String? = null,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val haptics = LocalHapticFeedback.current
    val hapticStrength = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.98f else 1f, label = "press")
    val paint = surface
        ?: if (selected || checked == true) palette.surfaceAccent else palette.surfaceDefault
    val border = borderColor ?: palette.tileBorder()
    // label and second line would otherwise stand as two nodes: replacing the name
    // replaces both, and the second line must not be lost.
    val spoken = labelSpeech ?: label
    val stateText = when {
        selected -> stringResource(R.string.a11y_chosen, spoken)
        checked == true -> stringResource(R.string.a11y_on, spoken)
        checked == false -> stringResource(R.string.a11y_off, spoken)
        state != null -> stringResource(R.string.a11y_state, spoken, state)
        else -> null
    }
    // also announced when only the *name* is replaced: hanging the whole semantics block on
    // the state left a row with `labelSpeech` and no state without any description at all,
    // and five colour rows all read as "this colour".
    val announcement = when {
        stateText != null -> stateText + (secondary?.let { ". $it" } ?: "")
        labelSpeech != null -> spoken + (secondary?.let { ". $it" } ?: "")
        else -> null
    }

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
                        onClick = { haptics.tap(hapticStrength); onClick() },
                        onLongClick = onLongClick?.let { handler ->
                            { haptics.longPress(hapticStrength); handler() }
                        },
                    )
                },
            )
            .then(
                if (announcement != null) {
                    Modifier.semantics {
                        if (selected) this.selected = true
                        contentDescription = announcement
                    }
                } else {
                    Modifier
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
        // the constraints hold for both lines: label and second line share the height, so
        // both have to be measured in the same place.
        BoxWithConstraints(Modifier.weight(1f)) {
          val widthPx = constraints.maxWidth
          val maxHeightPx = constraints.maxHeight
          Column {
            // shrink first, then break or cut: at 200 % the list read "Nachrichte / n" and
            // "Alles zurückset...". the longest word is measured, since the line breaks at
            // it. see BigHeading, same there.
            val measurer = rememberTextMeasurer()
            val baseStyle = LocalTextStyle.current
            val steps = labelLadder(22f * scale).map { size ->
                    // `fontFamily` is usually null, meaning the theme's. set as a field of
                    // a copy the same null means none, and then the measuring font would
                    // not be the drawing one.
                    val step = baseStyle.copy(
                        fontSize = size.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (fontFamily != null) step.copy(fontFamily = fontFamily) else step
                }
                val style = remember(label, scale, widthPx, fontFamily) {
                    val word = AnnotatedString(longestWord(label))
                    steps.firstOrNull { measurer.measure(word, it).size.width <= widthPx }
                        ?: steps.last()
                }
                // three lines when three lines have room, measured and not guessed: in a
                // list the height is open, on the call screen it is fixed at 72 dp, and a
                // third line there would be cut off instead of shortened.
                val mayGrow = remember(label, style, widthPx, maxHeightPx) {
                    val height = measurer.measure(
                        text = AnnotatedString(label),
                        style = style,
                        maxLines = 3,
                        constraints = Constraints(maxWidth = widthPx),
                    ).size.height
                    height <= maxHeightPx
                }
                // the same arguments as before the ladder, only the size comes from it: a
                // whole TextStyle replaces the inherited font and changes small things like
                // letter spacing along the way.
                Text(
                    text = label,
                    color = paint.ink,
                    fontSize = style.fontSize,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (mayGrow) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            if (secondary != null) {
                // the second line may get a third too where there is room: the warning
                // about an orphaned folder otherwise ended on "cannot be undo...", the half
                // sentence that is the whole reason to think first. data-carrying lines
                // stay at one, where an even row height keeps the list calm.
                //
                // from the drawn style, not freshly built, or it measures in the default
                // font and draws in the user's.
                val secondaryStyle = baseStyle.copy(fontSize = (15f * scale).sp)
                val secondaryLines = remember(secondary, scale, widthPx, maxHeightPx) {
                    if (secondaryMaxLines < 2) {
                        secondaryMaxLines
                    } else {
                        val height = measurer.measure(
                            text = AnnotatedString(secondary),
                            style = secondaryStyle,
                            maxLines = 3,
                            constraints = Constraints(maxWidth = widthPx),
                        ).size.height
                        // the label above needs its room too.
                        val labelHeight = measurer.measure(
                            text = AnnotatedString(label),
                            style = style,
                            maxLines = 3,
                            constraints = Constraints(maxWidth = widthPx),
                        ).size.height
                        if (height + labelHeight <= maxHeightPx) 3 else secondaryMaxLines
                    }
                }
                Text(
                    text = secondary,
                    color = paint.ink,
                    fontSize = (15f * scale).sp,
                    maxLines = secondaryLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
          }
        }
    }
}

/**
 * square button with an icon instead of a word.
 *
 * only for side matters that must fit beside a heading: two labelled buttons do not fit
 * side by side on three inches at 1.35 system scale.
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

/** heading over a section of a list. */
@Composable
fun BigHeading(text: String, modifier: Modifier = Modifier) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val measurer = rememberTextMeasurer()
    // shrink first, then break: compose splits a word mid-way as soon as it does not fit a
    // line on its own, and a heading broken mid-word reads like a fault. the longest word
    // is measured, since the line breaks at it.
    BoxWithConstraints(modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        val widthPx = constraints.maxWidth
        // built on the theme's font, not on the default: measured in the wrong one the
        // heading came out a step too small, and `style` replaces the inherited font.
        val baseStyle = LocalTextStyle.current
        val steps = labelLadder(26f * scale).map { size ->
            size to baseStyle.copy(fontSize = size.sp, fontWeight = FontWeight.Bold)
        }
        val style = remember(text, scale, widthPx) {
            val word = AnnotatedString(longestWord(text))
            steps.firstOrNull { measurer.measure(word, it.second).size.width <= widthPx }?.second
                ?: steps.last().second
        }
        Text(text = text, color = palette.onBackground, style = style)
    }
}
