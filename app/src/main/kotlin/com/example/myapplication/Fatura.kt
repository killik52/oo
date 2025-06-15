package com.example.myapplication// app/src/main/java/com/example/myapplication/Fatura.kt

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "faturas")
data class Fatura(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "numero_fatura") val numeroFatura: String?,
    @ColumnInfo(name = "cliente_nome") val clienteNome: String, // Renomeado de 'cliente'
    @ColumnInfo(name = "cliente_id") val clienteId: Long?, // Referência ao ID do cliente
    @ColumnInfo(name = "subtotal") val subtotal: Double,
    @ColumnInfo(name = "desconto") val desconto: Double,
    @ColumnInfo(name = "desconto_percent") val descontoPercent: Boolean,
    @ColumnInfo(name = "taxa_entrega") val taxaEntrega: Double,
    @ColumnInfo(name = "saldo_devedor") val saldoDevedor: Double,
    @ColumnInfo(name = "data") val data: String,
    @ColumnInfo(name = "observacao") val observacao: String?, // Corresponde ao campo 'notas' do antigo
    @ColumnInfo(name = "foi_enviada") val foiEnviada: Boolean
) : Parcelable