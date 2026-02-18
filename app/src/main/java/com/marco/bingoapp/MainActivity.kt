package com.marco.bingoapp

import android.content.Intent
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
            val intent = Intent(this, CrearCartonesActivity::class.java)
            startActivity(intent)
        }


        binding.btnJugar.setOnClickListener {
            val intent = Intent(this, SorteoActivity::class.java)
            startActivity(intent)
        }

        binding.btnPagos.setOnClickListener {
            val intent = Intent(this, PagosActivity::class.java)
            startActivity(intent)
        }

        binding.btnVentas.setOnClickListener {
            val intent = Intent(this, VentasActivity::class.java)
            startActivity(intent)
        }

// Ir a Modalidades (Antes de jugar, el usuario debe elegir cómo se gana)
        binding.btnModalidades.setOnClickListener {
            val intent = Intent(this, ModalidadesActivity::class.java)
            startActivity(intent)
        }

        binding.btnConfiguracion.setOnClickListener {
            Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
        }
    }
}