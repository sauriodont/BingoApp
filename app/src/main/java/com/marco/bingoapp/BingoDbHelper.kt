package com.marco.bingoapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class BingoDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "bdbingo.db"
        // Versión 4: Incluye Cartones, Modalidades, Pagos y Columnas de Venta
        const val DATABASE_VERSION = 4

        // Tabla Cartones
        const val TABLE_CARTONES = "cartones"
        const val COLUMN_ID = "id"
        const val COLUMN_JUGADOR = "jugador"
        const val COLUMN_NUMEROS = "numeros"
        const val COLUMN_FECHA = "fecha"
        const val COLUMN_COMPRADOR = "comprador"
        const val COLUMN_PAGADO = "pagado"

        // Tabla Modalidades
        const val TABLE_MODALIDADES = "modalidades"
        const val COLUMN_MOD_NOMBRE = "nombre"
        const val COLUMN_MOD_CONFIG = "configuracion"

        // Tabla Pagos
        const val TABLE_PAGOS = "pagos_sorteo"
        const val COLUMN_PAGO_ID = "id"
        const val COLUMN_PAGO_FECHA = "fecha"
        const val COLUMN_PAGO_DETALLE = "detalle_json"
        const val COLUMN_PAGO_TOTAL = "monto_total"
        const val TABLE_ESTADO_SORTEO = "estado_sorteo"
        const val COLUMN_ESTADO_DATA = "data_json" // Guardaremos aquí los números que ya salieron
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tabla Cartones con todas sus columnas (v4)
        db.execSQL("""
            CREATE TABLE $TABLE_CARTONES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_JUGADOR TEXT, 
                $COLUMN_NUMEROS TEXT NOT NULL,
                $COLUMN_FECHA TEXT,
                $COLUMN_COMPRADOR TEXT DEFAULT '',
                $COLUMN_PAGADO INTEGER DEFAULT 0
            )
        """)

        db.execSQL("CREATE TABLE $TABLE_ESTADO_SORTEO (id INTEGER PRIMARY KEY, $COLUMN_ESTADO_DATA TEXT)")

        // Crear tabla Modalidades
        db.execSQL("""
            CREATE TABLE $TABLE_MODALIDADES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MOD_NOMBRE TEXT NOT NULL,
                $COLUMN_MOD_CONFIG TEXT NOT NULL
            )
        """)

        // Crear tabla Pagos
        db.execSQL("""
            CREATE TABLE $TABLE_PAGOS (
                $COLUMN_PAGO_ID INTEGER PRIMARY KEY AUTOINCREMENT, 
                $COLUMN_PAGO_FECHA TEXT, 
                $COLUMN_PAGO_TOTAL REAL, 
                $COLUMN_PAGO_DETALLE TEXT
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Escalera de migración para no perder datos de versiones anteriores
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_MODALIDADES ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_MOD_NOMBRE TEXT, $COLUMN_MOD_CONFIG TEXT)")
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_PAGOS ($COLUMN_PAGO_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_PAGO_FECHA TEXT, $COLUMN_PAGO_TOTAL REAL, $COLUMN_PAGO_DETALLE TEXT)")
        }
        if (oldVersion < 4) {
            try {
                // Agregar columnas de ventas si el usuario viene de v1 o v2
                db.execSQL("ALTER TABLE $TABLE_CARTONES ADD COLUMN $COLUMN_COMPRADOR TEXT DEFAULT ''")
                db.execSQL("ALTER TABLE $TABLE_CARTONES ADD COLUMN $COLUMN_PAGADO INTEGER DEFAULT 0")
            } catch (e: Exception) {
                // Las columnas ya existen
            }
        }
    }

    // --- MÉTODOS DE PAGOS ---

    fun guardarReportePago(totalVendido: Double, detalleJson: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PAGO_TOTAL, totalVendido)
            put(COLUMN_PAGO_DETALLE, detalleJson)
            put(COLUMN_PAGO_FECHA, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        return db.insert(TABLE_PAGOS, null, values)
    }

    fun contarCartonesPagados(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CARTONES WHERE $COLUMN_PAGADO = 1", null)
        var count = 0
        if (cursor.moveToFirst()) count = cursor.getInt(0)
        cursor.close()
        return count
    }

    // --- MÉTODOS DE CARTONES ---

    fun insertarCarton(jugador: String, numerosJson: String, fecha: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_JUGADOR, jugador)
            put(COLUMN_NUMEROS, numerosJson)
            put(COLUMN_FECHA, fecha)
            put(COLUMN_COMPRADOR, "")
            put(COLUMN_PAGADO, 0)
        }
        return db.insert(TABLE_CARTONES, null, values)
    }

    fun obtenerTodosLosCartones(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_CARTONES ORDER BY $COLUMN_ID ASC", null)
    }

    fun obtenerCartonPorId(id: String): Cursor {
        val db = this.readableDatabase
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

    // --- MÉTODOS DE MODALIDADES ---

    fun insertarModalidad(nombre: String, configuracion: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MOD_NOMBRE, nombre)
            put(COLUMN_MOD_CONFIG, configuracion)
        }
        return db.insert(TABLE_MODALIDADES, null, values)
    }

    fun obtenerTodasLasModalidades(): Cursor {
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
    fun obtenerUltimoReporte(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_PAGOS ORDER BY $COLUMN_PAGO_ID DESC LIMIT 1", null)
    }

    // En BingoDbHelper.kt

    fun establecerSorteoEnCurso(enCurso: Boolean) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("id", 1)
            put("en_curso", if (enCurso) 1 else 0)
        }
        // Usamos REPLACE para que siempre sea la fila 1
        db.insertWithOnConflict("estado_sorteo", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun estaSorteoEnCurso(): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT en_curso FROM estado_sorteo WHERE id = 1", null)
        var resultado = false
        if (cursor.moveToFirst()) {
            resultado = cursor.getInt(0) == 1
        }
        cursor.close()
        return resultado
    }
}