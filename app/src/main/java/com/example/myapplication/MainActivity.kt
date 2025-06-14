package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var faturaAdapter: FaturaAdapter
    private val viewModel: MainActivityViewModel by viewModels()

    // Permissões
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val writeStorageGranted = permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val readMediaGranted = permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)

        if (cameraGranted && writeStorageGranted && readMediaGranted) {
            // Permissões concedidas, pode prosseguir com a operação (ex: escanear)
            showToast(getString(R.string.permission_granted))
        } else {
            showToast(getString(R.string.permission_denied_camera))
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission_needed_camera))
                .setMessage(getString(R.string.go_to_settings))
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


    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            // Cancelado
            showToast("Leitura de código de barras cancelada")
        } else {
            // Sucesso
            emitBeep()
            val scannedCode = result.contents
            showToast("Código escaneado: $scannedCode")
            // Iniciar SecondScreenActivity com o código escaneado
            val intent = Intent(this, SecondScreenActivity::class.java).apply {
                putExtra("SCANNED_BARCODE", scannedCode)
            }
            startSecondScreenLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left) // Aplica animação
        }
    }


    private val startSecondScreenLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadFaturas() // Recarrega as faturas após salvar/editar
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "" // Remover o título padrão para usar o TextView

        setupRecyclerView()
        setupObservers()
        setupListeners()

        checkAndRequestPermissions()

        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.fab.startAnimation(fadeInAnimation)
    }

    private fun setupRecyclerView() {
        faturaAdapter = FaturaAdapter(
            onItemClick = { faturaId ->
                val intent = Intent(this, SecondScreenActivity::class.java).apply {
                    putExtra("FATURA_ID", faturaId)
                }
                startSecondScreenLauncher.launch(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left) // Animação de entrada
            },
            onItemLongClick = { faturaId ->
                showDeleteConfirmationDialog(faturaId)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = faturaAdapter
            addItemDecoration(VerticalSpaceItemDecoration(16))
        }
    }

    private fun setupObservers() {
        viewModel.faturas.observe(this, Observer { faturas ->
            faturaAdapter.submitList(faturas)
            binding.progressBar.visibility = View.GONE
            binding.textNoInvoices.visibility = if (faturas.isEmpty()) View.VISIBLE else View.GONE
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.errorMessage.observe(this, Observer { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun setupListeners() {
        binding.fab.setOnClickListener {
            val intent = Intent(this, SecondScreenActivity::class.java)
            startSecondScreenLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left) // Animação de entrada
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.loadFaturas(query)
                hideKeyboard()
                return true
            }

            override fun onOnQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank() && binding.searchView.query.isNotEmpty()) {
                    viewModel.loadFaturas() // Recarrega todas as faturas se o texto for limpo
                }
                return false
            }
        })

        binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank() && binding.searchView.query.isNotEmpty()) {
                    viewModel.loadFaturas() // Recarrega todas as faturas se o texto for limpo
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchView.setOnCloseListener {
            viewModel.loadFaturas() // Recarrega todas as faturas ao fechar a busca
            false
        }
    }

    private fun showDeleteConfirmationDialog(faturaId: Long) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_permanently_title))
            .setMessage(getString(R.string.dialog_delete_permanently_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteFatura(faturaId)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFaturas() // Recarrega as faturas sempre que a atividade é retomada
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_scan_barcode -> {
                startBarcodeScan()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, DefinicoesActivity::class.java))
                true
            }
            R.id.action_financial_summary -> {
                startActivity(Intent(this, ResumoFinanceiroActivity::class.java))
                true
            }
            R.id.action_trash -> {
                startActivity(Intent(this, LixeiraActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startBarcodeScan() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
        } else {
            val options = ScanOptions()
            options.setPrompt("Aponte a câmera para um código de barras")
            options.setBeepEnabled(true)
            options.setOrientationLocked(false) // Permite rotação
            options.setBarcodeImageEnabled(false)
            barcodeLauncher.launch(options)
        }
    }

    private fun emitBeep() {
        val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // Android 9 e abaixo
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { // Android 10, 11, 12
            // READ_EXTERNAL_STORAGE e WRITE_EXTERNAL_STORAGE são "soft-deprecated" e não mais necessárias para acesso a arquivos específicos da app
            // Mas para acesso a Downloads, MediaStore ainda pode precisar
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 e acima
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}