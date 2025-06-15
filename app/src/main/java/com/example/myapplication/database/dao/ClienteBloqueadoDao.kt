// app/src/main/java/com/example/myapplication/database/dao/ClienteBloqueadoDao.kt
package com.example.myapplication.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.ClienteBloqueado
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteBloqueadoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clienteBloqueado: ClienteBloqueado): Long

    @Update
    suspend fun update(clienteBloqueado: ClienteBloqueado)

    @Delete
    suspend fun delete(clienteBloqueado: ClienteBloqueado)

    @Query("SELECT * FROM clientes_bloqueados WHERE id = :id")
    suspend fun getClienteBloqueadoById(id: Long): ClienteBloqueado?

    @Query("SELECT * FROM clientes_bloqueados WHERE nome = :nome OR cpf = :cpf OR cnpj = :cnpj LIMIT 1")
    suspend fun getClienteBloqueadoByNomeCpfCnpj(nome: String, cpf: String, cnpj: String): ClienteBloqueado?

    @Query("SELECT * FROM clientes_bloqueados WHERE numero_serial LIKE :serial OR nome = :nome LIMIT 1")
    suspend fun getClienteBloqueadoBySerialOrNome(serial: String, nome: String): ClienteBloqueado?

    @Query("SELECT * FROM clientes_bloqueados ORDER BY nome ASC")
    fun getAllClientesBloqueados(): Flow<List<ClienteBloqueado>>
}