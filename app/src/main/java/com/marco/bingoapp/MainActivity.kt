package com.example.bingoapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bingoapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val numerosSalidos = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usamos ViewBinding para acceder al layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Acción del botón "Generar número"
        binding.btnGenerar.setOnClickListener {
            val numero = generarNumero()
            val letra = determinarLetra(numero)
            binding.txtNumero.text = "$letra $numero"
            numerosSalidos.add(numero)
        }
    }

    private fun generarNumero(): Int {
        var numero: Int
        do {
            numero = (1..75).random()
        } while (numerosSalidos.contains(numero))
        return numero
    }

    private fun determinarLetra(numero: Int): String {
        return when (numero) {
            in 1..15 -> "B"
            in 16..30 -> "I"
            in 31..45 -> "N"
            in 46..60 -> "G"
            in 61..75 -> "O"
            else -> ""
        }
    }
}