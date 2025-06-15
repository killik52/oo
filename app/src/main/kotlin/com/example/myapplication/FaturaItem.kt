package com.example.myapplication// app/src/main/java/com/example/myapplication/FaturaItem.kt

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "fatura_itens")
data class FaturaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "fatura_id") val faturaId: Long,
    @ColumnInfo(name = "artigo_id") val artigoId: Long?, // ID do artigo original (se existir)
    @ColumnInfo(name = "quantidade") val quantidade: Int,
    @ColumnInfo(name = "preco_unitario") val precoUnitario: Double, // Preço por unidade no momento da fatura
    @ColumnInfo(name = "nome_artigo") val nomeArtigo: String, // Nome do artigo no momento da fatura (para histórico)
    @ColumnInfo(name = "numero_serie") val numeroSerie: String?, // Número de série no momento da fatura
    @ColumnInfo(name = "descricao") val descricao: String? // Descrição no momento da fatura
) : Parcelable