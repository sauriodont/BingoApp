package com.marco.bingoapp

import android.content.ContentValues
import android.graphics.*
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CrearCartonesActivity : AppCompatActivity() {

    private lateinit var dbHelper: BingoDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_cartones)

        dbHelper = BingoDbHelper(this)

        val txtCantidad = findViewById<EditText>(R.id.txtCantidad)
        val btnCrear = findViewById<Button>(R.id.btnCrear)

        btnCrear.setOnClickListener {
            val cantidad = txtCantidad.text.toString().toIntOrNull() ?: 0
            if (cantidad > 0) {
                val carpeta = obtenerCarpetaCartones()
                val fecha = obtenerFechaActual()

                for (i in 1..cantidad) {
                    val cartonJson = generarCartonJson()
                    insertarCarton("Jugador $i", cartonJson, fecha)

                    val gson = Gson()
                    val tipo = object : TypeToken<Map<String, List<Any>>>() {}.type
                    val carton: Map<String, List<Any>> = gson.fromJson(cartonJson, tipo)

                    val bitmap = generarImagenCarton(carton, i, fecha)
                    guardarImagen(bitmap, carpeta, "carton_$i")
                }

                Toast.makeText(this, "$cantidad cartones creados y guardados en la galería", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generarCartonJson(): String {
        val gson = Gson()
        val carton = mapOf(
            "B" to List(5) { (1..15).random() },
            "I" to List(5) { (16..30).random() },
            "N" to listOf((31..45).random(), (31..45).random(), "LIBRE", (31..45).random(), (31..45).random()),
            "G" to List(5) { (46..60).random() },
            "O" to List(5) { (61..75).random() }
        )
        return gson.toJson(carton)
    }

    private fun insertarCarton(jugador: String, cartonJson: String, fecha: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("jugador", jugador)
            put("numeros", cartonJson)
            put("fecha", fecha)
        }
        db.insert("cartones", null, values)
    }

    private fun obtenerFechaActual(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun obtenerCarpetaCartones(): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val cartonesDir = File(picturesDir, "Cartones")

        if (!cartonesDir.exists()) {
            cartonesDir.mkdirs()
        } else {
            cartonesDir.listFiles()?.forEach { it.delete() }
        }
        return cartonesDir
    }

    private fun generarImagenCarton(carton: Map<String, List<Any>>, numero: Int, fecha: String): Bitmap {
        val ancho = 600
        val alto = 800
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, ancho.toFloat(), alto.toFloat(), paint)

        val letras = listOf("B", "I", "N", "G", "O")
        paint.textSize = 50f
        paint.color = Color.BLUE
        for (i in letras.indices) {
            canvas.drawText(letras[i], 100f + i * 100, 100f, paint)
        }

        paint.textSize = 40f
        paint.color = Color.BLACK
        for ((col, letra) in letras.withIndex()) {
            val numeros = carton[letra] ?: emptyList()
            for ((fila, valor) in numeros.withIndex()) {
                val texto = valor.toString()
                canvas.drawText(texto, 100f + col * 100, 200f + fila * 100, paint)
            }
        }

        paint.textSize = 30f
        canvas.drawText("Cartón No. $numero", 50f, alto - 80f, paint)
        canvas.drawText("Fecha: $fecha", 50f, alto - 40f, paint)

        return bitmap
    }

    private fun guardarImagen(bitmap: Bitmap, carpeta: File, nombre: String) {
        val archivo = File(carpeta, "$nombre.png")
        val outputStream = FileOutputStream(archivo)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        MediaScannerConnection.scanFile(this, arrayOf(archivo.absolutePath), null, null)
    }
}