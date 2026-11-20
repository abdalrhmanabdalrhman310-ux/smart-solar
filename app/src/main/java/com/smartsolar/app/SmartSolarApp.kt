package com.smartsolar.app

import android.app.Application
import com.smartsolar.app.data.local.SessionPreferencesManager
import com.smartsolar.app.data.remote.NetworkClientFactory
import com.smartsolar.app.data.repository.AuthRepository
import com.smartsolar.app.data.repository.CatalogRepository
import com.smartsolar.app.data.repository.ServicesRepository

class AppContainer(application: Application) {
    val moshi = NetworkClientFactory.buildMoshi()
    val sessionPreferences = SessionPreferencesManager(application, moshi)
    val networkFactory = NetworkClientFactory(application, sessionPreferences, moshi)
    val authRepository = AuthRepository(application, networkFactory, sessionPreferences)
    val catalogRepository = CatalogRepository(application, networkFactory)
    val servicesRepository = ServicesRepository(networkFactory)
}

class SmartSolarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
