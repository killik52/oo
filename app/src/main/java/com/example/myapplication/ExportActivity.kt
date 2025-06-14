package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityExportBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ExportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExportBinding
    private lateinit var dbHelper: ClienteDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.export_data)

        setupListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.buttonExportPdf.setOnClickListener {
            exportPdf()
        }
        binding.buttonExportData.setOnClickListener {
            exportDatabase()
        }
        binding.buttonExportResumoPdf.setOnClickListener {
            // Este botão precisaria de uma lógica para gerar o PDF de resumo financeiro.
            // Se generateResumoPdf estiver em PdfGenerationUtils, chame de lá
            // Exemplo: PdfGenerationUtils.generateResumoPdf(this, ...)
            Toast.makeText(this, "Funcionalidade de exportar resumo PDF não implementada.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportPdf() {
        // Lógica de exportação de PDF de fatura individual
        // Geralmente, isso é feito a partir da tela da fatura
        Toast.makeText(this, "Selecione uma fatura para exportar o PDF.", Toast.LENGTH_SHORT).show()
    }

    private fun exportDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
            val fileName = "database_backup_$timeStamp.db"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadsDir, Constants.APP_DIR_NAME)
            if (!appDir.exists()) {
                appDir.mkdirs()
            }
            val outputFile = File(appDir, fileName)

            try {
                dbHelper.exportDatabase(outputFile)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExportActivity, getString(R.string.export_data_success, outputFile.absolutePath), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExportActivity, getString(R.string.export_data_error, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}