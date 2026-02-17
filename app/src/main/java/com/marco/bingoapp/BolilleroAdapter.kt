package com.marco.bingoapp

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BolilleroAdapter(private val marcados: Set<Int>) : RecyclerView.Adapter<BolilleroAdapter.ViewHolder>() {

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tv = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                80
            )
            gravity = Gravity.CENTER
            textSize = 14f
                    setTextColor(Color.WHITE)
        }
        return ViewHolder(tv)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val numero = position + 1
        holder.textView.text = numero.toString()

        if (marcados.contains(numero)) {
            holder.textView.setBackgroundColor(Color.parseColor("#FF9800")) // Naranja: Salido
            holder.textView.setTextColor(Color.BLACK)
        } else {
            holder.textView.setBackgroundColor(Color.TRANSPARENT)
            holder.textView.setTextColor(Color.WHITE)
        }
    }

    override fun getItemCount() = 75
}