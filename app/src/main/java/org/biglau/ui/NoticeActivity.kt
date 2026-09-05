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
 * a notice that stands until tapped away.
 *
 * an activity and not a dialog: [Notice] is also called from places without a screen, such
 * as [org.biglau.actions.Intents] when an app is missing, where a dialog has no window.
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
                hideCutLabels = config.appearance.hideCutLabels,
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
                    // text at the top, button at the bottom where the thumb rests.
                    Column(Modifier.fillMaxSize()) {
                        // the notice is this screen's content and gets heading size: in
                        // ordinary size over a large button, the button called and the
                        // message whispered.
                        BigHeading(text)
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
        const val EXTRA_TEXT = Notice.EXTRA_TEXT
    }
}
