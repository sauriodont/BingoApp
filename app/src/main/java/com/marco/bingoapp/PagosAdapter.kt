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

// DEFINE LA CLASE AQUÍ UNA SOLA VEZ
class PagosAdapter(
    val listaPremios: MutableList<Premio>,
    val totalVendido: () -> Double,
    val onDataChanged: () -> Unit
) : RecyclerView.Adapter<PagosAdapter.ViewHolder>() {
    // ... (resto del código del adaptador igual al anterior)

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tvNombreModalidadItem)
        val porc: EditText = v.findViewById(R.id.etPorcentajeItem)
        val monto: EditText = v.findViewById(R.id.etMontoItem)
        val btnEliminar: ImageButton = v.findViewById(R.id.btnEliminarPremio)
        var porcWatcher: TextWatcher? = null
        var montoWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pago_modalidad, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val premio = listaPremios[holder.adapterPosition]
        holder.porc.removeTextChangedListener(holder.porcWatcher)
        holder.monto.removeTextChangedListener(holder.montoWatcher)

        holder.nombre.text = premio.nombre
        holder.porc.setText(if (premio.porcentaje > 0) String.format("%.2f", premio.porcentaje) else "")
        holder.monto.setText(if (premio.monto > 0) String.format("%.2f", premio.monto) else "")

        holder.porcWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (holder.porc.hasFocus()) {
                    val p = s.toString().toDoubleOrNull() ?: 0.0
                    val total = totalVendido()
                    val m = (total * p) / 100
                    premio.porcentaje = p
                    premio.monto = m
                    holder.monto.setText(String.format("%.2f", m))
                    onDataChanged()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        holder.montoWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (holder.monto.hasFocus()) {
                    val m = s.toString().toDoubleOrNull() ?: 0.0
                    val total = totalVendido()
                    val p = if (total > 0) (m * 100) / total else 0.0
                    premio.monto = m
                    premio.porcentaje = p
                    holder.porc.setText(String.format("%.2f", p))
                    onDataChanged()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        holder.porc.addTextChangedListener(holder.porcWatcher)
        holder.monto.addTextChangedListener(holder.montoWatcher)

        holder.btnEliminar.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                listaPremios.removeAt(currentPos)
                notifyItemRemoved(currentPos)
                notifyItemRangeChanged(currentPos, listaPremios.size)
                onDataChanged()
            }
        }
    }

    override fun getItemCount() = listaPremios.size
}