package dev.kicker.hometiles.tiles.probe

import android.app.Activity
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log

/**
 * asks the default launcher to pin a shortcut, the way a browser pins a web app.
 *
 * debug build only. `adb shell am start -n <pkg>/dev.kicker.hometiles.tiles.probe.ProbePinActivity
 * --es label Weather` - the shortcut opens this activity again, which then just closes.
 */
class ProbePinActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val label = intent.getStringExtra(EXTRA_LABEL)
        if (label != null) {
            val manager = getSystemService(ShortcutManager::class.java)
            val info = ShortcutInfo.Builder(this, "probe-pin-$label")
                .setShortLabel(label)
                .setIcon(Icon.createWithResource(this, android.R.drawable.ic_menu_compass))
                .setIntent(Intent(Intent.ACTION_VIEW).setClass(this, ProbePinActivity::class.java))
                .build()
            val asked = manager.isRequestPinShortcutSupported && manager.requestPinShortcut(info, null)
            Log.i(TAG, "pin request for $label: supported=${manager.isRequestPinShortcutSupported} asked=$asked")
        } else {
            Log.i(TAG, "opened from the pinned shortcut")
        }
        finish()
    }

    private companion object {
        const val TAG = "ProbePin"
        const val EXTRA_LABEL = "label"
    }
}
