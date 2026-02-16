package com.marco.bingoapp

import android.database.Cursor
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VentasAdapter(private var cursor: Cursor) : RecyclerView.Adapter<VentasAdapter.VentasViewHolder>() {

    class VentasViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val txtId: TextView = v.findViewById(R.id.tvColNumero)
        val txtNom: TextView = v.findViewById(R.id.tvColNombre)
        val txtPag: TextView = v.findViewById(R.id.tvColPagado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentasViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_venta, parent, false)
        return VentasViewHolder(v)
    }

    override fun onBindViewHolder(holder: VentasViewHolder, position: Int) {
        if (cursor.moveToPosition(position)) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val nom = cursor.getString(cursor.getColumnIndexOrThrow("comprador"))
            val pag = cursor.getInt(cursor.getColumnIndexOrThrow("pagado"))

            // Formato de número: 01, 02, etc.
            holder.txtId.text = String.format("%02d", id)

            // Si el nombre está vacío en la BD, mostrar guiones
            holder.txtNom.text = if (nom.isNullOrEmpty()) "---" else nom

            // Icono de estado
            holder.txtPag.text = if (pag == 1) "✅" else "❌"

            // Colores alternos para que parezca una tabla profesional
            if (position % 2 == 0) {
                holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0")) // Crema claro
            } else {
                holder.itemView.setBackgroundColor(Color.parseColor("#FFE0B2")) // Naranja claro
            }
        }
    }

    override fun getItemCount(): Int = cursor.count

    // Función vital para actualizar la lista sin cerrar la pantalla
    fun swapCursor(newCursor: Cursor) {
        cursor.close()
        cursor = newCursor
        notifyDataSetChanged()
    }
}