package com.marco.bingoapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.marco.bingoapp.databinding.ActivityVentasBinding

class VentasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVentasBinding
    private lateinit var dbHelper: BingoDbHelper
    private var listaIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVentasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BingoDbHelper(this)
        cargarSpinner()
    }

    private fun cargarSpinner() {
        val cursor = dbHelper.obtenerTodosLosCartones()
        listaIds.clear()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                listaIds.add(id.toString())
            } while (cursor.moveToNext())
        }
        cursor.close()

        if (listaIds.isEmpty()) {
            Toast.makeText(this, "ERROR: No hay cartones. Ve a 'Crear Cartones' primero.", Toast.LENGTH_LONG).show()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaIds)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnCartones.adapter = adapter
    }
}