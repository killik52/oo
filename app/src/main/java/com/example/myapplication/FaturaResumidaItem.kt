package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FaturaResumidaItem(
    val id: Long,
    val clienteNome: String, // Renomeado para consistência
    val data: String,
    val total: Double, // Renomeado de saldoDevedor para total
    val foiEnviada: Boolean
) : Parcelable