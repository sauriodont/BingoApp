package com.marco.bingoapp

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.marco.bingoapp.databinding.ActivityModalidadesBinding

class ModalidadesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModalidadesBinding
    private lateinit var dbHelper: BingoDbHelper
    private lateinit var adapter: ModalidadesAdapter
    private val celdasSeleccionadas = Array(5) { BooleanArray(5) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModalidadesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BingoDbHelper(this)
        setupGridInteractivo()
        setupRecyclerView()

        binding.btnGuardarModalidad.setOnClickListener { validarYGuardar() }

        // Botón Limpiar (asegúrate de tenerlo en tu XML o añádelo)
        binding.btnLimpiarGrid.setOnClickListener { limpiarGrid() }
    }

    private fun setupGridInteractivo() {
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                val cellIdName = "cell_${i}_${j}"
                val resId = resources.getIdentifier(cellIdName, "id", packageName)
                val view = findViewById<TextView>(resId)

                // Celda central LIBRE por defecto
                if (i == 2 && j == 2) {
                    celdasSeleccionadas[i][j] = true
                    view.setBackgroundColor(Color.parseColor("#8BC34A"))
                }

                view.setOnClickListener {
                    if (i == 2 && j == 2) return@setOnClickListener // No desmarcar el centro

                    celdasSeleccionadas[i][j] = !celdasSeleccionadas[i][j]
                    view.setBackgroundColor(if (celdasSeleccionadas[i][j])
                        Color.parseColor("#8BC34A") else Color.parseColor("#2196F3"))
                }
            }
        }
    }

    private fun limpiarGrid() {
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                if (i == 2 && j == 2) continue
                celdasSeleccionadas[i][j] = false
                val resId = resources.getIdentifier("cell_${i}_${j}", "id", packageName)
                findViewById<TextView>(resId).setBackgroundColor(Color.parseColor("#2196F3"))
            }
        }
    }

    private fun setupRecyclerView() {
        val lista = cargarModalidadesDeBD()
        adapter = ModalidadesAdapter(lista) { modalidad ->
            dbHelper.eliminarModalidad(modalidad.id)
            actualizarLista()
        }
        binding.rvModalidades.layoutManager = LinearLayoutManager(this)
        binding.rvModalidades.adapter = adapter
    }

    private fun cargarModalidadesDeBD(): MutableList<Modalidad> {
        val lista = mutableListOf<Modalidad>()
        val cursor = dbHelper.obtenerTodasLasModalidades()
        if (cursor.moveToFirst()) {
            do {
                lista.add(Modalidad(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getString(cursor.getColumnIndexOrThrow("configuracion"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    private fun actualizarLista() {
        adapter.lista = cargarModalidadesDeBD()
        adapter.notifyDataSetChanged()
    }

    private fun validarYGuardar() {
        val nombre = binding.txtNombreModalidad.text.toString().trim()
        val coordenadas = mutableListOf<String>()

        for (i in 0 until 5) {
            for (j in 0 until 5) {
                if (celdasSeleccionadas[i][j]) coordenadas.add("$i,$j")
            }
        }

        val configString = coordenadas.joinToString(";")

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show()
            return
        }

        // VALIDACIÓN DE DUPLICADOS
        if (dbHelper.existeModalidad(nombre, configString)) {
            Toast.makeText(this, "El nombre o el patrón ya existen", Toast.LENGTH_LONG).show()
            return
        }

        val id = dbHelper.insertarModalidad(nombre, configString)
        if (id != -1L) {
            Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show()
            binding.txtNombreModalidad.text.clear()
            limpiarGrid()
            actualizarLista()
        }
    }
}