package com.blackboxbench.reproduction

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/** Neutral helper for copying a synthetic SQLite asset into writable app data. */
object LocalDatabase {
    fun copyFromAssets(context: Context, assetName: String, fileName: String): File {
        val destination = context.getDatabasePath(fileName)
        if (!destination.exists()) {
            destination.parentFile?.mkdirs()
            context.assets.open(assetName).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return destination
    }

    fun open(context: Context, assetName: String, fileName: String): SQLiteDatabase =
        SQLiteDatabase.openDatabase(
            copyFromAssets(context, assetName, fileName).absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
}
