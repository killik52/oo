// app/src/main/java/com/example/myapplication/database/AppDatabase.kt
package com.example.myapplication.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.*
import com.example.myapplication.database.dao.*

@Database(
    entities = [
        Artigo::class,
        Cliente::class,
        ClienteBloqueado::class,
        Fatura::class,
        FaturaItem::class,
        FaturaNotaEntity::class,
        FaturaFotoEntity::class,
        FaturaLixeira::class,
        InformacoesEmpresaEntity::class,
        InstrucoesPagamentoEntity::class
    ],
    version = 1, // Comece com a versão 1
    exportSchema = false // Defina como true para exportar o esquema para um arquivo JSON
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artigoDao(): ArtigoDao
    abstract fun clienteDao(): ClienteDao
    abstract fun clienteBloqueadoDao(): ClienteBloqueadoDao
    abstract fun faturaDao(): FaturaDao
    abstract fun faturaItemDao(): FaturaItemDao
    abstract fun faturaNotaDao(): FaturaNotaDao
    abstract fun faturaFotoDao(): FaturaFotoDao
    abstract fun faturaLixeiraDao(): FaturaLixeiraDao
    abstract fun informacoesEmpresaDao(): InformacoesEmpresaDao
    abstract fun instrucoesPagamentoDao(): InstrucoesPagamentoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookv6_database" // Nome do seu novo banco de dados Room
                )
                    // Se você tiver migrações complexas do SQLiteOpenHelper, você precisará de um ou mais addMigrations aqui
                    // Exemplo: .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration() // Isso recria o banco de dados se as migrações não forem encontradas/válidas. Ideal para desenvolvimento, mas perigoso em produção (perde dados).
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Exemplo de migração (você precisaria de uma para cada versão do seu banco SQLiteOpenHelper)
        // Se você não tem migrações de dados importantes, fallbackToDestructiveMigration é mais simples no início.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Exemplo de alteração de tabela: adicionar uma coluna
                // database.execSQL("ALTER TABLE clientes ADD COLUMN novo_campo TEXT");
            }
        }
    }
}