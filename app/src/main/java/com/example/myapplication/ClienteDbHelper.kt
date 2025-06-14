package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.myapplication.ArtigoContract.ArtigoEntry
import com.example.myapplication.ClienteBloqueado.ClientesBloqueadosEntry
import com.example.myapplication.ClienteContract.ClienteEntry
import com.example.myapplication.FaturaContract.FaturaEntry
import com.example.myapplication.FaturaContract.FaturaFotoEntry
import com.example.myapplication.FaturaContract.FaturaItemEntry
import com.example.myapplication.FaturaLixeiraContract.FaturaLixeiraEntry
import com.example.myapplication.InformacoesEmpresaContract.InformacoesEmpresaEntry
import com.example.myapplication.InstrucoesPagamentoContract.InstrucoesPagamentoEntry
import com.example.myapplication.NotasContract.NotasEntry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClienteDbHelper(private val context: Context) :
    SQLiteOpenHelper(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Create Cliente table
        db.execSQL(ClienteEntry.SQL_CREATE_ENTRIES)
        // Create Artigo table
        db.execSQL(ArtigoEntry.SQL_CREATE_ENTRIES)
        // Create Fatura table
        db.execSQL(FaturaEntry.SQL_CREATE_ENTRIES)
        // Create FaturaItem table
        db.execSQL(FaturaItemEntry.SQL_CREATE_ENTRIES)
        // Create FaturaFoto table
        db.execSQL(FaturaFotoEntry.SQL_CREATE_ENTRIES)
        // Create ClientesBloqueados table
        db.execSQL(ClientesBloqueadosEntry.SQL_CREATE_ENTRIES)
        // Create InformacoesEmpresa table
        db.execSQL(InformacoesEmpresaEntry.SQL_CREATE_ENTRIES)
        // Create InstrucoesPagamento table
        db.execSQL(InstrucoesPagamentoEntry.SQL_CREATE_ENTRIES)
        // Create Notas table
        db.execSQL(NotasEntry.SQL_CREATE_ENTRIES)
        // Create FaturaLixeira table
        db.execSQL(FaturaLixeiraEntry.SQL_CREATE_ENTRIES)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Implement database migration logic here.
        // This is a common pattern for handling schema changes over different app versions.

        // Example: if upgrading from version 1 to 2, add 'cnpj' column to 'cliente' table
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_CNPJ} TEXT")
            db.execSQL("ALTER TABLE ${ArtigoEntry.TABLE_NAME} ADD COLUMN ${ArtigoEntry.COLUMN_NAME_NUMERO_SERIE} TEXT")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE ${FaturaEntry.TABLE_NAME} ADD COLUMN ${FaturaEntry.COLUMN_NAME_FOI_ENVIADA} INTEGER DEFAULT 0")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_CPF} TEXT")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_LOGRADOURO} TEXT")
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_NUMERO} TEXT")
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_COMPLEMENTO} TEXT")
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_BAIRRO} TEXT")
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_CIDADE} TEXT")
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_ESTADO} TEXT")
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_CEP} TEXT")
        }
        if (oldVersion < 6) {
            db.execSQL(ClientesBloqueadosEntry.SQL_CREATE_ENTRIES)
        }
        if (oldVersion < 7) {
            db.execSQL(InformacoesEmpresaEntry.SQL_CREATE_ENTRIES)
        }
        if (oldVersion < 8) {
            db.execSQL(InstrucoesPagamentoEntry.SQL_CREATE_ENTRIES)
        }
        if (oldVersion < 9) {
            db.execSQL(NotasEntry.SQL_CREATE_ENTRIES)
        }
        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_EMAIL} TEXT")
            db.execSQL("ALTER TABLE ${ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteEntry.COLUMN_NAME_TELEFONE} TEXT")
        }
        if (oldVersion < 11) {
            db.execSQL("ALTER TABLE ${FaturaEntry.TABLE_NAME} ADD COLUMN ${FaturaEntry.COLUMN_NAME_OBSERVACAO} TEXT")
            db.execSQL("ALTER TABLE ${FaturaEntry.TABLE_NAME} ADD COLUMN ${FaturaEntry.COLUMN_NAME_DESCONTO} REAL DEFAULT 0.0")
            db.execSQL("ALTER TABLE ${FaturaEntry.TABLE_NAME} ADD COLUMN ${FaturaEntry.COLUMN_NAME_TAXA_ENTREGA} REAL DEFAULT 0.0")
            db.execSQL("ALTER TABLE ${FaturaEntry.TABLE_NAME} ADD COLUMN ${FaturaEntry.COLUMN_NAME_MODO_SIMPLIFICADO} INTEGER DEFAULT 0")
        }
        if (oldVersion < 12) {
            db.execSQL(FaturaLixeiraEntry.SQL_CREATE_ENTRIES)
        }
    }


    fun addArtigo(artigo: Artigo): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(ArtigoEntry.COLUMN_NAME_NOME, artigo.nome)
            put(ArtigoEntry.COLUMN_NAME_PRECO_UNITARIO, artigo.precoUnitario)
            put(ArtigoEntry.COLUMN_NAME_DESCRICAO, artigo.descricao)
            put(ArtigoEntry.COLUMN_NAME_NUMERO_SERIE, artigo.numeroSerie)
        }

        // Log para depuração:
        Log.d("ClienteDbHelper", "Tentando inserir artigo: ${artigo.nome}")
        Log.d("ClienteDbHelper", "ContentValues para artigo: $values")

        val newRowId = db.insert(ArtigoEntry.TABLE_NAME, null, values)

        // Log para depuração:
        Log.d("ClienteDbHelper", "Resultado da inserção de artigo (newRowId): $newRowId")

        return newRowId != -1L
    }

    fun updateArtigo(artigo: Artigo): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(ArtigoEntry.COLUMN_NAME_NOME, artigo.nome)
            put(ArtigoEntry.COLUMN_NAME_PRECO_UNITARIO, artigo.precoUnitario)
            put(ArtigoEntry.COLUMN_NAME_DESCRICAO, artigo.descricao)
            put(ArtigoEntry.COLUMN_NAME_NUMERO_SERIE, artigo.numeroSerie)
        }

        val selection = "${ArtigoEntry.COLUMN_NAME_ID} LIKE ?"
        val selectionArgs = arrayOf(artigo.id.toString())

        // Log para depuração:
        Log.d("ClienteDbHelper", "Tentando atualizar artigo ID: ${artigo.id}")
        Log.d("ClienteDbHelper", "ContentValues para atualização de artigo: $values")

        val count = db.update(
            ArtigoEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
        // Log para depuração:
        Log.d("ClienteDbHelper", "Número de linhas atualizadas (artigo): $count")

        return count > 0
    }

    fun getArtigoById(id: Long): Artigo? {
        val db = this.readableDatabase
        var artigo: Artigo? = null

        val projection = arrayOf(
            ArtigoEntry.COLUMN_NAME_ID,
            ArtigoEntry.COLUMN_NAME_NOME,
            ArtigoEntry.COLUMN_NAME_PRECO_UNITARIO,
            ArtigoEntry.COLUMN_NAME_DESCRICAO,
            ArtigoEntry.COLUMN_NAME_NUMERO_SERIE
        )

        val selection = "${ArtigoEntry.COLUMN_NAME_ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        val cursor = db.query(
            ArtigoEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        with(cursor) {
            if (moveToFirst()) {
                val itemId = getLong(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_ID))
                val nome = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_NOME))
                val precoUnitario = getDouble(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_PRECO_UNITARIO))
                val descricao = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_DESCRICAO))
                val numeroSerie = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_NUMERO_SERIE))

                artigo = Artigo(itemId, nome, precoUnitario, 1, numeroSerie, descricao)
            }
        }
        cursor.close()
        return artigo
    }

    fun getAllArtigos(): List<Artigo> {
        val artigosList = mutableListOf<Artigo>()
        val db = this.readableDatabase

        val cursor = db.query(
            ArtigoEntry.TABLE_NAME,
            null, // all columns
            null, // all rows
            null,
            null,
            null,
            ArtigoEntry.COLUMN_NAME_NOME + " ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                val itemId = getLong(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_ID))
                val nome = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_NOME))
                val precoUnitario = getDouble(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_PRECO_UNITARIO))
                val descricao = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_DESCRICAO))
                val numeroSerie = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_NUMERO_SERIE))

                artigosList.add(Artigo(itemId, nome, precoUnitario, 1, numeroSerie, descricao))
            }
        }
        cursor.close()
        return artigosList
    }

    fun searchArtigos(query: String): List<Artigo> {
        val artigosList = mutableListOf<Artigo>()
        val db = this.readableDatabase
        val selection = "${ArtigoEntry.COLUMN_NAME_NOME} LIKE ? OR ${ArtigoEntry.COLUMN_NAME_DESCRICAO} LIKE ? OR ${ArtigoEntry.COLUMN_NAME_NUMERO_SERIE} LIKE ?"
        val fuzzyQuery = "%$query%"
        val selectionArgs = arrayOf(fuzzyQuery, fuzzyQuery, fuzzyQuery)

        val cursor = db.query(
            ArtigoEntry.TABLE_NAME,
            null, // all columns
            selection,
            selectionArgs,
            null,
            null,
            ArtigoEntry.COLUMN_NAME_NOME + " ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                val itemId = getLong(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_ID))
                val nome = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_NOME))
                val precoUnitario = getDouble(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_PRECO_UNITARIO))
                val descricao = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_DESCRICAO))
                val numeroSerie = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_NUMERO_SERIE))

                artigosList.add(Artigo(itemId, nome, precoUnitario, 1, numeroSerie, descricao))
            }
        }
        cursor.close()
        return artigosList
    }

    fun deleteArtigo(artigoId: Long): Boolean {
        val db = this.writableDatabase
        val selection = "${ArtigoEntry.COLUMN_NAME_ID} LIKE ?"
        val selectionArgs = arrayOf(artigoId.toString())
        val deletedRows = db.delete(ArtigoEntry.TABLE_NAME, selection, selectionArgs)
        return deletedRows > 0
    }

    fun addCliente(cliente: Cliente): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(ClienteEntry.COLUMN_NAME_NOME, cliente.nome)
            put(ClienteEntry.COLUMN_NAME_ENDERECO, cliente.endereco)
            put(ClienteEntry.COLUMN_NAME_TELEFONE, cliente.telefone)
            put(ClienteEntry.COLUMN_NAME_EMAIL, cliente.email)
            put(ClienteEntry.COLUMN_NAME_CPF, cliente.cpf)
            put(ClienteEntry.COLUMN_NAME_CNPJ, cliente.cnpj)
            put(ClienteEntry.COLUMN_NAME_LOGRADOURO, cliente.logradouro)
            put(ClienteEntry.COLUMN_NAME_NUMERO, cliente.numero)
            put(ClienteEntry.COLUMN_NAME_COMPLEMENTO, cliente.complemento)
            put(ClienteEntry.COLUMN_NAME_BAIRRO, cliente.bairro)
            put(ClienteEntry.COLUMN_NAME_CIDADE, cliente.cidade)
            put(ClienteEntry.COLUMN_NAME_ESTADO, cliente.estado)
            put(ClienteEntry.COLUMN_NAME_CEP, cliente.cep)
        }

        val newRowId = db.insert(ClienteEntry.TABLE_NAME, null, values)
        return newRowId != -1L
    }

    fun updateCliente(cliente: Cliente): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(ClienteEntry.COLUMN_NAME_NOME, cliente.nome)
            put(ClienteEntry.COLUMN_NAME_ENDERECO, cliente.endereco)
            put(ClienteEntry.COLUMN_NAME_TELEFONE, cliente.telefone)
            put(ClienteEntry.COLUMN_NAME_EMAIL, cliente.email)
            put(ClienteEntry.COLUMN_NAME_CPF, cliente.cpf)
            put(ClienteEntry.COLUMN_NAME_CNPJ, cliente.cnpj)
            put(ClienteEntry.COLUMN_NAME_LOGRADOURO, cliente.logradouro)
            put(ClienteEntry.COLUMN_NAME_NUMERO, cliente.numero)
            put(ClienteEntry.COLUMN_NAME_COMPLEMENTO, cliente.complemento)
            put(ClienteEntry.COLUMN_NAME_BAIRRO, cliente.bairro)
            put(ClienteEntry.COLUMN_NAME_CIDADE, cliente.cidade)
            put(ClienteEntry.COLUMN_NAME_ESTADO, cliente.estado)
            put(ClienteEntry.COLUMN_NAME_CEP, cliente.cep)
        }

        val selection = "${ClienteEntry.COLUMN_NAME_ID} LIKE ?"
        val selectionArgs = arrayOf(cliente.id.toString())

        val count = db.update(
            ClienteEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
        return count > 0
    }

    fun getClienteById(id: Long): Cliente? {
        val db = this.readableDatabase
        var cliente: Cliente? = null

        val projection = arrayOf(
            ClienteEntry.COLUMN_NAME_ID,
            ClienteEntry.COLUMN_NAME_NOME,
            ClienteEntry.COLUMN_NAME_ENDERECO,
            ClienteEntry.COLUMN_NAME_TELEFONE,
            ClienteEntry.COLUMN_NAME_EMAIL,
            ClienteEntry.COLUMN_NAME_CPF,
            ClienteEntry.COLUMN_NAME_CNPJ,
            ClienteEntry.COLUMN_NAME_LOGRADOURO,
            ClienteEntry.COLUMN_NAME_NUMERO,
            ClienteEntry.COLUMN_NAME_COMPLEMENTO,
            ClienteEntry.COLUMN_NAME_BAIRRO,
            ClienteEntry.COLUMN_NAME_CIDADE,
            ClienteEntry.COLUMN_NAME_ESTADO,
            ClienteEntry.COLUMN_NAME_CEP
        )

        val selection = "${ClienteEntry.COLUMN_NAME_ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        val cursor = db.query(
            ClienteEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        with(cursor) {
            if (moveToFirst()) {
                val itemId = getLong(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ID))
                val nome = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_NOME))
                val endereco = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ENDERECO))
                val telefone = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_TELEFONE))
                val email = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_EMAIL))
                val cpf = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CPF))
                val cnpj = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CNPJ))
                val logradouro = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_LOGRADOURO))
                val numero = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_NUMERO))
                val complemento = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_COMPLEMENTO))
                val bairro = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_BAIRRO))
                val cidade = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CIDADE))
                val estado = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ESTADO))
                val cep = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CEP))

                cliente = Cliente(itemId, nome, endereco, telefone, email, cpf, cnpj, logradouro, numero, complemento, bairro, cidade, estado, cep)
            }
        }
        cursor.close()
        return cliente
    }

    fun getAllClientes(): List<Cliente> {
        val clientesList = mutableListOf<Cliente>()
        val db = this.readableDatabase

        val cursor = db.query(
            ClienteEntry.TABLE_NAME,
            null, // all columns
            null, // all rows
            null,
            null,
            null,
            ClienteEntry.COLUMN_NAME_NOME + " ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                val itemId = getLong(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ID))
                val nome = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_NOME))
                val endereco = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ENDERECO))
                val telefone = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_TELEFONE))
                val email = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_EMAIL))
                val cpf = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CPF))
                val cnpj = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CNPJ))
                val logradouro = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_LOGRADOURO))
                val numero = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_NUMERO))
                val complemento = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_COMPLEMENTO))
                val bairro = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_BAIRRO))
                val cidade = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CIDADE))
                val estado = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ESTADO))
                val cep = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CEP))

                clientesList.add(Cliente(itemId, nome, endereco, telefone, email, cpf, cnpj, logradouro, numero, complemento, bairro, cidade, estado, cep))
            }
        }
        cursor.close()
        return clientesList
    }

    fun getRecentClientes(limit: Int): List<Cliente> {
        val clientesList = mutableListOf<Cliente>()
        val db = this.readableDatabase

        val cursor = db.query(
            ClienteEntry.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "${ClienteEntry.COLUMN_NAME_ID} DESC", // Or a timestamp column if available
            limit.toString()
        )

        with(cursor) {
            while (moveToNext()) {
                val itemId = getLong(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ID))
                val nome = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_NOME))
                val endereco = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ENDERECO))
                val telefone = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_TELEFONE))
                val email = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_EMAIL))
                val cpf = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CPF))
                val cnpj = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CNPJ))
                val logradouro = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_LOGRADOURO))
                val numero = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_NUMERO))
                val complemento = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_COMPLEMENTO))
                val bairro = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_BAIRRO))
                val cidade = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CIDADE))
                val estado = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ESTADO))
                val cep = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CEP))

                clientesList.add(Cliente(itemId, nome, endereco, telefone, email, cpf, cnpj, logradouro, numero, complemento, bairro, cidade, estado, cep))
            }
        }
        cursor.close()
        return clientesList
    }

    fun searchClientes(query: String): List<Cliente> {
        val clientesList = mutableListOf<Cliente>()
        val db = this.readableDatabase
        val selection = "${ClienteEntry.COLUMN_NAME_NOME} LIKE ? OR ${ClienteEntry.COLUMN_NAME_CPF} LIKE ? OR ${ClienteEntry.COLUMN_NAME_CNPJ} LIKE ?"
        val fuzzyQuery = "%$query%"
        val selectionArgs = arrayOf(fuzzyQuery, fuzzyQuery, fuzzyQuery)

        val cursor = db.query(
            ClienteEntry.TABLE_NAME,
            null, // all columns
            selection,
            selectionArgs,
            null,
            null,
            ClienteEntry.COLUMN_NAME_NOME + " ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                val itemId = getLong(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ID))
                val nome = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_NOME))
                val endereco = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ENDERECO))
                val telefone = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_TELEFONE))
                val email = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_EMAIL))
                val cpf = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CPF))
                val cnpj = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CNPJ))
                val logradouro = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_LOGRADOURO))
                val numero = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_NUMERO))
                val complemento = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_COMPLEMENTO))
                val bairro = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_BAIRRO))
                val cidade = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CIDADE))
                val estado = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ESTADO))
                val cep = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CEP))

                clientesList.add(Cliente(itemId, nome, endereco, telefone, email, cpf, cnpj, logradouro, numero, complemento, bairro, cidade, estado, cep))
            }
        }
        cursor.close()
        return clientesList
    }

    fun deleteCliente(clienteId: Long): Boolean {
        val db = this.writableDatabase
        val selection = "${ClienteEntry.COLUMN_NAME_ID} LIKE ?"
        val selectionArgs = arrayOf(clienteId.toString())
        val deletedRows = db.delete(ClienteEntry.TABLE_NAME, selection, selectionArgs)
        return deletedRows > 0
    }

    fun addFatura(fatura: Fatura): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(FaturaEntry.COLUMN_NAME_CLIENTE_ID, fatura.clienteId)
            put(FaturaEntry.COLUMN_NAME_DATA, fatura.data)
            put(FaturaEntry.COLUMN_NAME_TOTAL, fatura.total)
            put(FaturaEntry.COLUMN_NAME_FOI_ENVIADA, if (fatura.foiEnviada) 1 else 0)
            put(FaturaEntry.COLUMN_NAME_OBSERVACAO, fatura.observacao)
            put(FaturaEntry.COLUMN_NAME_DESCONTO, fatura.desconto)
            put(FaturaEntry.COLUMN_NAME_TAXA_ENTREGA, fatura.taxaEntrega)
            put(FaturaEntry.COLUMN_NAME_MODO_SIMPLIFICADO, if (fatura.modoSimplificado) 1 else 0)
        }
        return db.insert(FaturaEntry.TABLE_NAME, null, values)
    }

    fun updateFatura(fatura: Fatura): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(FaturaEntry.COLUMN_NAME_CLIENTE_ID, fatura.clienteId)
            put(FaturaEntry.COLUMN_NAME_DATA, fatura.data)
            put(FaturaEntry.COLUMN_NAME_TOTAL, fatura.total)
            put(FaturaEntry.COLUMN_NAME_FOI_ENVIADA, if (fatura.foiEnviada) 1 else 0)
            put(FaturaEntry.COLUMN_NAME_OBSERVACAO, fatura.observacao)
            put(FaturaEntry.COLUMN_NAME_DESCONTO, fatura.desconto)
            put(FaturaEntry.COLUMN_NAME_TAXA_ENTREGA, fatura.taxaEntrega)
            put(FaturaEntry.COLUMN_NAME_MODO_SIMPLIFICADO, if (fatura.modoSimplificado) 1 else 0)
        }

        val selection = "${FaturaEntry.COLUMN_NAME_ID} = ?"
        val selectionArgs = arrayOf(fatura.id.toString())

        val count = db.update(
            FaturaEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
        return count > 0
    }

    fun getFaturaById(faturaId: Long): Fatura? {
        val db = this.readableDatabase
        var fatura: Fatura? = null

        val projection = arrayOf(
            FaturaEntry.COLUMN_NAME_ID,
            FaturaEntry.COLUMN_NAME_CLIENTE_ID,
            FaturaEntry.COLUMN_NAME_DATA,
            FaturaEntry.COLUMN_NAME_TOTAL,
            FaturaEntry.COLUMN_NAME_FOI_ENVIADA,
            FaturaEntry.COLUMN_NAME_OBSERVACAO,
            FaturaEntry.COLUMN_NAME_DESCONTO,
            FaturaEntry.COLUMN_NAME_TAXA_ENTREGA,
            FaturaEntry.COLUMN_NAME_MODO_SIMPLIFICADO
        )

        val selection = "${FaturaEntry.COLUMN_NAME_ID} = ?"
        val selectionArgs = arrayOf(faturaId.toString())

        val cursor = db.query(
            FaturaEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        with(cursor) {
            if (moveToFirst()) {
                val id = getLong(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_ID))
                val clienteId = getLong(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_CLIENTE_ID))
                val data = getString(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_DATA))
                val total = getDouble(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_TOTAL))
                val foiEnviada = getInt(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_FOI_ENVIADA)) == 1
                val observacao = getString(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_OBSERVACAO))
                val desconto = getDouble(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_DESCONTO))
                val taxaEntrega = getDouble(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_TAXA_ENTREGA))
                val modoSimplificado = getInt(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_MODO_SIMPLIFICADO)) == 1

                fatura = Fatura(id, clienteId, data, total, foiEnviada, observacao, desconto, taxaEntrega, modoSimplificado)
            }
        }
        cursor.close()
        return fatura
    }

    fun getAllFaturasResumidas(query: String? = null): List<FaturaResumidaItem> {
        val faturasList = mutableListOf<FaturaResumidaItem>()
        val db = this.readableDatabase

        val selectionArgs = mutableListOf<String>()
        var selection: String? = null

        query?.let {
            val fuzzyQuery = "%$it%"
            selection = "${ClienteEntry.COLUMN_NAME_NOME} LIKE ? OR ${FaturaEntry.COLUMN_NAME_DATA} LIKE ? OR ${FaturaEntry.COLUMN_NAME_OBSERVACAO} LIKE ?"
            selectionArgs.add(fuzzyQuery)
            selectionArgs.add(fuzzyQuery)
            selectionArgs.add(fuzzyQuery)
        }

        val cursor = db.query(
            "${FaturaEntry.TABLE_NAME} INNER JOIN ${ClienteEntry.TABLE_NAME} ON ${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_CLIENTE_ID} = ${ClienteEntry.TABLE_NAME}.${ClienteEntry.COLUMN_NAME_ID}",
            arrayOf(
                "${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_ID}",
                "${ClienteEntry.TABLE_NAME}.${ClienteEntry.COLUMN_NAME_NOME} AS cliente_nome",
                "${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_DATA}",
                "${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_TOTAL}",
                "${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_FOI_ENVIADA}"
            ),
            selection,
            selectionArgs.toTypedArray(),
            null,
            null,
            "${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_ID} DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_ID))
                val clienteNome = getString(getColumnIndexOrThrow("cliente_nome"))
                val data = getString(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_DATA))
                val total = getDouble(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_TOTAL))
                val foiEnviada = getInt(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_FOI_ENVIADA)) == 1

                faturasList.add(FaturaResumidaItem(id, clienteNome, data, total, foiEnviada))
            }
        }
        cursor.close()
        return faturasList
    }

    fun deleteFatura(faturaId: Long): Boolean {
        val db = this.writableDatabase
        val success = try {
            db.beginTransaction()
            // Obter a fatura antes de deletar
            val fatura = getFaturaById(faturaId)
            if (fatura != null) {
                // Inserir na lixeira
                val lixeiraValues = ContentValues().apply {
                    put(FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL, fatura.id)
                    put(FaturaLixeiraEntry.COLUMN_NAME_CLIENTE_ID, fatura.clienteId)
                    put(FaturaLixeiraEntry.COLUMN_NAME_DATA, fatura.data)
                    put(FaturaLixeiraEntry.COLUMN_NAME_TOTAL, fatura.total)
                    put(FaturaLixeiraEntry.COLUMN_NAME_FOI_ENVIADA, if (fatura.foiEnviada) 1 else 0)
                    put(FaturaLixeiraEntry.COLUMN_NAME_OBSERVACAO, fatura.observacao)
                    put(FaturaLixeiraEntry.COLUMN_NAME_DESCONTO, fatura.desconto)
                    put(FaturaLixeiraEntry.COLUMN_NAME_TAXA_ENTREGA, fatura.taxaEntrega)
                    put(FaturaLixeiraEntry.COLUMN_NAME_MODO_SIMPLIFICADO, if (fatura.modoSimplificado) 1 else 0)
                    put(FaturaLixeiraEntry.COLUMN_NAME_DATA_EXCLUSAO, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                }
                val lixeiraId = db.insert(FaturaLixeiraEntry.TABLE_NAME, null, lixeiraValues)
                if (lixeiraId == -1L) {
                    throw IOException("Failed to insert into trash")
                }

                // Mover itens da fatura para a lixeira
                val faturaItems = getArtigoItemsByFaturaId(faturaId)
                faturaItems.forEach { item ->
                    val itemLixeiraValues = ContentValues().apply {
                        put(FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL, faturaId)
                        put(ArtigoEntry.COLUMN_NAME_NOME, item.nome)
                        put(ArtigoEntry.COLUMN_NAME_PRECO_UNITARIO, item.precoUnitario)
                        put(ArtigoEntry.COLUMN_NAME_QUANTIDADE, item.quantidade)
                        put(ArtigoEntry.COLUMN_NAME_NUMERO_SERIE, item.numeroSerie)
                        put(ArtigoEntry.COLUMN_NAME_DESCRICAO, item.descricao)
                    }
                    db.insert(FaturaLixeiraEntry.TABLE_NAME_ITENS, null, itemLixeiraValues)
                }

                // Mover fotos da fatura para a lixeira
                val faturaFotos = getFaturaPhotos(faturaId)
                faturaFotos.forEach { photoPath ->
                    val photoLixeiraValues = ContentValues().apply {
                        put(FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL, faturaId)
                        put(FaturaEntry.COLUMN_NAME_FOTO_PATH, photoPath)
                    }
                    db.insert(FaturaLixeiraEntry.TABLE_NAME_FOTOS, null, photoLixeiraValues)
                }
            }

            // Deletar fatura original e seus itens/fotos
            val faturaSelection = "${FaturaEntry.COLUMN_NAME_ID} = ?"
            val faturaSelectionArgs = arrayOf(faturaId.toString())
            val deletedFaturaRows = db.delete(FaturaEntry.TABLE_NAME, faturaSelection, faturaSelectionArgs)

            val itemSelection = "${FaturaItemEntry.COLUMN_NAME_FATURA_ID} = ?"
            val itemSelectionArgs = arrayOf(faturaId.toString())
            db.delete(FaturaItemEntry.TABLE_NAME, itemSelection, itemSelectionArgs)

            val photoSelection = "${FaturaFotoEntry.COLUMN_NAME_FATURA_ID} = ?"
            val photoSelectionArgs = arrayOf(faturaId.toString())
            db.delete(FaturaFotoEntry.TABLE_NAME, photoSelection, photoSelectionArgs)

            db.setTransactionSuccessful()
            deletedFaturaRows > 0
        } catch (e: Exception) {
            Log.e("ClienteDbHelper", "Erro ao mover fatura para lixeira: ${e.message}", e)
            false
        } finally {
            db.endTransaction()
        }
        return success
    }

    fun restoreFaturaFromLixeira(faturaIdOriginal: Long): Boolean {
        val db = this.writableDatabase
        var success = false
        db.beginTransaction()
        try {
            // Obter dados da fatura da lixeira
            val faturaLixeira = getFaturaFromLixeira(faturaIdOriginal)
            if (faturaLixeira != null) {
                // Inserir de volta na tabela de faturas
                val faturaValues = ContentValues().apply {
                    put(FaturaEntry.COLUMN_NAME_ID, faturaLixeira.id) // Manter o ID original
                    put(FaturaEntry.COLUMN_NAME_CLIENTE_ID, faturaLixeira.clienteId)
                    put(FaturaEntry.COLUMN_NAME_DATA, faturaLixeira.data)
                    put(FaturaEntry.COLUMN_NAME_TOTAL, faturaLixeira.total)
                    put(FaturaEntry.COLUMN_NAME_FOI_ENVIADA, if (faturaLixeira.foiEnviada) 1 else 0)
                    put(FaturaEntry.COLUMN_NAME_OBSERVACAO, faturaLixeira.observacao)
                    put(FaturaEntry.COLUMN_NAME_DESCONTO, faturaLixeira.desconto)
                    put(FaturaEntry.COLUMN_NAME_TAXA_ENTREGA, faturaLixeira.taxaEntrega)
                    put(FaturaEntry.COLUMN_NAME_MODO_SIMPLIFICADO, if (faturaLixeira.modoSimplificado) 1 else 0)
                }
                val newFaturaId = db.insertWithOnConflict(FaturaEntry.TABLE_NAME, null, faturaValues, SQLiteDatabase.CONFLICT_REPLACE)
                if (newFaturaId == -1L) throw IOException("Failed to restore fatura")

                // Restaurar itens da fatura
                val itemsLixeira = getArtigoItemsByFaturaIdFromLixeira(faturaIdOriginal)
                itemsLixeira.forEach { item ->
                    val itemValues = ContentValues().apply {
                        put(FaturaItemEntry.COLUMN_NAME_FATURA_ID, faturaIdOriginal)
                        put(FaturaItemEntry.COLUMN_NAME_ARTIGO_ID, item.id) // Supondo que id seja o id do artigo original
                        put(FaturaItemEntry.COLUMN_NAME_QUANTIDADE, item.quantidade)
                        put(FaturaItemEntry.COLUMN_NAME_PRECO_UNITARIO, item.precoUnitario) // Adicionar se necessário
                        put(FaturaItemEntry.COLUMN_NAME_NOME_ARTIGO, item.nome) // Adicionar se necessário
                        put(FaturaItemEntry.COLUMN_NAME_NUMERO_SERIE, item.numeroSerie) // Adicionar se necessário
                        put(FaturaItemEntry.COLUMN_NAME_DESCRICAO, item.descricao) // Adicionar se necessário
                    }
                    db.insert(FaturaItemEntry.TABLE_NAME, null, itemValues)
                }

                // Restaurar fotos da fatura
                val photosLixeira = getFaturaPhotosFromLixeira(faturaIdOriginal)
                photosLixeira.forEach { path ->
                    val photoValues = ContentValues().apply {
                        put(FaturaFotoEntry.COLUMN_NAME_FATURA_ID, faturaIdOriginal)
                        put(FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH, path)
                    }
                    db.insert(FaturaFotoEntry.TABLE_NAME, null, photoValues)
                }

                // Deletar da lixeira após restauração
                val selection = "${FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL} = ?"
                val selectionArgs = arrayOf(faturaIdOriginal.toString())
                db.delete(FaturaLixeiraEntry.TABLE_NAME, selection, selectionArgs)
                db.delete(FaturaLixeiraEntry.TABLE_NAME_ITENS, selection, selectionArgs)
                db.delete(FaturaLixeiraEntry.TABLE_NAME_FOTOS, selection, selectionArgs)

                db.setTransactionSuccessful()
                success = true
            }
        } catch (e: Exception) {
            Log.e("ClienteDbHelper", "Erro ao restaurar fatura da lixeira: ${e.message}", e)
        } finally {
            db.endTransaction()
        }
        return success
    }

    fun deleteFaturaPermanently(faturaIdOriginal: Long): Boolean {
        val db = this.writableDatabase
        val selection = "${FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL} = ?"
        val selectionArgs = arrayOf(faturaIdOriginal.toString())
        var deletedRows = 0
        db.beginTransaction()
        try {
            deletedRows += db.delete(FaturaLixeiraEntry.TABLE_NAME_ITENS, selection, selectionArgs)
            deletedRows += db.delete(FaturaLixeiraEntry.TABLE_NAME_FOTOS, selection, selectionArgs)
            deletedRows += db.delete(FaturaLixeiraEntry.TABLE_NAME, selection, selectionArgs)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("ClienteDbHelper", "Erro ao excluir fatura permanentemente: ${e.message}", e)
        } finally {
            db.endTransaction()
        }
        return deletedRows > 0
    }

    fun getFaturaFromLixeira(faturaIdOriginal: Long): Fatura? {
        val db = this.readableDatabase
        var fatura: Fatura? = null
        val cursor = db.query(
            FaturaLixeiraEntry.TABLE_NAME,
            null,
            "${FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL} = ?",
            arrayOf(faturaIdOriginal.toString()),
            null, null, null
        )
        with(cursor) {
            if (moveToFirst()) {
                val id = getLong(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL))
                val clienteId = getLong(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_CLIENTE_ID))
                val data = getString(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_DATA))
                val total = getDouble(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_TOTAL))
                val foiEnviada = getInt(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_FOI_ENVIADA)) == 1
                val observacao = getString(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_OBSERVACAO))
                val desconto = getDouble(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_DESCONTO))
                val taxaEntrega = getDouble(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_TAXA_ENTREGA))
                val modoSimplificado = getInt(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_MODO_SIMPLIFICADO)) == 1

                fatura = Fatura(id, clienteId, data, total, foiEnviada, observacao, desconto, taxaEntrega, modoSimplificado)
            }
            cursor.close()
            return fatura
        }
    }

    fun getAllFaturasInLixeira(): List<FaturaResumidaItem> {
        val faturasList = mutableListOf<FaturaResumidaItem>()
        val db = this.readableDatabase

        val cursor = db.query(
            "${FaturaLixeiraEntry.TABLE_NAME} INNER JOIN ${ClienteEntry.TABLE_NAME} ON ${FaturaLixeiraEntry.TABLE_NAME}.${FaturaLixeiraEntry.COLUMN_NAME_CLIENTE_ID} = ${ClienteEntry.TABLE_NAME}.${ClienteEntry.COLUMN_NAME_ID}",
            arrayOf(
                "${FaturaLixeiraEntry.TABLE_NAME}.${FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL}",
                "${ClienteEntry.TABLE_NAME}.${ClienteEntry.COLUMN_NAME_NOME} AS cliente_nome",
                "${FaturaLixeiraEntry.TABLE_NAME}.${FaturaLixeiraEntry.COLUMN_NAME_DATA}",
                "${FaturaLixeiraEntry.TABLE_NAME}.${FaturaLixeiraEntry.COLUMN_NAME_TOTAL}",
                "${FaturaLixeiraEntry.TABLE_NAME}.${FaturaLixeiraEntry.COLUMN_NAME_FOI_ENVIADA}",
                "${FaturaLixeiraEntry.TABLE_NAME}.${FaturaLixeiraEntry.COLUMN_NAME_DATA_EXCLUSAO}"
            ),
            null,
            null,
            null,
            null,
            "${FaturaLixeiraEntry.TABLE_NAME}.${FaturaLixeiraEntry.COLUMN_NAME_DATA_EXCLUSAO} DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL))
                val clienteNome = getString(getColumnIndexOrThrow("cliente_nome"))
                val data = getString(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_DATA))
                val total = getDouble(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_TOTAL))
                val foiEnviada = getInt(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_FOI_ENVIADA)) == 1
                val dataExclusao = getString(getColumnIndexOrThrow(FaturaLixeiraEntry.COLUMN_NAME_DATA_EXCLUSAO))
                faturasList.add(FaturaResumidaItem(id, clienteNome, data, total, foiEnviada, dataExclusao))
            }
        }
        cursor.close()
        return faturasList
    }

    fun addFaturaItem(faturaItem: FaturaItem): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(FaturaItemEntry.COLUMN_NAME_FATURA_ID, faturaItem.faturaId)
            put(FaturaItemEntry.COLUMN_NAME_ARTIGO_ID, faturaItem.artigoId)
            put(FaturaItemEntry.COLUMN_NAME_QUANTIDADE, faturaItem.quantidade)
            put(FaturaItemEntry.COLUMN_NAME_PRECO_UNITARIO, faturaItem.precoUnitario)
            put(FaturaItemEntry.COLUMN_NAME_NOME_ARTIGO, faturaItem.nomeArtigo)
            put(FaturaItemEntry.COLUMN_NAME_NUMERO_SERIE, faturaItem.numeroSerie)
            put(FaturaItemEntry.COLUMN_NAME_DESCRICAO, faturaItem.descricao)
        }
        val newRowId = db.insert(FaturaItemEntry.TABLE_NAME, null, values)
        return newRowId != -1L
    }

    fun deleteFaturaItemsByFaturaId(faturaId: Long): Boolean {
        val db = this.writableDatabase
        val selection = "${FaturaItemEntry.COLUMN_NAME_FATURA_ID} = ?"
        val selectionArgs = arrayOf(faturaId.toString())
        val deletedRows = db.delete(FaturaItemEntry.TABLE_NAME, selection, selectionArgs)
        return deletedRows > 0
    }

    fun getArtigoItemsByFaturaId(faturaId: Long): List<ArtigoItem> {
        val artigosList = mutableListOf<ArtigoItem>()
        val db = this.readableDatabase

        val selection = "${FaturaItemEntry.COLUMN_NAME_FATURA_ID} = ?"
        val selectionArgs = arrayOf(faturaId.toString())

        val cursor = db.query(
            FaturaItemEntry.TABLE_NAME,
            null, // all columns
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(FaturaItemEntry.COLUMN_NAME_ARTIGO_ID)) // Pode ser o ID do artigo original ou -1 se for um item customizado
                val nomeArtigo = getString(getColumnIndexOrThrow(FaturaItemEntry.COLUMN_NAME_NOME_ARTIGO))
                val quantidade = getInt(getColumnIndexOrThrow(FaturaItemEntry.COLUMN_NAME_QUANTIDADE))
                val precoUnitario = getDouble(getColumnIndexOrThrow(FaturaItemEntry.COLUMN_NAME_PRECO_UNITARIO))
                val numeroSerie = getString(getColumnIndexOrThrow(FaturaItemEntry.COLUMN_NAME_NUMERO_SERIE))
                val descricao = getString(getColumnIndexOrThrow(FaturaItemEntry.COLUMN_NAME_DESCRICAO))

                artigosList.add(ArtigoItem(id, nomeArtigo, precoUnitario, quantidade, numeroSerie, descricao))
            }
        }
        cursor.close()
        return artigosList
    }

    fun getArtigoItemsByFaturaIdFromLixeira(faturaIdOriginal: Long): List<ArtigoItem> {
        val artigosList = mutableListOf<ArtigoItem>()
        val db = this.readableDatabase

        val selection = "${FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL} = ?"
        val selectionArgs = arrayOf(faturaIdOriginal.toString())

        val cursor = db.query(
            FaturaLixeiraEntry.TABLE_NAME_ITENS,
            null, // all columns
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        with(cursor) {
            while (moveToNext()) {
                val nomeArtigo = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_NOME))
                val precoUnitario = getDouble(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_PRECO_UNITARIO))
                val quantidade = getInt(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_QUANTIDADE))
                val numeroSerie = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_NUMERO_SERIE))
                val descricao = getString(getColumnIndexOrThrow(ArtigoEntry.COLUMN_NAME_DESCRICAO))
                // Note: ArtigoItem expects an ID. If original article ID is not stored, use -1 or a dummy value.
                artigosList.add(ArtigoItem(-1L, nomeArtigo, precoUnitario, quantidade, numeroSerie, descricao))
            }
        }
        cursor.close()
        return artigosList
    }

    fun addFaturaPhoto(faturaId: Long, photoPath: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(FaturaFotoEntry.COLUMN_NAME_FATURA_ID, faturaId)
            put(FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH, photoPath)
        }
        val newRowId = db.insert(FaturaFotoEntry.TABLE_NAME, null, values)
        return newRowId != -1L
    }

    fun deleteFaturaPhotosByFaturaId(faturaId: Long): Boolean {
        val db = this.writableDatabase
        val selection = "${FaturaFotoEntry.COLUMN_NAME_FATURA_ID} = ?"
        val selectionArgs = arrayOf(faturaId.toString())
        val deletedRows = db.delete(FaturaFotoEntry.TABLE_NAME, selection, selectionArgs)
        return deletedRows > 0
    }

    fun getFaturaPhotos(faturaId: Long): List<String> {
        val photosList = mutableListOf<String>()
        val db = this.readableDatabase

        val selection = "${FaturaFotoEntry.COLUMN_NAME_FATURA_ID} = ?"
        val selectionArgs = arrayOf(faturaId.toString())

        val cursor = db.query(
            FaturaFotoEntry.TABLE_NAME,
            arrayOf(FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH),
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        with(cursor) {
            while (moveToNext()) {
                photosList.add(getString(getColumnIndexOrThrow(FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH)))
            }
        }
        cursor.close()
        return photosList
    }

    fun getFaturaPhotosFromLixeira(faturaIdOriginal: Long): List<String> {
        val photosList = mutableListOf<String>()
        val db = this.readableDatabase

        val selection = "${FaturaLixeiraEntry.COLUMN_NAME_FATURA_ID_ORIGINAL} = ?"
        val selectionArgs = arrayOf(faturaIdOriginal.toString())

        val cursor = db.query(
            FaturaLixeiraEntry.TABLE_NAME_FOTOS,
            arrayOf(FaturaEntry.COLUMN_NAME_FOTO_PATH), // Correção: usar FaturaEntry.COLUMN_NAME_FOTO_PATH
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        with(cursor) {
            while (moveToNext()) {
                photosList.add(getString(getColumnIndexOrThrow(FaturaEntry.COLUMN_NAME_FOTO_PATH)))
            }
        }
        cursor.close()
        return photosList
    }

    fun getInformacoesEmpresa(): InformacoesEmpresa? {
        val db = this.readableDatabase
        var info: InformacoesEmpresa? = null
        val cursor = db.query(InformacoesEmpresaEntry.TABLE_NAME, null, null, null, null, null, null)
        with(cursor) {
            if (moveToFirst()) {
                val id = getLong(getColumnIndexOrThrow(InformacoesEmpresaEntry.COLUMN_NAME_ID))
                val nome = getString(getColumnIndexOrThrow(InformacoesEmpresaEntry.COLUMN_NAME_NOME))
                val endereco = getString(getColumnIndexOrThrow(InformacoesEmpresaEntry.COLUMN_NAME_ENDERECO))
                val telefone = getString(getColumnIndexOrThrow(InformacoesEmpresaEntry.COLUMN_NAME_TELEFONE))
                val email = getString(getColumnIndexOrThrow(InformacoesEmpresaEntry.COLUMN_NAME_EMAIL))
                val cnpj = getString(getColumnIndexOrThrow(InformacoesEmpresaEntry.COLUMN_NAME_CNPJ))
                info = InformacoesEmpresa(id, nome, endereco, telefone, email, cnpj)
            }
        }
        cursor.close()
        return info
    }

    fun saveInformacoesEmpresa(info: InformacoesEmpresa): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(InformacoesEmpresaEntry.COLUMN_NAME_NOME, info.nome)
            put(InformacoesEmpresaEntry.COLUMN_NAME_ENDERECO, info.endereco)
            put(InformacoesEmpresaEntry.COLUMN_NAME_TELEFONE, info.telefone)
            put(InformacoesEmpresaEntry.COLUMN_NAME_EMAIL, info.email)
            put(InformacoesEmpresaEntry.COLUMN_NAME_CNPJ, info.cnpj)
        }
        // Always try to replace, as there should only be one entry
        val newRowId = db.insertWithOnConflict(InformacoesEmpresaEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return newRowId != -1L
    }

    fun getInstrucoesPagamento(): String? {
        val db = this.readableDatabase
        var instrucoes: String? = null
        val cursor = db.query(InstrucoesPagamentoEntry.TABLE_NAME, null, null, null, null, null, null)
        with(cursor) {
            if (moveToFirst()) {
                instrucoes = getString(getColumnIndexOrThrow(InstrucoesPagamentoEntry.COLUMN_NAME_INSTRUCOES))
            }
        }
        cursor.close()
        return instrucoes
    }

    fun saveInstrucoesPagamento(instrucoes: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(InstrucoesPagamentoEntry.COLUMN_NAME_INSTRUCOES, instrucoes)
        }
        // Always try to replace, as there should only be one entry
        val newRowId = db.insertWithOnConflict(InstrucoesPagamentoEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return newRowId != -1L
    }

    fun addNota(nota: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(NotasEntry.COLUMN_NAME_CONTEUDO, nota)
        }
        val newRowId = db.insert(NotasEntry.TABLE_NAME, null, values)
        return newRowId != -1L
    }

    fun updateNota(id: Long, nota: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(NotasEntry.COLUMN_NAME_CONTEUDO, nota)
        }
        val selection = "${NotasEntry.COLUMN_NAME_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        val count = db.update(NotasEntry.TABLE_NAME, values, selection, selectionArgs)
        return count > 0
    }

    fun deleteNota(id: Long): Boolean {
        val db = this.writableDatabase
        val selection = "${NotasEntry.COLUMN_NAME_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        val deletedRows = db.delete(NotasEntry.TABLE_NAME, selection, selectionArgs)
        return deletedRows > 0
    }

    fun getAllNotas(): List<Nota> {
        val notasList = mutableListOf<Nota>()
        val db = this.readableDatabase

        val cursor = db.query(
            NotasEntry.TABLE_NAME,
            null, // all columns
            null, // all rows
            null,
            null,
            null,
            "${NotasEntry.COLUMN_NAME_ID} DESC" // Order by ID descending for most recent first
        )

        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(NotasEntry.COLUMN_NAME_ID))
                val conteudo = getString(getColumnIndexOrThrow(NotasEntry.COLUMN_NAME_CONTEUDO))
                notasList.add(Nota(id, conteudo))
            }
        }
        cursor.close()
        return notasList
    }

    fun addBlockedClient(clienteId: Long): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(ClientesBloqueadosEntry.COLUMN_NAME_CLIENTE_ID, clienteId)
        }
        val newRowId = db.insert(ClientesBloqueadosEntry.TABLE_NAME, null, values)
        return newRowId != -1L
    }

    fun removeBlockedClient(clienteId: Long): Boolean {
        val db = this.writableDatabase
        val selection = "${ClientesBloqueadosEntry.COLUMN_NAME_CLIENTE_ID} = ?"
        val selectionArgs = arrayOf(clienteId.toString())
        val deletedRows = db.delete(ClientesBloqueadosEntry.TABLE_NAME, selection, selectionArgs)
        return deletedRows > 0
    }

    fun isClienteBlocked(clienteId: Long): Boolean {
        val db = this.readableDatabase
        val selection = "${ClientesBloqueadosEntry.COLUMN_NAME_CLIENTE_ID} = ?"
        val selectionArgs = arrayOf(clienteId.toString())
        val cursor = db.query(
            ClientesBloqueadosEntry.TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        val isBlocked = cursor.count > 0
        cursor.close()
        return isBlocked
    }

    fun getAllBlockedClients(): List<Cliente> {
        val blockedClientsList = mutableListOf<Cliente>()
        val db = this.readableDatabase

        val query = "SELECT ${ClienteEntry.TABLE_NAME}.* FROM ${ClienteEntry.TABLE_NAME} INNER JOIN ${ClientesBloqueadosEntry.TABLE_NAME} ON ${ClienteEntry.TABLE_NAME}.${ClienteEntry.COLUMN_NAME_ID} = ${ClientesBloqueadosEntry.TABLE_NAME}.${ClientesBloqueadosEntry.COLUMN_NAME_CLIENTE_ID} ORDER BY ${ClienteEntry.COLUMN_NAME_NOME} ASC"

        val cursor = db.rawQuery(query, null)

        with(cursor) {
            while (moveToNext()) {
                val itemId = getLong(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ID))
                val nome = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_NOME))
                val endereco = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ENDERECO))
                val telefone = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_TELEFONE))
                val email = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_EMAIL))
                val cpf = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CPF))
                val cnpj = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CNPJ))
                val logradouro = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_LOGRADOURO))
                val numero = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_NUMERO))
                val complemento = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_COMPLEMENTO))
                val bairro = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_BAIRRO))
                val cidade = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CIDADE))
                val estado = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_ESTADO))
                val cep = getString(getColumnIndexOrThrow(ClienteEntry.COLUMN_NAME_CEP))

                blockedClientsList.add(Cliente(itemId, nome, endereco, telefone, email, cpf, cnpj, logradouro, numero, complemento, bairro, cidade, estado, cep))
            }
        }
        cursor.close()
        return blockedClientsList
    }

    fun exportDatabase(outputFile: File) {
        val dbFile = context.getDatabasePath(Constants.DATABASE_NAME)
        if (!dbFile.exists()) {
            throw IOException("Database file does not exist: ${dbFile.absolutePath}")
        }

        var source: FileChannel? = null
        var destination: FileChannel? = null
        try {
            source = FileInputStream(dbFile).channel
            destination = FileOutputStream(outputFile).channel
            destination.transferFrom(source, 0, source.size())
            Log.d("ClienteDbHelper", "Database exported successfully to: ${outputFile.absolutePath}")
        } finally {
            source?.close()
            destination?.close()
        }
    }

    fun importDatabase(inputFile: File) {
        val dbFile = context.getDatabasePath(Constants.DATABASE_NAME)
        if (dbFile.exists()) {
            dbFile.delete() // Delete existing database
        }

        var source: FileChannel? = null
        var destination: FileChannel? = null
        try {
            source = FileInputStream(inputFile).channel
            destination = FileOutputStream(dbFile).channel
            destination.transferFrom(source, 0, source.size())
            Log.d("ClienteDbHelper", "Database imported successfully from: ${inputFile.absolutePath}")
        } finally {
            source?.close()
            destination?.close()
        }
    }

    // Funções para resumo financeiro
    fun getMonthlyRevenue(): List<ResumoMensalItem> {
        val monthlyRevenueList = mutableListOf<ResumoMensalItem>()
        val db = this.readableDatabase

        val query = """
        SELECT
            strftime('%Y-%m', ${FaturaEntry.COLUMN_NAME_DATA}) AS month_year,
            SUM(${FaturaEntry.COLUMN_NAME_TOTAL}) AS total_revenue,
            COUNT(${FaturaEntry.COLUMN_NAME_ID}) AS total_invoices
        FROM
            ${FaturaEntry.TABLE_NAME}
        GROUP BY
            month_year
        ORDER BY
            month_year ASC
    """.trimIndent()

        val cursor = db.rawQuery(query, null)

        with(cursor) {
            while (moveToNext()) {
                val monthYear = getString(getColumnIndexOrThrow("month_year"))
                val totalRevenue = getDouble(getColumnIndexOrThrow("total_revenue"))
                val totalInvoices = getInt(getColumnIndexOrThrow("total_invoices"))
                monthlyRevenueList.add(ResumoMensalItem(monthYear, totalRevenue, totalInvoices))
            }
        }
        cursor.close()
        return monthlyRevenueList
    }

    fun getRevenueByClient(startDate: String? = null, endDate: String? = null): List<ResumoClienteItem> {
        val clientRevenueList = mutableListOf<ResumoClienteItem>()
        val db = this.readableDatabase

        val selectionArgs = mutableListOf<String>()
        var whereClause = ""

        if (startDate != null && endDate != null) {
            whereClause = "WHERE ${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_DATA} BETWEEN ? AND ?"
            selectionArgs.add(startDate)
            selectionArgs.add(endDate)
        }

        val query = """
        SELECT
            ${ClienteEntry.TABLE_NAME}.${ClienteEntry.COLUMN_NAME_ID} AS client_id,
            ${ClienteEntry.TABLE_NAME}.${ClienteEntry.COLUMN_NAME_NOME} AS client_name,
            SUM(${FaturaEntry.COLUMN_NAME_TOTAL}) AS total_revenue_client,
            COUNT(${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_ID}) AS total_invoices_client
        FROM
            ${FaturaEntry.TABLE_NAME}
        INNER JOIN
            ${ClienteEntry.TABLE_NAME} ON ${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_CLIENTE_ID} = ${ClienteEntry.TABLE_NAME}.${ClienteEntry.COLUMN_NAME_ID}
        $whereClause
        GROUP BY
            client_id, client_name
        ORDER BY
            total_revenue_client DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, selectionArgs.toTypedArray())

        with(cursor) {
            while (moveToNext()) {
                val clientId = getLong(getColumnIndexOrThrow("client_id"))
                val clientName = getString(getColumnIndexOrThrow("client_name"))
                val totalRevenue = getDouble(getColumnIndexOrThrow("total_revenue_client"))
                val totalInvoices = getInt(getColumnIndexOrThrow("total_invoices_client"))
                clientRevenueList.add(ResumoClienteItem(clientId, clientName, totalRevenue, totalInvoices))
            }
        }
        cursor.close()
        return clientRevenueList
    }

    fun getRevenueByArtigo(startDate: String? = null, endDate: String? = null): List<ResumoArtigoItem> {
        val articleRevenueList = mutableListOf<ResumoArtigoItem>()
        val db = this.readableDatabase

        val selectionArgs = mutableListOf<String>()
        var whereClause = ""

        if (startDate != null && endDate != null) {
            whereClause = "WHERE ${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_DATA} BETWEEN ? AND ?"
            selectionArgs.add(startDate)
            selectionArgs.add(endDate)
        }

        val query = """
        SELECT
            ${ArtigoEntry.TABLE_NAME}.${ArtigoEntry.COLUMN_NAME_ID} AS article_id,
            ${ArtigoEntry.TABLE_NAME}.${ArtigoEntry.COLUMN_NAME_NOME} AS article_name,
            SUM(${FaturaItemEntry.TABLE_NAME}.${FaturaItemEntry.COLUMN_NAME_QUANTIDADE} * ${FaturaItemEntry.TABLE_NAME}.${FaturaItemEntry.COLUMN_NAME_PRECO_UNITARIO}) AS total_revenue_article,
            SUM(${FaturaItemEntry.TABLE_NAME}.${FaturaItemEntry.COLUMN_NAME_QUANTIDADE}) AS total_sold_quantity
        FROM
            ${FaturaItemEntry.TABLE_NAME}
        INNER JOIN
            ${FaturaEntry.TABLE_NAME} ON ${FaturaItemEntry.TABLE_NAME}.${FaturaItemEntry.COLUMN_NAME_FATURA_ID} = ${FaturaEntry.TABLE_NAME}.${FaturaEntry.COLUMN_NAME_ID}
        INNER JOIN
            ${ArtigoEntry.TABLE_NAME} ON ${FaturaItemEntry.TABLE_NAME}.${FaturaItemEntry.COLUMN_NAME_ARTIGO_ID} = ${ArtigoEntry.TABLE_NAME}.${ArtigoEntry.COLUMN_NAME_ID}
        $whereClause
        GROUP BY
            article_id, article_name
        ORDER BY
            total_revenue_article DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, selectionArgs.toTypedArray())

        with(cursor) {
            while (moveToNext()) {
                val articleId = getLong(getColumnIndexOrThrow("article_id"))
                val articleName = getString(getColumnIndexOrThrow("article_name"))
                val totalRevenue = getDouble(getColumnIndexOrThrow("total_revenue_article"))
                val totalQuantity = getInt(getColumnIndexOrThrow("total_sold_quantity"))
                articleRevenueList.add(ResumoArtigoItem(articleId, articleName, totalRevenue, totalQuantity))
            }
        }
        cursor.close()
        return articleRevenueList
    }
}