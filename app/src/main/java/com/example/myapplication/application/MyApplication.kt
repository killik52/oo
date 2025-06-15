package com.example.myapplication.application

import android.app.Application
import com.example.myapplication.database.AppDatabase

class MyApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }
}