package com.oorep.android

import android.app.Application
import com.oorep.android.data.db.DatabaseHelper
import com.oorep.android.domain.RepertorizationEngine

class OorepApplication : Application() {

    lateinit var databaseHelper: DatabaseHelper
        private set

    lateinit var repertorizationEngine: RepertorizationEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        databaseHelper = DatabaseHelper(this)
        repertorizationEngine = RepertorizationEngine(databaseHelper)
    }

    companion object {
        lateinit var instance: OorepApplication
            private set
    }
}
