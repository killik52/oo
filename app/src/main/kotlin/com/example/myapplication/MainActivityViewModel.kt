package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.application.MyApplication
import database.dao.FaturaDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val _faturas = MutableLiveData<List<FaturaResumidaItem>>()
    val faturas: LiveData<List<FaturaResumidaItem>> = _faturas

    private val faturaDao: FaturaDao = (application as MyApplication).database.faturaDao()

    init {
        carregarFaturas()
    }

    fun carregarFaturas() {
        viewModelScope.launch {
            faturaDao.getAllFaturas().collectLatest { faturaList ->
                val resumoFaturas = faturaList.map { fatura ->
                    // Converta a data para o formato "dd/MM/yyyy"
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val formattedData = try {
                        val date = inputFormat.parse(fatura.data)
                        outputFormat.format(date!!)
                    } catch (e: Exception) {
                        fatura.data // Em caso de erro, mantém a data original
                    }

                    FaturaResumidaItem(
                        id = fatura.id,
                        numeroFatura = fatura.numeroFatura ?: "N/A",
                        clienteNome = fatura.clienteNome,
                        artigosSerial = emptyList(), // Isso precisaria ser carregado separadamente se necessário
                        saldoDevedor = fatura.saldoDevedor,
                        data = formattedData, // Data formatada
                        foiEnviada = fatura.foiEnviada
                    )
                }
                withContext(Dispatchers.Main) {
                    _faturas.value = resumoFaturas
                }
            }
        }
    }
}
