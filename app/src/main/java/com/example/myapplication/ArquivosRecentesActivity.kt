package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Adicionado import
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.ArtigoContract.ArtigoEntry
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityArquivosRecentesBinding // Corrigido caminho do binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArquivosRecentesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArquivosRecentesBinding
    private lateinit var dbHelper: ClienteDbHelper
    private lateinit var artigoAdapter: ArtigoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArquivosRecentesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar) // Corrigido: `binding.toolbar`
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.articles_list) // Alterado para um string resource

        setupRecyclerView()
        loadRecentArtigos()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        artigoAdapter = ArtigoAdapter(
            onItemClick = { artigo ->
                // Ao clicar, pode-se abrir a tela de edição do artigo ou adicionar à fatura
                val resultIntent = Intent().apply {
                    putExtra("ARTIGO_SELECIONADO", artigo) // Artigo é Parcelable
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            },
            onItemLongClick = { artigo ->
                Toast.makeText(this, "Artigo ${artigo.nome} clicado longo", Toast.LENGTH_SHORT).show()
                // Implementar exclusão ou outras opções
            }
        )
        binding.recyclerViewRecentArtigos.apply { // Corrigido: `binding.recyclerViewRecentArtigos`
            layoutManager = LinearLayoutManager(this@ArquivosRecentesActivity)
            adapter = artigoAdapter
            addItemDecoration(VerticalSpaceItemDecoration(16))
        }
    }

    private fun loadRecentArtigos() {
        lifecycleScope.launch(Dispatchers.IO) {
            val artigos = dbHelper.getAllArtigos().take(Constants.ARTIGO_SELECTION_LIMIT)
            withContext(Dispatchers.Main) {
                if (artigos.isNotEmpty()) {
                    artigoAdapter.submitList(artigos) // Usar submitList
                    binding.textNoRecentArtigos.visibility = View.GONE // Corrigido: `binding.textNoRecentArtigos`
                    binding.recyclerViewRecentArtigos.visibility = View.VISIBLE // Corrigido: `binding.recyclerViewRecentArtigos`
                } else {
                    binding.textNoRecentArtigos.visibility = View.VISIBLE // Corrigido: `binding.textNoRecentArtigos`
                    binding.recyclerViewRecentArtigos.visibility = View.GONE // Corrigido: `binding.recyclerViewRecentArtigos`
                }
            }
        }
    }
}