package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu // Adicionado import
import android.view.MenuItem // Adicionado import
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // Adicionado import
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ClienteContract.ClienteEntry
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityClienteBinding // Corrigido o caminho do binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClienteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClienteBinding
    private lateinit var dbHelper: ClienteDbHelper
    private var clienteId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar) // Corrigido: `binding.toolbar`
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.client_details) // String resource

        clienteId = intent.getLongExtra("CLIENTE_ID", -1L)
        if (clienteId != -1L) {
            loadCliente(clienteId)
        } else {
            Toast.makeText(this, "ID do cliente inválido.", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.buttonEditCliente.setOnClickListener { // Corrigido: `binding.buttonEditCliente`
            val intent = Intent(this, CriarNovoClienteActivity::class.java).apply {
                putExtra("CLIENTE_ID", clienteId)
            }
            startActivity(intent)
        }

        binding.buttonDeleteCliente.setOnClickListener { // Corrigido: `binding.buttonDeleteCliente`
            showDeleteConfirmationDialog()
        }

        binding.buttonToggleBlock.setOnClickListener { // Corrigido: `binding.buttonToggleBlock`
            toggleClienteBlockStatus()
        }
    }

    private fun loadCliente(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val cliente = dbHelper.getClienteById(id)
            val isBlocked = dbHelper.isClienteBlocked(id)
            withContext(Dispatchers.Main) {
                cliente?.let {
                    binding.textClienteNome.text = it.nome
                    binding.textClienteEndereco.text = it.endereco ?: "N/A"
                    binding.textClienteTelefone.text = it.telefone ?: "N/A"
                    binding.textClienteEmail.text = it.email ?: "N/A"
                    binding.textClienteCpfCnpj.text = when {
                        !it.cpf.isNullOrBlank() -> "CPF: ${it.cpf}"
                        !it.cnpj.isNullOrBlank() -> "CNPJ: ${it.cnpj}"
                        else -> "N/A"
                    }
                    binding.textClienteLogradouro.text = it.logradouro ?: "N/A"
                    binding.textClienteNumero.text = it.numero ?: "N/A"
                    binding.textClienteComplemento.text = it.complemento ?: "N/A"
                    binding.textClienteBairro.text = it.bairro ?: "N/A"
                    binding.textClienteCidade.text = it.cidade ?: "N/A"
                    binding.textClienteEstado.text = it.estado ?: "N/A"
                    binding.textClienteCep.text = it.cep ?: "N/A"

                    updateBlockButtonState(isBlocked)
                } ?: run {
                    Toast.makeText(this@ClienteActivity, "Cliente não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_client_dialog_title))
            .setMessage(getString(R.string.delete_client_dialog_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteCliente()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteCliente() {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = dbHelper.deleteCliente(clienteId)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@ClienteActivity, getString(R.string.client_deleted_success), Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK) // Indica que algo foi alterado
                    finish()
                } else {
                    Toast.makeText(this@ClienteActivity, getString(R.string.client_deleted_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleClienteBlockStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            val isBlocked = dbHelper.isClienteBlocked(clienteId)
            val success: Boolean
            val messageResId: Int

            if (isBlocked) {
                success = dbHelper.removeBlockedClient(clienteId)
                messageResId = if (success) R.string.unblock_client_success else R.string.unblock_client_error
            } else {
                success = dbHelper.addBlockedClient(clienteId)
                messageResId = if (success) R.string.block_client_success else R.string.block_client_error
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ClienteActivity, getString(messageResId), Toast.LENGTH_SHORT).show()
                updateBlockButtonState(!isBlocked) // Atualiza o estado visual do botão
            }
        }
    }

    private fun updateBlockButtonState(isBlocked: Boolean) {
        if (isBlocked) {
            binding.buttonToggleBlock.text = getString(R.string.unblock_client_button_text) // String resource
            binding.buttonToggleBlock.setBackgroundColor(ContextCompat.getColor(this, R.color.green_700))
        } else {
            binding.buttonToggleBlock.text = getString(R.string.block_client_button_text) // String resource
            binding.buttonToggleBlock.setBackgroundColor(ContextCompat.getColor(this, R.color.red_700))
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarregar cliente caso a edição tenha ocorrido em CriarNovoClienteActivity
        if (clienteId != -1L) {
            loadCliente(clienteId)
        }
    }

    // Menu options (already existed in previous code, ensure they are still correct)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_cliente_details, menu) // Ensure this menu XML exists
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_client -> { // Ensure these IDs exist in your menu_cliente_details.xml
                binding.buttonEditCliente.performClick()
                true
            }
            R.id.action_delete_client -> {
                binding.buttonDeleteCliente.performClick()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}