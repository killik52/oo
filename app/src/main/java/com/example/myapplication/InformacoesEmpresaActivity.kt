package com.example.myapplication

import api.CnpjApiService
import api.RetrofitClient
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.InformacoesEmpresaContract.InformacoesEmpresaEntry
import com.example.myapplication.databinding.ActivityInformacoesEmpresaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InformacoesEmpresaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInformacoesEmpresaBinding
    private lateinit var dbHelper: ClienteDbHelper
    private lateinit var cnpjApiService: CnpjApiService

    private var selectedLogoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedLogoUri = it
            displayLogo(it)
            saveLogoPath(it.toString())
        } ?: Toast.makeText(this, getString(R.string.no_logo_selected), Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInformacoesEmpresaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)
        cnpjApiService = RetrofitClient.cnpjApiService

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.company_info)

        loadInformacoesEmpresa()
        setupListeners()
        setupTextWatchers()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.buttonSaveCompanyInfo.setOnClickListener {
            saveInformacoesEmpresa()
        }
        binding.buttonSearchCnpj.setOnClickListener {
            searchCnpj()
        }
        binding.buttonSelectLogo.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.buttonRemoveLogo.setOnClickListener {
            removeLogo()
        }
    }

    private fun setupTextWatchers() {
        binding.editTextCompanyCnpj.addTextChangedListener(CnpjMask(binding.editTextCompanyCnpj))
    }

    private fun loadInformacoesEmpresa() {
        lifecycleScope.launch(Dispatchers.IO) {
            val info = dbHelper.getInformacoesEmpresa()
            val savedLogoPath = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Constants.LOGO_PATH_KEY, null)

            withContext(Dispatchers.Main) {
                info?.let {
                    binding.editTextCompanyName.setText(it.nome)
                    binding.editTextCompanyAddress.setText(it.endereco)
                    binding.editTextCompanyPhone.setText(it.telefone)
                    binding.editTextCompanyEmail.setText(it.email)
                    binding.editTextCompanyCnpj.setText(it.cnpj)
                }

                savedLogoPath?.let { path ->
                    try {
                        val uri = Uri.parse(path)
                        displayLogo(uri)
                        selectedLogoUri = uri
                    } catch (e: Exception) {
                        Log.e("InformacoesEmpresa", "Erro ao carregar URI do logo salvo: ${e.message}")
                        removeLogo() // Remove path if invalid
                    }
                } ?: run {
                    displayNoLogoState()
                }
            }
        }
    }

    private fun saveInformacoesEmpresa() {
        val nome = binding.editTextCompanyName.text.toString().trim()
        val endereco = binding.editTextCompanyAddress.text.toString().trim()
        val telefone = binding.editTextCompanyPhone.text.toString().trim()
        val email = binding.editTextCompanyEmail.text.toString().trim()
        val cnpj = binding.editTextCompanyCnpj.text.toString().trim()

        val info = InformacoesEmpresa(
            id = 1L, // Apenas uma entrada para informações da empresa
            nome = nome,
            endereco = if (endereco.isNotEmpty()) endereco else null,
            telefone = if (telefone.isNotEmpty()) telefone else null,
            email = if (email.isNotEmpty()) email else null,
            cnpj = if (cnpj.isNotEmpty()) cnpj else null
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val success = dbHelper.saveInformacoesEmpresa(info)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@InformacoesEmpresaActivity, getString(R.string.company_info_saved_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@InformacoesEmpresaActivity, getString(R.string.company_info_saved_error), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun searchCnpj() {
        val cnpj = binding.editTextCompanyCnpj.text.toString().replace("[^0-9]".toRegex(), "")
        if (cnpj.length != 14) {
            Toast.makeText(this, getString(R.string.cnpj_invalid_format), Toast.LENGTH_SHORT).show()
            return
        }

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextCompanyCnpj.windowToken, 0)

        binding.progressBarCnpj.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = cnpjApiService.getCnpjData(cnpj)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val cnpjData = response.body()
                        cnpjData?.let {
                            binding.editTextCompanyName.setText(it.nome)
                            // Monta o endereço completo a partir dos campos da API
                            val enderecoCompleto = "${it.endereco?.logradouro ?: ""} ${it.endereco?.numero ?: ""}" +
                                    (if (!it.endereco?.complemento.isNullOrBlank()) ", ${it.endereco?.complemento}" else "") +
                                    (if (!it.endereco?.bairro.isNullOrBlank()) ", ${it.endereco?.bairro}" else "") +
                                    (if (!it.endereco?.municipio.isNullOrBlank()) ", ${it.endereco?.municipio}" else "") +
                                    (if (!it.endereco?.uf.isNullOrBlank()) " - ${it.endereco?.uf}" else "") +
                                    (if (!it.endereco?.cep.isNullOrBlank()) ", CEP: ${it.endereco?.cep}" else "")
                            binding.editTextCompanyAddress.setText(enderecoCompleto.trim().removePrefix(","))
                            binding.editTextCompanyPhone.setText(it.telefone)
                            binding.editTextCompanyEmail.setText(it.email)
                        } ?: Toast.makeText(this@InformacoesEmpresaActivity, getString(R.string.cnpj_not_found), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@InformacoesEmpresaActivity, getString(R.string.cnpj_search_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CnpjSearch", "Erro ao buscar CNPJ: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@InformacoesEmpresaActivity, getString(R.string.cnpj_search_failed) + ": ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBarCnpj.visibility = View.GONE
                }
            }
        }
    }

    private fun displayLogo(uri: Uri) {
        binding.imageViewLogo.setImageURI(uri)
        binding.imageViewLogo.visibility = View.VISIBLE
        binding.textNoLogoSelected.visibility = View.GONE
        binding.buttonRemoveLogo.visibility = View.VISIBLE
    }

    private fun displayNoLogoState() {
        binding.imageViewLogo.visibility = View.GONE
        binding.textNoLogoSelected.visibility = View.VISIBLE
        binding.buttonRemoveLogo.visibility = View.GONE
    }

    private fun saveLogoPath(path: String) {
        val sharedPrefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(Constants.LOGO_PATH_KEY, path)
            apply()
        }
        Toast.makeText(this, getString(R.string.logo_updated_success), Toast.LENGTH_SHORT).show()
    }

    private fun removeLogo() {
        val sharedPrefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            remove(Constants.LOGO_PATH_KEY)
            apply()
        }
        selectedLogoUri = null
        displayNoLogoState()
        Toast.makeText(this, getString(R.string.logo_removed), Toast.LENGTH_SHORT).show()
    }
}