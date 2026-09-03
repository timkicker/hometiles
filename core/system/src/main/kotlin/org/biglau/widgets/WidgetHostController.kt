package org.biglau.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.TypedValue

/**
 * Haelt die Widgets am Leben.
 *
 * Zwei Dinge sind hier nicht offensichtlich. Erstens muss der Host beim Anzeigen zuhoeren
 * und beim Verlassen aufhoeren, sonst laufen Widget-Aktualisierungen im Hintergrund weiter
 * und kosten Akku. Zweitens darf nur der Standard-Launcher ein Widget ohne Rueckfrage
 * binden; sonst muss der Nutzer im Systemdialog zustimmen - das ist kein Fehler, sondern
 * der vorgesehene Weg, und die Oberflaeche muss ihn anbieten koennen.
 */
class WidgetHostController(context: Context) {

    private val appContext = context.applicationContext
    private val manager = AppWidgetManager.getInstance(appContext)
    private val host = AppWidgetHost(appContext, HOST_ID)

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    fun providers(): List<WidgetProviderRow> {
        val packageManager = appContext.packageManager
        val rows = runCatching { manager.installedProviders }.getOrNull().orEmpty().map { info ->
            WidgetProviderRow(
                packageName = info.provider.packageName,
                className = info.provider.className,
                label = info.loadLabel(packageManager).orEmpty(),
                appLabel = runCatching {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(info.provider.packageName, 0),
                    ).toString()
                }.getOrDefault(info.provider.packageName),
                minWidthDp = info.minWidth.toDp(),
                minHeightDp = info.minHeight.toDp(),
                resizeHorizontal = info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0,
                resizeVertical = info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0,
                needsConfiguration = info.configure != null,
            )
        }
        return WidgetFit.sorted(rows)
    }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun release(widgetId: Int) {
        runCatching { host.deleteAppWidgetId(widgetId) }
    }

    /** Bindet ohne Rueckfrage - geht nur als Standard-Launcher. */
    fun bindDirectly(widgetId: Int, component: String): Boolean {
        val target = component.toComponent() ?: return false
        return runCatching { manager.bindAppWidgetIdIfAllowed(widgetId, target) }.getOrDefault(false)
    }

    /** Der Systemdialog, in dem der Nutzer der Bindung zustimmt. */
    fun bindRequestIntent(widgetId: Int, component: String): Intent? {
        val target = component.toComponent() ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, target)
        }
    }

    fun startConfiguration(activity: Activity, widgetId: Int, requestCode: Int): Boolean = runCatching {
        host.startAppWidgetConfigureActivityForResult(activity, widgetId, 0, requestCode, null)
        true
    }.getOrDefault(false)

    fun createView(context: Context, widgetId: Int): AppWidgetHostView? {
        val info = runCatching { manager.getAppWidgetInfo(widgetId) }.getOrNull() ?: return null
        return runCatching { host.createView(context, widgetId, info) }.getOrNull()
    }

    /** Teilt dem Widget mit, wie viel Platz es hat - sonst zeichnet es fuer eine Standardgroesse. */
    fun resize(view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.updateAppWidgetSize(android.os.Bundle.EMPTY, widthDp, heightDp, widthDp, heightDp)
            } else {
                // `updateAppWidgetSize(Bundle, …)` ist seit Android 12 durch die Fassung mit
                // Größenliste abgelöst; auf Android 11 gibt es nur diese.
                @Suppress("DEPRECATION")
                view.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
            }
        }
    }

    private fun Int.toDp(): Int {
        val density = appContext.resources.displayMetrics.density
        return if (density > 0f) (this / density).toInt() else this
    }

    private fun String.toComponent(): ComponentName? =
        ComponentName.unflattenFromString(this)

    companion object {
        const val HOST_ID = 0x42167
        const val REQUEST_BIND = 9001
        const val REQUEST_CONFIGURE = 9002

        @Volatile
        private var instance: WidgetHostController? = null

        fun get(context: Context): WidgetHostController =
            instance ?: synchronized(this) {
                instance ?: WidgetHostController(context).also { instance = it }
            }
    }
}
