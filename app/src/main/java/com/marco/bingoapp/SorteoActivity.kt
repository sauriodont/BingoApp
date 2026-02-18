package com.marco.bingoapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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

// Modelo local para evitar conflictos de declaración
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

        // Inicializar bolillero (1-75)
        reiniciarListas()

        // Configurar RecyclerView del Bolillero (Tablero de números salidos)
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
            if (validarCondicionesSorteo()) {
                ejecutarInicioSorteo()
            }
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

        // Botón opcional para compartir la miniatura actual
        binding.gridMiniatura.setOnClickListener {
            val nombreMod = binding.tvNombreModActual.text.toString()
            compartirMiniaturaComoImagen(binding.gridMiniatura, nombreMod)
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
        binding.spnModalidadesSorteo.isEnabled = false // Bloquear para evitar errores en juego

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
        if (listaTotal.isEmpty()) {
            Toast.makeText(this, "Bolillero vacío", Toast.LENGTH_SHORT).show()
            return
        }
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
    }

    private fun verificarGanadores() {
        val pos = binding.spnModalidadesSorteo.selectedItemPosition
        if (pos == AdapterView.INVALID_POSITION) return

        val modalidadActual = modalidadesActivas[pos]
        val tipoLista = object : TypeToken<List<Int>>() {}.type
        val indicesPatron: List<Int> = Gson().fromJson(modalidadActual.configuracion, tipoLista)

        // IMPORTANTE: Requiere que implementes obtenerCartonesPagadosDetalle en el Helper
        val cartonesPagados: List<CartonObjeto> = dbHelper.obtenerCartonesPagadosDetalle()
        val ganadores = mutableListOf<String>()

        for (carton in cartonesPagados) {
            val numerosCarton = carton.numeros.split(",").map { it.trim().toInt() }
            var aciertos = 0

            for (indice in indicesPatron) {
                if (indice == 12) { // Espacio libre
                    aciertos++
                } else {
                    val numEnCelda = numerosCarton[indice]
                    if (numerosSalidos.contains(numEnCelda)) aciertos++
                }
            }

            if (aciertos == indicesPatron.size) {
                ganadores.add("Cartón #${carton.id} - ${carton.propietario}")
            }
        }

        if (ganadores.isEmpty()) {
            Toast.makeText(this, "Aún no hay ganadores", Toast.LENGTH_SHORT).show()
        } else {
            mostrarDialogoGanador("¡BINGO!\n\n" + ganadores.joinToString("\n"))
        }
    }

    private fun mostrarDialogoGanador(mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle("¡Tenemos Ganador!")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("Seguir Sorteo") { d, _ -> d.dismiss() }
            .setNeutralButton("Cambiar Modalidad") { _, _ ->
                binding.spnModalidadesSorteo.isEnabled = true
            }
            .show()
    }

    private fun dibujarMiniatura(configuracionJson: String) {
        binding.gridMiniatura.removeAllViews()
        try {
            val tipoLista = object : TypeToken<List<Int>>() {}.type
            val celdasActivas: List<Int> = Gson().fromJson(configuracionJson, tipoLista)

            for (i in 0 until 25) {
                val celda = View(this)
                val params = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                ).apply {
                    width = 0
                    height = 0
                    setMargins(2, 2, 2, 2)
                }
                celda.layoutParams = params
                celda.setBackgroundColor(if (celdasActivas.contains(i)) Color.RED else Color.WHITE)
                binding.gridMiniatura.addView(celda)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun compartirMiniaturaComoImagen(view: View, nombreMod: String) {
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            val folder = File(externalCacheDir, "images")
            folder.mkdirs()
            val file = File(folder, "patron.png")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.close()

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir Patrón: $nombreMod"))
        } catch (e: Exception) { Toast.makeText(this, "Error al compartir", Toast.LENGTH_SHORT).show() }
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
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, "⚠️ $mensaje", Toast.LENGTH_LONG).show()
    }
}