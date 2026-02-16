package com.marco.bingoapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.marco.bingoapp.databinding.ActivityVentasBinding

class VentasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVentasBinding
    private lateinit var dbHelper: BingoDbHelper
    private lateinit var ventasAdapter: VentasAdapter
    private var listaIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVentasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BingoDbHelper(this)

        // 1. Inicializar componentes
        setupSpinner()
        setupRecyclerView()

        // 2. Listener para cargar datos cuando se cambia el número en el Spinner
        binding.spnCartones.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                cargarDatosDelCarton(listaIds[position])
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // 3. Botón Guardar Venta
        binding.btnGuardarVenta.setOnClickListener {
            guardarVenta()
        }

        // 4. Botón Copiar para WhatsApp
        binding.btnCopiarLista.setOnClickListener {
            copiarParaWhatsApp()
        }
    }

    private fun setupSpinner() {
        val cursor = dbHelper.obtenerTodosLosCartones()
        listaIds.clear()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                listaIds.add(id.toString())
            } while (cursor.moveToNext())
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaIds)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnCartones.adapter = adapter
    }

    private fun setupRecyclerView() {
        // Obtenemos todos los cartones (vendidos y no vendidos)
        val cursor = dbHelper.obtenerTodosLosCartones()
        ventasAdapter = VentasAdapter(cursor)
        binding.rvVentas.layoutManager = LinearLayoutManager(this)
        binding.rvVentas.adapter = ventasAdapter
    }

    private fun cargarDatosDelCarton(id: String) {
        val cursor = dbHelper.obtenerCartonPorId(id)
        if (cursor.moveToFirst()) {
            val comprador = cursor.getString(cursor.getColumnIndexOrThrow("comprador"))
            val pagado = cursor.getInt(cursor.getColumnIndexOrThrow("pagado"))

            binding.etNombreComprador.setText(comprador)
            if (pagado == 1) binding.rbSi.isChecked = true else binding.rbNo.isChecked = true
        }
        cursor.close()
    }

    private fun guardarVenta() {
        val id = binding.spnCartones.selectedItem?.toString() ?: return
        val nombre = binding.etNombreComprador.text.toString().trim()
        val pago = if (binding.rbSi.isChecked) 1 else 0

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingresa el nombre del comprador", Toast.LENGTH_SHORT).show()
            return
        }

        dbHelper.actualizarVenta(id, nombre, pago)

        // Refrescamos el RecyclerView con los nuevos datos de la BD
        ventasAdapter.swapCursor(dbHelper.obtenerTodosLosCartones())

        Toast.makeText(this, "Cartón #$id actualizado", Toast.LENGTH_SHORT).show()
    }

    private fun copiarParaWhatsApp() {
        val cursor = dbHelper.obtenerTodosLosCartones()
        val sb = StringBuilder("*📝 LISTA COMPLETA DE CARTONES - BINGO*\n\n")

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val nom = cursor.getString(cursor.getColumnIndexOrThrow("comprador"))
                val pagado = cursor.getInt(cursor.getColumnIndexOrThrow("pagado"))

                // Formateamos el número del cartón (ej: #01, #05)
                val numeroFormateado = String.format("%02d", id)

                // Lógica para el estado del cartón
                val estado = when {
                    nom.isNullOrEmpty() -> "DISPONIBLE 🆓" // Si no tiene nombre
                    pagado == 1 -> "$nom ✅ (PAGADO)"     // Si tiene nombre y pagó
                    else -> "$nom ⏳ (NO PAGO)"          // Si tiene nombre pero no pagó
                }

                sb.append("*Cartón #$numeroFormateado*: $estado\n")

            } while (cursor.moveToNext())
        }
        cursor.close()

        // Copiar al portapapeles
        val clip = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("lista_bingo", sb.toString())
        clip.setPrimaryClip(clipData)

        Toast.makeText(this, "Lista completa copiada al portapapeles", Toast.LENGTH_SHORT).show()
    }
}