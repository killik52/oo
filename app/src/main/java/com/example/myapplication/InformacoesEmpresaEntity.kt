// app/src/main/java/com/example/myapplication/InformacoesEmpresaEntity.kt
package com.example.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "informacoes_empresa")
data class InformacoesEmpresaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nome_empresa") val nomeEmpresa: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "telefone") val telefone: String?,
    @ColumnInfo(name = "informacoes_adicionais") val informacoesAdicionais: String?,
    @ColumnInfo(name = "cnpj") val cnpj: String?,
    @ColumnInfo(name = "cep") val cep: String?,
    @ColumnInfo(name = "estado") val estado: String?,
    @ColumnInfo(name = "pais") val pais: String?,
    @ColumnInfo(name = "cidade") val cidade: String?
)