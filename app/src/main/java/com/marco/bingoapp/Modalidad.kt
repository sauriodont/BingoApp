package com.marco.bingoapp

data class Modalidad(
    val id: Int,
    val nombre: String,
    val configuracion: String // Guardará un JSON como "[0,1,2,12...]"
)