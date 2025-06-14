package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ArtigoContract.ArtigoEntry
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityCriarNovoArtigoBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CriarNovoArtigoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCriarNovoArtigoBinding
    private lateinit var dbHelper: ClienteDbHelper
    private var artigoId: Long = -1 // -1 indica novo artigo
    private var selectedArtigoItem: Artigo? = null

    private var currentPhotoUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            currentPhotoUri?.let { uri ->
                recognizeTextFromImage(uri)
            }
        } else {
            Toast.makeText(this, "Captura de imagem cancelada ou falhou.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCriarNovoArtigoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.add_new_article)

        artigoId = intent.getLongExtra("ARTIGO_ID", -1L)
        if (artigoId != -1L) {
            loadArtigo(artigoId)
        }

        setupListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.buttonSaveArtigo.setOnClickListener {
            saveArtigo()
        }

        binding.buttonScanSerial.setOnClickListener {
            checkCameraPermission()
        }
    }

    private fun loadArtigo(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val artigo = dbHelper.getArtigoById(id)
            withContext(Dispatchers.Main) {
                artigo?.let {
                    selectedArtigoItem = it
                    binding.editTextArtigoNome.setText(it.nome)
                    binding.editTextArtigoPreco.setText(it.precoUnitario.toString())
                    binding.editTextArtigoDescricao.setText(it.descricao)
                    binding.editTextNumeroSerie.setText(it.numeroSerie)
                    binding.buttonSaveArtigo.text = getString(R.string.save_article)
                    supportActionBar?.title = getString(R.string.article_name)
                } ?: run {
                    Toast.makeText(this@CriarNovoArtigoActivity, "Artigo não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun saveArtigo() {
        val nome = binding.editTextArtigoNome.text.toString().trim()
        val precoStr = binding.editTextArtigoPreco.text.toString().trim()
        val descricao = binding.editTextArtigoDescricao.text.toString().trim()
        val numeroSerie = binding.editTextNumeroSerie.text.toString().trim()

        if (nome.isEmpty() || precoStr.isEmpty()) {
            Toast.makeText(this, "Nome e Preço são obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        val precoUnitario = precoStr.toDoubleOrNull()
        if (precoUnitario == null || precoUnitario <= 0) {
            Toast.makeText(this, "Preço unitário inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        val artigo = Artigo(
            id = artigoId,
            nome = nome,
            precoUnitario = precoUnitario,
            quantidade = 1,
            descricao = if (descricao.isNotEmpty()) descricao else null,
            numeroSerie = if (numeroSerie.isNotEmpty()) numeroSerie else null
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                val articleAdded: Boolean
                if (artigoId == -1L) {
                    Log.d("CriarNovoArtigo", "Tentando adicionar novo artigo: $artigo")
                    articleAdded = dbHelper.addArtigo(artigo)
                    Log.d("CriarNovoArtigo", "addArtigo retornou: $articleAdded")
                } else {
                    Log.d("CriarNovoArtigo", "Tentando atualizar artigo: $artigo")
                    articleAdded = dbHelper.updateArtigo(artigo)
                    Log.d("CriarNovoArtigo", "updateArtigo retornou: $articleAdded")
                }

                db.setTransactionSuccessful()

                withContext(Dispatchers.Main) {
                    if (articleAdded) {
                        Toast.makeText(this@CriarNovoArtigoActivity, getString(R.string.article_added_success), Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@CriarNovoArtigoActivity, getString(R.string.article_added_error), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CriarNovoArtigo", "Erro ao salvar artigo: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CriarNovoArtigoActivity, getString(R.string.article_added_error) + ": ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                db.endTransaction()
            }
        }
    }


    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_needed_camera))
            .setMessage(getString(R.string.permission_needed_camera))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_denied_camera))
            .setMessage(getString(R.string.permission_denied_camera))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun startCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, "Erro ao criar arquivo de imagem.", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also {
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                it
            )
            takePictureLauncher.launch(currentPhotoUri)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    private fun recognizeTextFromImage(imageUri: Uri) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(this, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val resultText = visionText.text
                    if (resultText.isNotEmpty()) {
                        binding.editTextNumeroSerie.setText(resultText)
                        Toast.makeText(this, "Texto reconhecido e preenchido.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, getString(R.string.scan_failed_no_text), Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TextRecognition", "Erro ao reconhecer texto: ${e.message}", e)
                    Toast.makeText(this, getString(R.string.scan_failed_processing), Toast.LENGTH_SHORT).show()
                }
        } catch (e: IOException) {
            Log.e("TextRecognition", "Erro ao carregar imagem para reconhecimento: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar imagem.", Toast.LENGTH_SHORT).show()
        }
    }
}