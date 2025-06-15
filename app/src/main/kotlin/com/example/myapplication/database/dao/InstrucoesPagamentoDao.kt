// app/src/main/java/com/example/myapplication/database/dao/InstrucoesPagamentoDao.kt

package com.example.myapplication.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.InstrucoesPagamentoEntity

@Dao
interface InstrucoesPagamentoDao { // <--- ESTE NOME DEVE SER InstrucoesPagamentoDao
    @Insert
    suspend fun insert(instrucoes: InstrucoesPagamentoEntity)

    @Update
    suspend fun update(instrucoes: InstrucoesPagamentoEntity)

    @Query("SELECT * FROM instrucoes_pagamento WHERE id = :id")
    suspend fun getById(id: Int): InstrucoesPagamentoEntity?

    @Query("SELECT * FROM instrucoes_pagamento LIMIT 1")
    suspend fun getInstrucoes(): InstrucoesPagamentoEntity?

    @Query("DELETE FROM instrucoes_pagamento")
    suspend fun clearTable()
}