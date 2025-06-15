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
import database.dao.ClienteBloqueadoDao
import database.dao.ClienteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListarClientesActivity : AppCompatActivity() { // O nome da classe deve ser ListarClientesActivity

    private lateinit var searchView: SearchView
    private lateinit var clientesRecyclerView: RecyclerView
    private lateinit var clientesAdapter: ResumoClienteAdapter
    private lateinit var voltarButton: TextView
    private lateinit var addButton: ImageView

    // DAOs do Room
    private lateinit var clienteDao: ClienteDao
    private lateinit var clienteBloqueadoDao: ClienteBloqueadoDao

    private val ADICIONAR_CLIENTE_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_clientes)

        // Inicializa os DAOs do Room
        val application = application as MyApplication
        clienteDao = application.database.clienteDao()
        clienteBloqueadoDao = application.database.clienteBloqueadoDao()

        initComponents()
        setupListeners()
        setupRecyclerView()
        loadClientes()
    }

    private fun initComponents() {
        searchView = findViewById(R.id.searchView)
        clientesRecyclerView = findViewById(R.id.clientesRecyclerView)
        voltarButton = findViewById(R.id.voltarButton)
        addButton = findViewById(R.id.addButton)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        addButton.setOnClickListener {
            val intent = Intent(this, AdicionarClienteActivity::class.java)
            startActivityForResult(intent, ADICIONAR_CLIENTE_REQUEST_CODE)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                buscarClientes(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadClientes()
                }
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        clientesRecyclerView.layoutManager = LinearLayoutManager(this)
        clientesAdapter = ResumoClienteAdapter(
            onItemClick = { cliente ->
                val intent = Intent(this, AdicionarClienteActivity::class.java).apply {
                    putExtra("CLIENTE_ID", cliente.id)
                }
                startActivityForResult(intent, ADICIONAR_CLIENTE_REQUEST_CODE)
            },
            onItemLongClick = { cliente ->
                showDeleteOrBlockDialog(cliente)
            }
        )
        clientesRecyclerView.adapter = clientesAdapter

        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        clientesRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))
    }

    private fun loadClientes() {
        lifecycleScope.launch {
            clienteDao.getAllClientes().collectLatest { clientes ->
                val resumoClientes = clientes.map { cliente ->
                    ResumoClienteItem(
                        id = cliente.id,
                        nome = cliente.nome,
                        telefone = cliente.telefone
                    )
                }
                clientesAdapter.updateClientes(resumoClientes)
                Log.d("ListarClientesActivity", "Clientes carregados: ${resumoClientes.size}")
            }
        }
    }

    private fun buscarClientes(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            clienteDao.getAllClientes().collectLatest { allClients ->
                val filteredClients = allClients.filter {
                    it.nome.contains(query, ignoreCase = true) ||
                            (it.telefone?.contains(query, ignoreCase = true) == true) ||
                            (it.email?.contains(query, ignoreCase = true) == true)
                }.map { cliente ->
                    ResumoClienteItem(
                        id = cliente.id,
                        nome = cliente.nome,
                        telefone = cliente.telefone
                    )
                }
                withContext(Dispatchers.Main) {
                    clientesAdapter.updateClientes(filteredClients)
                    Log.d("ListarClientesActivity", "Clientes encontrados na busca: ${filteredClients.size}")
                    if (filteredClients.isEmpty()) {
                        showToast("Nenhum cliente encontrado para '$query'.")
                    }
                }
            }
        }
    }

    private fun showDeleteOrBlockDialog(cliente: ResumoClienteItem) {
        val options = arrayOf("Excluir Cliente", "Bloquear Cliente")
        AlertDialog.Builder(this)
            .setTitle("Opções para ${cliente.nome}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> deleteCliente(cliente)
                    1 -> blockCliente(cliente)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteCliente(cliente: ResumoClienteItem) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir o cliente ${cliente.nome}?")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val clienteToDelete = clienteDao.getClienteById(cliente.id)
                        if (clienteToDelete != null) {
                            clienteDao.delete(clienteToDelete)
                            withContext(Dispatchers.Main) {
                                showToast("Cliente '${cliente.nome}' excluído com sucesso.")
                                // loadClientes() // collectLatest já irá atualizar
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showToast("Erro: Cliente não encontrado para exclusão.")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao excluir cliente: ${e.message}")
                            Log.e("ListarClientesActivity", "Erro ao excluir cliente: ${e.message}", e)
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun blockCliente(cliente: ResumoClienteItem) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Bloqueio")
            .setMessage("Tem certeza que deseja bloquear o cliente ${cliente.nome}? Ele será movido para a lista de bloqueados.")
            .setPositiveButton("Bloquear") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val clienteToBlock = clienteDao.getClienteById(cliente.id)
                        if (clienteToBlock != null) {
                            val clienteBloqueado = ClienteBloqueado(
                                nome = clienteToBlock.nome,
                                telefone = clienteToBlock.telefone,
                                email = clienteToBlock.email,
                                informacoesAdicionais = clienteToBlock.informacoesAdicionais,
                                cpf = clienteToBlock.cpf,
                                cnpj = clienteToBlock.cnpj,
                                logradouro = clienteToBlock.logradouro,
                                numero = clienteToBlock.numero,
                                complemento = clienteToBlock.complemento,
                                bairro = clienteToBlock.bairro,
                                municipio = clienteToBlock.municipio,
                                uf = clienteToBlock.uf,
                                cep = clienteToBlock.cep,
                                numeroSerial = clienteToBlock.numeroSerial
                            )
                            val newBlockedId = clienteBloqueadoDao.insert(clienteBloqueado)

                            if (newBlockedId != -1L) {
                                clienteDao.delete(clienteToBlock)
                                withContext(Dispatchers.Main) {
                                    showToast("Cliente '${cliente.nome}' bloqueado com sucesso.")
                                    // loadClientes() // collectLatest já irá atualizar
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    showToast("Erro ao bloquear cliente.")
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showToast("Erro: Cliente não encontrado para bloquear.")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao bloquear cliente: ${e.message}")
                            Log.e("ListarClientesActivity", "Erro ao bloquear cliente: ${e.message}", e)
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADICIONAR_CLIENTE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadClientes() // Recarrega os clientes quando um novo é adicionado/editado
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
