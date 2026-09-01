package org.biglau.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.R
import org.biglau.data.ConfigStore
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * Eine Meldung, die stehen bleibt, bis sie weggetippt wird.
 *
 * Bewusst eine eigene Activity und kein Dialog: [Notice] wird auch aus Stellen gerufen,
 * die keinen Bildschirm haben - aus [org.biglau.actions.Intents] etwa, wenn eine App
 * fehlt. Ein Dialog braeuchte dort ein Fenster, das es nicht gibt.
 */
class NoticeActivity : BigLauActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val text = intent?.getStringExtra(EXTRA_TEXT).orEmpty()
        val store = ConfigStore.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            BigLauTheme(
                config.appearance.theme,
                config.appearance.textScale,
                haptics = config.behaviour.haptics,
                font = config.appearance.font,
                labelScale = config.appearance.labelScale,
                iconPercent = config.appearance.iconPercent,
                icons = config.appearance.icons,
                cornerRadiusDp = config.appearance.cornerRadiusDp,
            ) {
                val palette = LocalBigPalette.current
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    // Text oben, Knopf unten. Dazwischen darf Luft sein: der Daumen liegt
                    // am unteren Rand, und ein Knopf gleich unter dem Text laesst sich auf
                    // diesem Geraet nur mit der zweiten Hand treffen.
                    Column(Modifier.fillMaxSize()) {
                        Text(
                            text = text,
                            color = palette.onBackground,
                            fontSize = 19.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        BigRow(
                            label = stringResource(R.string.notice_close),
                            surface = palette.surfaceAccent,
                            onClick = { finish() },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_TEXT = "text"
    }
}
