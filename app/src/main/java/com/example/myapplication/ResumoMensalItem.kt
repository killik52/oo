package com.example.myapplication

data class ResumoMensalItem(
    val mesAno: String,
    val valorTotal: Double,
    val totalFaturas: Int // Adicionada para refletir COUNT no SQL
)