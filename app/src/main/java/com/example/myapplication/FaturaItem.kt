package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FaturaItem(
    val id: Long = 0, // ID do próprio item na tabela fatura_itens
    val faturaId: Long,
    val artigoId: Long, // ID do artigo original (se existir)
    val quantidade: Int,
    val precoUnitario: Double,
    val nomeArtigo: String, // Nome do artigo no momento da fatura (para histórico)
    val numeroSerie: String?, // Número de série no momento da fatura
    val descricao: String? // Descrição no momento da fatura
) : Parcelable