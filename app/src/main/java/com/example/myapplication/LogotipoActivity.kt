package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class LogotipoActivity : AppCompatActivity() {

    private lateinit var voltarButton: TextView
    private lateinit var logoImageView: ImageView
    private lateinit var escolherLogoButton: Button
    private lateinit var removerLogoButton: Button

    private val REQUEST_CODE_PICK_IMAGE = 100
    private val PERMISSION_REQUEST_CODE = 200

    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logotipo)

        initComponents()
        setupListeners()
        loadSavedLogo()
    }

    private fun initComponents() {
        voltarButton = findViewById(R.id.voltarButton)
        logoImageView = findViewById(R.id.logoImageView)
        escolherLogoButton = findViewById(R.id.escolherLogoButton)
        removerLogoButton = findViewById(R.id.removerLogoButton)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        escolherLogoButton.setOnClickListener {
            checkAndRequestPermissions()
        }

        removerLogoButton.setOnClickListener {
            removeLogo()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            openImageChooser()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (allGranted) {
                openImageChooser()
            } else {
                showToast("Permissão de leitura de armazenamento negada. Não é possível escolher a imagem.")
                if (permissions.any { p ->
                        (p == Manifest.permission.READ_MEDIA_IMAGES || p == Manifest.permission.READ_EXTERNAL_STORAGE) &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(this, p) &&
                                ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED
                    }) {
                    AlertDialog.Builder(this)
                        .setTitle("Permissão Necessária")
                        .setMessage("Este aplicativo precisa de permissão para ler imagens para definir o logotipo. Por favor, habilite-a nas configurações do aplicativo.")
                        .setPositiveButton("Configurações") { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    logoImageView.setImageBitmap(bitmap)
                    saveLogoToInternalStorage(bitmap)
                } catch (e: Exception) {
                    Log.e("LogotipoActivity", "Erro ao carregar imagem: ${e.message}", e)
                    showToast("Erro ao carregar imagem.")
                }
            }
        }
    }

    private fun saveLogoToInternalStorage(bitmap: Bitmap) {
        val fileName = "app_logo.png"
        val file = File(filesDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            // Salvar o URI do arquivo interno no SharedPreferences para persistência
            getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("logo_uri", file.toURI().toString())
                .apply()
            showToast("Logotipo salvo com sucesso!")
        } catch (e: IOException) {
            Log.e("LogotipoActivity", "Erro ao salvar logo: ${e.message}", e)
            showToast("Erro ao salvar logotipo.")
        }
    }

    private fun loadSavedLogo() {
        val savedUriString = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("logo_uri", null)
        savedUriString?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                if (uri.scheme == "file") { // Verifique se é um URI de arquivo
                    val file = File(uri.path)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        logoImageView.setImageBitmap(bitmap)
                        selectedImageUri = uri // Mantenha o URI selecionado atualizado
                    } else {
                        Log.w("LogotipoActivity", "Arquivo de logo não encontrado: ${uri.path}")
                        removeLogoFromPreferences() // Limpar se o arquivo não existir
                    }
                } else {
                    // Tente carregar via ContentResolver para outros esquemas de URI
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        logoImageView.setImageBitmap(bitmap)
                        selectedImageUri = uri
                    } ?: run {
                        Log.w("LogotipoActivity", "URI de logo inválido ou arquivo não acessível: $uriString")
                        removeLogoFromPreferences()
                    }
                }
            } catch (e: Exception) {
                Log.e("LogotipoActivity", "Erro ao carregar logo salvo: ${e.message}", e)
                removeLogoFromPreferences()
            }
        }
    }

    private fun removeLogo() {
        val fileName = "app_logo.png"
        val file = File(filesDir, fileName)
        if (file.exists()) {
            if (file.delete()) {
                removeLogoFromPreferences()
                logoImageView.setImageResource(R.drawable.ic_launcher_foreground) // Voltar ao padrão
                selectedImageUri = null
                showToast("Logotipo removido.")
            } else {
                showToast("Erro ao remover logotipo do armazenamento.")
            }
        } else {
            removeLogoFromPreferences()
            logoImageView.setImageResource(R.drawable.ic_launcher_foreground) // Voltar ao padrão
            selectedImageUri = null
            showToast("Nenhum logotipo para remover.")
        }
    }

    private fun removeLogoFromPreferences() {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .edit()
            .remove("logo_uri")
            .apply()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
