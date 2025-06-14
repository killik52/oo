package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View // Adicionado
import android.view.inputmethod.InputMethodManager // Adicionado
import android.widget.EditText // Adicionado
import android.widget.Toast // Adicionado
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.ClienteDbHelper // Já existente
import com.example.myapplication.databinding.ActivityAdicionarClienteBinding // Já existente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdicionarClienteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdicionarClienteBinding
    private lateinit var dbHelper: ClienteDbHelper
    private lateinit var clienteAdapter: ClienteAdapter // Corrigido o tipo do adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdicionarClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar) // Acessando a toolbar via binding
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.select_client)

        setupRecyclerView()
        setupListeners()
        loadRecentClients()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        clienteAdapter = ClienteAdapter(
            onItemClick = { cliente: Cliente -> // Tipo explícito para 'cliente'
                val resultIntent = Intent().apply {
                    putExtra("CLIENTE_SELECIONADO", cliente.id)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            },
            onItemLongClick = { cliente: Cliente -> // Tipo explícito para 'cliente'
                Toast.makeText(this, "Cliente ${cliente.nome} clicado longo", Toast.LENGTH_SHORT).show()
                true // Retornar true para consumir o evento de clique longo
            }
        )
        binding.recyclerViewClientes.apply { // Acessando via binding
            layoutManager = LinearLayoutManager(this@AdicionarClienteActivity)
            adapter = clienteAdapter
            addItemDecoration(VerticalSpaceItemDecoration(16))
        }
    }

    private fun setupListeners() {
        binding.buttonAddNewClient.setOnClickListener { // Acessando via binding
            val intent = Intent(this, CriarNovoClienteActivity::class.java)
            startActivityForResult(intent, Constants.REQUEST_CODE_ADD_NEW_CLIENT)
        }

        binding.searchViewClientes.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchClients(query)
                hideKeyboard(binding.searchViewClientes) // Acessando via binding
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean { // Corrigido 'onOnQueryTextChange' para 'onQueryTextChange'
                if (newText.isNullOrBlank()) { // Condição otimizada, remove a verificação de .query.isNotEmpty()
                    loadRecentClients()
                }
                return false
            }
        })

        binding.searchViewClientes.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank() && binding.searchViewClientes.query.isNotEmpty()) {
                    loadRecentClients()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchViewClientes.setOnCloseListener { // Acessando via binding
            loadRecentClients()
            false
        }
    }

    private fun loadRecentClients() {
        lifecycleScope.launch(Dispatchers.IO) {
            val recentClients = dbHelper.getRecentClientes(Constants.CLIENTE_RECENT_LIMIT)
            withContext(Dispatchers.Main) {
                if (recentClients.isNotEmpty()) {
                    clienteAdapter.submitList(recentClients) // Usar submitList
                    binding.textRecentClients.visibility = View.VISIBLE // Acessando via binding
                    binding.textNoClientsFound.visibility = View.GONE // Acessando via binding
                } else {
                    binding.textRecentClients.visibility = View.GONE // Acessando via binding
                    binding.textNoClientsFound.text = getString(R.string.no_clients_found) // Acessando via binding e string resource
                    binding.textNoClientsFound.visibility = View.VISIBLE // Acessando via binding
                }
            }
        }
    }

    private fun searchClients(query: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            val searchResults = if (query.isNullOrBlank()) {
                dbHelper.getAllClientes()
            } else {
                dbHelper.searchClientes(query)
            }
            withContext(Dispatchers.Main) {
                if (searchResults.isNotEmpty()) {
                    clienteAdapter.submitList(searchResults)
                    binding.textRecentClients.visibility = View.GONE // Acessando via binding
                    binding.textNoClientsFound.visibility = View.GONE // Acessando via binding
                } else {
                    clienteAdapter.submitList(emptyList()) // Limpar lista se não houver resultados
                    binding.textRecentClients.visibility = View.GONE // Acessando via binding
                    binding.textNoClientsFound.text = getString(R.string.no_clients_found_search) // Acessando via binding e string resource
                    binding.textNoClientsFound.visibility = View.VISIBLE // Acessando via binding
                }
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_ADD_NEW_CLIENT && resultCode == Activity.RESULT_OK) {
            loadRecentClients() // Recarregar lista após adicionar/editar cliente
        }
    }
}