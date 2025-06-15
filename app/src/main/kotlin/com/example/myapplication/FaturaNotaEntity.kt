package com.example.myapplication// app/src/main/java/com/example/myapplication/FaturaNotaEntity.kt

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fatura_notas")
data class FaturaNotaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "fatura_id") val faturaId: Long,
    @ColumnInfo(name = "nota_conteudo") val notaConteudo: String
)