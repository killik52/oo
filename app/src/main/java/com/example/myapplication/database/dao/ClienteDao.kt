// app/src/main/java/com/example/myapplication/database/dao/ClienteDao.kt
package com.example.myapplication.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.Cliente
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cliente: Cliente): Long

    @Update
    suspend fun update(cliente: Cliente)

    @Delete
    suspend fun delete(cliente: Cliente)

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun getClienteById(id: Long): Cliente?

    @Query("SELECT * FROM clientes WHERE nome = :nome")
    suspend fun getClienteByNome(nome: String): Cliente?

    @Query("SELECT * FROM clientes ORDER BY id DESC LIMIT :limit")
    fun getRecentClientes(limit: Int): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun getAllClientes(): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes WHERE cpf = :cpf OR cnpj = :cnpj LIMIT 1")
    suspend fun getClienteByCpfCnpj(cpf: String, cnpj: String): Cliente?
}