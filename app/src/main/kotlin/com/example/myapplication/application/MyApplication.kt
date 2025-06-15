package com.example.myapplication.application

import android.app.Application
import com.example.myapplication.database.AppDatabase

class MyApplication : Application() {
    val database: com.example.myapplication.database.AppDatabase by lazy {
        com.example.myapplication.database.AppDatabase.getDatabase(this)
    }
}