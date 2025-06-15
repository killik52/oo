package com.example.myapplication.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myapplication.Artigo
import com.example.myapplication.Cliente
import com.example.myapplication.ClienteBloqueado
import com.example.myapplication.Fatura
import com.example.myapplication.FaturaFotoEntity
import com.example.myapplication.FaturaItem
import com.example.myapplication.FaturaLixeira
import com.example.myapplication.InformacoesEmpresaEntity
import com.example.myapplication.InstrucoesPagamentoEntity
import com.example.myapplication.database.converter.LocalDateConverter
import com.example.myapplication.database.converter.BigDecimalConverter
import com.example.myapplication.database.dao.ArtigoDao
import com.example.myapplication.database.dao.ClienteBloqueadoDao
import com.example.myapplication.database.dao.ClienteDao
import com.example.myapplication.database.dao.FaturaDao
import com.example.myapplication.database.dao.FaturaFotoDao
import com.example.myapplication.database.dao.FaturaItemDao
import com.example.myapplication.database.dao.FaturaLixeiraDao
import com.example.myapplication.database.dao.FaturaNotaDao
import com.example.myapplication.database.dao.InformacoesEmpresaDao
import com.example.myapplication.database.dao.InstrucoesPagamentoDao

@Database(
    entities = [
        com.example.myapplication.database.Artigo::class,
        Cliente::class,
        ClienteBloqueado::class,
        Fatura::class,
        FaturaItem::class,
        FaturaFotoEntity::class,
        FaturaNotaEntity::class,
        FaturaLixeira::class,
        InformacoesEmpresaEntity::class,
        InstrucoesPagamentoEntity::class
    ],
    version = 1,
    exportSchema = true,
    autoMigrations = [
    ]
)
@TypeConverters(
    com.example.myapplication.database.converter.LocalDateConverter::class,
    com.example.myapplication.database.converter.BigDecimalConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artigoDao(): ArtigoDao
    abstract fun clienteDao(): ClienteDao
    abstract fun clienteBloqueadoDao(): ClienteBloqueadoDao
    abstract fun faturaDao(): FaturaDao
    abstract fun faturaItemDao(): FaturaItemDao
    abstract fun faturaFotoDao(): FaturaFotoDao
    abstract fun faturaNotaDao(): FaturaNotaDao
    abstract fun faturaLixeiraDao(): FaturaLixeiraDao
    abstract fun informacoesEmpresaDao(): InformacoesEmpresaDao
    abstract fun instrucoesPagamentoDao(): InstrucoesPagamentoDao
}
