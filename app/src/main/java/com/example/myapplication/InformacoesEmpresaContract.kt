package com.example.myapplication

import android.provider.BaseColumns

object InformacoesEmpresaContract {
    object InformacoesEmpresaEntry : BaseColumns {
        const val TABLE_NAME = "informacoes_empresa"
        const val COLUMN_NAME_NOME = "nome" // Renomeado de NOME_EMPRESA
        const val COLUMN_NAME_ENDERECO = "endereco"
        const val COLUMN_NAME_TELEFONE = "telefone"
        const val COLUMN_NAME_EMAIL = "email"
        const val COLUMN_NAME_CNPJ = "cnpj" // Renomeado de CNPJ_CPF
        // COLUMN_NAME_LOGO_PATH não deve ser aqui se for SharedPreferences
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${InformacoesEmpresaEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_NOME} TEXT," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_ENDERECO} TEXT," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_TELEFONE} TEXT," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_EMAIL} TEXT," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_CNPJ} TEXT)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${InformacoesEmpresaEntry.TABLE_NAME}"
}