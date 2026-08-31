package org.biglau

import android.app.Application
import org.biglau.apps.AppRepository
import org.biglau.data.ConfigStore
import org.biglau.safety.CrashRecorder

class BigLauApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Beide Singletons frueh aufbauen, damit der erste Frame des Homescreens
        // nicht auf Plattenzugriff wartet.
        // Zuerst: Abstuerze festhalten, damit der Notmodus etwas anzuzeigen hat.
        CrashRecorder.get(this).installHandler()
        ConfigStore.get(this)
        AppRepository.get(this)
    }
}
