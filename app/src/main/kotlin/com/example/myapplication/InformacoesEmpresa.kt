package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InformacoesEmpresa(
    val id: Long,
    val nome: String, // Alinhado com o ClienteDbHelper.kt
    val endereco: String?,
    val telefone: String?,
    val email: String?,
    val cnpj: String? // Alinhado com o ClienteDbHelper.kt
) : Parcelable