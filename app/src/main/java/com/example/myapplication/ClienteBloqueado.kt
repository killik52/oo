// app/src/main/java/com/example/myapplication/ClienteBloqueado.kt
package com.example.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes_bloqueados")
data class ClienteBloqueado(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "telefone") val telefone: String?,
    @ColumnInfo(name = "informacoes_adicionais") val informacoesAdicionais: String?,
    @ColumnInfo(name = "cpf") val cpf: String?,
    @ColumnInfo(name = "cnpj") val cnpj: String?,
    @ColumnInfo(name = "logradouro") val logradouro: String?,
    @ColumnInfo(name = "numero") val numero: String?,
    @ColumnInfo(name = "complemento") val complemento: String?,
    @ColumnInfo(name = "bairro") val bairro: String?,
    @ColumnInfo(name = "municipio") val municipio: String?,
    @ColumnInfo(name = "uf") val uf: String?,
    @ColumnInfo(name = "cep") val cep: String?,
    @ColumnInfo(name = "numero_serial") val numeroSerial: String?
)