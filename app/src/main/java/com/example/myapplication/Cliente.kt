// app/src/main/java/com/example/myapplication/Cliente.kt
package com.example.myapplication

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "telefone") val telefone: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "informacoes_adicionais") val informacoesAdicionais: String?,
    @ColumnInfo(name = "cpf") val cpf: String?,
    @ColumnInfo(name = "cnpj") val cnpj: String?,
    @ColumnInfo(name = "logradouro") val logradouro: String?,
    @ColumnInfo(name = "numero") val numero: String?,
    @ColumnInfo(name = "complemento") val complemento: String?,
    @ColumnInfo(name = "bairro") val bairro: String?,
    @ColumnInfo(name = "municipio") val municipio: String?, // Alterado de 'municipio' para 'cidade' para consistência
    @ColumnInfo(name = "uf") val uf: String?,   // Alterado de 'uf' para 'estado' para consistência
    @ColumnInfo(name = "cep") val cep: String?,
    @ColumnInfo(name = "numero_serial") val numeroSerial: String? // Pode ter múltiplos, como uma string separada por vírgulas
) : Parcelable