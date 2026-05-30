package dev.vikingsen.absclientapp.core.database

import android.content.Context

class AppDatabaseProvider(context: Context) {
    val database: AppDatabase = AppDatabase.create(context)
}
