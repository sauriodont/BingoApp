package com.marco.bingoapp

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PagosAdapter(
    val listaPremios: MutableList<Premio>,
    val totalVendido: () -> Double, // Función para obtener el total actualizado
    val onDataChanged: () -> Unit // Para avisar a la actividad que recalcule la Casa
) : RecyclerView.Adapter<PagosAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tvNombreModalidadItem)
        val porc: EditText = v.findViewById(R.id.etPorcentajeItem)
        val monto: EditText = v.findViewById(R.id.etMontoItem)
        val btnEliminar: ImageButton = v.findViewById(R.id.btnEliminarPremio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pago_modalidad, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val premio = listaPremios[position]
        holder.nombre.text = premio.nombre

        // Evitar bucles infinitos de TextWatcher
        holder.porc.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                holder.porc.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val p = s.toString().toDoubleOrNull() ?: 0.0
                        val m = (totalVendido() * p) / 100
                        premio.porcentaje = p
                        premio.monto = m
                        holder.monto.setText(String.format("%.2f", m))
                        onDataChanged()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            } else { holder.porc.clearFocus() }
        }

        holder.monto.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                holder.monto.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val m = s.toString().toDoubleOrNull() ?: 0.0
                        val p = if (totalVendido() > 0) (m * 100) / totalVendido() else 0.0
                        premio.monto = m
                        premio.porcentaje = p
                        holder.porc.setText(String.format("%.2f", p))
                        onDataChanged()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
        }

        holder.btnEliminar.setOnClickListener {
            listaPremios.removeAt(position)
            notifyDataSetChanged()
            onDataChanged()
        }
    }

    override fun getItemCount() = listaPremios.size
}