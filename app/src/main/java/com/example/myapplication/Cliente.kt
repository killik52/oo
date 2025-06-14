package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Cliente(
    val id: Long,
    val nome: String,
    val endereco: String?, // Usando endereco para refletir a junção de logradouro, numero, etc.
    val telefone: String?,
    val email: String?,
    val cpf: String?,
    val cnpj: String?,
    val logradouro: String?,
    val numero: String?,
    val complemento: String?,
    val bairro: String?,
    val cidade: String?, // Alterado de 'municipio' para 'cidade' para consistência
    val estado: String?,   // Alterado de 'uf' para 'estado' para consistência
    val cep: String?
) : Parcelable