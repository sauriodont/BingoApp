package com.marco.bingoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ModalidadesAdapter(
    var lista: MutableList<Modalidad>,
    val onEliminar: (Modalidad) -> Unit
) : RecyclerView.Adapter<ModalidadesAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tvNombreMod)
        val btnEliminar: ImageButton = v.findViewById(R.id.btnEliminarMod)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_modalidad, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.nombre.text = item.nombre
        holder.btnEliminar.setOnClickListener { onEliminar(item) }
    }

    override fun getItemCount() = lista.size
}

// Clase de apoyo
data class Modalidad(val id: Int, val nombre: String, val config: String)