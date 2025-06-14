package com.example.myapplication

import android.provider.BaseColumns

object ArtigoContract {
    object ArtigoEntry : BaseColumns {
        const val TABLE_NAME = "artigos"
        const val COLUMN_NAME_NOME = "nome"
        const val COLUMN_NAME_PRECO_UNITARIO = "preco_unitario" // Renomeado
        const val COLUMN_NAME_QUANTIDADE = "quantidade"
        const val COLUMN_NAME_DESCRICAO = "descricao"
        const val COLUMN_NAME_NUMERO_SERIE = "numero_serie" // Renomeado

        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_NOME TEXT,
                $COLUMN_NAME_PRECO_UNITARIO REAL,
                $COLUMN_NAME_QUANTIDADE INTEGER,
                $COLUMN_NAME_DESCRICAO TEXT,
                $COLUMN_NAME_NUMERO_SERIE TEXT
            )
        """

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}