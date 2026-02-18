package com.marco.bingoapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class BingoDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "bdbingo.db"
        const val DATABASE_VERSION = 6

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
        const val COLUMN_MOD_ACTIVA = "activa"

        // Tabla Pagos
        const val TABLE_PAGOS = "pagos_sorteo"
        const val COLUMN_PAGO_ID = "id"
        const val COLUMN_PAGO_FECHA = "fecha"
        const val COLUMN_PAGO_DETALLE = "detalle_json"
        const val COLUMN_PAGO_TOTAL = "monto_total"

        // Tabla Estado Sorteo
        const val TABLE_ESTADO_SORTEO = "estado_sorteo"
        const val COLUMN_ESTADO_DATA = "detalle_json"
        const val COLUMN_EN_CURSO = "en_curso"
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
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_ESTADO_SORTEO (
                id INTEGER PRIMARY KEY, 
                $COLUMN_ESTADO_DATA TEXT,
                $COLUMN_EN_CURSO INTEGER DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_MODALIDADES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MOD_NOMBRE TEXT NOT NULL,
                $COLUMN_MOD_CONFIG TEXT NOT NULL,
                $COLUMN_MOD_ACTIVA INTEGER DEFAULT 0
            )
        """)

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
        if (oldVersion < 5) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_ESTADO_SORTEO")
            db.execSQL("CREATE TABLE $TABLE_ESTADO_SORTEO (id INTEGER PRIMARY KEY, $COLUMN_ESTADO_DATA TEXT, $COLUMN_EN_CURSO INTEGER DEFAULT 0)")
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE $TABLE_MODALIDADES ADD COLUMN $COLUMN_MOD_ACTIVA INTEGER DEFAULT 0")
            } catch (e: Exception) {
                Log.e("BingoDbHelper", "Error upgrading database", e)
            }
        }
    }

    // --- MÉTODOS DE MODALIDADES ---

    fun insertarModalidad(nombre: String, configuracion: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MOD_NOMBRE, nombre)
            put(COLUMN_MOD_CONFIG, configuracion)
            put(COLUMN_MOD_ACTIVA, 0)
        }
        return db.insert(TABLE_MODALIDADES, null, values)
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

    fun obtenerTodasLasModalidades(): Cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_MODALIDADES", null)

    fun obtenerModalidadesActivasParaSorteo(): Cursor {
        val db = this.readableDatabase
        // CORREGIDO: Usamos la columna 'activa' (no es_activa)
        return db.rawQuery("SELECT * FROM $TABLE_MODALIDADES WHERE $COLUMN_MOD_ACTIVA = 1", null)
    }

    fun setModalidadActiva(id: Int, activa: Boolean) {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COLUMN_MOD_ACTIVA, if (activa) 1 else 0) }
        db.update(TABLE_MODALIDADES, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun desactivarTodasLasModalidades() {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COLUMN_MOD_ACTIVA, 0) }
        db.update(TABLE_MODALIDADES, values, null, null)
    }

    // --- MÉTODOS DE CARTONES ---

    fun insertarCarton(jugador: String, numerosJson: String, fecha: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_JUGADOR, jugador)
            put(COLUMN_NUMEROS, numerosJson)
            put(COLUMN_FECHA, fecha)
            put(COLUMN_PAGADO, 0)
        }
        return db.insert(TABLE_CARTONES, null, values)
    }

    fun obtenerTodosLosCartones(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_CARTONES ORDER BY $COLUMN_ID ASC", null)
    }

    fun obtenerCartonesPagadosDetalle(): List<CartonObjeto> {
        val lista = mutableListOf<CartonObjeto>()
        val db = this.readableDatabase
        // CORREGIDO: Usamos nombres de columnas reales de la tabla cartones
        val cursor = db.rawQuery("SELECT $COLUMN_ID, $COLUMN_JUGADOR, $COLUMN_NUMEROS FROM $TABLE_CARTONES WHERE $COLUMN_PAGADO = 1", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val jugador = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_JUGADOR))
                val numeros = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUMEROS))
                lista.add(CartonObjeto(id, jugador, numeros))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun contarCartonesPagados(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CARTONES WHERE $COLUMN_PAGADO = 1", null)
        var count = 0
        if (cursor.moveToFirst()) count = cursor.getInt(0)
        cursor.close()
        return count
    }

    fun contarTotalCartones(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CARTONES", null)
        var count = 0
        if (cursor.moveToFirst()) count = cursor.getInt(0)
        cursor.close()
        return count
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

    // --- MÉTODOS DE PAGOS Y REPORTES ---

    fun guardarReportePago(totalVendido: Double, detalleJson: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PAGO_TOTAL, totalVendido)
            put(COLUMN_PAGO_DETALLE, detalleJson)
            put(COLUMN_PAGO_FECHA, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        return db.insert(TABLE_PAGOS, null, values)
    }

    fun obtenerUltimoReporte(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_PAGOS ORDER BY $COLUMN_PAGO_ID DESC LIMIT 1", null)
    }

    // --- MÉTODOS DE ESTADO DEL SORTEO ---

    fun establecerSorteoEnCurso(enCurso: Boolean) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("id", 1)
            put(COLUMN_EN_CURSO, if (enCurso) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_ESTADO_SORTEO, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun estaSorteoEnCurso(): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_EN_CURSO FROM $TABLE_ESTADO_SORTEO WHERE id = 1", null)
        var resultado = false
        if (cursor.moveToFirst()) resultado = cursor.getInt(0) == 1
        cursor.close()
        return resultado
    }


    fun obtenerCartonPorId(id: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_CARTONES WHERE $COLUMN_ID = ?", arrayOf(id))
    }

    // Método alias para mantener compatibilidad
    fun contarCartonesPagos(): Int = contarCartonesPagados()
}

data class CartonObjeto(
    val id: Int,
    val propietario: String,
    val numeros: String
)