package com.levelup.app.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Each platform supplies the appropriate SQLite driver (AndroidSqliteDriver,
 * NativeSqliteDriver, etc). Implemented in [com.levelup.app.data.db.driver] under
 * androidMain/iosMain.
 */
expect class DriverFactory {
    fun createDriver(): SqlDriver
}
