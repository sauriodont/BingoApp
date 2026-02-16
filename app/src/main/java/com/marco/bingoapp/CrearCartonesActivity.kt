package com.marco.bingoapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CrearCartonesActivity : AppCompatActivity() {

    private lateinit var dbHelper: BingoDbHelper
    private var uriImagenFondo: Uri? = null
    private var colorPersonalizado: Int = Color.parseColor("#3F51B5")
    private lateinit var imgFondoPreview: ImageView

    // 1. Lanzador moderno para la galería (Reemplaza startActivityForResult)
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            uriImagenFreq(result.data?.data)
        }
    }

    private fun uriImagenFreq(uri: Uri?) {
        uriImagenFondo = uri
        uri?.let { imgFondoPreview.setImageURI(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_cartones)

        dbHelper = BingoDbHelper(this)

        val txtCantidad = findViewById<EditText>(R.id.txtCantidad)
        val btnImagenFondo = findViewById<Button>(R.id.btnImagenFondo)
        val btnColorEncabezado = findViewById<Button>(R.id.btnColorEncabezado)
        val btnVistaPrevia = findViewById<Button>(R.id.btnVistaPrevia)
        val btnCrear = findViewById<Button>(R.id.btnCrear)
        imgFondoPreview = findViewById(R.id.imgVistaPrevia)

        // Botón: Seleccionar imagen
        btnImagenFondo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }

        // Botón: Color
        btnColorEncabezado.setOnClickListener { mostrarSelectorColores() }

        // Botón: Vista Previa
        btnVistaPrevia.setOnClickListener {
            val fecha = obtenerFechaActual()
            val cartonData = generarEstructuraCarton()
            val bitmap = generarImagenCarton(cartonData, 0, fecha)
            imgFondoPreview.setImageBitmap(bitmap)
        }

        // Botón: Crear Cartones
        btnCrear.setOnClickListener {
            val cantidadTexto = txtCantidad.text.toString()
            if (cantidadTexto.isEmpty()) {
                txtCantidad.error = "Ingresa una cantidad"
                return@setOnClickListener
            }

            val cantidad = cantidadTexto.toInt()
            if (cantidad <= 0) return@setOnClickListener

            Toast.makeText(this, "Generando y guardando $cantidad cartones...", Toast.LENGTH_SHORT).show()

            val fecha = obtenerFechaActual()
            val carpeta = obtenerCarpetaCartonesLimpiarAntiguos()

            // Paso crítico: Limpiar base de datos para nueva serie
            dbHelper.borrarTodosLosCartones()

            // Pequeño retraso para permitir que el Toast se muestre antes del bucle pesado
            txtCantidad.postDelayed({
                try {
                    for (i in 1..cantidad) {
                        val cartonData = generarEstructuraCarton()
                        val cartonJson = Gson().toJson(cartonData)

                        // 2. GUARDAR EN BD: Esto habilita los datos para la pantalla VENTAS
                        dbHelper.insertarCarton("Jugador $i", cartonJson, fecha)

                        // 3. GENERAR IMAGEN: Archivo físico para compartir
                        val bitmap = generarImagenCarton(cartonData, i, fecha)
                        guardarImagen(bitmap, carpeta, "carton_numero_$i")
                        bitmap.recycle()
                    }

                    Toast.makeText(this, "¡Éxito! Base de datos y galería actualizadas.", Toast.LENGTH_LONG).show()
                    txtCantidad.text.clear()

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }, 500)
        }
    }

    private fun mostrarSelectorColores() {
        val colores = intArrayOf(Color.RED, Color.BLUE, Color.BLACK, Color.parseColor("#4CAF50"),
            Color.parseColor("#E91E63"), Color.parseColor("#FF9800"), Color.parseColor("#9C27B0"))
        val nombres = arrayOf("Rojo", "Azul", "Negro", "Verde", "Rosa", "Naranja", "Morado")

        AlertDialog.Builder(this)
            .setTitle("Color de diseño")
            .setItems(nombres) { _, i ->
                colorPersonalizado = colores[i]
            }.show()
    }

    private fun generarEstructuraCarton(): Map<String, List<Any>> {
        fun col(r: IntRange): List<Any> = r.shuffled().take(5).sorted()
        val nCol = (31..45).shuffled().take(4).toMutableList<Any>()
        nCol.add(2, "LIBRE")
        return mapOf("B" to col(1..15), "I" to col(16..30), "N" to nCol, "G" to col(46..60), "O" to col(61..75))
    }

    private fun obtenerFechaActual() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    private fun obtenerCarpetaCartonesLimpiarAntiguos(): File {
        val cartonesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CartonesBingo")
        if (cartonesDir.exists()) cartonesDir.listFiles()?.forEach { it.delete() } else cartonesDir.mkdirs()
        return cartonesDir
    }

    private fun guardarImagen(bitmap: Bitmap, carpeta: File, nombre: String) {
        val archivo = File(carpeta, "$nombre.png")
        FileOutputStream(archivo).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        MediaScannerConnection.scanFile(this, arrayOf(archivo.absolutePath), null, null)
    }

    private fun generarImagenCarton(carton: Map<String, List<Any>>, numero: Int, fecha: String): Bitmap {
        val cellSize = 120f
        val startX = 50f
        val startY = 50f
        val ancho = (5 * cellSize + 100).toInt()
        val alto = (7 * cellSize + 100).toInt()

        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)

        // Marca de agua
        uriImagenFondo?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    val rect = RectF(startX, startY + cellSize, startX + 5 * cellSize, startY + 6 * cellSize)
                    canvas.drawBitmap(bmp, null, rect, Paint().apply { alpha = 65 })
                }
            } catch (e: Exception) {}
        }

        // Estilos de Header/Footer
        paint.color = colorPersonalizado
        canvas.drawRect(startX, startY, startX + 5 * cellSize, startY + cellSize, paint)
        canvas.drawRect(startX, startY + 6 * cellSize, startX + 5 * cellSize, startY + 7 * cellSize, paint)

        // Cuadrícula
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        for (i in 0..7) canvas.drawLine(startX, startY + i * cellSize, startX + 5 * cellSize, startY + i * cellSize, paint)
        for (i in 0..5) canvas.drawLine(startX + i * cellSize, startY, startX + i * cellSize, startY + 6 * cellSize, paint)

        // Dibujar Números y Letras
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true

        // BINGO
        paint.color = Color.WHITE; paint.textSize = 60f
        val letras = listOf("B", "I", "N", "G", "O")
        letras.forEachIndexed { i, s -> canvas.drawText(s, startX + i * cellSize + cellSize/2, startY + cellSize/2 + 20f, paint) }

        // Números del cartón
        letras.forEachIndexed { colIndex, letra ->
            val filaDatos = carton[letra] as List<*>
            filaDatos.forEachIndexed { filaIndex, valor ->
                val x = startX + colIndex * cellSize + cellSize/2
                val y = startY + (filaIndex + 1) * cellSize + cellSize/2 + 15f
                if (valor == "LIBRE") {
                    canvas.drawRect(startX + colIndex * cellSize + 10, startY + (filaIndex + 1) * cellSize + 10, startX + (colIndex + 1) * cellSize - 10, startY + (filaIndex + 2) * cellSize - 10, Paint().apply { color = colorPersonalizado })
                    paint.color = Color.WHITE; paint.textSize = 25f
                    canvas.drawText("LIBRE", x, y, paint)
                } else {
                    paint.color = Color.BLACK; paint.textSize = 40f
                    canvas.drawText(valor.toString(), x, y, paint)
                }
            }
        }

        // Footer
        paint.color = Color.WHITE; paint.textSize = 30f
        canvas.drawText("Cartón #$numero", startX + 1.25f * cellSize, startY + 6.5f * cellSize + 10f, paint)
        canvas.drawText(fecha, startX + 3.75f * cellSize, startY + 6.5f * cellSize + 10f, paint)

        return bitmap
    }
}