package com.example.myapplication

import android.provider.BaseColumns

object FaturaLixeiraContract {
    object FaturaLixeiraEntry : BaseColumns {
        const val TABLE_NAME = "faturas_lixeira"
        const val COLUMN_NAME_FATURA_ID_ORIGINAL = "fatura_id_original"
        const val COLUMN_NAME_CLIENTE_ID = "cliente_id"
        const val COLUMN_NAME_DATA = "data"
        const val COLUMN_NAME_TOTAL = "total"
        const val COLUMN_NAME_FOI_ENVIADA = "foi_enviada"
        const val COLUMN_NAME_OBSERVACAO = "observacao"
        const val COLUMN_NAME_DESCONTO = "desconto"
        const val COLUMN_NAME_TAXA_ENTREGA = "taxa_entrega"
        const val COLUMN_NAME_MODO_SIMPLIFICADO = "modo_simplificado"
        const val COLUMN_NAME_DATA_EXCLUSAO = "data_exclusao" // Data em que foi para a lixeira

        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID_ORIGINAL INTEGER UNIQUE,
                $COLUMN_NAME_CLIENTE_ID INTEGER,
                $COLUMN_NAME_DATA TEXT,
                $COLUMN_NAME_TOTAL REAL,
                $COLUMN_NAME_FOI_ENVIADA INTEGER,
                $COLUMN_NAME_OBSERVACAO TEXT,
                $COLUMN_NAME_DESCONTO REAL,
                $COLUMN_NAME_TAXA_ENTREGA REAL,
                $COLUMN_NAME_MODO_SIMPLIFICADO INTEGER,
                $COLUMN_NAME_DATA_EXCLUSAO TEXT
            )
        """
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"

        // Tabela para itens de fatura na lixeira
        const val TABLE_NAME_ITENS = "fatura_itens_lixeira"
        const val SQL_CREATE_ITENS_ENTRIES = """
            CREATE TABLE $TABLE_NAME_ITENS (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID_ORIGINAL INTEGER,
                ${FaturaContract.FaturaItemEntry.COLUMN_NAME_ARTIGO_ID} INTEGER,
                ${FaturaContract.FaturaItemEntry.COLUMN_NAME_QUANTIDADE} INTEGER,
                ${FaturaContract.FaturaItemEntry.COLUMN_NAME_PRECO_UNITARIO} REAL,
                ${FaturaContract.FaturaItemEntry.COLUMN_NAME_NOME_ARTIGO} TEXT,
                ${FaturaContract.FaturaItemEntry.COLUMN_NAME_NUMERO_SERIE} TEXT,
                ${FaturaContract.FaturaItemEntry.COLUMN_NAME_DESCRICAO} TEXT,
                FOREIGN KEY($COLUMN_NAME_FATURA_ID_ORIGINAL) REFERENCES $TABLE_NAME(${BaseColumns._ID}) ON DELETE CASCADE
            )
        """
        const val SQL_DELETE_ITENS_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME_ITENS"


        // Tabela para fotos de fatura na lixeira
        const val TABLE_NAME_FOTOS = "fatura_fotos_lixeira"
        const val SQL_CREATE_FOTOS_ENTRIES = """
            CREATE TABLE $TABLE_NAME_FOTOS (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID_ORIGINAL INTEGER,
                ${FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH} TEXT,
                FOREIGN KEY($COLUMN_NAME_FATURA_ID_ORIGINAL) REFERENCES $TABLE_NAME(${BaseColumns._ID}) ON DELETE CASCADE
            )
        """
        const val SQL_DELETE_FOTOS_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME_FOTOS"
    }
}