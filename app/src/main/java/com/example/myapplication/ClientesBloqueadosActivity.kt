package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.ClienteBloqueadoDao
import com.example.myapplication.database.dao.ClienteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientesBloqueadosActivity : AppCompatActivity() {

    private lateinit var clientesBloqueadosRecyclerView: RecyclerView
    private lateinit var clientesBloqueadosAdapter: ClienteBloqueadoAdapter
    private lateinit var voltarButton: TextView

    // DAOs do Room
    private lateinit var clienteBloqueadoDao: ClienteBloqueadoDao
    private lateinit var clienteDao: ClienteDao // Para restaurar clientes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clientes_bloqueados)

        // Inicializa os DAOs do Room
        val application = application as MyApplication
        clienteBloqueadoDao = application.database.clienteBloqueadoDao()
        clienteDao = application.database.clienteDao()

        initComponents()
        setupListeners()
        setupRecyclerView()
        loadClientesBloqueados()
    }

    private fun initComponents() {
        clientesBloqueadosRecyclerView = findViewById(R.id.clientesBloqueadosRecyclerView)
        voltarButton = findViewById(R.id.voltarButton)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        clientesBloqueadosRecyclerView.layoutManager = LinearLayoutManager(this)
        clientesBloqueadosAdapter = ClienteBloqueadoAdapter(
            onItemLongClick = { clienteBloqueado ->
                showRestoreAndDeleteDialog(clienteBloqueado)
            }
        )
        clientesBloqueadosRecyclerView.adapter = clientesBloqueadosAdapter

        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        clientesBloqueadosRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))
    }

    private fun loadClientesBloqueados() {
        lifecycleScope.launch {
            clienteBloqueadoDao.getAllClientesBloqueados().collectLatest { clientes ->
                clientesBloqueadosAdapter.updateClientes(clientes)
                Log.d("ClientesBloqueadosActivity", "Clientes bloqueados carregados: ${clientes.size}")
            }
        }
    }

    private fun showRestoreAndDeleteDialog(cliente: ClienteBloqueado) {
        val options = arrayOf("Restaurar Cliente", "Excluir Permanentemente")
        AlertDialog.Builder(this)
            .setTitle("Opções para ${cliente.nome}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> restoreCliente(cliente)
                    1 -> deleteClientePermanentemente(cliente)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun restoreCliente(clienteBloqueado: ClienteBloqueado) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Restauração")
            .setMessage("Tem certeza que deseja restaurar o cliente ${clienteBloqueado.nome}? Ele será movido de volta para a lista de clientes.")
            .setPositiveButton("Restaurar") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val cliente = Cliente(
                            nome = clienteBloqueado.nome,
                            telefone = clienteBloqueado.telefone,
                            email = clienteBloqueado.email,
                            informacoesAdicionais = clienteBloqueado.informacoesAdicionais,
                            cpf = clienteBloqueado.cpf,
                            cnpj = clienteBloqueado.cnpj,
                            logradouro = clienteBloqueado.logradouro,
                            numero = clienteBloqueado.numero,
                            complemento = clienteBloqueado.complemento,
                            bairro = clienteBloqueado.bairro,
                            municipio = clienteBloqueado.municipio,
                            uf = clienteBloqueado.uf,
                            cep = clienteBloqueado.cep,
                            numeroSerial = clienteBloqueado.numeroSerial
                        )
                        val newClienteId = clienteDao.insert(cliente)

                        if (newClienteId != -1L) {
                            clienteBloqueadoDao.delete(clienteBloqueado)
                            withContext(Dispatchers.Main) {
                                showToast("Cliente '${clienteBloqueado.nome}' restaurado com sucesso.")
                                loadClientesBloqueados() // Recarrega a lista
                                setResult(RESULT_OK) // Notifica a MainActivity sobre a restauração
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showToast("Erro ao restaurar cliente.")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao restaurar cliente: ${e.message}")
                            Log.e("ClientesBloqueadosActivity", "Erro ao restaurar cliente: ${e.message}", e)
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteClientePermanentemente(cliente: ClienteBloqueado) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão Permanente")
            .setMessage("Tem certeza que deseja EXCLUIR PERMANENTEMENTE o cliente ${cliente.nome}? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        clienteBloqueadoDao.delete(cliente)
                        withContext(Dispatchers.Main) {
                            showToast("Cliente '${cliente.nome}' excluído permanentemente.")
                            loadClientesBloqueados() // Recarrega a lista
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao excluir cliente permanentemente: ${e.message}")
                            Log.e("ClientesBloqueadosActivity", "Erro ao excluir permanentemente: ${e.message}", e)
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
