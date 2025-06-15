package com.example.myapplication// app/src/main/java/com/example/myapplication/MainActivity.kt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityMainBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.application.MyApplication
import database.dao.FaturaDao // Importe o DAO

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    // Remova dbHelper e adicione os DAOs que você precisa
    private lateinit var faturaDao: FaturaDao // Adicione esta linha
    private lateinit var clienteDao: ClienteDao
    private lateinit var faturaItemDao: FaturaItemDao
    private lateinit var faturaLixeiraDao: FaturaLixeiraDao


    private var isGridViewVisible = false
    private val SECOND_SCREEN_REQUEST_CODE = 1
    private val STORAGE_PERMISSION_CODE = 100
    private val LIXEIRA_REQUEST_CODE = 1002
    private lateinit var faturaAdapter: FaturaResumidaAdapter
    private var isSearchActive = false
    private var mediaPlayer: MediaPlayer? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            // showToast("Leitura cancelada")
        } else {
            val barcodeValue = result.contents
            Log.d("MainActivity", "Código de barras lido (bruto): '$barcodeValue'")
            emitBeep()
            openInvoiceByBarcode(barcodeValue)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MainActivity", "onCreate chamado com ViewBinding")

        // Inicialize os DAOs usando a instância do AppDatabase da sua Application
        val application = application as MyApplication
        faturaDao = application.database.faturaDao()
        clienteDao = application.database.clienteDao()
        faturaItemDao = application.database.faturaItemDao()
        faturaLixeiraDao = application.database.faturaLixeiraDao()

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.beep)
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                Log.e("MainActivity", "Erro no MediaPlayer: what=$what, extra=$extra")
                showToast("Erro ao inicializar o som de beep")
                true
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao inicializar MediaPlayer: ${e.message}")
            showToast("Erro ao carregar o som de beep")
        }

        binding.recyclerViewFaturas.layoutManager = LinearLayoutManager(this)
        faturaAdapter = FaturaResumidaAdapter(
            this,
            onItemClick = { fatura ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val faturaDb = faturaDao.getFaturaById(fatura.id)
                        if (faturaDb != null) {
                            withContext(Dispatchers.Main) {
                                val intent = Intent(
                                    this@MainActivity,
                                    SecondScreenActivity::class.java
                                ).apply {
                                    putExtra("fatura_id", faturaDb.id)
                                    putExtra("foi_enviada", faturaDb.foiEnviada)
                                }
                                startActivityForResult(intent, SECOND_SCREEN_REQUEST_CODE)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showToast("Fatura não encontrada.")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("MainActivity", "Erro ao abrir fatura: ${e.message}")
                            showToast("Erro ao abrir fatura: ${e.message}")
                        }
                    }
                }
            },
            onItemLongClick = { fatura ->
                Log.d("MainActivity", "Iniciando exclusão da fatura ID=${fatura.id}")
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val faturaOriginal = faturaDao.getFaturaById(fatura.id)
                        if (faturaOriginal != null) {
                            val faturaParaLixeira = FaturaLixeira(
                                numeroFatura = faturaOriginal.numeroFatura,
                                clienteNome = faturaOriginal.clienteNome,
                                artigosJson = faturaOriginal.observacao, // Mapeando artigos antigos para observação para reter dados
                                subtotal = faturaOriginal.subtotal,
                                desconto = faturaOriginal.desconto,
                                descontoPercent = faturaOriginal.descontoPercent,
                                taxaEntrega = faturaOriginal.taxaEntrega,
                                saldoDevedor = faturaOriginal.saldoDevedor,
                                data = faturaOriginal.data,
                                fotosJson = null, // Você precisará migrar as fotos para cá, ou criar uma tabela de fotos separada para a lixeira
                                notasJson = faturaOriginal.observacao // Você precisará migrar as notas para cá
                            )

                            val newRowId = faturaLixeiraDao.insert(faturaParaLixeira)
                            if (newRowId != -1L) {
                                val rowsDeleted = faturaDao.deleteFaturaById(fatura.id)
                                withContext(Dispatchers.Main) {
                                    if (rowsDeleted > 0) {
                                        viewModel.carregarFaturas()
                                    } else {
                                        showToast("Erro ao mover fatura para a lixeira.")
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    showToast("Erro ao mover fatura para a lixeira.")
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showToast("Fatura não encontrada.")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e(
                                "MainActivity",
                                "Erro ao mover fatura para a lixeira: ${e.message}",
                                e
                            )
                            showToast("Erro ao mover fatura: ${e.message}")
                        }
                    }
                }
            }
        )
        binding.recyclerViewFaturas.adapter = faturaAdapter

        if (binding.recyclerViewFaturas.itemDecorationCount > 0) {
            for (i in (binding.recyclerViewFaturas.itemDecorationCount - 1) downTo 0) {
                binding.recyclerViewFaturas.getItemDecorationAt(i)?.let {
                    binding.recyclerViewFaturas.removeItemDecoration(it)
                }
            }
        }
        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        binding.recyclerViewFaturas.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))

        viewModel.faturas.observe(this) { faturas ->
            faturaAdapter.updateFaturas(faturas)
            Log.d("MainActivity", "Adapter atualizado com dados do ViewModel. Total: ${faturas.size}")
        }

        val menuOptionsAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.menu_options,
            R.layout.item_menu
        )
        binding.menuGridView.adapter = menuOptionsAdapter

        binding.menuGridView.setOnItemClickListener { _, _, position, _ ->
            try {
                val selectedOption = menuOptionsAdapter.getItem(position).toString()
                when (selectedOption) {
                    "Fatura" -> toggleGridView()
                    "Cliente" -> { // Corrigido para "Cliente"
                        startActivity(Intent(this, ListarClientesActivity::class.java))
                        toggleGridView()
                    }
                    "Artigo" -> {
                        startActivity(Intent(this, ListarArtigosActivity::class.java))
                        toggleGridView()
                    }
                    "Lixeira" -> {
                        startActivityForResult(Intent(this, LixeiraActivity::class.java), LIXEIRA_REQUEST_CODE)
                        toggleGridView()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao abrir atividade: ${e.message}")
                showToast("Erro ao abrir a tela: ${e.message}")
            }
        }

        binding.faturaTitleContainer.setOnClickListener {
            toggleGridView()
        }

        binding.dollarIcon.setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.CODE_128)
                setPrompt("Escaneie o código de barras no PDF")
                setCameraId(0)
                setBeepEnabled(false)
                setOrientationLocked(false)
            }
            barcodeLauncher.launch(options)
        }

        binding.homeIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.moreIcon.setOnClickListener {
            val intent = Intent(this, DefinicoesActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.searchButton.setOnClickListener {
            Log.d("MainActivity", "Botão de busca clicado")
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.search_dialog_title))

            val input = EditText(this)
            input.hint = getString(R.string.search_dialog_hint)
            builder.setView(input)

            builder.setPositiveButton(getString(R.string.search_dialog_positive_button)) { dialog, _ ->
                val query = input.text.toString().trim()
                Log.d("MainActivity", "Botão 'Pesquisar' clicado no diálogo, termo: '$query'")
                if (query.isEmpty()) {
                    showToast(getString(R.string.search_empty_query_message))
                    viewModel.carregarFaturas()
                    isSearchActive = false
                } else {
                    buscarFaturas(query)
                    isSearchActive = true
                }
                dialog.dismiss()
            }
            builder.setNegativeButton(getString(R.string.search_dialog_negative_button)) { dialog, _ ->
                Log.d("MainActivity", "Botão 'Cancelar' clicado no diálogo")
                dialog.cancel()
            }
            builder.show()
        }

        binding.graficosButton.setOnClickListener {
            Log.d("MainActivity", "Botão de Gráficos clicado")
            val intent = Intent(this, ResumoFinanceiroActivity::class.java)
            startActivity(intent)
        }

        logDatabaseContents()

        binding.addButton.setOnClickListener {
            // showToast("Botão 'Adicionar' clicado com animação!")
            requestStorageAndCameraPermissions()
        }
    }

    private fun emitBeep() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                    try {
                        player.prepare()
                    } catch (e: IllegalStateException){
                        Log.e("MainActivity", "Erro ao preparar MediaPlayer após stop: ${e.message}")
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer.create(this, R.raw.beep)
                    }
                }
                player.start()
            } ?: run {
                mediaPlayer = MediaPlayer.create(this, R.raw.beep)
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao reproduzir som de beep: ${e.message}")
        }
    }

    private fun openInvoiceByBarcode(barcodeValue: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cleanedBarcodeValue = barcodeValue.trim()
                val faturaIdFromBarcode = cleanedBarcodeValue.toLongOrNull() ?: cleanedBarcodeValue.replace("[^0-9]".toRegex(), "").toLongOrNull()

                if (faturaIdFromBarcode == null) {
                    withContext(Dispatchers.Main) {
                        showToast("Código de barras inválido: $cleanedBarcodeValue")
                    }
                    return@launch
                }
                abrirFaturaPorId(faturaIdFromBarcode, cleanedBarcodeValue)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Erro ao abrir fatura por código de barras: ${e.message}")
                    showToast("Erro ao abrir fatura: ${e.message}")
                }
            }
        }
    }

    private fun abrirFaturaPorId(faturaId: Long, barcodeScaneado: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val fatura = faturaDao.getFaturaById(faturaId)
            if (fatura != null) {
                withContext(Dispatchers.Main) {
                    Log.d("MainActivity", "Fatura encontrada com ID: $faturaId. Foi enviada: ${fatura.foiEnviada}")
                    val intent = Intent(this@MainActivity, SecondScreenActivity::class.java).apply {
                        putExtra("fatura_id", fatura.id)
                        putExtra("foi_enviada", fatura.foiEnviada)
                    }
                    startActivityForResult(intent, SECOND_SCREEN_REQUEST_CODE)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Log.w("MainActivity", "Fatura não encontrada com ID: $faturaId (código de barras: $barcodeScaneado)")
                    showToast("Fatura não encontrada para o código de barras: $barcodeScaneado")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume chamado")
        if (!isSearchActive) {
            viewModel.carregarFaturas()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart chamado")
    }

    override fun onBackPressed() {
        if (isSearchActive) {
            Log.d("MainActivity", "Botão Voltar pressionado, busca estava ativa. Restaurando lista completa de faturas.")
            viewModel.carregarFaturas()
            isSearchActive = false
        } else if (isGridViewVisible) {
            toggleGridView()
        } else {
            super.onBackPressed()
        }
    }

    private fun buscarFaturas(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val tempFaturasList = mutableListOf<FaturaResumidaItem>()
            try {
                Log.d("MainActivity", "Iniciando busca de faturas com o termo: '$query'")

                // Use o Flow diretamente do DAO e colete os dados
                faturaDao.getFaturamentoMensal(
                    startDate = null, // Ajuste conforme a necessidade da sua busca
                    endDate = null,   // Ajuste conforme a necessidade da sua busca
                    searchQuery = query
                ).collect { faturasFromDb ->
                    faturasFromDb.forEach { faturaData ->
                        // Para cada item retornado pela sua query de busca (que pode ser FaturamentoMensal ou outra, dependendo da query real)
                        // Você precisará adaptar o mapeamento para FaturaResumidaItem
                        // Assumindo que você quer mapear algo como o número da fatura, cliente e saldo devedor
                        // A sua query atual em FaturaDao.kt para `getFaturamentoMensal` agrupa por mês/ano,
                        // então o resultado não é diretamente Fatura, mas sim FaturamentoMensal.
                        // Para buscar faturas individuais, você precisaria de uma query diferente.

                        // Por agora, vamos simular o mapeamento para FaturaResumidaItem
                        // Isso vai depender de como você quer que os resultados da busca sejam exibidos
                        val fatura = faturaDao.getFaturaById(faturaData.id) // Você precisaria que `id` existisse no seu FaturamentoMensal ou buscar por outros campos
                        if (fatura != null) {
                            val serialNumbers = mutableListOf<String?>()
                            // Isso é um exemplo, você precisa popular serialNumbers do fatura.artigosJson
                            // se você tiver a string de artigos salva na Fatura Entity.
                            // Para simplificar, vou assumir que você tem a string de artigos na Fatura, ou buscar FaturaItem entities.

                            // Exemplo de como serialNumbers seria populado se artigos fossem uma string
                            // fatura.artigos?.split("|")?.forEach { artigoStr ->
                            //    val parts = artigoStr.split(",")
                            //    if (parts.size >= 5) {
                            //        serialNumbers.add(parts[4].takeIf { s -> s.isNotEmpty() && s != "null" })
                            //    }
                            //}

                            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val formattedData = try {
                                val date = inputFormat.parse(fatura.data)
                                outputFormat.format(date!!)
                            } catch (e: Exception) {
                                fatura.data
                            }

                            tempFaturasList.add(
                                FaturaResumidaItem(
                                    fatura.id,
                                    fatura.numeroFatura ?: "N/A",
                                    fatura.clienteNome,
                                    serialNumbers, // Precisa ser populado corretamente
                                    fatura.saldoDevedor,
                                    formattedData,
                                    fatura.foiEnviada
                                )
                            )
                        }
                    }
                }


                withContext(Dispatchers.Main) {
                    faturaAdapter.updateFaturas(tempFaturasList)
                    Log.d("MainActivity", "Lista atualizada com ${tempFaturasList.size} faturas após busca.")
                    if (tempFaturasList.isEmpty()) {
                        showToast("Nenhuma fatura encontrada para '$query'.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Erro ao buscar faturas: ${e.message}", e)
                    showToast("Erro ao buscar faturas: ${e.message}")
                    viewModel.carregarFaturas()
                    isSearchActive = false
                }
            }
        }
    }

    private fun logDatabaseContents() {
        lifecycleScope.launch(Dispatchers.IO) {
            faturaDao.getAllFaturas().collect { faturas ->
                Log.d("DB_CONTENT_FATURAS", "--- Conteúdo da Tabela Faturas ---")
                faturas.forEach { fatura ->
                    Log.d("DB_CONTENT_FATURAS", "ID: ${fatura.id}, Num: ${fatura.numeroFatura}, Cliente: ${fatura.clienteNome}, Saldo: ${fatura.saldoDevedor}, Data: ${fatura.data}, Enviada: ${fatura.foiEnviada}")
                }
            }
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkCameraPermission(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return cameraPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStorageAndCameraPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!checkCameraPermission()) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (!checkStoragePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), STORAGE_PERMISSION_CODE)
        } else {
            openSecondScreen()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            var allEssentialPermissionsGranted = true
            for (i in permissions.indices) {
                val permission = permissions[i]
                val grantResult = grantResults[i]

                if (permission == Manifest.permission.CAMERA ||
                    permission == Manifest.permission.READ_MEDIA_IMAGES ||
                    permission == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allEssentialPermissionsGranted = false
                    }
                }
            }

            if (allEssentialPermissionsGranted) {
                showToast("Permissões concedidas!")
                openSecondScreen()
            } else {
                showToast("Algumas permissões essenciais foram negadas. Funcionalidades podem ser limitadas.")
                if (permissions.any { p ->
                        (p == Manifest.permission.CAMERA || p == Manifest.permission.READ_MEDIA_IMAGES || p == Manifest.permission.READ_EXTERNAL_STORAGE) &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(this, p) &&
                                ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED
                    }) {
                    AlertDialog.Builder(this)
                        .setTitle("Permissões Necessárias")
                        .setMessage("Este aplicativo precisa de permissões de câmera e armazenamento para funcionar corretamente. Por favor, habilite-as nas configurações do aplicativo.")
                        .setPositiveButton("Configurações") { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else if (permissions.any { p ->
                        (p == Manifest.permission.CAMERA || p == Manifest.permission.READ_MEDIA_IMAGES || p == Manifest.permission.READ_EXTERNAL_STORAGE) &&
                                ActivityCompat.shouldShowRequestPermissionRationale(this, p)
                    }) {
                    AlertDialog.Builder(this)
                        .setTitle("Permissões Requeridas")
                        .setMessage("As permissões de câmera e armazenamento são necessárias para adicionar fotos e salvar faturas. Por favor, conceda as permissões.")
                        .setPositiveButton("OK") { _, _ ->
                            requestStorageAndCameraPermissions()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }
    }

    private fun openSecondScreen() {
        val intent = Intent(this, SecondScreenActivity::class.java)
        startActivityForResult(intent, SECOND_SCREEN_REQUEST_CODE)
    }

    private fun toggleGridView() {
        if (isGridViewVisible) {
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            slideUp.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    binding.menuGridView.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            binding.menuGridView.startAnimation(slideUp)
        } else {
            binding.menuGridView.visibility = View.VISIBLE
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            binding.menuGridView.startAnimation(slideDown)
        }
        isGridViewVisible = !isGridViewVisible
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SECOND_SCREEN_REQUEST_CODE && resultCode == RESULT_OK) {
            viewModel.carregarFaturas()
            isSearchActive = false
        } else if (requestCode == LIXEIRA_REQUEST_CODE && resultCode == RESULT_OK) {
            val faturaRestaurada = data?.getBooleanExtra("fatura_restaurada", false) ?: false
            val restoredFaturaId = data?.getLongExtra("fatura_id", -1L) ?: -1L

            if (faturaRestaurada && restoredFaturaId != -1L) {
                viewModel.carregarFaturas()
                isSearchActive = false
                val intent = Intent(this, SecondScreenActivity::class.java).apply {
                    putExtra("fatura_id", restoredFaturaId)
                }
                startActivityForResult(intent, SECOND_SCREEN_REQUEST_CODE)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        // Remova a linha abaixo
        // dbHelper?.close()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
        Log.d("MainActivity", "onDestroy chamado")
    }
}