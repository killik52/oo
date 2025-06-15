// app/src/main/java/com/example/myapplication/database/dao/FaturaLixeiraDao.kt
package com.example.myapplication.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.FaturaLixeira
import kotlinx.coroutines.flow.Flow

@Dao
interface FaturaLixeiraDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(faturaLixeira: FaturaLixeira): Long

    @Delete
    suspend fun delete(faturaLixeira: FaturaLixeira)

    @Query("SELECT * FROM faturas_lixeira ORDER BY data DESC")
    fun getAllFaturasLixeira(): Flow<List<FaturaLixeira>>

    @Query("SELECT * FROM faturas_lixeira WHERE id = :id")
    suspend fun getFaturaLixeiraById(id: Long): FaturaLixeira?

    @Query("DELETE FROM faturas_lixeira WHERE id = :id")
    suspend fun deleteFaturaLixeiraById(id: Long): Int
}