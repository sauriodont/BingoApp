package com.marco.bingoapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.marco.bingoapp.databinding.ActivityPagosBinding
import java.util.Locale

class PagosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagosBinding
    private lateinit var dbHelper: BingoDbHelper
    private lateinit var pagosAdapter: PagosAdapter
    private var listaSeleccionada = mutableListOf<Premio>()
    private var totalVendido: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BingoDbHelper(this)

        // 1. Configurar RecyclerView
        setupRecyclerView()

        // 2. Cargar datos iniciales
        val cantPagos = dbHelper.contarCartonesPagados()
        binding.tvCartonesVendidos.text = cantPagos.toString()
        setupSpinnerModalidades()

        // 3. Listeners de cálculo automático
        binding.etPrecioCarton.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                actualizarCasa()
                pagosAdapter.notifyDataSetChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 4. Botón Agregar Modalidad a la lista de premios
        binding.btnAgregarMod.setOnClickListener {
            val modalidadSeleccionada = binding.spnModalidades.selectedItem?.toString()

            if (modalidadSeleccionada != null) {
                if (!listaSeleccionada.any { it.nombre == modalidadSeleccionada }) {
                    val nuevoPremio = Premio(modalidadSeleccionada, 0.0, 0.0)
                    listaSeleccionada.add(nuevoPremio)
                    pagosAdapter.notifyItemInserted(listaSeleccionada.size - 1)
                    actualizarCasa()
                } else {
                    Toast.makeText(this, "Ya agregaste esta modalidad", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5. Botón Guardar Reporte (Integridad de datos)
        binding.btnGuardar.setOnClickListener {
            if (listaSeleccionada.isEmpty()) {
                Toast.makeText(this, "No hay premios para guardar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val gson = Gson()
                val listaParaGuardar = listaSeleccionada.toMutableList()

                // Evitar duplicados de la fila CASA
                listaParaGuardar.removeAll { it.nombre == "CASA" }

                val montoCasa = binding.etMontoCasa.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                val porcCasa = binding.etPorcCasa.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0

                listaParaGuardar.add(Premio("CASA", porcCasa, montoCasa))

                val detalleJson = gson.toJson(listaParaGuardar)
                val idInsertado = dbHelper.guardarReportePago(totalVendido, detalleJson)

                if (idInsertado != -1L) {
                    Toast.makeText(this, "✅ Reporte guardado con éxito", Toast.LENGTH_LONG).show()
                    binding.btnGuardar.isEnabled = false // Evitar múltiples guardados
                } else {
                    Toast.makeText(this, "❌ Error en la base de datos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCalcular.setOnClickListener { actualizarCasa() }
        binding.btnCopiar.setOnClickListener { copiarAWhatsApp() }
    }

    private fun setupRecyclerView() {
        pagosAdapter = PagosAdapter(listaSeleccionada, {
            // Esta lambda devuelve el total vendido actual para los cálculos del adaptador
            val precio = binding.etPrecioCarton.text.toString().toDoubleOrNull() ?: 0.0
            val cant = binding.tvCartonesVendidos.text.toString().toIntOrNull() ?: 0
            precio * cant
        }, {
            // Esta lambda se ejecuta cuando se cambia un porcentaje en el adaptador
            actualizarCasa()
        })
        binding.rvPremiosSeleccionados.layoutManager = LinearLayoutManager(this)
        binding.rvPremiosSeleccionados.adapter = pagosAdapter
    }

    private fun actualizarCasa() {
        val precio = binding.etPrecioCarton.text.toString().toDoubleOrNull() ?: 0.0
        val cant = binding.tvCartonesVendidos.text.toString().toIntOrNull() ?: 0
        totalVendido = precio * cant

        var sumaPremios = 0.0
        listaSeleccionada.forEach { sumaPremios += it.monto }

        val montoCasa = totalVendido - sumaPremios
        val porcCasa = if (totalVendido > 0) (montoCasa * 100) / totalVendido else 0.0

        // Forzamos Locale.US para usar siempre punto decimal
        binding.etMontoCasa.setText(String.format(Locale.US, "%.2f", montoCasa))
        binding.etPorcCasa.setText(String.format(Locale.US, "%.2f", porcCasa))
    }

    private fun setupSpinnerModalidades() {
        val modalidades = mutableListOf<String>()
        val cursor = dbHelper.obtenerTodasLasModalidades()
        if (cursor.moveToFirst()) {
            do {
                modalidades.add(cursor.getString(cursor.getColumnIndexOrThrow("nombre")))
            } while (cursor.moveToNext())
        }
        cursor.close()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modalidades)
        binding.spnModalidades.adapter = adapter
    }

    private fun copiarAWhatsApp() {
        val sb = StringBuilder("*💰 PREMIOS DEL SORTEO*\n\n")
        sb.append("*Total Recaudado:* ${String.format(Locale.US, "%.2f", totalVendido)} Bs.\n")
        sb.append("----------------------------\n")
        listaSeleccionada.forEach {
            sb.append("*${it.nombre}:* ${String.format(Locale.US, "%.2f", it.monto)} Bs. (${String.format(Locale.US, "%.1f", it.porcentaje)}%)\n")
        }
        sb.append("----------------------------\n")
        sb.append("*CASA:* ${binding.etMontoCasa.text} Bs.")

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Reporte de Premios", sb.toString()))
        Toast.makeText(this, "Reporte copiado para WhatsApp", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Verificamos si hay un sorteo activo para bloquear la edición
        val enCurso = dbHelper.estaSorteoEnCurso()
        bloquearEdicion(enCurso)
    }

    private fun bloquearEdicion(bloquear: Boolean) {
        binding.btnGuardar.isEnabled = !bloquear
        binding.btnAgregarMod.isEnabled = !bloquear
        binding.etPrecioCarton.isEnabled = !bloquear

        if (bloquear) {
            binding.root.alpha = 0.7f
            Toast.makeText(this, "Modo lectura: Sorteo en curso", Toast.LENGTH_SHORT).show()
        } else {
            binding.root.alpha = 1.0f
        }
    }
}

// Data class para manejar los premios
data class Premio(val nombre: String, var porcentaje: Double, var monto: Double)