package com.example.myapplication

import api.CnpjApiService
import api.RetrofitClient
import android.app.Activity // Adicionado import
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ClienteContract.ClienteEntry
import com.example.myapplication.databinding.ActivityCriarNovoClienteBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CriarNovoClienteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCriarNovoClienteBinding
    private lateinit var dbHelper: ClienteDbHelper
    private lateinit var cnpjApiService: CnpjApiService
    private var clienteId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCriarNovoClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)
        cnpjApiService = RetrofitClient.cnpjApiService

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.add_new_client)

        clienteId = intent.getLongExtra("CLIENTE_ID", -1L)
        if (clienteId != -1L) {
            loadCliente(clienteId)
        }

        setupListeners()
        setupTextWatchers()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.buttonSaveClient.setOnClickListener {
            saveCliente()
        }

        binding.buttonSearchCnpj.setOnClickListener {
            searchCnpj()
        }
    }

    private fun setupTextWatchers() {
        binding.editTextCnpj.addTextChangedListener(CnpjMask(binding.editTextCnpj))
        binding.editTextCep.addTextChangedListener(CepMask(binding.editTextCep))
        binding.editTextTelefone.addTextChangedListener(PhoneMask(binding.editTextTelefone))
        binding.editTextCpf.addTextChangedListener(CpfMask(binding.editTextCpf))
    }

    private fun loadCliente(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val cliente = dbHelper.getClienteById(id)
            withContext(Dispatchers.Main) {
                cliente?.let {
                    binding.editTextNome.setText(it.nome)
                    binding.editTextEmail.setText(it.email)
                    binding.editTextTelefone.setText(it.telefone)
                    binding.editTextCpf.setText(it.cpf)
                    binding.editTextCnpj.setText(it.cnpj)
                    binding.editTextLogradouro.setText(it.logradouro)
                    binding.editTextNumero.setText(it.numero)
                    binding.editTextComplemento.setText(it.complemento)
                    binding.editTextBairro.setText(it.bairro)
                    binding.editTextCidade.setText(it.cidade)
                    binding.editTextEstado.setText(it.estado)
                    binding.editTextCep.setText(it.cep)
                    binding.buttonSaveClient.text = getString(R.string.save_client)
                    supportActionBar?.title = getString(R.string.client_details)
                } ?: run {
                    Toast.makeText(this@CriarNovoClienteActivity, "Cliente não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun saveCliente() {
        val nome = binding.editTextNome.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val telefone = binding.editTextTelefone.text.toString().trim()
        val cpf = binding.editTextCpf.text.toString().trim()
        val cnpj = binding.editTextCnpj.text.toString().trim()
        val logradouro = binding.editTextLogradouro.text.toString().trim()
        val numero = binding.editTextNumero.text.toString().trim()
        val complemento = binding.editTextComplemento.text.toString().trim()
        val bairro = binding.editTextBairro.text.toString().trim()
        val cidade = binding.editTextCidade.text.toString().trim()
        val estado = binding.editTextEstado.text.toString().trim()
        val cep = binding.editTextCep.text.toString().trim()

        if (nome.isEmpty()) {
            Toast.makeText(this, "Nome é obrigatório.", Toast.LENGTH_SHORT).show()
            return
        }

        val cliente = Cliente(
            id = clienteId,
            nome = nome,
            endereco = null, // Endereço completo será montado ou deixado como nulo
            telefone = if (telefone.isNotEmpty()) telefone else null,
            email = if (email.isNotEmpty()) email else null,
            cpf = if (cpf.isNotEmpty()) cpf else null,
            cnpj = if (cnpj.isNotEmpty()) cnpj else null,
            logradouro = if (logradouro.isNotEmpty()) logradouro else null,
            numero = if (numero.isNotEmpty()) numero else null,
            complemento = if (complemento.isNotEmpty()) complemento else null,
            bairro = if (bairro.isNotEmpty()) bairro else null,
            cidade = if (cidade.isNotEmpty()) cidade else null,
            estado = if (estado.isNotEmpty()) estado else null,
            cep = if (cep.isNotEmpty()) cep else null
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val success = if (clienteId == -1L) {
                dbHelper.addCliente(cliente)
            } else {
                dbHelper.updateCliente(cliente)
            }
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@CriarNovoClienteActivity, getString(R.string.add_client_success), Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@CriarNovoClienteActivity, getString(R.string.add_client_error), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun searchCnpj() {
        val cnpj = binding.editTextCnpj.text.toString().replace("[^0-9]".toRegex(), "")
        if (cnpj.length != 14) {
            Toast.makeText(this, getString(R.string.cnpj_invalid_format), Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard(binding.editTextCnpj)
        binding.progressBarCnpj.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = cnpjApiService.getCnpjData(cnpj)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val cnpjData = response.body()
                        cnpjData?.let {
                            binding.editTextNome.setText(it.nome)
                            binding.editTextTelefone.setText(it.telefone)
                            binding.editTextEmail.setText(it.email)
                            binding.editTextLogradouro.setText(it.endereco?.logradouro)
                            binding.editTextNumero.setText(it.endereco?.numero)
                            binding.editTextComplemento.setText(it.endereco?.complemento)
                            binding.editTextBairro.setText(it.endereco?.bairro)
                            binding.editTextCidade.setText(it.endereco?.municipio) // Aqui precisa ser `municipio` da resposta da API
                            binding.editTextEstado.setText(it.endereco?.uf) // Aqui precisa ser `uf` da resposta da API
                            binding.editTextCep.setText(it.endereco?.cep)
                        } ?: Toast.makeText(this@CriarNovoClienteActivity, getString(R.string.cnpj_not_found), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CriarNovoClienteActivity, getString(R.string.cnpj_search_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CnpjSearch", "Erro ao buscar CNPJ: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CriarNovoClienteActivity, getString(R.string.cnpj_search_failed) + ": ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBarCnpj.visibility = View.GONE
                }
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}