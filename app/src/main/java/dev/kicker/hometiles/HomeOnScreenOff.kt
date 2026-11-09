package dev.kicker.hometiles

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import dev.kicker.hometiles.a11y.ScreenOffHome
import dev.kicker.hometiles.data.ConfigStore

/**
 * see [ScreenOffHome]. screen off is only sent to receivers registered at run time, so the
 * app registers it once; HomeTiles is the home screen and its process runs anyway.
 */
object HomeOnScreenOff {

    fun register(context: Context) {
        val app = context.applicationContext
        app.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val enabled = ConfigStore.get(app).current.behaviour.homeOnScreenOff
                    val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    if (!ScreenOffHome.goHome(enabled, audio.mode)) return
                    // ACTION_MAIN: MainActivity takes that as the home gesture and leaves a
                    // side screen too.
                    app.startActivity(
                        Intent(app, MainActivity::class.java)
                            .setAction(Intent.ACTION_MAIN)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
            IntentFilter(Intent.ACTION_SCREEN_OFF),
        )
    }
}
