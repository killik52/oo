package com.example.myapplication

import android.provider.BaseColumns

object FaturaContract {
    object FaturaEntry : BaseColumns {
        const val TABLE_NAME = "faturas"
        const val COLUMN_NAME_CLIENTE_ID = "cliente_id" // Refere-se ao ID do cliente
        const val COLUMN_NAME_DATA = "data"
        const val COLUMN_NAME_TOTAL = "total" // Total geral da fatura
        const val COLUMN_NAME_FOI_ENVIADA = "foi_enviada" // 0 ou 1
        const val COLUMN_NAME_OBSERVACAO = "observacao"
        const val COLUMN_NAME_DESCONTO = "desconto" // Valor do desconto
        const val COLUMN_NAME_TAXA_ENTREGA = "taxa_entrega"
        const val COLUMN_NAME_MODO_SIMPLIFICADO = "modo_simplificado" // 0 ou 1

        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_CLIENTE_ID INTEGER,
                $COLUMN_NAME_DATA TEXT,
                $COLUMN_NAME_TOTAL REAL,
                $COLUMN_NAME_FOI_ENVIADA INTEGER DEFAULT 0,
                $COLUMN_NAME_OBSERVACAO TEXT,
                $COLUMN_NAME_DESCONTO REAL DEFAULT 0.0,
                $COLUMN_NAME_TAXA_ENTREGA REAL DEFAULT 0.0,
                $COLUMN_NAME_MODO_SIMPLIFICADO INTEGER DEFAULT 0,
                FOREIGN KEY($COLUMN_NAME_CLIENTE_ID) REFERENCES ${ClienteContract.ClienteEntry.TABLE_NAME}(${BaseColumns._ID})
            )
        """

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    object FaturaItemEntry : BaseColumns {
        const val TABLE_NAME = "fatura_itens"
        const val COLUMN_NAME_FATURA_ID = "fatura_id"
        const val COLUMN_NAME_ARTIGO_ID = "artigo_id" // Pode ser o ID do artigo ou -1 para custom
        const val COLUMN_NAME_QUANTIDADE = "quantidade"
        const val COLUMN_NAME_PRECO_UNITARIO = "preco_unitario" // Usar preco_unitario
        const val COLUMN_NAME_NOME_ARTIGO = "nome_artigo" // Para itens que não são artigos do DB
        const val COLUMN_NAME_NUMERO_SERIE = "numero_serie" // Para itens que não são artigos do DB
        const val COLUMN_NAME_DESCRICAO = "descricao" // Para itens que não são artigos do DB


        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID INTEGER,
                $COLUMN_NAME_ARTIGO_ID INTEGER,
                $COLUMN_NAME_QUANTIDADE INTEGER,
                $COLUMN_NAME_PRECO_UNITARIO REAL,
                $COLUMN_NAME_NOME_ARTIGO TEXT,
                $COLUMN_NAME_NUMERO_SERIE TEXT,
                $COLUMN_NAME_DESCRICAO TEXT,
                FOREIGN KEY($COLUMN_NAME_FATURA_ID) REFERENCES ${FaturaEntry.TABLE_NAME}(${BaseColumns._ID}) ON DELETE CASCADE
            )
        """
        // FOREIGN KEY($COLUMN_NAME_ARTIGO_ID) REFERENCES ${ArtigoContract.ArtigoEntry.TABLE_NAME}(${BaseColumns._ID}) // Removido ON DELETE CASCADE para artigo_id
        // Remover FOREIGN KEY para cliente_id, pois FaturaEntry já tem
        // const val COLUMN_NAME_CLIENTE_ID = "cliente_id" // Removida, já em FaturaEntry

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    // FaturaNotaEntry parece não ser usada no ClienteDbHelper, removendo para simplificar
    // object FaturaNotaEntry : BaseColumns { /* ... */ }

    object FaturaFotoEntry : BaseColumns {
        const val TABLE_NAME = "fatura_fotos"
        const val COLUMN_NAME_FATURA_ID = "fatura_id"
        const val COLUMN_NAME_PHOTO_PATH = "photo_path"

        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID INTEGER,
                $COLUMN_NAME_PHOTO_PATH TEXT,
                FOREIGN KEY($COLUMN_NAME_FATURA_ID) REFERENCES ${FaturaEntry.TABLE_NAME}(${BaseColumns._ID}) ON DELETE CASCADE
            )
        """

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}