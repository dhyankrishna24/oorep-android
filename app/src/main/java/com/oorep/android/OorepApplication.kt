package com.oorep.android

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.oorep.android.data.db.DatabaseHelper
import com.oorep.android.domain.RepertorizationEngine

class OorepApplication : MultiDexApplication() {

    lateinit var databaseHelper: DatabaseHelper
        private set

    lateinit var repertorizationEngine: RepertorizationEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val prefs = getSharedPreferences("oorep_crash", Context.MODE_PRIVATE)
                prefs.edit().putString("last_crash", throwable.stackTraceToString()).commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        databaseHelper = DatabaseHelper(this)
        repertorizationEngine = RepertorizationEngine(databaseHelper)
    }

    companion object {
        lateinit var instance: OorepApplication
            private set
    }
}
