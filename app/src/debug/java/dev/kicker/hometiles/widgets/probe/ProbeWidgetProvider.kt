package dev.kicker.hometiles.widgets.probe

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import dev.kicker.hometiles.R

/**
 * a widget that wants to be set up first, the way the mail and door-entry widgets of issue #9
 * do. debug build only: it makes the configure step testable without anyone's account.
 */
class ProbeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            manager.updateAppWidget(id, RemoteViews(context.packageName, R.layout.probe_widget))
        }
    }
}
