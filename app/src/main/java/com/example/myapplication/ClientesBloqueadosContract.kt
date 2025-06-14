package com.example.myapplication

import android.provider.BaseColumns

object ClientesBloqueadosContract {
    object ClientesBloqueadosEntry : BaseColumns {
        const val TABLE_NAME = "clientes_bloqueados"
        const val COLUMN_NAME_CLIENTE_ID = "cliente_id" // Apenas o ID do cliente bloqueado

        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_CLIENTE_ID INTEGER UNIQUE,
                FOREIGN KEY($COLUMN_NAME_CLIENTE_ID) REFERENCES ${ClienteContract.ClienteEntry.TABLE_NAME}(${BaseColumns._ID}) ON DELETE CASCADE
            )
        """
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}