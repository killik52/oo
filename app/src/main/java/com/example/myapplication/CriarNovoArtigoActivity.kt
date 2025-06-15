package com.example.myapplication

import android.app.Activity
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.ArtigoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CriarNovoArtigoActivity : AppCompatActivity() {

    private lateinit var nomeArtigoEditText: EditText
    private lateinit var precoArtigoEditText: EditText
    private lateinit var quantidadeArtigoEditText: EditText
    private lateinit var descontoArtigoEditText: EditText
    private lateinit var descontoPercentCheckbox: CheckBox
    private lateinit var descricaoArtigoEditText: EditText
    private lateinit var numeroSerialArtigoEditText: EditText
    private lateinit var guardarFaturaCheckBox: CheckBox
    private lateinit var salvarArtigoButton: Button
    private lateinit var voltarButton: TextView
    private lateinit var bloquearArtigoButton: Button // Adicionado conforme o layout original

    // DAO do Room
    private lateinit var artigoDao: ArtigoDao

    private var artigoId: Long = -1 // -1 para novo artigo, ID do artigo para edição

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_novo_artigo)

        // Inicializa o DAO do Room
        val application = application as MyApplication
        artigoDao = application.database.artigoDao()

        initComponents()
        setupListeners()

        artigoId = intent.getLongExtra("ARTIGO_ID", -1)
        if (artigoId != -1L) {
            loadArtigoData(artigoId)
        }
    }

    private fun initComponents() {
        nomeArtigoEditText = findViewById(R.id.nomeArtigoEditText)
        precoArtigoEditText = findViewById(R.id.precoArtigoEditText)
        quantidadeArtigoEditText = findViewById(R.id.quantidadeArtigoEditText)
        descontoArtigoEditText = findViewById(R.id.descontoArtigoEditText)
        descontoPercentCheckbox = findViewById(R.id.descontoPercentCheckbox)
        descricaoArtigoEditText = findViewById(R.id.descricaoArtigoEditText)
        numeroSerialArtigoEditText = findViewById(R.id.numeroSerialArtigoEditText)
        guardarFaturaCheckBox = findViewById(R.id.guardarFaturaCheckBox)
        salvarArtigoButton = findViewById(R.id.salvarArtigoButton)
        voltarButton = findViewById(R.id.voltarButton)
        bloquearArtigoButton = findViewById(R.id.bloquearArtigoButton) // Inicialização
    }

    private fun setupListeners() {
        salvarArtigoButton.setOnClickListener {
            salvarOuAtualizarArtigo()
        }

        voltarButton.setOnClickListener {
            onBackPressed()
        }

        bloquearArtigoButton.setOnClickListener {
            showToast("Funcionalidade de bloquear artigo não implementada.")
            // Implemente a lógica para "bloquear" um artigo, se necessário.
            // Isso pode significar movê-lo para uma tabela de artigos bloqueados
            // ou simplesmente removê-lo da lista principal.
        }

        // Lógica para desabilitar quantidade e desconto se o artigo for guardado em fatura
        guardarFaturaCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                quantidadeArtigoEditText.isEnabled = false
                quantidadeArtigoEditText.setText("1") // Quantidade padrão 1 para artigos "recentes"
                descontoArtigoEditText.isEnabled = false
                descontoArtigoEditText.setText("0.0") // Desconto padrão 0 para artigos "recentes"
                descontoPercentCheckbox.isEnabled = false
                descontoPercentCheckbox.isChecked = false
            } else {
                quantidadeArtigoEditText.isEnabled = true
                descontoArtigoEditText.isEnabled = true
                descontoPercentCheckbox.isEnabled = true
            }
        }
    }

    private fun loadArtigoData(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val artigo = artigoDao.getArtigoById(id)
            withContext(Dispatchers.Main) {
                if (artigo != null) {
                    nomeArtigoEditText.setText(artigo.nome)
                    precoArtigoEditText.setText(String.format(Locale.getDefault(), "%.2f", artigo.preco))
                    quantidadeArtigoEditText.setText(artigo.quantidade.toString())
                    descontoArtigoEditText.setText(String.format(Locale.getDefault(), "%.2f", artigo.desconto))
                    descontoPercentCheckbox.isChecked = artigo.desconto > 0 // Pode não ser preciso, se não houver um campo no Artigo para isso
                    descricaoArtigoEditText.setText(artigo.descricao)
                    numeroSerialArtigoEditText.setText(artigo.numeroSerial)
                    guardarFaturaCheckBox.isChecked = artigo.guardarFatura

                    salvarArtigoButton.text = getString(R.string.atualizar_artigo)
                    bloquearArtigoButton.visibility = View.VISIBLE
                } else {
                    showToast("Erro: Artigo não encontrado.")
                    Log.e("CriarNovoArtigoActivity", "Artigo com ID $id não encontrado para edição.")
                    finish()
                }
            }
        }
    }

    private fun salvarOuAtualizarArtigo() {
        val nome = nomeArtigoEditText.text.toString().trim()
        val precoStr = precoArtigoEditText.text.toString().trim()
        val quantidadeStr = quantidadeArtigoEditText.text.toString().trim()
        val descontoStr = descontoArtigoEditText.text.toString().trim()
        val descricao = descricaoArtigoEditText.text.toString().trim()
        val numeroSerial = numeroSerialArtigoEditText.text.toString().trim()
        val guardarFatura = guardarFaturaCheckBox.isChecked

        if (nome.isEmpty()) {
            showToast("O nome do artigo é obrigatório.")
            return
        }
        if (precoStr.isEmpty() || precoStr.toDoubleOrNull() == null) {
            showToast("O preço do artigo é inválido.")
            return
        }
        if (quantidadeStr.isEmpty() || quantidadeStr.toIntOrNull() == null) {
            showToast("A quantidade é inválida.")
            return
        }
        if (descontoStr.isEmpty() || descontoStr.toDoubleOrNull() == null) {
            showToast("O desconto é inválido.")
            return
        }

        val preco = precoStr.toDouble()
        val quantidade = quantidadeStr.toInt()
        val desconto = descontoStr.toDouble()

        lifecycleScope.launch(Dispatchers.IO) {
            var existingArtigo: Artigo? = null
            if (numeroSerial.isNotEmpty()) {
                existingArtigo = artigoDao.getArtigoBySerial(numeroSerial)
            } else {
                // Se não houver número de série, verifica se existe artigo com o mesmo nome e preço
                // (Esta lógica pode precisar ser mais refinada dependendo das suas regras de negócio)
                // Por simplicidade, vou apenas verificar se o nome é duplicado (considerando que artigos sem serial são únicos pelo nome)
                val allArtigos = artigoDao.getAllArtigos() // Obtém todos os artigos como Flow
                    .let { flow ->
                        // Collect the Flow to get the current list
                        var list: List<Artigo>? = null
                        flow.collectLatest {
                            list = it
                            // Break out of the collectLatest loop once the list is available
                            return@collectLatest
                        }
                        list
                    }
                existingArtigo = allArtigos?.find { it.nome == nome && it.id != artigoId }
            }

            if (existingArtigo != null && existingArtigo.id != artigoId) {
                withContext(Dispatchers.Main) {
                    showToast("Já existe um artigo com este nome ou número de série.")
                }
                return@launch
            }

            val artigo = Artigo(
                id = if (artigoId == -1L) 0 else artigoId,
                nome = nome,
                preco = preco,
                quantidade = quantidade,
                desconto = desconto,
                descricao = descricao.takeIf { it.isNotEmpty() },
                numeroSerial = numeroSerial.takeIf { it.isNotEmpty() },
                guardarFatura = guardarFatura
            )

            try {
                if (artigoId == -1L) {
                    val newId = artigoDao.insert(artigo)
                    if (newId != -1L) {
                        withContext(Dispatchers.Main) {
                            showToast("Artigo adicionado com sucesso!")
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao adicionar artigo.")
                        }
                    }
                } else {
                    artigoDao.update(artigo)
                    withContext(Dispatchers.Main) {
                        showToast("Artigo atualizado com sucesso!")
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CriarNovoArtigoActivity", "Erro ao salvar/atualizar artigo: ${e.message}", e)
                    showToast("Erro ao salvar/atualizar artigo: ${e.message}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
