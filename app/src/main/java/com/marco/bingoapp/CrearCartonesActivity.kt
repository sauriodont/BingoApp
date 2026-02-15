package com.marco.bingoapp

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
    private var rutaImagenFondo: String? = null
    private var colorEncabezado: Int = Color.BLUE
    private lateinit var imgFondoPreview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_cartones)

        dbHelper = BingoDbHelper(this)

        val txtCantidad = findViewById<EditText>(R.id.txtCantidad)
        val btnImagenFondo = findViewById<Button>(R.id.btnImagenFondo)
        val btnColorEncabezado = findViewById<Button>(R.id.btnColorEncabezado)
        val btnVistaPrevia = findViewById<Button>(R.id.btnVistaPrevia)
        val btnCrear = findViewById<Button>(R.id.btnCrear)
        val imgVistaPrevia = findViewById<ImageView>(R.id.imgVistaPrevia)
        imgFondoPreview = findViewById(R.id.imgVistaPrevia) // pequeño preview del fondo

        // Seleccionar imagen de fondo
        btnImagenFondo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 100)
        }

        // Seleccionar color encabezado (ejemplo simple)
        btnColorEncabezado.setOnClickListener {
            colorEncabezado = Color.RED
            Toast.makeText(this, "Color encabezado cambiado", Toast.LENGTH_SHORT).show()
        }

        // Vista previa
        btnVistaPrevia.setOnClickListener {
            val fecha = obtenerFechaActual()
            val cartonJson = generarCartonJson()
            val gson = Gson()
            val tipo = object : TypeToken<Map<String, List<Any>>>() {}.type
            val carton: Map<String, List<Any>> = gson.fromJson(cartonJson, tipo)
            val bitmap = generarImagenCarton(carton, 1, fecha)
            imgVistaPrevia.setImageBitmap(bitmap)
        }

        // Crear cartones
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

    // Obtener ruta real del archivo desde un Uri
    private fun getRealPathFromURI(contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = contentResolver.query(contentUri, proj, null, null, null)
            val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            cursor?.getString(columnIndex ?: 0)
        } finally {
            cursor?.close()
        }
    }

    // Recibir imagen seleccionada
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val selectedImage: Uri? = data?.data
            selectedImage?.let {
                rutaImagenFondo = getRealPathFromURI(it)
                imgFondoPreview.setImageURI(it) // mostrar preview
                Toast.makeText(this, "Imagen seleccionada correctamente", Toast.LENGTH_SHORT).show()
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

    private fun guardarImagen(bitmap: Bitmap, carpeta: File, nombre: String) {
        val archivo = File(carpeta, "$nombre.png")
        val outputStream = FileOutputStream(archivo)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        MediaScannerConnection.scanFile(this, arrayOf(archivo.absolutePath), null, null)
    }

    private fun generarImagenCarton(carton: Map<String, List<Any>>, numero: Int, fecha: String): Bitmap {
        val cellSize = 100f
        val columnas = 5
        val filas = 7 // 1 fila BINGO + 5 filas números + 1 fila número/fecha
        val ancho = (columnas * cellSize + 100).toInt()
        val alto = (filas * cellSize + 150).toInt()
        val startX = 50f
        val startY = 50f

        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Fondo blanco inicial
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, ancho.toFloat(), alto.toFloat(), paint)

        // Dibujar cuadrícula y contenido
        val gridPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

        for (row in 0 until filas) {
            if (row == filas - 1) {
                val lastRowTop = startY + row * cellSize
                val lastRowBottom = lastRowTop + cellSize
                val midX = startX + (columnas * cellSize) / 2
                canvas.drawRect(startX, lastRowTop, midX, lastRowBottom, gridPaint)
                canvas.drawRect(midX, lastRowTop, startX + columnas * cellSize, lastRowBottom, gridPaint)
            } else {
                for (col in 0 until columnas) {
                    val left = startX + col * cellSize
                    val top = startY + row * cellSize
                    val right = left + cellSize
                    val bottom = top + cellSize
                    canvas.drawRect(left, top, right, bottom, gridPaint)
                }
            }
        }

        // Borde externo
        canvas.drawRect(startX, startY, startX + columnas * cellSize, startY + filas * cellSize, borderPaint)

        // Encabezado BINGO
        val letras = listOf("B", "I", "N", "G", "O")
        paint.textSize = 50f
        paint.color = colorEncabezado
        paint.textAlign = Paint.Align.CENTER
        for (i in letras.indices) {
            val x = startX + i * cellSize + cellSize / 2
            val y = startY + cellSize / 2 + 15f
            canvas.drawText(letras[i], x, y, paint)
        }

        // Números
        paint.textSize = 36f
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        for ((col, letra) in letras.withIndex()) {
            val numeros = carton[letra] ?: emptyList()
            for ((fila, valor) in numeros.withIndex()) {
                val texto = if (valor is Number) valor.toInt().toString() else valor.toString()
                val x = startX + col * cellSize + cellSize / 2
                val y = startY + (fila + 1) * cellSize + cellSize / 2 + 12f

                if (texto == "LIBRE") {
                    val librePaint = Paint().apply {
                        color = Color.YELLOW
                        style = Paint.Style.FILL
                    }
                    val left = startX + col * cellSize
                    val top = startY + (fila + 1) * cellSize
                    val right = left + cellSize
                    val bottom = top + cellSize
                    canvas.drawRect(left, top, right, bottom, librePaint)
                }
                canvas.drawText(texto, x, y, paint)
            }
        }

        // Última fila: número y fecha
        val lastRowTop = startY + (filas - 1) * cellSize
        val midX = startX + (columnas * cellSize) / 2

        paint.textSize = 28f
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER

        val leftCellX = startX + (midX - startX) / 2
        val leftCellY = lastRowTop + cellSize / 2 + 10f
        canvas.drawText("Cartón No. $numero", leftCellX, leftCellY, paint)

        val rightCellX = midX + (startX + columnas * cellSize - midX) / 2
        val rightCellY = lastRowTop + cellSize / 2 + 10f
        canvas.drawText("Fecha: $fecha", rightCellX, rightCellY, paint)

        // 🔑 Superponer la imagen seleccionada como marca de agua transparente
        rutaImagenFondo?.let {
            val fondo = BitmapFactory.decodeFile(it)
            fondo?.let { bmp ->
                val scaled = Bitmap.createScaledBitmap(
                    bmp,
                    (columnas * cellSize).toInt(),
                    (filas * cellSize).toInt(),
                    true
                )
                val paintFondo = Paint().apply { alpha = 80 } // transparencia baja
                canvas.drawBitmap(scaled, startX, startY, paintFondo)
            }
        }

        return bitmap
    }
}