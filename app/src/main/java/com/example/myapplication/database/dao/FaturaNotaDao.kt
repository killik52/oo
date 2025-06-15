// app/src/main/java/com/example/myapplication/database/dao/FaturaNotaDao.kt
package com.example.myapplication.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.FaturaNotaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FaturaNotaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(faturaNota: FaturaNotaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(faturaNotas: List<FaturaNotaEntity>)

    @Query("SELECT * FROM fatura_notas WHERE fatura_id = :faturaId")
    fun getNotesForFatura(faturaId: Long): Flow<List<FaturaNotaEntity>>

    @Query("DELETE FROM fatura_notas WHERE fatura_id = :faturaId")
    suspend fun deleteNotesForFatura(faturaId: Long): Int

    @Query("DELETE FROM fatura_notas WHERE nota_conteudo = :noteContent")
    suspend fun deleteNoteByContent(noteContent: String): Int
}