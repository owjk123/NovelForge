package com.novelforge.app

import android.app.Application
import com.novelforge.app.data.api.ApiClient
import com.novelforge.app.data.db.AppDatabase

class NovelForgeApplication : Application() {
    
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        // 初始化 ApiClient
        ApiClient.initialize(this)
    }
    
    companion object {
        lateinit var instance: NovelForgeApplication
            private set
    }
}
