// app/src/main/java/com/example/myapplication/FaturaLixeira.kt
package com.example.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faturas_lixeira")
data class FaturaLixeira(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "numero_fatura") val numeroFatura: String?,
    @ColumnInfo(name = "cliente_nome") val clienteNome: String?,
    @ColumnInfo(name = "artigos_json") val artigosJson: String?, // Armazenar como JSON ou String serializada
    @ColumnInfo(name = "subtotal") val subtotal: Double?,
    @ColumnInfo(name = "desconto") val desconto: Double?,
    @ColumnInfo(name = "desconto_percent") val descontoPercent: Boolean?,
    @ColumnInfo(name = "taxa_entrega") val taxaEntrega: Double?,
    @ColumnInfo(name = "saldo_devedor") val saldoDevedor: Double?,
    @ColumnInfo(name = "data") val data: String?,
    @ColumnInfo(name = "fotos_json") val fotosJson: String?, // Armazenar como JSON ou String serializada
    @ColumnInfo(name = "notas_json") val notasJson: String? // Armazenar como JSON ou String serializada
)