package com.example.myapplication

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.FaturaFotoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GaleriaFotosActivity : AppCompatActivity() {

    private lateinit var fotosGridView: GridView
    private lateinit var fotoAdapter: FotoAdapter
    private lateinit var voltarButton: TextView

    // DAO do Room
    private lateinit var faturaFotoDao: FaturaFotoDao

    private var faturaId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galeria_fotos)

        // Inicializa o DAO do Room
        val application = application as MyApplication
        faturaFotoDao = application.database.faturaFotoDao()

        faturaId = intent.getLongExtra("fatura_id", -1)
        if (faturaId == -1L) {
            showToast("ID da fatura inválido.")
            finish()
            return
        }

        initComponents()
        setupListeners()
        setupGridView()
        loadFotos()
    }

    private fun initComponents() {
        fotosGridView = findViewById(R.id.fotosGridView)
        voltarButton = findViewById(R.id.voltarButton)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupGridView() {
        fotoAdapter = FotoAdapter(this, emptyList()) { photoPath ->
            showFullscreenPhoto(photoPath)
        }
        fotosGridView.adapter = fotoAdapter

        fotosGridView.setOnItemLongClickListener { _, _, position, _ ->
            val photoPath = fotoAdapter.getItem(position) as String
            showDeleteConfirmationDialog(photoPath)
            true
        }
    }

    private fun loadFotos() {
        lifecycleScope.launch {
            faturaFotoDao.getPhotosForFatura(faturaId).collectLatest { fotos ->
                // O DAO retorna FaturaFotoEntity, precisamos mapear para a lista de String de paths
                val photoPaths = fotos.map { it.photoPath }
                fotoAdapter.updatePhotos(photoPaths)
                Log.d("GaleriaFotosActivity", "Fotos carregadas: ${photoPaths.size}")
            }
        }
    }

    private fun showFullscreenPhoto(photoPath: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_fullscreen_photo, null)
        val fullImageView: ImageView = dialogView.findViewById(R.id.fullImageView)
        fullImageView.setImageURI(Uri.parse(photoPath))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<ImageView>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(photoPath: String) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Foto")
            .setMessage("Tem certeza que deseja excluir esta foto?")
            .setPositiveButton("Excluir") { dialog, _ ->
                deletePhoto(photoPath)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletePhoto(photoPath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rowsDeleted = faturaFotoDao.deletePhotoByPath(photoPath)
                withContext(Dispatchers.Main) {
                    if (rowsDeleted > 0) {
                        showToast("Foto excluída.")
                        // loadFotos() // O collectLatest já irá recarregar automaticamente
                    } else {
                        showToast("Erro ao excluir foto.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Erro ao excluir foto: ${e.message}")
                    Log.e("GaleriaFotosActivity", "Erro ao excluir foto: ${e.message}", e)
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
