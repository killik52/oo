package com.example.myapplication

import android.app.Activity
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
import com.example.myapplication.database.dao.FaturaDao
import com.example.myapplication.database.dao.FaturaLixeiraDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class LixeiraActivity : AppCompatActivity() {

    private lateinit var faturaLixeiraRecyclerView: RecyclerView
    private lateinit var faturaLixeiraAdapter: FaturaLixeiraAdapter
    private lateinit var voltarButton: TextView

    // DAOs do Room
    private lateinit var faturaLixeiraDao: FaturaLixeiraDao
    private lateinit var faturaDao: FaturaDao // Para restaurar faturas

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lixeira)

        // Inicializa os DAOs do Room
        val application = application as MyApplication
        faturaLixeiraDao = application.database.faturaLixeiraDao()
        faturaDao = application.database.faturaDao()

        initComponents()
        setupListeners()
        setupRecyclerView()
        loadFaturasLixeira()
    }

    private fun initComponents() {
        faturaLixeiraRecyclerView = findViewById(R.id.faturaLixeiraRecyclerView)
        voltarButton = findViewById(R.id.voltarButton)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        faturaLixeiraRecyclerView.layoutManager = LinearLayoutManager(this)
        faturaLixeiraAdapter = FaturaLixeiraAdapter(
            onItemClick = { faturaLixeira ->
                showRestoreAndDeleteDialog(faturaLixeira)
            }
        )
        faturaLixeiraRecyclerView.adapter = faturaLixeiraAdapter

        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        faturaLixeiraRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))
    }

    private fun loadFaturasLixeira() {
        lifecycleScope.launch {
            faturaLixeiraDao.getAllFaturasLixeira().collectLatest { faturas ->
                faturaLixeiraAdapter.updateFaturas(faturas)
                Log.d("LixeiraActivity", "Faturas na lixeira carregadas: ${faturas.size}")
            }
        }
    }

    private fun showRestoreAndDeleteDialog(faturaLixeira: FaturaLixeira) {
        val options = arrayOf("Restaurar Fatura", "Excluir Permanentemente")
        AlertDialog.Builder(this)
            .setTitle("Opções para Fatura ${faturaLixeira.numeroFatura}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> restoreFatura(faturaLixeira)
                    1 -> deleteFaturaPermanentemente(faturaLixeira)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun restoreFatura(faturaLixeira: FaturaLixeira) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Restauração")
            .setMessage("Tem certeza que deseja restaurar a fatura ${faturaLixeira.numeroFatura}? Ela será movida de volta para a lista principal.")
            .setPositiveButton("Restaurar") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val fatura = Fatura(
                            // Note: ID será auto-gerado para a nova entrada na tabela de faturas ativas
                            numeroFatura = faturaLixeira.numeroFatura,
                            clienteNome = faturaLixeira.clienteNome ?: "Desconhecido",
                            clienteId = null, // Se você tiver o clienteId na FaturaLixeira, use-o
                            subtotal = faturaLixeira.subtotal ?: 0.0,
                            desconto = faturaLixeira.desconto ?: 0.0,
                            descontoPercent = faturaLixeira.descontoPercent ?: false,
                            taxaEntrega = faturaLixeira.taxaEntrega ?: 0.0,
                            saldoDevedor = faturaLixeira.saldoDevedor ?: 0.0,
                            data = faturaLixeira.data ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis()),
                            observacao = faturaLixeira.notasJson, // Mapeando de volta o que foi salvo
                            foiEnviada = false // Fatura restaurada não foi enviada novamente
                        )
                        val newFaturaId = faturaDao.insert(fatura)

                        if (newFaturaId != -1L) {
                            faturaLixeiraDao.delete(faturaLixeira)
                            withContext(Dispatchers.Main) {
                                showToast("Fatura '${faturaLixeira.numeroFatura}' restaurada com sucesso.")
                                loadFaturasLixeira() // Recarrega a lista da lixeira
                                val returnIntent = Intent().apply {
                                    putExtra("fatura_restaurada", true)
                                    putExtra("fatura_id", newFaturaId)
                                }
                                setResult(Activity.RESULT_OK, returnIntent)
                                finish() // Opcional: fechar a LixeiraActivity
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showToast("Erro ao restaurar fatura.")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao restaurar fatura: ${e.message}")
                            Log.e("LixeiraActivity", "Erro ao restaurar fatura: ${e.message}", e)
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun deleteFaturaPermanentemente(fatura: FaturaLixeira) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão Permanente")
            .setMessage("Tem certeza que deseja EXCLUIR PERMANENTEMENTE a fatura ${fatura.numeroFatura}? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        faturaLixeiraDao.delete(fatura)
                        withContext(Dispatchers.Main) {
                            showToast("Fatura '${fatura.numeroFatura}' excluída permanentemente.")
                            // loadFaturasLixeira() // collectLatest já irá atualizar
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao excluir fatura permanentemente: ${e.message}")
                            Log.e("LixeiraActivity", "Erro ao excluir permanentemente: ${e.message}", e)
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
