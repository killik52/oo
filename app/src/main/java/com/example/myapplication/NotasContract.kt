package com.example.myapplication

import android.provider.BaseColumns

object NotasContract {
    object NotasEntry : BaseColumns { // Renomeado para NotasEntry para consistência
        const val TABLE_NAME = "notas"
        const val COLUMN_NAME_CONTEUDO = "conteudo" // Renomeado para 'conteudo'
        // Removidas: TITULO e DATA se não forem usadas.
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${NotasEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                "${NotasEntry.COLUMN_NAME_CONTEUDO} TEXT)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${NotasEntry.TABLE_NAME}"
}