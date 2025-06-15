// app/src/main/java/com/example/myapplication/database/dao/FaturaFotoDao.kt
package com.example.myapplication.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.FaturaFotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FaturaFotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(faturaFoto: FaturaFotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(faturaFotos: List<FaturaFotoEntity>)

    @Query("SELECT * FROM fatura_fotos WHERE fatura_id = :faturaId")
    fun getPhotosForFatura(faturaId: Long): Flow<List<FaturaFotoEntity>>

    @Query("DELETE FROM fatura_fotos WHERE fatura_id = :faturaId")
    suspend fun deletePhotosForFatura(faturaId: Long): Int

    @Query("DELETE FROM fatura_fotos WHERE photo_path = :photoPath")
    suspend fun deletePhotoByPath(photoPath: String): Int
}