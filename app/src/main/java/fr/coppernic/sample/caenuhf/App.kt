package fr.coppernic.sample.caenuhf

import android.app.Application
import timber.log.Timber
import timber.log.Timber.Forest.plant

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        plant(Timber.DebugTree())
    }
}