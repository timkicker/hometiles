package dev.kicker.hometiles.widgets.probe

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log

/**
 * the setup step of [ProbeWidgetProvider]. answers at once with RESULT_OK, as a real setup
 * screen does once the user has picked an account.
 */
class ProbeWidgetConfigActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        Log.i("ProbeWidget", "configure id=$id")
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        finish()
    }
}
