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
import com.example.myapplication.ClienteContract.ClienteEntry
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityListarClientesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListarClientesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListarClientesBinding
    private lateinit var dbHelper: ClienteDbHelper
    private lateinit var clienteAdapter: ClienteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListarClientesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.client_list_title)

        setupRecyclerView()
        setupListeners()
        loadAllClientes()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        clienteAdapter = ClienteAdapter(
            onItemClick = { cliente ->
                val intent = Intent(this, ClienteActivity::class.java).apply {
                    putExtra("CLIENTE_ID", cliente.id)
                }
                startActivity(intent)
            },
            onItemLongClick = { cliente ->
                Toast.makeText(this, "Cliente ${cliente.nome} clicado longo", Toast.LENGTH_SHORT).show()
                // Implementar opções de exclusão/bloqueio se necessário
            }
        )
        binding.recyclerViewClientes.apply {
            layoutManager = LinearLayoutManager(this@ListarClientesActivity)
            adapter = clienteAdapter
            addItemDecoration(VerticalSpaceItemDecoration(16))
        }
    }

    private fun setupListeners() {
        binding.buttonAddNewClient.setOnClickListener {
            val intent = Intent(this, CriarNovoClienteActivity::class.java)
            startActivityForResult(intent, Constants.REQUEST_CODE_ADD_NEW_CLIENT)
        }

        binding.searchViewClientes.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchClientes(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    loadAllClientes()
                }
                return false
            }
        })

        binding.searchViewClientes.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank() && binding.searchViewClientes.query.isNotEmpty()) {
                    loadAllClientes()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchViewClientes.setOnCloseListener {
            loadAllClientes()
            false
        }
    }

    private fun loadAllClientes() {
        lifecycleScope.launch(Dispatchers.IO) {
            val clientes = dbHelper.getAllClientes()
            withContext(Dispatchers.Main) {
                if (clientes.isNotEmpty()) {
                    clienteAdapter.submitList(clientes)
                    binding.textNoClientsFound.visibility = View.GONE
                    binding.recyclerViewClientes.visibility = View.VISIBLE
                } else {
                    binding.textNoClientsFound.visibility = View.VISIBLE
                    binding.recyclerViewClientes.visibility = View.GONE
                }
            }
        }
    }

    private fun searchClientes(query: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            val searchResults = if (query.isNullOrBlank()) {
                dbHelper.getAllClientes()
            } else {
                dbHelper.searchClientes(query)
            }
            withContext(Dispatchers.Main) {
                if (searchResults.isNotEmpty()) {
                    clienteAdapter.submitList(searchResults)
                    binding.textNoClientsFound.visibility = View.GONE
                    binding.recyclerViewClientes.visibility = View.VISIBLE
                } else {
                    clienteAdapter.submitList(emptyList())
                    binding.textNoClientsFound.text = getString(R.string.no_clients_found_search)
                    binding.textNoClientsFound.visibility = View.VISIBLE
                    binding.recyclerViewClientes.visibility = View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadAllClientes() // Recarregar lista sempre que a atividade é retomada
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_ADD_NEW_CLIENT && resultCode == RESULT_OK) {
            loadAllClientes() // Recarregar lista após adicionar/editar cliente
        }
    }
}