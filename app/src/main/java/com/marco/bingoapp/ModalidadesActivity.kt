package com.marco.bingoapp

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// Asegúrate de que el paquete sea el correcto según tu proyecto
import com.marco.bingoapp.databinding.ActivityModalidadesBinding

class ModalidadesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModalidadesBinding
    // Matriz para rastrear qué celdas están activas
    private val celdasSeleccionadas = Array(5) { BooleanArray(5) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar el binding
        binding = ActivityModalidadesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGridInteractivo()

        // El ID en el XML es btnGuardarModalidad
        binding.btnGuardarModalidad.setOnClickListener {
            val nombre = binding.txtNombreModalidad.text.toString()
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Escribe un nombre para la modalidad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            guardarModalidad(nombre)
        }
    }

    private fun setupGridInteractivo() {
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                val cellIdName = "cell_${i}_${j}"
                val resId = resources.getIdentifier(cellIdName, "id", packageName)
                val view = findViewById<TextView>(resId)

                // Si es la celda central, la dejamos marcada por defecto
                if (i == 2 && j == 2) {
                    celdasSeleccionadas[i][j] = true
                }

                view.setOnClickListener {
                    celdasSeleccionadas[i][j] = !celdasSeleccionadas[i][j]

                    if (celdasSeleccionadas[i][j]) {
                        view.setBackgroundColor(Color.parseColor("#8BC34A")) // Verde
                    } else {
                        view.setBackgroundColor(Color.parseColor("#2196F3")) // Azul
                    }

                    // Mantenemos el texto "LIBRE" si es la celda central
                    if (i == 2 && j == 2) {
                        view.text = "LIBRE"
                    }
                }
            }
        }
    }

    private fun guardarModalidad(nombre: String) {
        val coordenadas = mutableListOf<String>()
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                if (celdasSeleccionadas[i][j]) {
                    coordenadas.add("$i,$j")
                }
            }
        }

        if (coordenadas.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una celda del patrón", Toast.LENGTH_SHORT).show()
            return
        }

        // Convertir lista a String: "0,0;0,1;4,4"
        val configString = coordenadas.joinToString(";")

        val dbHelper = BingoDbHelper(this)
        val id = dbHelper.insertarModalidad(nombre, configString)

        if (id != -1L) {
            Toast.makeText(this, "Modalidad '$nombre' guardada con éxito", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Error al guardar en la base de datos", Toast.LENGTH_SHORT).show()
        }
    }
}