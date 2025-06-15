package com.example.myapplication

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.FaturaNotaDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotasActivity : AppCompatActivity() {

    private lateinit var notasRecyclerView: RecyclerView
    private lateinit var notaAdapter: NotaAdapter
    private lateinit var adicionarNotaButton: Button
    private lateinit var voltarButton: TextView

    // DAO do Room
    private lateinit var faturaNotaDao: FaturaNotaDao

    private var faturaId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notas)

        // Inicializa o DAO do Room
        val application = application as MyApplication
        faturaNotaDao = application.database.faturaNotaDao()

        faturaId = intent.getLongExtra("fatura_id", -1)
        if (faturaId == -1L) {
            showToast("ID da fatura inválido.")
            finish()
            return
        }

        initComponents()
        setupListeners()
        setupRecyclerView()
        loadNotas()
    }

    private fun initComponents() {
        notasRecyclerView = findViewById(R.id.notasRecyclerView)
        adicionarNotaButton = findViewById(R.id.adicionarNotaButton)
        voltarButton = findViewById(R.id.voltarButton)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        adicionarNotaButton.setOnClickListener {
            showAddNoteDialog()
        }
    }

    private fun setupRecyclerView() {
        notasRecyclerView.layoutManager = LinearLayoutManager(this)
        notaAdapter = NotaAdapter(this, emptyList()) { nota ->
            showDeleteConfirmationDialog(nota)
        }
        notasRecyclerView.adapter = notaAdapter

        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        notasRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))
    }

    private fun loadNotas() {
        lifecycleScope.launch {
            faturaNotaDao.getNotesForFatura(faturaId).collectLatest { notas ->
                // O DAO retorna FaturaNotaEntity, precisamos mapear para a lista de String de conteúdo
                val notaContents = notas.map { it.notaConteudo }
                notaAdapter.updateNotes(notaContents)
                Log.d("NotasActivity", "Notas carregadas: ${notaContents.size}")
            }
        }
    }

    private fun showAddNoteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_nota, null)
        val noteEditText: EditText = dialogView.findViewById(R.id.noteEditText)

        AlertDialog.Builder(this)
            .setTitle("Adicionar Nova Nota")
            .setView(dialogView)
            .setPositiveButton("Adicionar") { dialog, _ ->
                val noteContent = noteEditText.text.toString().trim()
                if (noteContent.isNotEmpty()) {
                    addNota(noteContent)
                } else {
                    showToast("A nota não pode estar vazia.")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addNota(noteContent: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val faturaNota = FaturaNotaEntity(
                    faturaId = faturaId,
                    notaConteudo = noteContent
                )
                val newRowId = faturaNotaDao.insert(faturaNota)
                withContext(Dispatchers.Main) {
                    if (newRowId != -1L) {
                        showToast("Nota adicionada.")
                        // loadNotas() // collectLatest já irá atualizar
                    } else {
                        showToast("Erro ao adicionar nota.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Erro ao adicionar nota: ${e.message}")
                    Log.e("NotasActivity", "Erro ao adicionar nota: ${e.message}", e)
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(noteContent: String) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Nota")
            .setMessage("Tem certeza que deseja excluir esta nota?")
            .setPositiveButton("Excluir") { dialog, _ ->
                deleteNota(noteContent)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteNota(noteContent: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rowsDeleted = faturaNotaDao.deleteNoteByContent(noteContent)
                withContext(Dispatchers.Main) {
                    if (rowsDeleted > 0) {
                        showToast("Nota excluída.")
                        // loadNotas() // collectLatest já irá atualizar
                    } else {
                        showToast("Erro ao excluir nota.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Erro ao excluir nota: ${e.message}")
                    Log.e("NotasActivity", "Erro ao excluir nota: ${e.message}", e)
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
