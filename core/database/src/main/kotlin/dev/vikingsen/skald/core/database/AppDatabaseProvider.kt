package dev.vikingsen.skald.core.database

import android.content.Context

class AppDatabaseProvider(context: Context) {
    val database: AppDatabase = AppDatabase.create(context)
}
