package com.example.myapplication

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.ArtigoDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.util.Locale

class ThirdScreenActivity : AppCompatActivity() {

    private lateinit var voltarButton: TextView
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var artigosRecyclerView: RecyclerView
    private lateinit var artigosAdapter: ArtigoAdapter // Adapter para exibir artigos disponíveis
    private lateinit var adicionarArtigoManualButton: Button
    private lateinit var finalizarSelecaoButton: Button

    private lateinit var faturaItensList: MutableList<FaturaItem> // Lista de itens que serão retornados
    private var faturaId: Long = -1 // ID da fatura, pode ser -1 se for nova
    private var isFaturaSent: Boolean = false // Para saber se pode editar

    // DAO do Room
    private lateinit var artigoDao: ArtigoDao

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third_screen)

        // Inicializa o DAO do Room
        val application = application as MyApplication
        artigoDao = application.database.artigoDao()

        faturaId = intent.getLongExtra("fatura_id", -1)
        isFaturaSent = intent.getBooleanExtra("is_fatura_sent", false)

        val faturaItemsJson = intent.getStringExtra("fatura_itens_json")
        faturaItensList = if (faturaItemsJson != null) {
            gson.fromJson(faturaItemsJson, object : TypeToken<MutableList<FaturaItem>>() {}.type)
        } else {
            mutableListOf()
        }

        initComponents()
        setupListeners()
        setupRecyclerView()
        loadArtigos()
    }

    private fun initComponents() {
        voltarButton = findViewById(R.id.voltarButton)
        searchView = findViewById(R.id.searchView)
        artigosRecyclerView = findViewById(R.id.artigosRecyclerView)
        adicionarArtigoManualButton = findViewById(R.id.adicionarArtigoManualButton)
        finalizarSelecaoButton = findViewById(R.id.finalizarSelecaoButton)

        if (isFaturaSent) {
            adicionarArtigoManualButton.isEnabled = false
            finalizarSelecaoButton.isEnabled = false
            searchView.isEnabled = false
            showToast("Fatura enviada. Não é possível modificar artigos.")
        }
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                buscarArtigos(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadArtigos()
                }
                return true
            }
        })

        adicionarArtigoManualButton.setOnClickListener {
            if (!isFaturaSent) {
                showAddArticleManualDialog()
            }
        }

        finalizarSelecaoButton.setOnClickListener {
            returnResult()
        }
    }

    private fun setupRecyclerView() {
        artigosRecyclerView.layoutManager = LinearLayoutManager(this)
        artigosAdapter = ArtigoAdapter(
            onItemClick = { artigo ->
                if (!isFaturaSent) {
                    showAddArticleFromListDialog(artigo)
                }
            },
            onItemLongClick = { artigo ->
                showToast("Funcionalidade de edição de artigo não implementada diretamente aqui.")
                // Aqui você pode, por exemplo, ir para CriarNovoArtigoActivity para editar o artigo
            }
        )
        artigosRecyclerView.adapter = artigosAdapter

        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        artigosRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))
    }

    private fun loadArtigos() {
        lifecycleScope.launch {
            artigoDao.getAllArtigos().collectLatest { artigos ->
                artigosAdapter.updateArtigos(artigos)
                Log.d("ThirdScreenActivity", "Artigos disponíveis carregados: ${artigos.size}")
            }
        }
    }

    private fun buscarArtigos(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            artigoDao.getAllArtigos().collectLatest { allArtigos ->
                val filteredArtigos = allArtigos.filter {
                    it.nome.contains(query, ignoreCase = true) ||
                            (it.numeroSerial?.contains(query, ignoreCase = true) == true) ||
                            (it.descricao?.contains(query, ignoreCase = true) == true)
                }
                withContext(Dispatchers.Main) {
                    artigosAdapter.updateArtigos(filteredArtigos)
                    if (filteredArtigos.isEmpty()) {
                        showToast("Nenhum artigo encontrado para '$query'.")
                    }
                }
            }
        }
    }

    private fun showAddArticleManualDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_article_to_invoice, null)
        val nameEditText: EditText = dialogView.findViewById(R.id.articleNameEditText)
        val priceEditText: EditText = dialogView.findViewById(R.id.articlePriceEditText)
        val quantityEditText: EditText = dialogView.findViewById(R.id.articleQuantityEditText)
        val serialEditText: EditText = dialogView.findViewById(R.id.articleSerialEditText)
        val descriptionEditText: EditText = dialogView.findViewById(R.id.articleDescriptionEditText)

        AlertDialog.Builder(this)
            .setTitle("Adicionar Artigo Manualmente")
            .setView(dialogView)
            .setPositiveButton("Adicionar") { dialog, _ ->
                val name = nameEditText.text.toString().trim()
                val price = priceEditText.text.toString().toDoubleOrNull()
                val quantity = quantityEditText.text.toString().toIntOrNull()
                val serial = serialEditText.text.toString().trim().takeIf { it.isNotEmpty() }
                val description = descriptionEditText.text.toString().trim().takeIf { it.isNotEmpty() }

                if (name.isNotEmpty() && price != null && quantity != null && quantity > 0) {
                    val newItem = FaturaItem(
                        faturaId = faturaId, // Será ajustado ao salvar a fatura
                        quantidade = quantity,
                        precoUnitario = price,
                        nomeArtigo = name,
                        numeroSerie = serial,
                        descricao = description
                    )
                    faturaItensList.add(newItem)
                    showToast("Artigo '${name}' adicionado à fatura.")
                } else {
                    showToast("Preencha todos os campos obrigatórios (Nome, Preço, Quantidade).")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddArticleFromListDialog(artigo: Artigo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_article_to_invoice, null)
        val nameEditText: EditText = dialogView.findViewById(R.id.articleNameEditText)
        val priceEditText: EditText = dialogView.findViewById(R.id.articlePriceEditText)
        val quantityEditText: EditText = dialogView.findViewById(R.id.articleQuantityEditText)
        val serialEditText: EditText = dialogView.findViewById(R.id.articleSerialEditText)
        val descriptionEditText: EditText = dialogView.findViewById(R.id.articleDescriptionEditText)

        nameEditText.setText(artigo.nome)
        priceEditText.setText(DecimalFormat("0.00").format(artigo.preco))
        quantityEditText.setText("1") // Padrão 1
        serialEditText.setText(artigo.numeroSerial)
        descriptionEditText.setText(artigo.descricao)

        nameEditText.isEnabled = false // Nome não pode ser alterado se vier da lista
        priceEditText.isEnabled = false // Preço não pode ser alterado se vier da lista

        AlertDialog.Builder(this)
            .setTitle("Adicionar Artigo '${artigo.nome}'")
            .setView(dialogView)
            .setPositiveButton("Adicionar") { dialog, _ ->
                val quantity = quantityEditText.text.toString().toIntOrNull()
                val serial = serialEditText.text.toString().trim().takeIf { it.isNotEmpty() }
                val description = descriptionEditText.text.toString().trim().takeIf { it.isNotEmpty() }

                if (quantity != null && quantity > 0) {
                    val newItem = FaturaItem(
                        faturaId = faturaId, // Será ajustado ao salvar a fatura
                        quantidade = quantity,
                        precoUnitario = artigo.preco, // Usa o preço do artigo original
                        nomeArtigo = artigo.nome,
                        numeroSerie = serial,
                        descricao = description
                    )
                    faturaItensList.add(newItem)
                    showToast("Artigo '${artigo.nome}' adicionado à fatura.")
                } else {
                    showToast("A quantidade deve ser um número válido maior que zero.")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun returnResult() {
        val resultIntent = Intent().apply {
            putExtra("updated_fatura_itens_json", gson.toJson(faturaItensList))
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onBackPressed() {
        // Ao voltar, retorne a lista atualizada de itens, mesmo que não tenha salvo
        returnResult()
        super.onBackPressed()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
