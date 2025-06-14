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
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityGaleriaFotosBinding
import com.example.myapplication.databinding.DialogFullscreenPhotoBinding
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GaleriaFotosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGaleriaFotosBinding
    private lateinit var fotoAdapter: FotoAdapter
    private var faturaId: Long = -1L
    private lateinit var dbHelper: ClienteDbHelper

    private var currentPhotoUri: Uri? = null // Para a foto tirada pela câmera

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            takePhotoFromCamera()
        } else {
            showPermissionDeniedDialog(Manifest.permission.CAMERA, getString(R.string.permission_needed_camera))
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            pickImageFromGallery()
        } else {
            showPermissionDeniedDialog(Manifest.permission.READ_EXTERNAL_STORAGE, getString(R.string.permission_needed_storage))
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val path = uri.toString()
                    dbHelper.addFaturaPhoto(faturaId, path)
                    withContext(Dispatchers.Main) {
                        loadFaturaPhotos()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Captura de imagem cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                val path = it.toString()
                dbHelper.addFaturaPhoto(faturaId, path)
                withContext(Dispatchers.Main) {
                    loadFaturaPhotos()
                }
            }
        } ?: Toast.makeText(this, "Seleção de imagem cancelada.", Toast.LENGTH_SHORT).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGaleriaFotosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.galeria_fotos)

        faturaId = intent.getLongExtra("FATURA_ID", -1L)
        if (faturaId == -1L) {
            Toast.makeText(this, "ID da fatura inválido.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        setupListeners()
        loadFaturaPhotos()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        fotoAdapter = FotoAdapter(
            fotos = mutableListOf(),
            onFotoClick = { uri ->
                showFullscreenPhoto(uri)
            },
            onFotoLongClick = { uri ->
                showDeletePhotoConfirmationDialog(uri)
            }
        )
        binding.recyclerViewFotos.apply {
            layoutManager = GridLayoutManager(this@GaleriaFotosActivity, 3) // 3 colunas de fotos
            adapter = fotoAdapter
        }
    }

    private fun setupListeners() {
        binding.buttonPickFromGallery.setOnClickListener {
            checkStoragePermissionsAndPickImage()
        }
        binding.buttonTakePhoto.setOnClickListener {
            checkCameraPermissionAndTakePhoto()
        }
    }

    private fun loadFaturaPhotos() {
        lifecycleScope.launch(Dispatchers.IO) {
            val photosPaths = dbHelper.getFaturaPhotos(faturaId)
            val photoUris = photosPaths.map { Uri.parse(it) }.toMutableList()
            withContext(Dispatchers.Main) {
                if (photoUris.isNotEmpty()) {
                    fotoAdapter.submitList(photoUris) // Usar submitList para ListAdapter
                    binding.textNoPhotos.visibility = View.GONE
                    binding.recyclerViewFotos.visibility = View.VISIBLE
                } else {
                    fotoAdapter.submitList(emptyList()) // Limpar lista se não houver fotos
                    binding.textNoPhotos.visibility = View.VISIBLE
                    binding.recyclerViewFotos.visibility = View.GONE
                }
            }
        }
    }

    private fun showFullscreenPhoto(uri: Uri) {
        val dialogBinding = DialogFullscreenPhotoBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        val photoView: PhotoView = dialogBinding.photoView
        Glide.with(this)
            .load(uri)
            .into(photoView)

        dialogBinding.buttonClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeletePhotoConfirmationDialog(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_photo_title)) // Adicionar string resource
            .setMessage(getString(R.string.delete_photo_message)) // Adicionar string resource
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deletePhoto(uri)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deletePhoto(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = dbHelper.deleteFaturaPhoto(faturaId, uri.toString())
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@GaleriaFotosActivity, getString(R.string.photo_deleted_success), Toast.LENGTH_SHORT).show()
                    loadFaturaPhotos()
                } else {
                    Toast.makeText(this@GaleriaFotosActivity, getString(R.string.photo_deleted_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                takePhotoFromCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionDeniedDialog(Manifest.permission.CAMERA, getString(R.string.permission_needed_camera))
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun takePhotoFromCamera() {
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

    private fun checkStoragePermissionsAndPickImage() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestStoragePermissionLauncher.launch(permissions.toTypedArray())
        } else {
            pickImageFromGallery()
        }
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun showPermissionDeniedDialog(permission: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_denied)) // Adicionar string resource
            .setMessage(message)
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}