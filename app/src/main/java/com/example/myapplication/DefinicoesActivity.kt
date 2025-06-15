package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.InformacoesEmpresaDao
import com.example.myapplication.database.dao.InstrucoesPagamentoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefinicoesActivity : AppCompatActivity() {

    private lateinit var voltarButton: TextView
    private lateinit var infoEmpresaButton: Button
    private lateinit var instrucoesPagamentoButton: Button
    private lateinit var recentesArtigosSwitch: Switch
    private lateinit var recentesClientesSwitch: Switch
    private lateinit var gerarPDFSwitch: Switch
    private lateinit var informacoesEmpresaDao: InformacoesEmpresaDao
    private lateinit var instrucoesPagamentoDao: InstrucoesPagamentoDao

    // SharedPreferences para o estado dos switches
    private val PREFS_NAME = "AppPrefs"
    private val KEY_RECENTES_ARTIGOS = "recentes_artigos_ativado"
    private val KEY_RECENTES_CLIENTES = "recentes_clientes_ativado"
    private val KEY_GERAR_PDF = "gerar_pdf_ativado"
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_definicoes)

        // Inicializa os DAOs do Room
        val application = application as MyApplication
        informacoesEmpresaDao = application.database.informacoesEmpresaDao()
        instrucoesPagamentoDao = application.database.instrucoesPagamentoDao()

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        initComponents()
        setupListeners()
        loadSwitchStates()
    }

    private fun initComponents() {
        voltarButton = findViewById(R.id.voltarButton)
        infoEmpresaButton = findViewById(R.id.infoEmpresaButton)
        instrucoesPagamentoButton = findViewById(R.id.instrucoesPagamentoButton)
        recentesArtigosSwitch = findViewById(R.id.recentesArtigosSwitch)
        recentesClientesSwitch = findViewById(R.id.recentesClientesSwitch)
        gerarPDFSwitch = findViewById(R.id.gerarPDFSwitch)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        infoEmpresaButton.setOnClickListener {
            val intent = Intent(this, InformacoesEmpresaActivity::class.java)
            startActivity(intent)
        }

        instrucoesPagamentoButton.setOnClickListener {
            val intent = Intent(this, InstrucoesPagamentoActivity::class.java)
            startActivity(intent)
        }

        recentesArtigosSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_RECENTES_ARTIGOS, isChecked).apply()
            Constants.RECENTES_ARTIGOS_ATIVADO = isChecked
            showToast("Artigos Recentes: ${if (isChecked) "Ativado" else "Desativado"}")
        }

        recentesClientesSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_RECENTES_CLIENTES, isChecked).apply()
            Constants.RECENTES_CLIENTES_ATIVADO = isChecked
            showToast("Clientes Recentes: ${if (isChecked) "Ativado" else "Desativado"}")
        }

        gerarPDFSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_GERAR_PDF, isChecked).apply()
            Constants.GERAR_PDF_ATIVADO = isChecked
            showToast("Gerar PDF: ${if (isChecked) "Ativado" else "Desativado"}")
        }
    }

    private fun loadSwitchStates() {
        recentesArtigosSwitch.isChecked = sharedPreferences.getBoolean(KEY_RECENTES_ARTIGOS, true)
        recentesClientesSwitch.isChecked = sharedPreferences.getBoolean(KEY_RECENTES_CLIENTES, true)
        gerarPDFSwitch.isChecked = sharedPreferences.getBoolean(KEY_GERAR_PDF, false)

        Constants.RECENTES_ARTIGOS_ATIVADO = recentesArtigosSwitch.isChecked
        Constants.RECENTES_CLIENTES_ATIVADO = recentesClientesSwitch.isChecked
        Constants.GERAR_PDF_ATIVADO = gerarPDFSwitch.isChecked
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
