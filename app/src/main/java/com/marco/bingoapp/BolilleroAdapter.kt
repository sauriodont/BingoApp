package com.marco.bingoapp

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BolilleroAdapter(private val numerosSalidos: List<Int>) :
    RecyclerView.Adapter<BolilleroAdapter.ViewHolder>() {

    // Lógica para que los números queden en columnas:
    // Col 1 (B): 1-15 | Col 2 (I): 16-30 | Col 3 (N): 31-45 ...
    private val listaOrganizada: List<Int> by lazy {
        val temp = mutableListOf<Int>()
        for (fila in 0 until 15) { // 15 filas
            temp.add(fila + 1)  // B
            temp.add(fila + 16) // I
            temp.add(fila + 31) // N
            temp.add(fila + 46) // G
            temp.add(fila + 61) // O
        }
        temp
    }

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tv = TextView(parent.context).apply {
            // Ajustamos el alto para que no sobre espacio (40dp aprox por celda)
            val escala = resources.displayMetrics.density
            val altoPixels = (35 * escala).toInt()

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                altoPixels
            )
            gravity = Gravity.CENTER
            textSize = 16f
            // Usamos un fondo programático simple para evitar el error de 'border_item'
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        }
        return ViewHolder(tv)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val numero = listaOrganizada[position]
        holder.textView.text = numero.toString()

        if (numerosSalidos.contains(numero)) {
            // NÚMERO QUE SALIÓ: Verde/Amarillo brillante
            holder.textView.setBackgroundColor(Color.parseColor("#A2D149"))
            holder.textView.setTextColor(Color.BLACK)
            holder.textView.setTypeface(null, Typeface.BOLD)
        } else {
            // NÚMERO PENDIENTE: Fondo blanco, texto gris oscuro
            holder.textView.setBackgroundColor(Color.WHITE)
            holder.textView.setTextColor(Color.DKGRAY)
            holder.textView.setTypeface(null, Typeface.NORMAL)
        }
    }

    override fun getItemCount(): Int = listaOrganizada.size
}