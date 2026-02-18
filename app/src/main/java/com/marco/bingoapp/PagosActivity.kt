package com.marco.bingoapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.marco.bingoapp.databinding.ActivityPagosBinding

class PagosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagosBinding
    private lateinit var dbHelper: BingoDbHelper
    private lateinit var adapter: PagosAdapter
    private val listaPremios = mutableListOf<Premio>()
    private var totalVendidoGlobal = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BingoDbHelper(this)

        setupRecyclerView()
        cargarConfiguracionInicial()
        mostrarUltimoReporte()

        // Botón Agregar desde el Spinner
        binding.btnAgregarMod.setOnClickListener {
            agregarModalidadSeleccionada()
        }

        // Botón Recalcular
        binding.btnCalcular.setOnClickListener {
            recalcularTotales()
        }

        // Botón Guardar
        binding.btnGuardar.setOnClickListener {
            guardarReporteFinal()
        }

        // Botón WhatsApp
        binding.btnCopiar.setOnClickListener {
            copiarAWhatsApp()
        }
    }

    private fun setupRecyclerView() {
        adapter = PagosAdapter(
            listaPremios,
            totalVendido = {
                val precio = binding.etPrecioCarton.text.toString().toDoubleOrNull() ?: 0.0
                val cantidad = dbHelper.contarCartonesPagados()
                cantidad * precio
            },
            onDataChanged = {
                actualizarGananciaCasa()
            }
        )
        binding.rvPremiosSeleccionados.layoutManager = LinearLayoutManager(this)
        binding.rvPremiosSeleccionados.adapter = adapter
    }

    private fun cargarConfiguracionInicial() {
        // Cargar cantidad de cartones
        val cant = dbHelper.contarCartonesPagados()
        binding.tvCartonesVendidos.text = cant.toString()

        // Cargar modalidades al Spinner
        val modalidadesDB = mutableListOf<String>()
        val cursor = dbHelper.obtenerTodasLasModalidades()
        if (cursor.moveToFirst()) {
            do {
                modalidadesDB.add(cursor.getString(cursor.getColumnIndexOrThrow("nombre")))
            } while (cursor.moveToNext())
        }
        cursor.close()

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modalidadesDB)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnModalidades.adapter = spinnerAdapter
    }

    private fun agregarModalidadSeleccionada() {
        val nombre = binding.spnModalidades.selectedItem?.toString() ?: return

        // Evitar duplicados en la lista de premios
        if (listaPremios.none { it.nombre == nombre }) {
            // Buscamos el ID real de la modalidad en la DB
            val cursor = dbHelper.obtenerTodasLasModalidades()
            var idMod = -1
            if (cursor.moveToFirst()) {
                do {
                    if (cursor.getString(cursor.getColumnIndexOrThrow("nombre")) == nombre) {
                        idMod = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()

            listaPremios.add(Premio(idMod, nombre))
            adapter.notifyDataSetChanged()
            recalcularTotales()
        }
    }

    private fun recalcularTotales() {
        val precio = binding.etPrecioCarton.text.toString().toDoubleOrNull() ?: 0.0
        val cantidad = dbHelper.contarCartonesPagados()
        totalVendidoGlobal = cantidad * precio

        // Forzar al adaptador a refrescar montos basados en el nuevo total
        adapter.notifyDataSetChanged()
        actualizarGananciaCasa()
    }

    private fun actualizarGananciaCasa() {
        val totalPremios = listaPremios.sumOf { it.monto }
        val ganancia = totalVendidoGlobal - totalPremios

        binding.etMontoCasa.setText(String.format("%.2f", ganancia))

        val porc = if (totalVendidoGlobal > 0) (ganancia * 100) / totalVendidoGlobal else 0.0
        binding.etPorcCasa.setText(String.format("%.1f%%", porc))

        if (ganancia < 0) binding.etMontoCasa.setTextColor(Color.RED)
        else binding.etMontoCasa.setTextColor(Color.BLACK)
    }

    private fun guardarReporteFinal() {
        if (listaPremios.isEmpty()) {
            Toast.makeText(this, "Agregue al menos una modalidad", Toast.LENGTH_SHORT).show()
            return
        }

        dbHelper.desactivarTodasLasModalidades()
        val detalleMapa = mutableMapOf<String, Double>()

        for (p in listaPremios) {
            if (p.id != -1) {
                dbHelper.setModalidadActiva(p.id, true)
                detalleMapa[p.nombre] = p.monto
            }
        }

        val totalEnPremios = listaPremios.sumOf { it.monto }
        val detalleJson = Gson().toJson(detalleMapa)

        dbHelper.guardarReportePago(totalEnPremios, detalleJson)
        mostrarUltimoReporte()
        Toast.makeText(this, "Reporte Guardado. Sorteo listo.", Toast.LENGTH_SHORT).show()
    }

    private fun copiarAWhatsApp() {
        val texto = StringBuilder()
        texto.append("*RESUMEN BINGO*\n")
        texto.append("Cartones: ${binding.tvCartonesVendidos.text}\n")
        listaPremios.forEach { texto.append("${it.nombre}: Bs. ${String.format("%.2f", it.monto)}\n") }
        texto.append("----------------\n")
        texto.append("*CASA:* Bs. ${binding.etMontoCasa.text}")

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Bingo", texto.toString()))
        Toast.makeText(this, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarUltimoReporte() {
        val cursor = dbHelper.obtenerUltimoReporte()
        if (cursor != null && cursor.moveToFirst()) {
            val fecha = cursor.getString(cursor.getColumnIndexOrThrow(BingoDbHelper.COLUMN_PAGO_FECHA))
            val total = cursor.getDouble(cursor.getColumnIndexOrThrow(BingoDbHelper.COLUMN_PAGO_TOTAL))
            val detalleJson = cursor.getString(cursor.getColumnIndexOrThrow(BingoDbHelper.COLUMN_PAGO_DETALLE))

            binding.tvFechaReporte.text = "Fecha: $fecha"
            binding.tvTotalReporte.text = "Total en Premios: Bs. ${String.format("%.2f", total)}"
            binding.gridReporteDetalle.removeAllViews()

            try {
                val tipoMapa = object : TypeToken<Map<String, Double>>() {}.type
                val detalles: Map<String, Double> = Gson().fromJson(detalleJson, tipoMapa)

                for ((mod, monto) in detalles) {
                    val tv = TextView(this).apply {
                        text = "• $mod: Bs. ${String.format("%.2f", monto)}"
                        setPadding(10, 5, 10, 5)
                        setTextColor(Color.DKGRAY)
                    }
                    binding.gridReporteDetalle.addView(tv)
                }
            } catch (e: Exception) {}
        }
        cursor?.close()
    }
}