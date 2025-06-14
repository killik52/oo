package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Fatura(
    val id: Long,
    val clienteId: Long,
    val data: String,
    val total: Double,
    val foiEnviada: Boolean,
    val observacao: String?,
    val desconto: Double,
    val taxaEntrega: Double,
    val modoSimplificado: Boolean
) : Parcelable