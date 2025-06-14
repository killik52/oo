package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResumoArtigoItem(
    val id: Long, // ID do artigo
    val nomeArtigo: String,
    val valorTotalVendido: Double,
    val quantidadeVendida: Int // Ordem ajustada
) : Parcelable