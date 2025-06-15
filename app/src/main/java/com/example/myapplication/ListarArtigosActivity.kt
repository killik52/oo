package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.ArtigoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListarArtigosActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var artigosRecyclerView: RecyclerView
    private lateinit var artigoAdapter: ArtigoAdapter
    private lateinit var voltarButton: TextView
    private lateinit var addButton: ImageView

    // DAO do Room
    private lateinit var artigoDao: ArtigoDao

    private val CRIAR_ARTIGO_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_artigos)

        // Inicializa o DAO do Room
        val application = application as MyApplication
        artigoDao = application.database.artigoDao()

        initComponents()
        setupListeners()
        setupRecyclerView()
        loadArtigos()
    }

    private fun initComponents() {
        searchView = findViewById(R.id.searchView)
        artigosRecyclerView = findViewById(R.id.artigosRecyclerView)
        voltarButton = findViewById(R.id.voltarButton)
        addButton = findViewById(R.id.addButton)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        addButton.setOnClickListener {
            val intent = Intent(this, CriarNovoArtigoActivity::class.java)
            startActivityForResult(intent, CRIAR_ARTIGO_REQUEST_CODE)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
    }

    private fun setupRecyclerView() {
        artigosRecyclerView.layoutManager = LinearLayoutManager(this)
        artigoAdapter = ArtigoAdapter(
            onItemClick = { artigo ->
                val intent = Intent(this, CriarNovoArtigoActivity::class.java).apply {
                    putExtra("ARTIGO_ID", artigo.id)
                }
                startActivityForResult(intent, CRIAR_ARTIGO_REQUEST_CODE)
            },
            onItemLongClick = { artigo ->
                showDeleteDialog(artigo)
            }
        )
        artigosRecyclerView.adapter = artigoAdapter

        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        artigosRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))
    }

    private fun loadArtigos() {
        lifecycleScope.launch {
            artigoDao.getAllArtigos().collectLatest { artigos ->
                artigoAdapter.updateArtigos(artigos)
                Log.d("ListarArtigosActivity", "Artigos carregados: ${artigos.size}")
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
                    artigoAdapter.updateArtigos(filteredArtigos)
                    Log.d("ListarArtigosActivity", "Artigos encontrados na busca: ${filteredArtigos.size}")
                    if (filteredArtigos.isEmpty()) {
                        showToast("Nenhum artigo encontrado para '$query'.")
                    }
                }
            }
        }
    }

    private fun showDeleteDialog(artigo: Artigo) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir o artigo '${artigo.nome}'?")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        artigoDao.delete(artigo)
                        withContext(Dispatchers.Main) {
                            showToast("Artigo '${artigo.nome}' excluído com sucesso.")
                            // loadArtigos() // collectLatest já irá atualizar
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao excluir artigo: ${e.message}")
                            Log.e("ListarArtigosActivity", "Erro ao excluir artigo: ${e.message}", e)
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CRIAR_ARTIGO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadArtigos() // Recarrega os artigos quando um novo é adicionado/editado
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
