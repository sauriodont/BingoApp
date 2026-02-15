package com.marco.bingoapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.marco.bingoapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCrearCartones.setOnClickListener {
            Toast.makeText(this, "Crear Cartones", Toast.LENGTH_SHORT).show()
        }

        binding.btnJugar.setOnClickListener {
            Toast.makeText(this, "Jugar", Toast.LENGTH_SHORT).show()
        }

        binding.btnPagos.setOnClickListener {
            Toast.makeText(this, "Pagos", Toast.LENGTH_SHORT).show()
        }

        binding.btnVentas.setOnClickListener {
            Toast.makeText(this, "Ventas", Toast.LENGTH_SHORT).show()
        }

        binding.btnModalidades.setOnClickListener {
            Toast.makeText(this, "Modalidades", Toast.LENGTH_SHORT).show()
        }

        binding.btnConfiguracion.setOnClickListener {
            Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
        }
    }
}