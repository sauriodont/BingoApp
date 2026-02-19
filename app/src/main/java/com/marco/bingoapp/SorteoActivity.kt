package com.marco.bingoapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.marco.bingoapp.databinding.ActivitySorteoBinding
import java.io.File
import java.io.FileOutputStream

data class ModalidadSorteo(val nombre: String, val configuracion: String)

class SorteoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySorteoBinding
    private lateinit var dbHelper: BingoDbHelper
    private var listaTotal = mutableListOf<Int>()
    private var numerosSalidos = mutableListOf<Int>()
    private lateinit var adapterBolillero: BolilleroAdapter
    private var sorteoIniciado = false
    private var modalidadesActivas = mutableListOf<ModalidadSorteo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySorteoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BingoDbHelper(this)
        reiniciarListas()

        // Configurar RecyclerView del Bolillero
        adapterBolillero = BolilleroAdapter(numerosSalidos)
        binding.rvBolillero.layoutManager = GridLayoutManager(this, 5)
        binding.rvBolillero.adapter = adapterBolillero

        cargarModalidadesDesdePagos()
        setupBotones()
    }

    private fun setupBotones() {
        binding.btnSacarNumero.isEnabled = false
        binding.btnBingo.isEnabled = false

        binding.btnIniciarSorteo.setOnClickListener {
            if (validarCondicionesSorteo()) ejecutarInicioSorteo()
        }

        // Botón Copiar: Comparte el bolillero escalado
        binding.btnCopiarBolillero.setOnClickListener {
            compartirBolilleroComoImagen(binding.rvBolillero)
        }

        // Capa invisible sobre el bolillero para toque directo
        try {
            val capa = binding.root.findViewById<View>(R.id.capaClicBolillero)
            capa?.setOnClickListener {
                compartirBolilleroComoImagen(binding.rvBolillero)
            }
        } catch (e: Exception) {}

        // Click en la miniatura del patrón
        binding.gridMiniatura.setOnClickListener {
            val nombreMod = binding.tvNombreModActual.text.toString()
            compartirMiniaturaComoImagen(binding.gridMiniatura, nombreMod)
        }

        binding.btnSacarNumero.setOnClickListener {
            if (sorteoIniciado) sacarNumero()
        }

        binding.btnBingo.setOnClickListener {
            verificarGanadores()
        }

        binding.btnFinalizarSorteo.setOnClickListener {
            dbHelper.establecerSorteoEnCurso(false)
            finish()
        }
    }

    private fun validarCondicionesSorteo(): Boolean {
        if (modalidadesActivas.isEmpty()) {
            mostrarError("No hay modalidades activas. Configúralas en Pagos.")
            return false
        }
        if (dbHelper.contarCartonesPagos() == 0) {
            mostrarError("No hay cartones pagados para sortear.")
            return false
        }
        return true
    }

    private fun ejecutarInicioSorteo() {
        sorteoIniciado = true
        binding.btnSacarNumero.isEnabled = true
        binding.btnBingo.isEnabled = true
        binding.spnModalidadesSorteo.isEnabled = false
        reiniciarSorteoCompleto()
        Toast.makeText(this, "¡Sorteo Iniciado!", Toast.LENGTH_SHORT).show()
    }

    private fun cargarModalidadesDesdePagos() {
        modalidadesActivas.clear()
        val cursor = dbHelper.obtenerModalidadesActivasParaSorteo()
        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
                val config = cursor.getString(cursor.getColumnIndexOrThrow("configuracion"))
                modalidadesActivas.add(ModalidadSorteo(nombre, config))
            } while (cursor.moveToNext())
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modalidadesActivas.map { it.nombre })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnModalidadesSorteo.adapter = adapter

        binding.spnModalidadesSorteo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                if (modalidadesActivas.isNotEmpty()) {
                    val mod = modalidadesActivas[pos]
                    binding.tvNombreModActual.text = mod.nombre
                    dibujarMiniatura(mod.configuracion)
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun sacarNumero() {
        if (listaTotal.isEmpty()) return
        val num = listaTotal.random()
        listaTotal.remove(num)
        numerosSalidos.add(num)

        val letra = when (num) {
            in 1..15 -> "B"
            in 16..30 -> "I"
            in 31..45 -> "N"
            in 46..60 -> "G"
            else -> "O"
        }

        binding.tvUltimaLetra.text = letra
        binding.tvUltimoNumero.text = num.toString()
        adapterBolillero.notifyDataSetChanged()
        binding.rvBolillero.scrollToPosition(numerosSalidos.size - 1)
        actualizarConteo()
    }

    private fun actualizarConteo() {
        binding.tvConteoNumeros.text = "Números salidos: ${numerosSalidos.size} de 75"
    }

    private fun verificarGanadores() {
        val pos = binding.spnModalidadesSorteo.selectedItemPosition
        if (pos == AdapterView.INVALID_POSITION) return

        val modalidadActual = modalidadesActivas[pos]
        val indicesPatron: List<Int> = Gson().fromJson(modalidadActual.configuracion, object : TypeToken<List<Int>>() {}.type)
        val cartonesPagados = dbHelper.obtenerCartonesPagadosDetalle()
        val ganadores = mutableListOf<String>()

        for (carton in cartonesPagados) {
            val numerosCarton = carton.numeros.split(",").map { it.trim().toInt() }
            var aciertos = 0
            for (indice in indicesPatron) {
                if (indice == 12 || (indice < numerosCarton.size && numerosSalidos.contains(numerosCarton[indice]))) aciertos++
            }
            if (aciertos == indicesPatron.size) ganadores.add("Cartón #${carton.id} - ${carton.propietario}")
        }

        if (ganadores.isEmpty()) {
            Toast.makeText(this, "Aún no hay ganadores", Toast.LENGTH_SHORT).show()
        } else {
            val textoGanadores = ganadores.joinToString("\n")
            binding.tvGanadoresInfo.text = textoGanadores
            mostrarDialogoGanador("¡BINGO!\n\n$textoGanadores")
        }
    }

    private fun mostrarDialogoGanador(mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle("¡Tenemos Ganador!")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("Seguir Sorteo") { d, _ -> d.dismiss() }
            .setNeutralButton("Cambiar Modalidad") { _, _ -> binding.spnModalidadesSorteo.isEnabled = true }
            .show()
    }

    private fun dibujarMiniatura(configuracionJson: String) {
        binding.gridMiniatura.removeAllViews()
        try {
            val celdasActivas: List<Int> = Gson().fromJson(configuracionJson, object : TypeToken<List<Int>>() {}.type)
            for (i in 0 until 25) {
                val celda = View(this)
                val params = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                    width = 0; height = 0; setMargins(2, 2, 2, 2)
                }
                celda.layoutParams = params
                celda.setBackgroundColor(if (celdasActivas.contains(i)) Color.RED else Color.WHITE)
                binding.gridMiniatura.addView(celda)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun compartirMiniaturaComoImagen(view: View, nombreModalidad: String) {
        compartirConEfectoTarjeta(view, "patron.png", "¡Atención! Modalidad: $nombreModalidad", 0.8f)
    }

    private fun compartirBolilleroComoImagen(view: View) {
        compartirConEfectoTarjeta(view, "bolillero.png", "📊 Estado del Bolillero (${numerosSalidos.size}/75)", 0.5f)
    }

    // Función que escala la imagen Y le añade un lienzo blanco alrededor (Efecto Tarjeta)
    private fun compartirConEfectoTarjeta(view: View, fileName: String, mensaje: String, factorEscala: Float) {
        try {
            if (view.width == 0 || view.height == 0) return

            // 1. Crear bitmap de la vista original
            val bitmapOriginal = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvasOriginal = Canvas(bitmapOriginal)
            canvasOriginal.drawColor(Color.WHITE)
            view.draw(canvasOriginal)

            // 2. Escalar el contenido
            val anchoEscalado = (view.width * factorEscala).toInt()
            val altoEscalado = (view.height * factorEscala).toInt()
            val bitmapEscalado = Bitmap.createScaledBitmap(bitmapOriginal, anchoEscalado, altoEscalado, true)

            // 3. Crear Lienzo (Padding blanco) para evitar que WhatsApp la estire
            // Añadimos un margen del 25% del ancho escalado
            val margen = (anchoEscalado * 0.50).toInt()
            val anchoLienzo = anchoEscalado + (margen * 2)
            val altoLienzo = altoEscalado + (margen * 2)

            val lienzoFinal = Bitmap.createBitmap(anchoLienzo, altoLienzo, Bitmap.Config.ARGB_8888)
            val canvasFinal = Canvas(lienzoFinal)
            canvasFinal.drawColor(Color.WHITE) // Fondo del "papel"

            // Dibujar el contenido escalado en el centro del lienzo
            canvasFinal.drawBitmap(bitmapEscalado, margen.toFloat(), margen.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))

            // 4. Guardar archivo con nombre único (evita caché de WhatsApp)
            val nombreUnico = "${System.currentTimeMillis()}_$fileName"
            val folder = File(externalCacheDir, "images").apply { mkdirs() }
            folder.listFiles()?.forEach { it.delete() } // Limpiar anteriores

            val file = File(folder, nombreUnico)
            val out = FileOutputStream(file)
            lienzoFinal.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.close()

            // 5. Compartir
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, mensaje)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Enviar Bingo"))

            bitmapOriginal.recycle()
            bitmapEscalado.recycle()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al generar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reiniciarListas() {
        listaTotal.clear()
        for (i in 1..75) listaTotal.add(i)
        numerosSalidos.clear()
    }

    private fun reiniciarSorteoCompleto() {
        reiniciarListas()
        binding.tvUltimaLetra.text = "-"
        binding.tvUltimoNumero.text = "00"
        adapterBolillero.notifyDataSetChanged()
        actualizarConteo()
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, "⚠️ $mensaje", Toast.LENGTH_LONG).show()
    }
}