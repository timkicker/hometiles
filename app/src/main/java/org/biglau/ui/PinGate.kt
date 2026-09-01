package org.biglau.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.biglau.security.Pin
import org.biglau.ui.theme.LocalBigPalette

/** Wie lange der Notausstieg gehalten werden muss. */
const val EMERGENCY_HOLD_MILLIS = 30_000L

/**
 * PIN-Eingabe. Zwei Betriebsarten: pruefen gegen einen gespeicherten Wert, oder eine neue
 * PIN aufnehmen ([onAccept] gesetzt).
 *
 * Der Notausstieg sitzt bewusst hier und nicht auf der ersten Kachel: die reagiert schon nach
 * einer halben Sekunde mit dem Editor, ein 30-Sekunden-Druck kaeme dort nie an. Und hier ist
 * die Stelle, an der eine vergessene PIN ueberhaupt erst wehtut.
 */
@Composable
fun PinGate(
    title: String,
    explainer: String?,
    wrongText: String,
    confirmLabel: String,
    onCheck: (String) -> Boolean,
    onAccept: (String) -> Unit,
    acceptOnComplete: Boolean,
    onEmergencyExit: (() -> Unit)? = null,
) {
    val palette = LocalBigPalette.current
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    var heldSeconds by remember { mutableStateOf(0) }

    if (onEmergencyExit != null) {
        LaunchedEffect(holding) {
            heldSeconds = 0
            if (!holding) return@LaunchedEffect
            while (heldSeconds < EMERGENCY_HOLD_MILLIS / 1000) {
                delay(1000)
                heldSeconds++
            }
            onEmergencyExit()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BigHeading(title)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onEmergencyExit == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    holding = true
                                    tryAwaitRelease()
                                    holding = false
                                },
                            )
                        }
                    }
                )
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            PinDots(entered.length)
        }

        // Erklaerung und Fehlermeldung teilen sich einen Platz fester Hoehe. Vorher
        // wechselten sie einander ab - fuenf Zeilen Erklaerung gegen eine Zeile Fehler -,
        // und die Tastatur sprang bei jedem Fehlversuch um mehrere Zentimeter. Wer dann
        // weitertippt, trifft die Taste daneben und haelt sich fuer vertippt.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 4.dp),
        ) {
            when {
                heldSeconds > 0 -> Text(
                    text = "${EMERGENCY_HOLD_MILLIS / 1000 - heldSeconds}",
                    color = palette.accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )

                wrong -> Text(
                    text = wrongText,
                    color = palette.danger,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                explainer != null -> Text(
                    text = explainer,
                    color = palette.onBackground,
                    fontSize = 15.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(Modifier.weight(1f)) {
            BigKeypad(
                onDigit = { digit ->
                    wrong = false
                    if (entered.length < Pin.MAX_LENGTH) entered += digit
                    if (acceptOnComplete && onCheck(entered)) onAccept(entered)
                },
                onBackspace = { entered = entered.dropLast(1); wrong = false },
            )
        }

        BigRow(
            label = confirmLabel,
            surface = palette.surfaceAccent,
            onClick = {
                if (onCheck(entered)) {
                    onAccept(entered)
                } else {
                    // Eingabe leeren. Blieb sie stehen, tippte man die naechste PIN hinten
                    // an die falsche an und kam nie wieder heraus - acht Punkte voll, und
                    // jede weitere Ziffer fiel lautlos weg.
                    wrong = true
                    entered = ""
                }
            },
        )
    }
}
