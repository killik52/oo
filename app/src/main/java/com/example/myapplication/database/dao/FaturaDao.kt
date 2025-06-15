// app/src/main/java/com/example/myapplication/database/dao/FaturaDao.kt
package com.example.myapplication.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.Fatura
import kotlinx.coroutines.flow.Flow

@Dao
interface FaturaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fatura: Fatura): Long

    @Update
    suspend fun update(fatura: Fatura)

    @Delete
    suspend fun delete(fatura: Fatura)

    @Query("SELECT * FROM faturas WHERE id = :id")
    suspend fun getFaturaById(id: Long): Fatura?

    @Query("SELECT * FROM faturas ORDER BY id DESC")
    fun getAllFaturas(): Flow<List<Fatura>>

    @Query("DELETE FROM faturas WHERE id = :id")
    suspend fun deleteFaturaById(id: Long): Int

    @Query("UPDATE faturas SET foi_enviada = 1 WHERE id = :id")
    suspend fun markFaturaAsSent(id: Long)

    // Queries para o ResumoFinanceiroActivity
    @Query("SELECT SUM(saldo_devedor) FROM faturas WHERE strftime('%m', data) = :month AND strftime('%Y', data) = :year")
    suspend fun getFaturamentoMesAno(month: String, year: String): Double?

    @Query("SELECT * FROM faturas WHERE data BETWEEN :startDate AND :endDate ORDER BY data DESC")
    suspend fun getFaturasInDateRange(startDate: String, endDate: String): List<Fatura>

    @Query("SELECT * FROM faturas WHERE data >= :startDate ORDER BY data DESC")
    suspend fun getFaturasFromDate(startDate: String): List<Fatura>

    @Query("SELECT * FROM faturas WHERE data <= :endDate ORDER BY data DESC")
    suspend fun getFaturasUntilDate(endDate: String): List<Fatura>

    @Query("""
        SELECT
            strftime('%m/%Y', data) as mes_ano_str,
            strftime('%Y', data) as ano,
            strftime('%m', data) as mes,
            SUM(saldo_devedor) as total_mes
        FROM faturas
        WHERE (:startDate IS NULL OR data >= :startDate) AND (:endDate IS NULL OR data <= :endDate)
        AND (:searchQuery IS NULL OR cliente_nome LIKE '%' || :searchQuery || '%' OR numero_fatura LIKE '%' || :searchQuery || '%')
        GROUP BY mes_ano_str, ano, mes
        ORDER BY ano DESC, mes DESC
    """)
    fun getFaturamentoMensal(startDate: String?, endDate: String?, searchQuery: String?): Flow<List<FaturamentoMensal>>

    @Query("""
        SELECT
            cliente_nome,
            SUM(saldo_devedor) as total_gasto_cliente
        FROM faturas
        WHERE (:startDate IS NULL OR data >= :startDate) AND (:endDate IS NULL OR data <= :endDate)
        AND (:searchQuery IS NULL OR cliente_nome LIKE '%' || :searchQuery || '%')
        GROUP BY cliente_nome
        ORDER BY total_gasto_cliente DESC
    """)
    fun getResumoPorCliente(startDate: String?, endDate: String?, searchQuery: String?): Flow<List<ResumoCliente>>

    @Query("""
        SELECT
            artigos_json,
            SUM(saldo_devedor) as total_fatura_para_artigo_sum
        FROM faturas
        WHERE (:startDate IS NULL OR data >= :startDate) AND (:endDate IS NULL OR data <= :endDate)
        AND (:searchQuery IS NULL OR artigos_json LIKE '%' || :searchQuery || '%')
        ORDER BY total_fatura_para_artigo_sum DESC
    """)
    fun getArtigosDataForAnalysis(startDate: String?, endDate: String?, searchQuery: String?): Flow<List<ArtigosJsonResult>>

    // Classes auxiliares para as queries de resumo
    data class FaturamentoMensal(
        val mes_ano_str: String,
        val ano: Int,
        val mes: Int,
        val total_mes: Double
    )

    data class ResumoCliente(
        val cliente_nome: String,
        val total_gasto_cliente: Double
    )

    data class ArtigosJsonResult(
        val artigos_json: String?
    )
}