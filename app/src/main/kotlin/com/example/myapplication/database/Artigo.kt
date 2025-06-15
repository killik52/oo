// app/src/main/java/com/example/myapplication/Artigo.kt
package com.example.myapplication.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "artigos")
data class Artigo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "preco") val preco: Double,
    @ColumnInfo(name = "quantidade") val quantidade: Int, // Quantidade padrão para itens recentes (geralmente 1)
    @ColumnInfo(name = "desconto") val desconto: Double = 0.0, // Desconto padrão para itens recentes (geralmente 0)
    @ColumnInfo(name = "descricao") val descricao: String?,
    @ColumnInfo(name = "guardar_fatura") val guardarFatura: Boolean = true, // Para indicar se deve aparecer em "recentes"
    @ColumnInfo(name = "numero_serial") val numeroSerial: String?
) : Parcelable