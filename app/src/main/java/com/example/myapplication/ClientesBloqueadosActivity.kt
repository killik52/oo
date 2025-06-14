package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityClientesBloqueadosBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientesBloqueadosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientesBloqueadosBinding
    private lateinit var dbHelper: ClienteDbHelper
    private lateinit var adapter: ClienteBloqueadoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientesBloqueadosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.blocked_clients)

        setupRecyclerView()
        loadBlockedClients()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        adapter = ClienteBloqueadoAdapter(
            onUnblockClick = { cliente ->
                showUnblockConfirmationDialog(cliente)
            },
            onItemClick = { cliente ->
                val intent = Intent(this, ClienteActivity::class.java).apply {
                    putExtra("CLIENTE_ID", cliente.id)
                }
                startActivity(intent)
            }
        )
        binding.recyclerViewBlockedClients.apply {
            layoutManager = LinearLayoutManager(this@ClientesBloqueadosActivity)
            adapter = adapter
            addItemDecoration(VerticalSpaceItemDecoration(16))
        }
    }

    private fun loadBlockedClients() {
        lifecycleScope.launch(Dispatchers.IO) {
            val blockedClients = dbHelper.getAllBlockedClients()
            withContext(Dispatchers.Main) {
                if (blockedClients.isNotEmpty()) {
                    adapter.submitList(blockedClients)
                    binding.textNoBlockedClients.visibility = View.GONE
                } else {
                    binding.textNoBlockedClients.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showUnblockConfirmationDialog(cliente: Cliente) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.unblock_client_success)) // Usando string para título de desbloqueio
            .setMessage("Tem certeza que deseja desbloquear ${cliente.nome}?")
            .setPositiveButton(getString(R.string.unblock_client_success)) { dialog, which ->
                unblockClient(cliente.id)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun unblockClient(clienteId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = dbHelper.removeBlockedClient(clienteId)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@ClientesBloqueadosActivity, getString(R.string.unblock_client_success), Toast.LENGTH_SHORT).show()
                    loadBlockedClients() // Recarrega a lista
                } else {
                    Toast.makeText(this@ClientesBloqueadosActivity, getString(R.string.unblock_client_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBlockedClients() // Recarrega a lista sempre que a atividade é retomada
    }
}