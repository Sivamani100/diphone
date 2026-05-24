package com.example.diphone

import android.app.Application
import android.content.Context
import com.example.diphone.core.data.repository.AppContainer
import com.example.diphone.core.data.system.NotificationChannels

class DiPhoneApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        
        // Create all 9 notification channels for Android 8+
        NotificationChannels.createAllChannels(this)
    }

    companion object {
        fun getContainer(context: Context): AppContainer {
            return (context.applicationContext as DiPhoneApp).container
        }
    }
}
