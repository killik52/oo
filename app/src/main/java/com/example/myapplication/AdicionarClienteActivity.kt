package com.example.myapplication

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.application.MyApplication // Importe a classe da sua aplicação
import com.example.myapplication.database.dao.ClienteDao // Importe o DAO do Cliente
import com.example.myapplication.database.dao.ClienteBloqueadoDao // Importe o DAO do ClienteBloqueado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdicionarClienteActivity : AppCompatActivity() {

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
    private lateinit var bloquearButton: Button
    private lateinit var voltarButton: Button

    // DAOs do Room
    private lateinit var clienteDao: ClienteDao
    private lateinit var clienteBloqueadoDao: ClienteBloqueadoDao

    private var clienteId: Long = -1 // -1 para novo cliente, ID do cliente para edição

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_cliente)

        // Inicializa os DAOs do Room
        val application = application as MyApplication
        clienteDao = application.database.clienteDao()
        clienteBloqueadoDao = application.database.clienteBloqueadoDao()

        initComponents()
        setupListeners()

        clienteId = intent.getLongExtra("CLIENTE_ID", -1)
        if (clienteId != -1L) {
            loadClienteData(clienteId)
        }
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
        bloquearButton = findViewById(R.id.bloquearButton)
        voltarButton = findViewById(R.id.voltarButton)

        // Configurar AutoCompleteTextView para o Estado
        val estados = resources.getStringArray(R.array.estados_brasil)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estados)
        estadoAutoComplete.setAdapter(adapter)

        // Máscaras para CPF e CNPJ
        cpfEditText.addTextChangedListener(Mask.insert("###.###.###-##", cpfEditText))
        cnpjEditText.addTextChangedListener(Mask.insert("##.###.###/####-##", cnpjEditText))
        telefoneEditText.addTextChangedListener(Mask.insert("(##)#####-####", telefoneEditText))
        cepEditText.addTextChangedListener(Mask.insert("#####-###", cepEditText))
    }

    private fun setupListeners() {
        salvarButton.setOnClickListener {
            salvarOuAtualizarCliente()
        }

        bloquearButton.setOnClickListener {
            bloquearCliente()
        }

        voltarButton.setOnClickListener {
            onBackPressed()
        }

        cpfEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length ?: 0 > 0) {
                    cnpjEditText.isEnabled = false
                    cnpjEditText.setText("")
                } else {
                    cnpjEditText.isEnabled = true
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cnpjEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length ?: 0 > 0) {
                    cpfEditText.isEnabled = false
                    cpfEditText.setText("")
                } else {
                    cpfEditText.isEnabled = true
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadClienteData(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val cliente = clienteDao.getClienteById(id)
            withContext(Dispatchers.Main) {
                if (cliente != null) {
                    nomeEditText.setText(cliente.nome)
                    telefoneEditText.setText(cliente.telefone)
                    emailEditText.setText(cliente.email)
                    informacoesAdicionaisEditText.setText(cliente.informacoesAdicionais)
                    cpfEditText.setText(cliente.cpf)
                    cnpjEditText.setText(cliente.cnpj)
                    logradouroEditText.setText(cliente.logradouro)
                    numeroEditText.setText(cliente.numero)
                    complementoEditText.setText(cliente.complemento)
                    bairroEditText.setText(cliente.bairro)
                    cidadeEditText.setText(cliente.municipio) // Mapeando para municipio
                    estadoAutoComplete.setText(cliente.uf) // Mapeando para uf
                    cepEditText.setText(cliente.cep)
                    numeroSerieEditText.setText(cliente.numeroSerial)

                    salvarButton.text = getString(R.string.atualizar)
                    bloquearButton.visibility = Button.VISIBLE
                } else {
                    showToast("Erro: Cliente não encontrado.")
                    Log.e("AdicionarClienteActivity", "Cliente com ID $id não encontrado para edição.")
                    finish()
                }
            }
        }
    }

    private fun salvarOuAtualizarCliente() {
        val nome = nomeEditText.text.toString().trim()
        val telefone = telefoneEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val infoAdicionais = informacoesAdicionaisEditText.text.toString().trim()
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

            // Verifica se já existe um cliente com o mesmo CPF/CNPJ (se informado)
            if (cpf.isNotEmpty() || cnpj.isNotEmpty()) {
                existingCliente = clienteDao.getClienteByCpfCnpj(cpf, cnpj)
            } else {
                // Se CPF/CNPJ não for informado, verifica se o nome já existe
                existingCliente = clienteDao.getClienteByNome(nome)
            }

            if (existingCliente != null && existingCliente.id != clienteId) {
                withContext(Dispatchers.Main) {
                    showToast("Já existe um cliente com este nome ou CPF/CNPJ.")
                }
                return@launch
            }

            val cliente = Cliente(
                id = if (clienteId == -1L) 0 else clienteId,
                nome = nome,
                telefone = telefone.takeIf { it.isNotEmpty() },
                email = email.takeIf { it.isNotEmpty() },
                informacoesAdicionais = infoAdicionais.takeIf { it.isNotEmpty() },
                cpf = cpf.takeIf { it.isNotEmpty() },
                cnpj = cnpj.takeIf { it.isNotEmpty() },
                logradouro = logradouro.takeIf { it.isNotEmpty() },
                numero = numero.takeIf { it.isNotEmpty() },
                complemento = complemento.takeIf { it.isNotEmpty() },
                bairro = bairro.takeIf { it.isNotEmpty() },
                municipio = cidade.takeIf { it.isNotEmpty() }, // Mapeando para municipio
                uf = estado.takeIf { it.isNotEmpty() }, // Mapeando para uf
                cep = cep.takeIf { it.isNotEmpty() },
                numeroSerial = numeroSerial.takeIf { it.isNotEmpty() }
            )

            try {
                val result: Long
                if (clienteId == -1L) {
                    result = clienteDao.insert(cliente)
                    if (result != -1L) {
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
                } else {
                    clienteDao.update(cliente)
                    withContext(Dispatchers.Main) {
                        showToast("Cliente atualizado com sucesso!")
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("AdicionarClienteActivity", "Erro ao salvar/atualizar cliente: ${e.message}", e)
                    showToast("Erro ao salvar/atualizar cliente: ${e.message}")
                }
            }
        }
    }

    private fun bloquearCliente() {
        if (clienteId == -1L) {
            showToast("Salve o cliente antes de bloqueá-lo.")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cliente = clienteDao.getClienteById(clienteId)
                if (cliente != null) {
                    // Crie um ClienteBloqueado a partir do Cliente
                    val clienteBloqueado = ClienteBloqueado(
                        nome = cliente.nome,
                        email = cliente.email,
                        telefone = cliente.telefone,
                        informacoesAdicionais = cliente.informacoesAdicionais,
                        cpf = cliente.cpf,
                        cnpj = cliente.cnpj,
                        logradouro = cliente.logradouro,
                        numero = cliente.numero,
                        complemento = cliente.complemento,
                        bairro = cliente.bairro,
                        municipio = cliente.municipio,
                        uf = cliente.uf,
                        cep = cliente.cep,
                        numeroSerial = cliente.numeroSerial
                    )
                    val newBlockedId = clienteBloqueadoDao.insert(clienteBloqueado)

                    if (newBlockedId != -1L) {
                        // Se inserido com sucesso, delete o cliente da tabela de clientes
                        clienteDao.delete(cliente)
                        withContext(Dispatchers.Main) {
                            showToast("Cliente bloqueado com sucesso!")
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao bloquear cliente.")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Cliente não encontrado para bloquear.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("AdicionarClienteActivity", "Erro ao bloquear cliente: ${e.message}", e)
                    showToast("Erro ao bloquear cliente: ${e.message}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}