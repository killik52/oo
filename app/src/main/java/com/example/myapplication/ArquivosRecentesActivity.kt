package com.example.myapplication

import android.app.Activity
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.ArtigoDao
import com.example.myapplication.database.dao.ClienteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest // Usar collectLatest para Flows
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArquivosRecentesActivity : AppCompatActivity() {

    private lateinit var artigosRecyclerView: RecyclerView
    private lateinit var clientesRecyclerView: RecyclerView
    private lateinit var artigosRecentesAdapter: ResumoArtigoAdapter
    private lateinit var clientesRecentesAdapter: ResumoClienteAdapter
    private lateinit var voltarButton: TextView

    // DAOs do Room
    private lateinit var artigoDao: ArtigoDao
    private lateinit var clienteDao: ClienteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arquivos_recentes)

        // Inicializa os DAOs do Room
        val application = application as MyApplication
        artigoDao = application.database.artigoDao()
        clienteDao = application.database.clienteDao()

        initComponents()
        setupListeners()
        setupRecyclerViews()
        loadRecentData()
    }

    private fun initComponents() {
        artigosRecyclerView = findViewById(R.id.artigosRecyclerView)
        clientesRecyclerView = findViewById(R.id.clientesRecyclerView)
        voltarButton = findViewById(R.id.voltarButton)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerViews() {
        artigosRecyclerView.layoutManager = LinearLayoutManager(this)
        artigosRecentesAdapter = ResumoArtigoAdapter(
            onItemClick = { artigo ->
                // Aqui você pode adicionar a lógica para editar o artigo
                // Por exemplo, iniciar ArtigoActivity com o ID do artigo
                Log.d("ArquivosRecentesActivity", "Artigo clicado: ${artigo.nome}")
            },
            onItemLongClick = { artigo ->
                // Lógica para remover do histórico ou bloquear
                Log.d("ArquivosRecentesActivity", "Artigo clicado longamente para remover: ${artigo.nome}")
                removeArtigoFromRecents(artigo)
            }
        )
        artigosRecyclerView.adapter = artigosRecentesAdapter

        clientesRecyclerView.layoutManager = LinearLayoutManager(this)
        clientesRecentesAdapter = ResumoClienteAdapter(
            onItemClick = { cliente ->
                // Aqui você pode adicionar a lógica para editar o cliente
                // Por exemplo, iniciar AdicionarClienteActivity com o ID do cliente
                Log.d("ArquivosRecentesActivity", "Cliente clicado: ${cliente.nome}")
            },
            onItemLongClick = { cliente ->
                // Lógica para remover do histórico ou bloquear
                Log.d("ArquivosRecentesActivity", "Cliente clicado longamente para remover: ${cliente.nome}")
                removeClienteFromRecents(cliente)
            }
        )
        clientesRecyclerView.adapter = clientesRecentesAdapter
    }

    private fun loadRecentData() {
        // Carrega artigos recentes
        lifecycleScope.launch {
            artigoDao.getRecentArtigos(Constants.RECENT_LIMIT).collectLatest { artigos ->
                val resumoArtigos = artigos.map { artigo ->
                    ResumoArtigoItem(
                        id = artigo.id,
                        nome = artigo.nome,
                        preco = artigo.preco
                    )
                }
                artigosRecentesAdapter.updateArtigos(resumoArtigos)
                Log.d("ArquivosRecentesActivity", "Artigos recentes carregados: ${resumoArtigos.size}")
            }
        }

        // Carrega clientes recentes
        lifecycleScope.launch {
            clienteDao.getRecentClientes(Constants.RECENT_LIMIT).collectLatest { clientes ->
                val resumoClientes = clientes.map { cliente ->
                    ResumoClienteItem(
                        id = cliente.id,
                        nome = cliente.nome,
                        telefone = cliente.telefone
                    )
                }
                clientesRecentesAdapter.updateClientes(resumoClientes)
                Log.d("ArquivosRecentesActivity", "Clientes recentes carregados: ${resumoClientes.size}")
            }
        }
    }

    private fun removeArtigoFromRecents(artigo: ResumoArtigoItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Atualiza o campo 'guardarFatura' no banco de dados para false
                artigoDao.updateGuardarFatura(artigo.id, false)
                withContext(Dispatchers.Main) {
                    showToast("Artigo '${artigo.nome}' removido dos recentes.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Erro ao remover artigo dos recentes: ${e.message}")
                    Log.e("ArquivosRecentesActivity", "Erro ao remover artigo: ${e.message}", e)
                }
            }
        }
    }

    private fun removeClienteFromRecents(cliente: ResumoClienteItem) {
        // Para clientes, a lógica de "recentes" pode ser mais complexa.
        // Se você não tiver um campo específico como "guardarFatura" para clientes,
        // pode precisar redefinir o campo 'data_cadastro' ou 'ultimo_acesso'
        // para uma data antiga, ou remover da lista de exibição localmente.
        // Como não há um campo 'guardarFatura' no Cliente, vou apenas simular a remoção da exibição.
        showToast("Lógica para remover cliente dos recentes não implementada (apenas demonstração).")
        Log.d("ArquivosRecentesActivity", "Tentativa de remover cliente: ${cliente.nome}")
        // Se for para remover permanentemente, seria clienteDao.delete(cliente.id)
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
