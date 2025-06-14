package com.example.myapplication

import android.provider.BaseColumns

object ClienteContract {
    object ClienteEntry : BaseColumns {
        const val TABLE_NAME = "clientes"
        const val COLUMN_NAME_NOME = "nome"
        const val COLUMN_NAME_EMAIL = "email"
        const val COLUMN_NAME_TELEFONE = "telefone"
        // Removida: const val COLUMN_NAME_INFORMACOES_ADICIONAIS = "informacoes_adicionais"
        const val COLUMN_NAME_CPF = "cpf"
        const val COLUMN_NAME_CNPJ = "cnpj"
        const val COLUMN_NAME_LOGRADOURO = "logradouro"
        const val COLUMN_NAME_NUMERO = "numero"
        const val COLUMN_NAME_COMPLEMENTO = "complemento"
        const val COLUMN_NAME_BAIRRO = "bairro"
        const val COLUMN_NAME_CIDADE = "cidade" // Renomeado de MUNICIPIO
        const val COLUMN_NAME_ESTADO = "estado" // Renomeado de UF
        const val COLUMN_NAME_CEP = "cep"
        // Removida: const val COLUMN_NAME_NUMERO_SERIAL = "numero_serial"
        // Adicionada para refletir o campo "endereco" do modelo que pode ser uma junção
        const val COLUMN_NAME_ENDERECO = "endereco_completo"


        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_NOME TEXT,
                $COLUMN_NAME_EMAIL TEXT,
                $COLUMN_NAME_TELEFONE TEXT,
                $COLUMN_NAME_CPF TEXT,
                $COLUMN_NAME_CNPJ TEXT,
                $COLUMN_NAME_LOGRADOURO TEXT,
                $COLUMN_NAME_NUMERO TEXT,
                $COLUMN_NAME_COMPLEMENTO TEXT,
                $COLUMN_NAME_BAIRRO TEXT,
                $COLUMN_NAME_CIDADE TEXT,
                $COLUMN_NAME_ESTADO TEXT,
                $COLUMN_NAME_CEP TEXT,
                $COLUMN_NAME_ENDERECO TEXT
            )
        """
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}