package com.example.myapplication

import android.app.Activity
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
import database.dao.ClienteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CriarNovoClienteActivity : AppCompatActivity() {

    private lateinit var nomeEditText: EditText
    private lateinit var telefoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var informacoesAdicionaisEditText: EditText
    private lateinit var cpfEditText: EditText
    private lateinit var cnpjEditText: EditText
    private lateinit var logradouroEditText: EditText
    private lateinit var numeroEditText: EditText
    private lateinit var complementoEditText: EditText
    private lateinit var bairroEditText: EditText
    private lateinit var cidadeEditText: EditText
    private lateinit var estadoAutoComplete: AutoCompleteTextView
    private lateinit var cepEditText: EditText
    private lateinit var numeroSerieEditText: EditText
    private lateinit var salvarButton: Button
    private lateinit var voltarButton: TextView

    // DAO do Room
    private lateinit var clienteDao: ClienteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_novo_cliente)

        // Inicializa o DAO do Room
        val application = application as MyApplication
        clienteDao = application.database.clienteDao()

        initComponents()
        setupListeners()
    }

    private fun initComponents() {
        nomeEditText = findViewById(R.id.nomeEditText)
        telefoneEditText = findViewById(R.id.telefoneEditText)
        emailEditText = findViewById(R.id.emailEditText)
        informacoesAdicionaisEditText = findViewById(R.id.informacoesAdicionaisEditText)
        cpfEditText = findViewById(R.id.cpfEditText)
        cnpjEditText = findViewById(R.id.cnpjEditText)
        logradouroEditText = findViewById(R.id.logradouroEditText)
        numeroEditText = findViewById(R.id.numeroEditText)
        complementoEditText = findViewById(R.id.complementoEditText)
        bairroEditText = findViewById(R.id.bairroEditText)
        cidadeEditText = findViewById(R.id.cidadeEditText)
        estadoAutoComplete = findViewById(R.id.estadoAutoComplete)
        cepEditText = findViewById(R.id.cepEditText)
        numeroSerieEditText = findViewById(R.id.numeroSerieEditText)
        salvarButton = findViewById(R.id.salvarButton)
        voltarButton = findViewById(R.id.voltarButton)

        // Configurar AutoCompleteTextView para o Estado
        val estados = resources.getStringArray(R.array.estados_brasil)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estados)
        estadoAutoComplete.setAdapter(adapter)

        // Máscaras para CPF, CNPJ e Telefone
        cpfEditText.addTextChangedListener(Mask.insert("###.###.###-##", cpfEditText))
        cnpjEditText.addTextChangedListener(Mask.insert("##.###.###/####-##", cnpjEditText))
        telefoneEditText.addTextChangedListener(Mask.insert("(##)#####-####", telefoneEditText))
        cepEditText.addTextChangedListener(Mask.insert("#####-###", cepEditText))
    }

    private fun setupListeners() {
        salvarButton.setOnClickListener {
            salvarNovoCliente()
        }

        voltarButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun salvarNovoCliente() {
        val nome = nomeEditText.text.toString().trim()
        val telefone = telefoneEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val informacoesAdicionais = informacoesAdicionaisEditText.text.toString().trim()
        val cpf = cpfEditText.text.toString().trim()
        val cnpj = cnpjEditText.text.toString().trim()
        val logradouro = logradouroEditText.text.toString().trim()
        val numero = numeroEditText.text.toString().trim()
        val complemento = complementoEditText.text.toString().trim()
        val bairro = bairroEditText.text.toString().trim()
        val cidade = cidadeEditText.text.toString().trim()
        val estado = estadoAutoComplete.text.toString().trim()
        val cep = cepEditText.text.toString().trim()
        val numeroSerial = numeroSerieEditText.text.toString().trim()

        if (nome.isEmpty()) {
            showToast("O nome do cliente é obrigatório.")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            var existingCliente: Cliente? = null
            if (cpf.isNotEmpty() || cnpj.isNotEmpty()) {
                existingCliente = clienteDao.getClienteByCpfCnpj(cpf, cnpj)
            } else {
                existingCliente = clienteDao.getClienteByNome(nome)
            }

            if (existingCliente != null) {
                withContext(Dispatchers.Main) {
                    showToast("Já existe um cliente com este nome ou CPF/CNPJ.")
                }
                return@launch
            }

            val cliente = Cliente(
                nome = nome,
                telefone = telefone.takeIf { it.isNotEmpty() },
                email = email.takeIf { it.isNotEmpty() },
                informacoesAdicionais = informacoesAdicionais.takeIf { it.isNotEmpty() },
                cpf = cpf.takeIf { it.isNotEmpty() },
                cnpj = cnpj.takeIf { it.isNotEmpty() },
                logradouro = logradouro.takeIf { it.isNotEmpty() },
                numero = numero.takeIf { it.isNotEmpty() },
                complemento = complemento.takeIf { it.isNotEmpty() },
                bairro = bairro.takeIf { it.isNotEmpty() },
                municipio = cidade.takeIf { it.isNotEmpty() },
                uf = estado.takeIf { it.isNotEmpty() },
                cep = cep.takeIf { it.isNotEmpty() },
                numeroSerial = numeroSerial.takeIf { it.isNotEmpty() }
            )

            try {
                val newId = clienteDao.insert(cliente)
                if (newId != -1L) {
                    withContext(Dispatchers.Main) {
                        showToast("Cliente adicionado com sucesso!")
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Erro ao adicionar cliente.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CriarNovoClienteActivity", "Erro ao adicionar cliente: ${e.message}", e)
                    showToast("Erro ao adicionar cliente: ${e.message}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
