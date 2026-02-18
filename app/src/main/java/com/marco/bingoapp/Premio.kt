package com.marco.bingoapp

data class Premio(
    val id: Int,
    val nombre: String,
    var porcentaje: Double = 0.0,
    var monto: Double = 0.0
)