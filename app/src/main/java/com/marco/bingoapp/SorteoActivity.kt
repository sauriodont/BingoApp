package com.marco.bingoapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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

class SorteoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySorteoBinding
    private lateinit var dbHelper: BingoDbHelper
    private lateinit var adapterBolillero: BolilleroAdapter

    private var listaTotal = (1..75).toMutableList()
    private var numerosSalidos = mutableSetOf<Int>()
    private var modalidadesSorteo = mutableListOf<ModalidadObjeto>()
    private var modalidadSeleccionada: ModalidadObjeto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySorteoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BingoDbHelper(this)

        // 1. Configurar UI Inicial
        setupBolillero()
        cargarModalidadesDisponibles()

        // 2. Recuperar estado si el sorteo ya estaba en curso
        if (dbHelper.estaSorteoEnCurso()) {
            cargarEstadoPartida()
            binding.btnIniciarSorteo.visibility = View.GONE
            binding.btnSacarNumero.isEnabled = true
        } else {
            binding.btnSacarNumero.isEnabled = false
            verificarRequisitosPrevios()
        }

        // 3. Listeners
        binding.btnIniciarSorteo.setOnClickListener { iniciarSorteo() }
        binding.btnSacarNumero.setOnClickListener { sacarNumero() }
        binding.btnCopiarBolillero.setOnClickListener { copiarVistaComoImagen(binding.rvBolillero) }
        binding.containerMiniatura.setOnClickListener { copiarVistaComoImagen(binding.gridMiniatura) }
        binding.btnBingo.setOnClickListener { confirmarBingo() }
        binding.btnFinalizarSorteo.setOnClickListener { finalizarSorteoTotal() }
    }

    private fun verificarRequisitosPrevios() {
        val cursor = dbHelper.obtenerUltimoReporte()
        if (cursor.count == 0) {
            Toast.makeText(this, "⚠️ Bloqueado: Primero guarda el reporte de pagos.", Toast.LENGTH_LONG).show()
            binding.btnIniciarSorteo.isEnabled = false
        }
        cursor.close()
    }

    private fun setupBolillero() {
        adapterBolillero = BolilleroAdapter(numerosSalidos)
        binding.rvBolillero.layoutManager = GridLayoutManager(this, 10)
        binding.rvBolillero.adapter = adapterBolillero
    }

    private fun cargarModalidadesDisponibles() {
        val cursor = dbHelper.obtenerTodasLasModalidades()
        modalidadesSorteo.clear()
        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
                val config = cursor.getString(cursor.getColumnIndexOrThrow("configuracion"))
                modalidadesSorteo.add(ModalidadObjeto(nombre, config))
            } while (cursor.moveToNext())
        }
        cursor.close()

        val nombres = modalidadesSorteo.map { it.nombre }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
        binding.spnModalidadesSorteo.adapter = adapter

        binding.spnModalidadesSorteo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                modalidadSeleccionada = modalidadesSorteo[pos]
                dibujarMiniatura(modalidadSeleccionada?.configuracion ?: "")
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun dibujarMiniatura(configJson: String) {
        binding.gridMiniatura.removeAllViews()
        val config = Gson().fromJson(configJson, Array<IntArray>::class.java) ?: return
        for (i in 0..4) {
            for (j in 0..4) {
                val view = View(this)
                val params = android.widget.GridLayout.LayoutParams()
                params.width = 25
                params.height = 25
                params.setMargins(2, 2, 2, 2)
                view.layoutParams = params
                view.setBackgroundColor(if (config[i][j] == 1 || (i == 2 && j == 2)) Color.YELLOW else Color.DKGRAY)
                binding.gridMiniatura.addView(view)
            }
        }
    }

    private fun iniciarSorteo() {
        dbHelper.establecerSorteoEnCurso(true)
        binding.btnIniciarSorteo.visibility = View.GONE
        binding.btnSacarNumero.isEnabled = true
        Toast.makeText(this, "🔒 Sorteo iniciado y bloqueado", Toast.LENGTH_SHORT).show()
    }

    private fun sacarNumero() {
        if (listaTotal.isEmpty()) {
            Toast.makeText(this, "¡Ya salieron todos los números!", Toast.LENGTH_SHORT).show()
            return
        }

        val num = listaTotal.random()
        listaTotal.remove(num)
        numerosSalidos.add(num)

        // CORRECCIÓN DEL WHEN: Estructura clara con flechas
        val letra = when (num) {
            in 1..15 -> "B"
            in 16..30 -> "I"
            in 31..45 -> "N"
            in 46..60 -> "G"
            else -> "O"
        }

        binding.tvUltimaLetra.text = letra
        binding.tvUltimoNumero.text = num.toString()

        // Copiar formato O69 al portapapeles
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Bingo", "$letra$num")
        clipboard.setPrimaryClip(clip)

        adapterBolillero.notifyDataSetChanged()
        verificarCartonesGanadores()
        guardarEstadoPartida()
    }

    private fun verificarCartonesGanadores() {
        if (modalidadSeleccionada == null) return
        val patron = Gson().fromJson(modalidadSeleccionada?.configuracion, Array<IntArray>::class.java)
        val cursor = dbHelper.obtenerTodosLosCartones() // Deberías filtrar por pagado = 1 en el Helper
        val ganadores = mutableListOf<String>()

        if (cursor.moveToFirst()) {
            do {
                if (cursor.getInt(cursor.getColumnIndexOrThrow("pagado")) == 1) {
                    val matriz = Gson().fromJson(cursor.getString(cursor.getColumnIndexOrThrow("numeros")), Array<IntArray>::class.java)
                    if (esGanador(matriz, patron)) {
                        ganadores.add("ID: ${cursor.getInt(0)} - ${cursor.getString(1)}")
                    }
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        binding.tvGanadoresInfo.text = if (ganadores.isEmpty()) "Buscando..." else "🏆 GANADORES:\n${ganadores.joinToString("\n")}"
    }

    private fun esGanador(carton: Array<IntArray>, patron: Array<IntArray>): Boolean {
        for (i in 0..4) {
            for (j in 0..4) {
                if (patron[i][j] == 1 && !(i == 2 && j == 2)) {
                    if (!numerosSalidos.contains(carton[i][j])) return false
                }
            }
        }
        return true
    }

    private fun copiarVistaComoImagen(view: View) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val file = File(cacheDir, "shared_image.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val clip = ClipData.newUri(contentResolver, "BingoImage", uri)
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        Toast.makeText(this, "📸 Imagen copiada al portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun guardarEstadoPartida() {
        val json = Gson().toJson(numerosSalidos)
        val db = dbHelper.writableDatabase
        val values = android.content.ContentValues().apply {
            put("id", 1)
            put("detalle_json", json)
        }
        db.insertWithOnConflict("estado_sorteo", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun cargarEstadoPartida() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM estado_sorteo WHERE id = 1", null)
        if (cursor.moveToFirst()) {
            val json = cursor.getString(cursor.getColumnIndexOrThrow("detalle_json"))
            val type = object : TypeToken<MutableSet<Int>>() {}.type
            val recuperados: MutableSet<Int> = Gson().fromJson(json, type)
            numerosSalidos.addAll(recuperados)
            listaTotal.removeAll(recuperados)
            adapterBolillero.notifyDataSetChanged()
        }
        cursor.close()
    }

    private fun confirmarBingo() {
        AlertDialog.Builder(this)
            .setTitle("¿BINGO CONFIRMADO?")
            .setMessage("Se registrará la modalidad como finalizada.")
            .setPositiveButton("SÍ") { _, _ ->
                // Aquí podrías quitar la modalidad del Spinner o marcarla como jugada
                Toast.makeText(this, "Bingo registrado", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("NO", null).show()
    }

    private fun finalizarSorteoTotal() {
        AlertDialog.Builder(this)
            .setTitle("¿FINALIZAR TODO EL SORTEO?")
            .setPositiveButton("SÍ") { _, _ ->
                dbHelper.establecerSorteoEnCurso(false)
                // Aquí llamarás al PDF Generator
                finish()
            }.setNegativeButton("VOLVER", null).show()
    }
}
data class ModalidadObjeto(val nombre: String, val configuracion: String)