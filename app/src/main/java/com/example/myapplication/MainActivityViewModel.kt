package com.example.myapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = ClienteDbHelper(application)

    private val _faturas = MutableLiveData<List<FaturaResumidaItem>>()
    val faturas: LiveData<List<FaturaResumidaItem>> = _faturas

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadFaturas()
    }

    fun loadFaturas(query: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val loadedFaturas = withContext(Dispatchers.IO) {
                    dbHelper.getAllFaturasResumidas(query)
                }
                // Garante que a atualização da UI (LiveData) ocorre na thread principal
                withContext(Dispatchers.Main) {
                    _faturas.value = loadedFaturas
                }
            } catch (e: Exception) {
                Log.e("MainActivityViewModel", "Erro ao carregar faturas: ${e.message}", e)
                // Garante que a atualização da UI (LiveData) ocorre na thread principal
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Erro ao carregar faturas: ${e.message}"
                }
            } finally {
                // Garante que a atualização da UI (LiveData) ocorre na thread principal
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun deleteFatura(faturaId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    dbHelper.deleteFatura(faturaId)
                }
                if (success) {
                    withContext(Dispatchers.Main) {
                        loadFaturas() // Recarrega a lista após a exclusão
                        // Você pode adicionar um LiveData para mensagens de sucesso também
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Erro ao excluir fatura."
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivityViewModel", "Erro ao excluir fatura: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Erro ao excluir fatura: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}