package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.ArtigoContract.ArtigoEntry
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityListarArtigosBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListarArtigosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListarArtigosBinding
    private lateinit var dbHelper: ClienteDbHelper
    private lateinit var artigoAdapter: ArtigoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListarArtigosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.articles_list)

        setupRecyclerView()
        setupListeners()
        loadAllArtigos()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        artigoAdapter = ArtigoAdapter(
            onItemClick = { artigo ->
                val resultIntent = Intent().apply {
                    putExtra("ARTIGO_SELECIONADO", artigo)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            },
            onItemLongClick = { artigo ->
                showDeleteConfirmationDialog(artigo)
            }
        )
        binding.recyclerViewArtigos.apply {
            layoutManager = LinearLayoutManager(this@ListarArtigosActivity)
            adapter = artigoAdapter
            addItemDecoration(VerticalSpaceItemDecoration(16))
        }
    }

    private fun setupListeners() {
        binding.buttonAddNewArtigo.setOnClickListener {
            val intent = Intent(this, CriarNovoArtigoActivity::class.java)
            startActivityForResult(intent, Constants.REQUEST_CODE_ADD_NEW_ARTICLE)
        }

        binding.searchViewArtigos.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchArtigos(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    loadAllArtigos()
                }
                return false
            }
        })

        binding.searchViewArtigos.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank() && binding.searchViewArtigos.query.isNotEmpty()) {
                    loadAllArtigos()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchViewArtigos.setOnCloseListener {
            loadAllArtigos()
            false
        }
    }

    private fun loadAllArtigos() {
        lifecycleScope.launch(Dispatchers.IO) {
            val artigos = dbHelper.getAllArtigos()
            withContext(Dispatchers.Main) {
                if (artigos.isNotEmpty()) {
                    artigoAdapter.submitList(artigos)
                    binding.textNoArtigosFound.visibility = View.GONE
                    binding.recyclerViewArtigos.visibility = View.VISIBLE
                } else {
                    binding.textNoArtigosFound.visibility = View.VISIBLE
                    binding.recyclerViewArtigos.visibility = View.GONE
                }
            }
        }
    }

    private fun searchArtigos(query: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            val searchResults = if (query.isNullOrBlank()) {
                dbHelper.getAllArtigos()
            } else {
                dbHelper.searchArtigos(query)
            }
            withContext(Dispatchers.Main) {
                if (searchResults.isNotEmpty()) {
                    artigoAdapter.submitList(searchResults)
                    binding.textNoArtigosFound.visibility = View.GONE
                    binding.recyclerViewArtigos.visibility = View.VISIBLE
                } else {
                    artigoAdapter.submitList(emptyList())
                    binding.textNoArtigosFound.text = getString(R.string.no_articles_found)
                    binding.textNoArtigosFound.visibility = View.VISIBLE
                    binding.recyclerViewArtigos.visibility = View.GONE
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(artigo: Artigo) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_article_dialog_title))
            .setMessage(getString(R.string.delete_article_dialog_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteArtigo(artigo.id)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteArtigo(artigoId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = dbHelper.deleteArtigo(artigoId)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@ListarArtigosActivity, getString(R.string.article_deleted_success), Toast.LENGTH_SHORT).show()
                    loadAllArtigos() // Recarrega a lista após a exclusão
                } else {
                    Toast.makeText(this@ListarArtigosActivity, getString(R.string.article_deleted_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_ADD_NEW_ARTICLE && resultCode == RESULT_OK) {
            loadAllArtigos() // Recarregar lista após adicionar/editar artigo
        }
    }
}