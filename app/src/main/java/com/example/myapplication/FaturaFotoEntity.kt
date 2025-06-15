// app/src/main/java/com/example/myapplication/FaturaFotoEntity.kt
package com.example.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fatura_fotos")
data class FaturaFotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "fatura_id") val faturaId: Long,
    @ColumnInfo(name = "photo_path") val photoPath: String
)