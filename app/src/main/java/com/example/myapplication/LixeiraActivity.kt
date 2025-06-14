package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.FaturaLixeiraContract.FaturaLixeiraEntry
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityLixeiraBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LixeiraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLixeiraBinding
    private lateinit var dbHelper: ClienteDbHelper
    private lateinit var faturaResumidaAdapter: FaturaResumidaAdapter // Usar FaturaResumidaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLixeiraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.trash_title) // Adicionar string resource

        setupRecyclerView()
        loadDeletedFaturas()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        faturaResumidaAdapter = FaturaResumidaAdapter( // Usar FaturaResumidaAdapter
            onItemClick = { faturaId ->
                // Ao clicar, pode-se mostrar detalhes ou perguntar para restaurar
                showRestoreConfirmationDialog(faturaId)
            },
            onItemLongClick = { faturaId ->
                showDeletePermanentlyConfirmationDialog(faturaId)
            }
        )
        binding.recyclerViewLixeira.apply {
            layoutManager = LinearLayoutManager(this@LixeiraActivity)
            adapter = faturaResumidaAdapter
            addItemDecoration(VerticalSpaceItemDecoration(16))
        }
    }

    private fun loadDeletedFaturas() {
        lifecycleScope.launch(Dispatchers.IO) {
            val deletedFaturas = dbHelper.getAllFaturasInLixeira()
            withContext(Dispatchers.Main) {
                if (deletedFaturas.isNotEmpty()) {
                    faturaResumidaAdapter.submitList(deletedFaturas)
                    binding.textNoDeletedInvoices.visibility = View.GONE
                    binding.recyclerViewLixeira.visibility = View.VISIBLE
                } else {
                    binding.textNoDeletedInvoices.visibility = View.VISIBLE
                    binding.recyclerViewLixeira.visibility = View.GONE
                }
            }
        }
    }

    private fun showRestoreConfirmationDialog(faturaId: Long) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_restore_title))
            .setMessage(getString(R.string.dialog_restore_message))
            .setPositiveButton(getString(R.string.restore)) { _, _ ->
                restoreFatura(faturaId)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun restoreFatura(faturaId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = dbHelper.restoreFaturaFromLixeira(faturaId)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@LixeiraActivity, getString(R.string.restore_invoice_success), Toast.LENGTH_SHORT).show()
                    loadDeletedFaturas() // Recarregar a lista
                } else {
                    Toast.makeText(this@LixeiraActivity, getString(R.string.restore_invoice_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeletePermanentlyConfirmationDialog(faturaId: Long) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_permanently_title))
            .setMessage(getString(R.string.dialog_delete_permanently_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteFaturaPermanently(faturaId)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteFaturaPermanently(faturaId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = dbHelper.deleteFaturaPermanently(faturaId)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@LixeiraActivity, getString(R.string.delete_invoice_permanently_success), Toast.LENGTH_SHORT).show()
                    loadDeletedFaturas() // Recarregar a lista
                } else {
                    Toast.makeText(this@LixeiraActivity, getString(R.string.delete_invoice_permanently_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}