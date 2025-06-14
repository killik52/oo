package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResumoClienteItem(
    val id: Long, // ID do cliente
    val nomeCliente: String,
    val totalCompras: Double,
    val totalFaturas: Int // Adicionada para refletir COUNT no SQL
) : Parcelable