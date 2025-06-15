// app/src/main/java/com/example/myapplication/database/dao/InformacoesEmpresaDao.kt
package com.example.myapplication.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.InformacoesEmpresaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InformacoesEmpresaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: InformacoesEmpresaEntity): Long

    @Update
    suspend fun update(info: InformacoesEmpresaEntity)

    @Query("SELECT * FROM informacoes_empresa LIMIT 1")
    fun getInformacoesEmpresa(): Flow<InformacoesEmpresaEntity?>
}