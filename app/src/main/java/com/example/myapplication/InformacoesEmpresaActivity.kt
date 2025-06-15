package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.InformacoesEmpresaDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InformacoesEmpresaActivity : AppCompatActivity() {

    private lateinit var voltarButton: TextView
    private lateinit var nomeEmpresaEditText: EditText
    private lateinit var emailEmpresaEditText: EditText
    private lateinit var telefoneEmpresaEditText: EditText
    private lateinit var informacoesAdicionaisEmpresaEditText: EditText
    private lateinit var cnpjEmpresaEditText: EditText
    private lateinit var cepEmpresaEditText: EditText
    private lateinit var estadoEmpresaAutoComplete: AutoCompleteTextView
    private lateinit var paisEmpresaEditText: EditText
    private lateinit var cidadeEmpresaEditText: EditText
    private lateinit var salvarButton: Button

    // DAO do Room
    private lateinit var informacoesEmpresaDao: InformacoesEmpresaDao

    private var informacoesEmpresaEntity: InformacoesEmpresaEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_informacoes_empresa)

        // Inicializa o DAO do Room
        val application = application as MyApplication
        informacoesEmpresaDao = application.database.informacoesEmpresaDao()

        initComponents()
        setupListeners()
        loadInformacoesEmpresa()
    }

    private fun initComponents() {
        voltarButton = findViewById(R.id.voltarButton)
        nomeEmpresaEditText = findViewById(R.id.nomeEmpresaEditText)
        emailEmpresaEditText = findViewById(R.id.emailEmpresaEditText)
        telefoneEmpresaEditText = findViewById(R.id.telefoneEmpresaEditText)
        informacoesAdicionaisEmpresaEditText = findViewById(R.id.informacoesAdicionaisEmpresaEditText)
        cnpjEmpresaEditText = findViewById(R.id.cnpjEmpresaEditText)
        cepEmpresaEditText = findViewById(R.id.cepEmpresaEditText)
        estadoEmpresaAutoComplete = findViewById(R.id.estadoEmpresaAutoComplete)
        paisEmpresaEditText = findViewById(R.id.paisEmpresaEditText)
        cidadeEmpresaEditText = findViewById(R.id.cidadeEmpresaEditText)
        salvarButton = findViewById(R.id.salvarButton)

        val estados = resources.getStringArray(R.array.estados_brasil)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estados)
        estadoEmpresaAutoComplete.setAdapter(adapter)

        cnpjEmpresaEditText.addTextChangedListener(Mask.insert("##.###.###/####-##", cnpjEmpresaEditText))
        telefoneEmpresaEditText.addTextChangedListener(Mask.insert("(##)#####-####", telefoneEmpresaEditText))
        cepEmpresaEditText.addTextChangedListener(Mask.insert("#####-###", cepEmpresaEditText))
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        salvarButton.setOnClickListener {
            salvarInformacoesEmpresa()
        }
    }

    private fun loadInformacoesEmpresa() {
        lifecycleScope.launch(Dispatchers.IO) {
            informacoesEmpresaDao.getInformacoesEmpresa().collectLatest { info ->
                withContext(Dispatchers.Main) {
                    informacoesEmpresaEntity = info
                    info?.let {
                        nomeEmpresaEditText.setText(it.nomeEmpresa)
                        emailEmpresaEditText.setText(it.email)
                        telefoneEmpresaEditText.setText(it.telefone)
                        informacoesAdicionaisEmpresaEditText.setText(it.informacoesAdicionais)
                        cnpjEmpresaEditText.setText(it.cnpj)
                        cepEmpresaEditText.setText(it.cep)
                        estadoEmpresaAutoComplete.setText(it.estado)
                        paisEmpresaEditText.setText(it.pais)
                        cidadeEmpresaEditText.setText(it.cidade)
                    }
                    Log.d("InformacoesEmpresaActivity", "Informações da empresa carregadas.")
                }
            }
        }
    }

    private fun salvarInformacoesEmpresa() {
        val nomeEmpresa = nomeEmpresaEditText.text.toString().trim()
        val email = emailEmpresaEditText.text.toString().trim()
        val telefone = telefoneEmpresaEditText.text.toString().trim()
        val informacoesAdicionais = informacoesAdicionaisEmpresaEditText.text.toString().trim()
        val cnpj = cnpjEmpresaEditText.text.toString().trim()
        val cep = cepEmpresaEditText.text.toString().trim()
        val estado = estadoEmpresaAutoComplete.text.toString().trim()
        val pais = paisEmpresaEditText.text.toString().trim()
        val cidade = cidadeEmpresaEditText.text.toString().trim()

        lifecycleScope.launch(Dispatchers.IO) {
            val newInfo = InformacoesEmpresaEntity(
                id = informacoesEmpresaEntity?.id ?: 0, // Se existir, usa o ID existente, senão 0 para autoGenerate
                nomeEmpresa = nomeEmpresa.takeIf { it.isNotEmpty() },
                email = email.takeIf { it.isNotEmpty() },
                telefone = telefone.takeIf { it.isNotEmpty() },
                informacoesAdicionais = informacoesAdicionais.takeIf { it.isNotEmpty() },
                cnpj = cnpj.takeIf { it.isNotEmpty() },
                cep = cep.takeIf { it.isNotEmpty() },
                estado = estado.takeIf { it.isNotEmpty() },
                pais = pais.takeIf { it.isNotEmpty() },
                cidade = cidade.takeIf { it.isNotEmpty() }
            )

            try {
                if (informacoesEmpresaEntity == null) {
                    // Inserir um novo registro se não existir nenhum
                    informacoesEmpresaDao.insert(newInfo)
                    withContext(Dispatchers.Main) {
                        showToast("Informações da empresa salvas com sucesso!")
                    }
                } else {
                    // Atualizar o registro existente
                    informacoesEmpresaDao.update(newInfo)
                    withContext(Dispatchers.Main) {
                        showToast("Informações da empresa atualizadas com sucesso!")
                    }
                }
                loadInformacoesEmpresa() // Recarrega para atualizar informacoesEmpresaEntity
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("InformacoesEmpresaActivity", "Erro ao salvar informações da empresa: ${e.message}", e)
                    showToast("Erro ao salvar informações da empresa: ${e.message}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
