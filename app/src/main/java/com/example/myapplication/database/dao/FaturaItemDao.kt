// app/src/main/java/com/example/myapplication/database/dao/FaturaItemDao.kt
package com.example.myapplication.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.FaturaItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FaturaItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(faturaItem: FaturaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(faturaItems: List<FaturaItem>)

    @Update
    suspend fun update(faturaItem: FaturaItem)

    @Delete
    suspend fun delete(faturaItem: FaturaItem)

    @Query("SELECT * FROM fatura_itens WHERE fatura_id = :faturaId")
    fun getItemsForFatura(faturaId: Long): Flow<List<FaturaItem>>

    @Query("DELETE FROM fatura_itens WHERE fatura_id = :faturaId")
    suspend fun deleteItemsForFatura(faturaId: Long)
}