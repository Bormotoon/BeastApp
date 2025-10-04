package com.beast.app

import android.app.Application
import androidx.work.Configuration
import com.beast.app.data.DataInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BeastApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var dataInitializer: DataInitializer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize seed data on first run
        applicationScope.launch {
            dataInitializer.initializeIfNeeded()
        }
    }
}
