package com.marco.bingoapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BingoDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE cartones (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                jugador TEXT NOT NULL,
                numeros TEXT NOT NULL,
                fecha TEXT
            )
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS cartones")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "bdbingo.db"
        const val DATABASE_VERSION = 1
    }
}