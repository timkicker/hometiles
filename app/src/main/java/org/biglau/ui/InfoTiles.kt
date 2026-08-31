package org.biglau.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.biglau.info.BatteryInfo
import org.biglau.info.BatteryReading
import org.biglau.info.ClockTick
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.LocalTextScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Uhrzeit auf einer Kachel. Aktualisiert sich zur vollen Minute, nicht sekuendlich -
 * auf einem 2000-mAh-Akku ist das ein Unterschied, und Sekunden zeigt sie ohnehin nicht.
 */
@Composable
fun ClockContent(
    cellWidth: Dp,
    cellHeight: Dp,
    showDate: Boolean,
    twentyFourHour: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(ClockTick.millisUntilNextMinute(System.currentTimeMillis()))
            now = System.currentTimeMillis()
        }
    }

    val locale = Locale.getDefault()
    val timeFormat = remember(twentyFourHour, locale) {
        SimpleDateFormat(if (twentyFourHour) "HH:mm" else "h:mm a", locale)
    }
    val dateFormat = remember(locale) { SimpleDateFormat("EEEE, d. MMMM", locale) }

    val timeText = timeFormat.format(Date(now))
    val timeSize = singleLineSizeSp(timeText, cellWidth.value, cellHeight.value, scale)

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = timeText,
            color = palette.onTile,
            fontSize = dpSp(timeSize),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
        if (showDate) {
            Text(
                text = dateFormat.format(Date(now)),
                color = palette.onTile,
                fontSize = dpSp((timeSize * 0.3f).coerceAtLeast(12f)),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

/**
 * Ladestand als Zahl plus Balken. Bei unbekanntem Stand ein Fragezeichen statt eines
 * leeren Balkens - der saehe aus wie "leer" und waere eine falsche Aussage.
 */
@Composable
fun BatteryContent(
    reading: BatteryReading?,
    cellWidth: Dp,
    cellHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val percent = reading?.let { BatteryInfo.percent(it) }
    val charging = reading?.let { BatteryInfo.isCharging(it) } ?: false
    val fraction = BatteryInfo.fraction(percent)
    val low = BatteryInfo.isLow(percent)
    val percentText = if (percent == null) "?" else "$percent %"
    val numberSize = singleLineSizeSp(percentText, cellWidth.value, cellHeight.value, scale, maxSp = 64f)

    Column(
        modifier = modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = percentText,
            color = if (low) palette.danger else palette.onTile,
            fontSize = dpSp(numberSize),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
        if (charging) {
            Text(
                text = "⚡",
                color = palette.onTile,
                fontSize = dpSp(numberSize * 0.5f),
            )
        }
        if (fraction != null) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.35f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(50))
                        .background(if (low) palette.danger else palette.onTile),
                )
            }
        }
    }
}
