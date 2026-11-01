package com.example

import android.app.Application
import com.example.data.AppRepository
import com.example.data.local.AppDatabase

class GhanshyamApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: AppRepository by lazy {
        AppRepository(this, database)
    }
}
