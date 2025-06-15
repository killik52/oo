// app/src/main/java/com/example/myapplication/database/dao/ArtigoDao.kt
package com.example.myapplication.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.Artigo
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtigoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artigo: Artigo): Long

    @Update
    suspend fun update(artigo: Artigo)

    @Delete
    suspend fun delete(artigo: Artigo)

    @Query("SELECT * FROM artigos WHERE id = :id")
    suspend fun getArtigoById(id: Long): Artigo?

    @Query("SELECT * FROM artigos WHERE guardar_fatura = 1 ORDER BY id DESC LIMIT :limit")
    fun getRecentArtigos(limit: Int): Flow<List<Artigo>>

    @Query("SELECT * FROM artigos ORDER BY nome ASC")
    fun getAllArtigos(): Flow<List<Artigo>>

    @Query("UPDATE artigos SET guardar_fatura = :guardar WHERE id = :id")
    suspend fun updateGuardarFatura(id: Long, guardar: Boolean)

    @Query("SELECT * FROM artigos WHERE numero_serial = :serial LIMIT 1")
    suspend fun getArtigoBySerial(serial: String): Artigo?
}