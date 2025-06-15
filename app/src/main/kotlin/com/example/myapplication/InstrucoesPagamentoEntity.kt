package com.example.myapplication// app/src/main/java/com/example/myapplication/InstrucoesPagamentoEntity.kt

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "instrucoes_pagamento")
data class InstrucoesPagamentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pix") val pix: String?,
    @ColumnInfo(name = "banco") val banco: String?,
    @ColumnInfo(name = "agencia") val agencia: String?,
    @ColumnInfo(name = "conta") val conta: String?,
    @ColumnInfo(name = "outras_instrucoes") val outrasInstrucoes: String?
)