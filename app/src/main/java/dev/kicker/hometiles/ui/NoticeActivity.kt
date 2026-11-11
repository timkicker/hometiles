package dev.kicker.hometiles.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.kicker.hometiles.info.BatteryInfo
import dev.kicker.hometiles.info.BatteryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import dev.kicker.hometiles.R
import dev.kicker.hometiles.data.ConfigStore
import dev.kicker.hometiles.ui.theme.HomeTilesTheme
import dev.kicker.hometiles.ui.theme.LocalBigPalette

/**
 * a notice that stands until tapped away.
 *
 * an activity and not a dialog: [Notice] is also called from places without a screen, such
 * as [dev.kicker.hometiles.actions.Intents] when an app is missing, where a dialog has no window.
 */
class NoticeActivity : HomeTilesActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val text = intent?.getStringExtra(EXTRA_TEXT).orEmpty()
        val store = ConfigStore.get(this)
        if (intent?.getBooleanExtra(EXTRA_UNTIL_CHARGING, false) == true) {
            showOverLockScreen()
            // the battery notice goes by itself once the cable is in: whoever plugged it in
            // has done what it asked, and a notice still standing afterwards says otherwise.
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    BatteryRepository.readings(this@NoticeActivity)
                        .first { BatteryInfo.isCharging(it) }
                    finish()
                }
            }
        }

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            HomeTilesTheme(
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

    /** the same two calls as on the message screen, see SmsActivity. */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            // deprecated since 8.1, and still the only way on 8.0, the oldest android we promise.
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    companion object {
        const val EXTRA_TEXT = Notice.EXTRA_TEXT

        /** over the lock screen and gone once the phone charges. see BatteryWatch. */
        const val EXTRA_UNTIL_CHARGING = "untilCharging"
    }
}
