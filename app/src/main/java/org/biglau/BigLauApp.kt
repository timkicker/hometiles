package org.biglau

import android.app.Application
import org.biglau.apps.AppRepository
import org.biglau.data.ConfigStore

class BigLauApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Beide Singletons frueh aufbauen, damit der erste Frame des Homescreens
        // nicht auf Plattenzugriff wartet.
        ConfigStore.get(this)
        AppRepository.get(this)
    }
}
