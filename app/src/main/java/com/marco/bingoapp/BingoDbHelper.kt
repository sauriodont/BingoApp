package com.marco.bingoapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BingoDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "bdbingo.db"
        const val DATABASE_VERSION = 3 // Versión 3: Soporte para Ventas

        const val TABLE_CARTONES = "cartones"
        const val COLUMN_ID = "id"
        const val COLUMN_JUGADOR = "jugador"
        const val COLUMN_NUMEROS = "numeros"
        const val COLUMN_FECHA = "fecha"
        const val COLUMN_COMPRADOR = "comprador"
        const val COLUMN_PAGADO = "pagado"

        const val TABLE_MODALIDADES = "modalidades"
        const val COLUMN_MOD_NOMBRE = "nombre"
        const val COLUMN_MOD_CONFIG = "configuracion"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_CARTONES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_JUGADOR TEXT, 
                $COLUMN_NUMEROS TEXT NOT NULL,
                $COLUMN_FECHA TEXT,
                $COLUMN_COMPRADOR TEXT DEFAULT '',
                $COLUMN_PAGADO INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_MODALIDADES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MOD_NOMBRE TEXT NOT NULL,
                $COLUMN_MOD_CONFIG TEXT NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_MODALIDADES ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_MOD_NOMBRE TEXT, $COLUMN_MOD_CONFIG TEXT)")
        }
        if (oldVersion < 4) {
            // Si el usuario ya tenía la tabla, agregamos las columnas de ventas
            db.execSQL("ALTER TABLE $TABLE_CARTONES ADD COLUMN $COLUMN_COMPRADOR TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE_CARTONES ADD COLUMN $COLUMN_PAGADO INTEGER DEFAULT 0")
        }
    }

    // --- MÉTODOS DE CARTONES ---

    fun insertarCarton(jugador: String, numerosJson: String, fecha: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_JUGADOR, jugador)
            put(COLUMN_NUMEROS, numerosJson)
            put(COLUMN_FECHA, fecha)
            put(COLUMN_COMPRADOR, "") // Importante: Inicializar vacío
            put(COLUMN_PAGADO, 0)      // Importante: Inicializar en 0 (No pagado)
        }
        return db.insert(TABLE_CARTONES, null, values)
    }

    fun obtenerTodosLosCartones(): android.database.Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_CARTONES ORDER BY $COLUMN_ID ASC", null)
    }
    fun obtenerCartonPorId(id: String): android.database.Cursor {
        val db = this.readableDatabase
        // Usamos el ID para filtrar el cartón específico
        return db.rawQuery("SELECT * FROM $TABLE_CARTONES WHERE $COLUMN_ID = ?", arrayOf(id))
    }
    fun actualizarVenta(id: String, comprador: String, pagado: Int): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COMPRADOR, comprador)
            put(COLUMN_PAGADO, pagado)
        }
        return db.update(TABLE_CARTONES, values, "$COLUMN_ID = ?", arrayOf(id))
    }

    fun borrarTodosLosCartones() {
        val db = this.writableDatabase
        db.delete(TABLE_CARTONES, null, null)
        db.execSQL("DELETE FROM sqlite_sequence WHERE name='$TABLE_CARTONES'")
    }

    fun insertarModalidad(nombre: String, configuracion: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MOD_NOMBRE, nombre)
            put(COLUMN_MOD_CONFIG, configuracion)
        }
        return db.insert(TABLE_MODALIDADES, null, values)
    }

    fun obtenerTodasLasModalidades(): android.database.Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_MODALIDADES", null)
    }

    fun eliminarModalidad(id: Int): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_MODALIDADES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun existeModalidad(nombre: String, configuracion: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_MODALIDADES WHERE $COLUMN_MOD_NOMBRE = ? OR $COLUMN_MOD_CONFIG = ?",
            arrayOf(nombre, configuracion)
        )
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }
}