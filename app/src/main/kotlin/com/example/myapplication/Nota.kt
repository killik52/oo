package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Nota(
    val id: Long,
    val conteudo: String // Nome alinhado com ClienteDbHelper
) : Parcelable