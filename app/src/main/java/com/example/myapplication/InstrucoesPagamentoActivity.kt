package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityInstrucoesPagamentoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstrucoesPagamentoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstrucoesPagamentoBinding
    private lateinit var dbHelper: ClienteDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstrucoesPagamentoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.payment_instructions)

        loadInstrucoesPagamento()
        setupListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.buttonGuardar.setOnClickListener { // Guardar text
            saveInstrucoesPagamento()
        }
    }

    private fun loadInstrucoesPagamento() {
        lifecycleScope.launch(Dispatchers.IO) {
            val instrucoes = dbHelper.getInstrucoesPagamento()
            withContext(Dispatchers.Main) {
                binding.editTextInstrucoesPagamento.setText(instrucoes)
            }
        }
    }

    private fun saveInstrucoesPagamento() {
        val instrucoes = binding.editTextInstrucoesPagamento.text.toString().trim()

        lifecycleScope.launch(Dispatchers.IO) {
            val success = dbHelper.saveInstrucoesPagamento(instrucoes)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@InstrucoesPagamentoActivity, getString(R.string.payment_instructions_saved_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@InstrucoesPagamentoActivity, getString(R.string.payment_instructions_saved_error), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}