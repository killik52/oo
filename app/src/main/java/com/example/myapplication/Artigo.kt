package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Artigo(
    val id: Long,
    val nome: String,
    val precoUnitario: Double, // Renomeado para consistência
    val quantidade: Int,
    val descricao: String?,
    val numeroSerie: String? // Renomeado para consistência
) : Parcelable