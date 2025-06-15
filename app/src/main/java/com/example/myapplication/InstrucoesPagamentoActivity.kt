package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.InstrucoesPagamentoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstrucoesPagamentoActivity : AppCompatActivity() {

    private lateinit var voltarButton: TextView
    private lateinit var pixEditText: EditText
    private lateinit var bancoEditText: EditText
    private lateinit var agenciaEditText: EditText
    private lateinit var contaEditText: EditText
    private lateinit var outrasInstrucoesEditText: EditText
    private lateinit var salvarButton: Button

    // DAO do Room
    private lateinit var instrucoesPagamentoDao: InstrucoesPagamentoDao

    private var instrucoesPagamentoEntity: InstrucoesPagamentoEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instrucoes_pagamento)

        // Inicializa o DAO do Room
        val application = application as MyApplication
        instrucoesPagamentoDao = application.database.instrucoesPagamentoDao()

        initComponents()
        setupListeners()
        loadInstrucoesPagamento()
    }

    private fun initComponents() {
        voltarButton = findViewById(R.id.voltarButton)
        pixEditText = findViewById(R.id.pixEditText)
        bancoEditText = findViewById(R.id.bancoEditText)
        agenciaEditText = findViewById(R.id.agenciaEditText)
        contaEditText = findViewById(R.id.contaEditText)
        outrasInstrucoesEditText = findViewById(R.id.outrasInstrucoesEditText)
        salvarButton = findViewById(R.id.salvarButton)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        salvarButton.setOnClickListener {
            salvarInstrucoesPagamento()
        }
    }

    private fun loadInstrucoesPagamento() {
        lifecycleScope.launch(Dispatchers.IO) {
            instrucoesPagamentoDao.getInstrucoesPagamento().collectLatest { instrucoes ->
                withContext(Dispatchers.Main) {
                    instrucoesPagamentoEntity = instrucoes
                    instrucoes?.let {
                        pixEditText.setText(it.pix)
                        bancoEditText.setText(it.banco)
                        agenciaEditText.setText(it.agencia)
                        contaEditText.setText(it.conta)
                        outrasInstrucoesEditText.setText(it.outrasInstrucoes)
                    }
                    Log.d("InstrucoesPagamentoActivity", "Instruções de pagamento carregadas.")
                }
            }
        }
    }

    private fun salvarInstrucoesPagamento() {
        val pix = pixEditText.text.toString().trim()
        val banco = bancoEditText.text.toString().trim()
        val agencia = agenciaEditText.text.toString().trim()
        val conta = contaEditText.text.toString().trim()
        val outrasInstrucoes = outrasInstrucoesEditText.text.toString().trim()

        lifecycleScope.launch(Dispatchers.IO) {
            val newInstrucoes = InstrucoesPagamentoEntity(
                id = instrucoesPagamentoEntity?.id ?: 0, // Se existir, usa o ID existente, senão 0 para autoGenerate
                pix = pix.takeIf { it.isNotEmpty() },
                banco = banco.takeIf { it.isNotEmpty() },
                agencia = agencia.takeIf { it.isNotEmpty() },
                conta = conta.takeIf { it.isNotEmpty() },
                outrasInstrucoes = outrasInstrucoes.takeIf { it.isNotEmpty() }
            )

            try {
                if (instrucoesPagamentoEntity == null) {
                    // Inserir um novo registro se não existir nenhum
                    instrucoesPagamentoDao.insert(newInstrucoes)
                    withContext(Dispatchers.Main) {
                        showToast("Instruções de pagamento salvas com sucesso!")
                    }
                } else {
                    // Atualizar o registro existente
                    instrucoesPagamentoDao.update(newInstrucoes)
                    withContext(Dispatchers.Main) {
                        showToast("Instruções de pagamento atualizadas com sucesso!")
                    }
                }
                loadInstrucoesPagamento() // Recarrega para atualizar instrucoesPagamentoEntity
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("InstrucoesPagamentoActivity", "Erro ao salvar instruções de pagamento: ${e.message}", e)
                    showToast("Erro ao salvar instruções de pagamento: ${e.message}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
