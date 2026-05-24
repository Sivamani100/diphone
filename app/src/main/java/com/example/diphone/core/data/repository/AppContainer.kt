package com.example.diphone.core.data.repository

import android.content.Context
import com.example.diphone.core.data.local.AppDatabase

class AppContainer(private val context: Context) {
    
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }
    
    val appDao by lazy {
        database.appDao()
    }
    
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }
    
    val blockedRepository: BlockedRepository by lazy {
        BlockedRepository(appDao)
    }
    
    val contactsRepository: ContactsRepository by lazy {
        ContactsRepository(context, appDao)
    }
    
    val callLogRepository: CallLogRepository by lazy {
        CallLogRepository(context, appDao)
    }
}
