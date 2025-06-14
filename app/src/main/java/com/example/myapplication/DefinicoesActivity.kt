package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityDefinicoesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DefinicoesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDefinicoesBinding
    private lateinit var dbHelper: ClienteDbHelper

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        if (writeGranted && readGranted) {
            Toast.makeText(this, "Permissões de armazenamento concedidas.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissões de armazenamento negadas.", Toast.LENGTH_SHORT).show()
            showPermissionDeniedDialog()
        }
    }

    private val backupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                performBackup(it)
            }
        } ?: Toast.makeText(this, "Backup cancelado.", Toast.LENGTH_SHORT).show()
    }

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            showRestoreConfirmationDialog(it)
        } ?: Toast.makeText(this, "Restauração cancelada.", Toast.LENGTH_SHORT).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDefinicoesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        setupListeners()
        checkAndRequestPermissions()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.cardInformacoesEmpresa.setOnClickListener {
            startActivity(Intent(this, InformacoesEmpresaActivity::class.java))
        }
        binding.cardInstrucoesPagamento.setOnClickListener {
            startActivity(Intent(this, InstrucoesPagamentoActivity::class.java))
        }
        binding.cardNotas.setOnClickListener {
            startActivity(Intent(this, NotasActivity::class.java))
        }
        binding.cardClientesBloqueados.setOnClickListener {
            startActivity(Intent(this, ClientesBloqueadosActivity::class.java))
        }
        binding.cardExportarDados.setOnClickListener {
            startActivity(Intent(this, ExportActivity::class.java))
        }
        binding.cardImportarDados.setOnClickListener {
            // Importar CSV de clientes, etc.
            // Aqui você precisaria de uma lógica para importar CSV de clientes
            // ou de outros dados específicos, que não está diretamente no DBHelper
            Toast.makeText(this, getString(R.string.importar_clientes_csv), Toast.LENGTH_SHORT).show()
        }
        binding.cardBackupDados.setOnClickListener {
            if (checkStoragePermissions()) {
                val backupFileName = "${Constants.BACKUP_ZIP_NAME}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
                backupLauncher.launch(backupFileName)
            } else {
                requestStoragePermissions()
            }
        }
        binding.cardRestaurarDados.setOnClickListener {
            if (checkStoragePermissions()) {
                restoreLauncher.launch(arrayOf("application/zip"))
            } else {
                requestStoragePermissions()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // For older Android versions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        } else {
            // Android 10 (API 29) e superior, Scoped Storage lida com isso.
            // Para arquivos da app, não precisa de permissão. Para arquivos compartilhados,
            // usa MediaStore ou Storage Access Framework. O backup/restore via SAF já lida.
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Com Scoped Storage, permissão não é necessária para arquivos da app
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE))
        } else {
            // Permissão não necessária para Scoped Storage
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_denied_storage))
            .setMessage(getString(R.string.permission_needed_storage))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private suspend fun performBackup(uri: Uri) {
        try {
            val dbPath = this.getDatabasePath(Constants.DATABASE_NAME).absolutePath
            val sharedPrefsPath = File(this.filesDir.parentFile, "shared_prefs/${Constants.PREFS_NAME}.xml").absolutePath
            val imagesDir = File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), Constants.APP_DIR_NAME).absolutePath

            val dbFile = File(dbPath)
            val sharedPrefsFile = File(sharedPrefsPath)
            val imagesFolder = File(imagesDir)

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipStream ->

                    // Adicionar banco de dados
                    if (dbFile.exists()) {
                        val entry = ZipEntry(dbFile.name)
                        zipStream.putNextEntry(entry)
                        FileInputStream(dbFile).use { it.copyTo(zipStream) }
                        zipStream.closeEntry()
                    }

                    // Adicionar SharedPreferences
                    if (sharedPrefsFile.exists()) {
                        val entry = ZipEntry(sharedPrefsFile.name)
                        zipStream.putNextEntry(entry)
                        FileInputStream(sharedPrefsFile).use { it.copyTo(zipStream) }
                        zipStream.closeEntry()
                    }

                    // Adicionar imagens (recursivamente)
                    if (imagesFolder.exists() && imagesFolder.isDirectory) {
                        zipFolder(imagesFolder, imagesFolder.name + File.separator, zipStream)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DefinicoesActivity, getString(R.string.backup_success, uri.lastPathSegment), Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("Backup", "Erro no backup: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DefinicoesActivity, getString(R.string.backup_error, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun zipFolder(folder: File, parentPath: String, zipStream: ZipOutputStream) {
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                zipFolder(file, parentPath + file.name + File.separator, zipStream)
            } else {
                val entry = ZipEntry(parentPath + file.name)
                zipStream.putNextEntry(entry)
                FileInputStream(file).use { it.copyTo(zipStream) }
                zipStream.closeEntry()
            }
        }
    }

    private fun showRestoreConfirmationDialog(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.restore_data))
            .setMessage(getString(R.string.import_data_warning))
            .setPositiveButton(getString(R.string.restore)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    performRestore(uri)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private suspend fun performRestore(uri: Uri) {
        try {
            val appFilesDir = this.filesDir
            contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry: ZipEntry?
                    while (zipStream.nextEntry.also { entry = it } != null) {
                        val outputFile = File(appFilesDir.parentFile, entry!!.name)
                        if (entry!!.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs() // Create parent directories if they don't exist
                            FileOutputStream(outputFile).use { it2 ->
                                zipStream.copyTo(it2)
                            }
                        }
                        zipStream.closeEntry()
                    }
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DefinicoesActivity, getString(R.string.restore_success), Toast.LENGTH_LONG).show()
                // Pode ser necessário reiniciar o app após a restauração do DB e SharedPreferences
            }
        } catch (e: Exception) {
            Log.e("Restore", "Erro na restauração: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DefinicoesActivity, getString(R.string.restore_error, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }
}