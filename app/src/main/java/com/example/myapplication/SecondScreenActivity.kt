package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.ArtigoDao
import com.example.myapplication.database.dao.ClienteDao
import com.example.myapplication.database.dao.FaturaDao
import com.example.myapplication.database.dao.FaturaItemDao
import com.example.myapplication.database.dao.FaturaNotaDao
import com.example.myapplication.database.dao.FaturaFotoDao
import com.example.myapplication.database.dao.InformacoesEmpresaDao
import com.example.myapplication.database.dao.InstrucoesPagamentoDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecondScreenActivity : AppCompatActivity() {

    private lateinit var clienteSpinner: Spinner
    private lateinit var numeroFaturaEditText: EditText
    private lateinit var dataFaturaTextView: TextView
    private lateinit var subtotalTextView: TextView
    private lateinit var descontoEditText: EditText
    private lateinit var descontoPercentCheckBox: CheckBox
    private lateinit var taxaEntregaEditText: EditText
    private lateinit var saldoDevedorTextView: TextView
    private lateinit var adicionarArtigoButton: Button
    private lateinit var artigosRecyclerView: RecyclerView
    private lateinit var faturaItensAdapter: FaturaItemAdapter
    private lateinit var salvarFaturaButton: Button
    private lateinit var voltarButton: TextView
    private lateinit var galeriaButton: ImageButton
    private lateinit var notasButton: ImageButton
    private lateinit var sendButton: ImageButton
    private lateinit var cameraButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var faturasRecentesButton: ImageButton // Adicionado conforme layout original
    private lateinit var gerarPdfSwitch: ImageView // Alterado de Switch para ImageView

    // DAOs do Room
    private lateinit var faturaDao: FaturaDao
    private lateinit var clienteDao: ClienteDao
    private lateinit var artigoDao: ArtigoDao
    private lateinit var faturaItemDao: FaturaItemDao
    private lateinit var faturaNotaDao: FaturaNotaDao
    private lateinit var faturaFotoDao: FaturaFotoDao
    private lateinit var informacoesEmpresaDao: InformacoesEmpresaDao
    private lateinit var instrucoesPagamentoDao: InstrucoesPagamentoDao

    private var currentFaturaId: Long = -1 // -1 para nova fatura, ID para edição
    private var isFaturaSent: Boolean = false // Indica se a fatura já foi enviada
    private var currentPhotoPath: String? = null // Para a foto tirada

    private val REQUEST_IMAGE_CAPTURE = 1
    private val PICK_IMAGE_REQUEST = 2
    private val CAMERA_PERMISSION_CODE = 101
    private val STORAGE_PERMISSION_CODE = 102

    private var faturaItemsList = mutableListOf<FaturaItem>() // Lista de itens para a fatura

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val faturaFotoEntity = FaturaFotoEntity(faturaId = currentFaturaId, photoPath = path)
                        faturaFotoDao.insert(faturaFotoEntity)
                        withContext(Dispatchers.Main) {
                            showToast("Foto salva com sucesso!")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao salvar foto: ${e.message}")
                            Log.e("SecondScreenActivity", "Erro ao salvar foto no DB: ${e.message}", e)
                        }
                    }
                }
            }
        } else {
            showToast("Foto não capturada.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_screen)

        // Inicializa os DAOs do Room
        val application = application as MyApplication
        faturaDao = application.database.faturaDao()
        clienteDao = application.database.clienteDao()
        artigoDao = application.database.artigoDao()
        faturaItemDao = application.database.faturaItemDao()
        faturaNotaDao = application.database.faturaNotaDao()
        faturaFotoDao = application.database.faturaFotoDao()
        informacoesEmpresaDao = application.database.informacoesEmpresaDao()
        instrucoesPagamentoDao = application.database.instrucoesPagamentoDao()

        initComponents()
        setupListeners()
        setupRecyclerView()

        currentFaturaId = intent.getLongExtra("fatura_id", -1)
        isFaturaSent = intent.getBooleanExtra("foi_enviada", false)

        if (currentFaturaId != -1L) {
            loadFaturaData(currentFaturaId)
            // Desabilita edição se a fatura já foi enviada
            setEditability(isFaturaSent)
            salvarFaturaButton.text = getString(R.string.atualizar_fatura)
        } else {
            // Nova fatura
            dataFaturaTextView.text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            faturaItemsList = mutableListOf() // Garante que a lista de itens está vazia para nova fatura
            updateTotals()
            setEditability(false)
        }

        loadClientesIntoSpinner()
        Log.d("SecondScreenActivity", "onCreate: currentFaturaId=$currentFaturaId, isFaturaSent=$isFaturaSent")
    }

    private fun initComponents() {
        clienteSpinner = findViewById(R.id.clienteSpinner)
        numeroFaturaEditText = findViewById(R.id.numeroFaturaEditText)
        dataFaturaTextView = findViewById(R.id.dataFaturaTextView)
        subtotalTextView = findViewById(R.id.subtotalTextView)
        descontoEditText = findViewById(R.id.descontoEditText)
        descontoPercentCheckBox = findViewById(R.id.descontoPercentCheckBox)
        taxaEntregaEditText = findViewById(R.id.taxaEntregaEditText)
        saldoDevedorTextView = findViewById(R.id.saldoDevedorTextView)
        adicionarArtigoButton = findViewById(R.id.adicionarArtigoButton)
        artigosRecyclerView = findViewById(R.id.artigosRecyclerView)
        salvarFaturaButton = findViewById(R.id.salvarFaturaButton)
        voltarButton = findViewById(R.id.voltarButton)
        galeriaButton = findViewById(R.id.galeriaButton)
        notasButton = findViewById(R.id.notasButton)
        sendButton = findViewById(R.id.sendButton)
        cameraButton = findViewById(R.id.cameraButton)
        deleteButton = findViewById(R.id.deleteButton)
        faturasRecentesButton = findViewById(R.id.faturasRecentesButton)
        gerarPdfSwitch = findViewById(R.id.gerarPdfSwitch) // É um ImageView, não um Switch
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        adicionarArtigoButton.setOnClickListener {
            if (currentFaturaId == -1L && faturaItemsList.isEmpty()) {
                showToast("Por favor, salve a fatura primeiro antes de adicionar artigos.")
            } else {
                val intent = Intent(this, ThirdScreenActivity::class.java).apply {
                    putExtra("fatura_id", currentFaturaId) // Passa o ID da fatura para adicionar itens
                    // Se for uma fatura nova, faturaId será -1. ThirdScreenActivity precisará lidar com isso.
                    // Para simplificar, vou passar os itens atuais para ThirdScreenActivity carregar/editar
                    putExtra("fatura_itens_json", Gson().toJson(faturaItemsList))
                    putExtra("is_fatura_sent", isFaturaSent) // Passa o estado de envio
                }
                startActivityForResult(intent, Constants.REQUEST_ADD_ARTICLES)
            }
        }

        salvarFaturaButton.setOnClickListener {
            salvarOuAtualizarFatura()
        }

        descontoEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateTotals()
        }
        taxaEntregaEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateTotals()
        }
        descontoPercentCheckBox.setOnCheckedChangeListener { _, _ -> updateTotals() }

        galeriaButton.setOnClickListener {
            if (currentFaturaId != -1L) {
                val intent = Intent(this, GaleriaFotosActivity::class.java).apply {
                    putExtra("fatura_id", currentFaturaId)
                }
                startActivity(intent)
            } else {
                showToast("Salve a fatura antes de ver a galeria de fotos.")
            }
        }

        notasButton.setOnClickListener {
            if (currentFaturaId != -1L) {
                val intent = Intent(this, NotasActivity::class.java).apply {
                    putExtra("fatura_id", currentFaturaId)
                }
                startActivity(intent)
            } else {
                showToast("Salve a fatura antes de adicionar notas.")
            }
        }

        sendButton.setOnClickListener {
            if (currentFaturaId != -1L) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val empresaInfo = informacoesEmpresaDao.getInformacoesEmpresa().firstOrNull()
                    val instrucoesPagamento = instrucoesPagamentoDao.getInstrucoesPagamento().firstOrNull()
                    val cliente = clienteDao.getClienteById(clienteSpinner.selectedItemTag as Long)

                    withContext(Dispatchers.Main) {
                        if (empresaInfo == null || instrucoesPagamento == null || cliente == null) {
                            showToast("Por favor, preencha as informações da empresa e instruções de pagamento, e selecione um cliente válido.")
                            return@withContext
                        }

                        // Verifique o estado da switch/ImageView "Gerar PDF"
                        val gerarPdfAtivado = Constants.GERAR_PDF_ATIVADO // Lê do Constants

                        if (gerarPdfAtivado) {
                            // Requer permissões de armazenamento antes de gerar PDF
                            requestStoragePermissionForPdfGeneration(fatura = getFaturaFromUI())
                        } else {
                            showToast("Gerar PDF está desativado nas configurações.")
                        }
                    }
                }
            } else {
                showToast("Salve a fatura antes de enviá-la.")
            }
        }

        cameraButton.setOnClickListener {
            if (currentFaturaId != -1L) {
                checkCameraPermission()
            } else {
                showToast("Salve a fatura antes de tirar fotos.")
            }
        }

        deleteButton.setOnClickListener {
            if (currentFaturaId != -1L) {
                confirmDeleteFatura()
            } else {
                showToast("Não há fatura para excluir.")
            }
        }

        faturasRecentesButton.setOnClickListener {
            showToast("Botão 'Faturas Recentes' clicado. Funcionalidade não implementada diretamente aqui.")
        }
    }

    private fun setEditability(isSent: Boolean) {
        val editable = !isSent

        clienteSpinner.isEnabled = editable
        numeroFaturaEditText.isEnabled = editable
        dataFaturaTextView.isEnabled = editable
        descontoEditText.isEnabled = editable
        descontoPercentCheckBox.isEnabled = editable
        taxaEntregaEditText.isEnabled = editable
        adicionarArtigoButton.isEnabled = editable
        salvarFaturaButton.isEnabled = editable
        deleteButton.isEnabled = editable
        cameraButton.isEnabled = editable
        notasButton.isEnabled = editable
        galeriaButton.isEnabled = editable

        // Ação de envio/PDF ainda deve ser possível mesmo se enviada
        sendButton.isEnabled = true

        if (isSent) {
            salvarFaturaButton.text = getString(R.string.fatura_enviada)
            salvarFaturaButton.alpha = 0.5f // Diminui a opacidade para indicar desabilitado
        } else {
            salvarFaturaButton.text = getString(R.string.salvar_fatura)
            salvarFaturaButton.alpha = 1.0f // Restaura a opacidade
        }
    }

    private fun setupRecyclerView() {
        artigosRecyclerView.layoutManager = LinearLayoutManager(this)
        faturaItensAdapter = FaturaItemAdapter(
            onItemClick = { item ->
                if (!isFaturaSent) {
                    showEditArticleDialog(item)
                } else {
                    showToast("Fatura enviada. Não é possível editar itens.")
                }
            },
            onItemLongClick = { item ->
                if (!isFaturaSent) {
                    confirmRemoveArticle(item)
                } else {
                    showToast("Fatura enviada. Não é possível remover itens.")
                }
            }
        )
        artigosRecyclerView.adapter = faturaItensAdapter

        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        artigosRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))
    }

    private fun updateTotals() {
        var subtotal = 0.0
        faturaItemsList.forEach { item ->
            subtotal += item.quantidade * item.precoUnitario
        }
        subtotalTextView.text = String.format(Locale.getDefault(), "%.2f", subtotal)

        var desconto = 0.0
        val descontoStr = descontoEditText.text.toString().trim()
        if (descontoStr.isNotEmpty()) {
            desconto = descontoStr.toDoubleOrNull() ?: 0.0
        }

        var taxaEntrega = 0.0
        val taxaEntregaStr = taxaEntregaEditText.text.toString().trim()
        if (taxaEntregaStr.isNotEmpty()) {
            taxaEntrega = taxaEntregaStr.toDoubleOrNull() ?: 0.0
        }

        var saldoDevedor = subtotal

        if (descontoPercentCheckBox.isChecked) {
            saldoDevedor -= (subtotal * (desconto / 100))
        } else {
            saldoDevedor -= desconto
        }
        saldoDevedor += taxaEntrega

        saldoDevedorTextView.text = String.format(Locale.getDefault(), "%.2f", saldoDevedor)
    }

    private fun loadClientesIntoSpinner() {
        lifecycleScope.launch {
            clienteDao.getAllClientes().collectLatest { clientes ->
                val clienteNames = clientes.map { it.nome }
                val adapter = object : ArrayAdapter<String>(
                    this@SecondScreenActivity,
                    android.R.layout.simple_spinner_item,
                    clienteNames
                ) {
                    override fun getItemId(position: Int): Long {
                        // Retorna o ID do cliente correspondente para usar como tag
                        return clientes[position].id
                    }

                    override fun getItem(position: Int): String? {
                        return clientes[position].nome
                    }

                    // Sobrescreva getView e getDropDownView se precisar de um layout personalizado
                }
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                clienteSpinner.adapter = adapter

                // Se estiver editando uma fatura, selecione o cliente correto
                if (currentFaturaId != -1L) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val fatura = faturaDao.getFaturaById(currentFaturaId)
                        withContext(Dispatchers.Main) {
                            fatura?.clienteId?.let { clienteId ->
                                val position = clientes.indexOfFirst { it.id == clienteId }
                                if (position != -1) {
                                    clienteSpinner.setSelection(position)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadFaturaData(faturaId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val fatura = faturaDao.getFaturaById(faturaId)
            val faturaItems = faturaItemDao.getItemsForFatura(faturaId).firstOrNull() ?: emptyList()

            withContext(Dispatchers.Main) {
                if (fatura != null) {
                    numeroFaturaEditText.setText(fatura.numeroFatura)
                    dataFaturaTextView.text = formatFaturaDate(fatura.data) // Formate a data
                    descontoEditText.setText(String.format(Locale.getDefault(), "%.2f", fatura.desconto))
                    descontoPercentCheckBox.isChecked = fatura.descontoPercent
                    taxaEntregaEditText.setText(String.format(Locale.getDefault(), "%.2f", fatura.taxaEntrega))

                    faturaItemsList.clear()
                    faturaItemsList.addAll(faturaItems)
                    faturaItensAdapter.updateItems(faturaItemsList)
                    updateTotals()

                    // Recarregar o spinner para ter certeza de que o cliente selecionado é carregado
                    loadClientesIntoSpinner()
                    Log.d("SecondScreenActivity", "Fatura e itens carregados: ${fatura.numeroFatura}")
                } else {
                    showToast("Fatura não encontrada.")
                    finish()
                }
            }
        }
    }

    private fun formatFaturaDate(dateString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return try {
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString // Retorna a string original em caso de erro
        }
    }

    private fun salvarOuAtualizarFatura() {
        val numeroFatura = numeroFaturaEditText.text.toString().trim()
        val dataFatura = dataFaturaTextView.text.toString().trim() // Já está formatada
        val clienteNome = clienteSpinner.selectedItem?.toString()
        val clienteId = clienteSpinner.selectedItemId // ID do cliente

        if (clienteNome.isNullOrEmpty() || clienteId == -1L) {
            showToast("Por favor, selecione um cliente.")
            return
        }
        if (numeroFatura.isEmpty()) {
            showToast("O número da fatura é obrigatório.")
            return
        }

        // Parse valores
        val subtotal = subtotalTextView.text.toString().toDoubleOrNull() ?: 0.0
        val desconto = descontoEditText.text.toString().toDoubleOrNull() ?: 0.0
        val descontoPercent = descontoPercentCheckBox.isChecked
        val taxaEntrega = taxaEntregaEditText.text.toString().toDoubleOrNull() ?: 0.0
        val saldoDevedor = saldoDevedorTextView.text.toString().toDoubleOrNull() ?: 0.0

        if (faturaItemsList.isEmpty()) {
            showToast("Adicione pelo menos um artigo à fatura.")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val fatura = Fatura(
                id = if (currentFaturaId == -1L) 0 else currentFaturaId,
                numeroFatura = numeroFatura,
                clienteNome = clienteNome,
                clienteId = clienteId,
                subtotal = subtotal,
                desconto = desconto,
                descontoPercent = descontoPercent,
                taxaEntrega = taxaEntrega,
                saldoDevedor = saldoDevedor,
                data = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()), // Sempre salva a data/hora atual da modificação
                observacao = null, // Notas agora em FaturaNotaEntity
                foiEnviada = isFaturaSent // Mantém o status de envio
            )

            try {
                val returnedFaturaId: Long
                if (currentFaturaId == -1L) {
                    returnedFaturaId = faturaDao.insert(fatura)
                    if (returnedFaturaId != -1L) {
                        currentFaturaId = returnedFaturaId
                        withContext(Dispatchers.Main) {
                            showToast("Fatura salva com sucesso! ID: $currentFaturaId")
                            salvarFaturaButton.text = getString(R.string.atualizar_fatura)
                            // Agora que temos o ID da fatura, podemos associar os itens a ela
                            saveFaturaItems(returnedFaturaId)
                            setResult(Activity.RESULT_OK) // Notifica a MainActivity
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao salvar fatura.")
                        }
                    }
                } else {
                    faturaDao.update(fatura)
                    withContext(Dispatchers.Main) {
                        showToast("Fatura atualizada com sucesso!")
                        saveFaturaItems(currentFaturaId) // Atualiza/insere itens existentes
                        setResult(Activity.RESULT_OK) // Notifica a MainActivity
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("SecondScreenActivity", "Erro ao salvar/atualizar fatura: ${e.message}", e)
                    showToast("Erro ao salvar/atualizar fatura: ${e.message}")
                }
            }
        }
    }

    private suspend fun saveFaturaItems(faturaId: Long) {
        // Primeiro, exclua todos os itens antigos para esta fatura
        faturaItemDao.deleteItemsForFatura(faturaId)
        // Em seguida, insira todos os itens da lista atualizados
        val itemsToInsert = faturaItemsList.map { it.copy(faturaId = faturaId) } // Garante que o faturaId está correto
        faturaItemDao.insertAll(itemsToInsert)
        Log.d("SecondScreenActivity", "Itens da fatura (${itemsToInsert.size}) salvos para Fatura ID: $faturaId")
    }

    private fun confirmRemoveArticle(item: FaturaItem) {
        AlertDialog.Builder(this)
            .setTitle("Remover Artigo")
            .setMessage("Tem certeza que deseja remover este artigo da fatura?")
            .setPositiveButton("Remover") { dialog, _ ->
                faturaItemsList.remove(item)
                faturaItensAdapter.updateItems(faturaItemsList)
                updateTotals()
                showToast("Artigo removido.")
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditArticleDialog(item: FaturaItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_article_to_invoice, null)
        val nameEditText: EditText = dialogView.findViewById(R.id.articleNameEditText)
        val priceEditText: EditText = dialogView.findViewById(R.id.articlePriceEditText)
        val quantityEditText: EditText = dialogView.findViewById(R.id.articleQuantityEditText)
        val serialEditText: EditText = dialogView.findViewById(R.id.articleSerialEditText)
        val descriptionEditText: EditText = dialogView.findViewById(R.id.articleDescriptionEditText)

        nameEditText.setText(item.nomeArtigo)
        priceEditText.setText(DecimalFormat("0.00").format(item.precoUnitario))
        quantityEditText.setText(item.quantidade.toString())
        serialEditText.setText(item.numeroSerie)
        descriptionEditText.setText(item.descricao)

        AlertDialog.Builder(this)
            .setTitle("Editar Artigo")
            .setView(dialogView)
            .setPositiveButton("Salvar") { dialog, _ ->
                val newName = nameEditText.text.toString().trim()
                val newPrice = priceEditText.text.toString().toDoubleOrNull()
                val newQuantity = quantityEditText.text.toString().toIntOrNull()
                val newSerial = serialEditText.text.toString().trim().takeIf { it.isNotEmpty() }
                val newDescription = descriptionEditText.text.toString().trim().takeIf { it.isNotEmpty() }

                if (newName.isNotEmpty() && newPrice != null && newQuantity != null && newQuantity > 0) {
                    val updatedItem = item.copy(
                        nomeArtigo = newName,
                        precoUnitario = newPrice,
                        quantidade = newQuantity,
                        numeroSerie = newSerial,
                        descricao = newDescription
                    )
                    val index = faturaItemsList.indexOf(item)
                    if (index != -1) {
                        faturaItemsList[index] = updatedItem
                        faturaItensAdapter.updateItems(faturaItemsList)
                        updateTotals()
                        showToast("Artigo atualizado.")
                    }
                } else {
                    showToast("Preencha todos os campos obrigatórios corretamente (Nome, Preço, Quantidade).")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_ADD_ARTICLES && resultCode == Activity.RESULT_OK) {
            val updatedItemsJson = data?.getStringExtra("updated_fatura_itens_json")
            if (updatedItemsJson != null) {
                val type = object : TypeToken<MutableList<FaturaItem>>() {}.type
                faturaItemsList = Gson().fromJson(updatedItemsJson, type)
                faturaItensAdapter.updateItems(faturaItemsList)
                updateTotals()
            }
        }
    }

    private fun requestStoragePermissionForPdfGeneration(fatura: Fatura) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE // No Android 10+, this is for mediaStore access
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            generateAndSendPdf(fatura)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Permissão de armazenamento concedida. Gerando PDF...")
                // Tentar gerar e enviar PDF novamente após a permissão
                generateAndSendPdf(getFaturaFromUI())
            } else {
                showToast("Permissão de armazenamento negada. Não é possível gerar o PDF.")
            }
        } else if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                showToast("Permissão da câmera negada.")
            }
        }
    }

    private fun generateAndSendPdf(fatura: Fatura) {
        lifecycleScope.launch(Dispatchers.IO) {
            val pdfFile = PdfGenerationUtils.generatePdf(this@SecondScreenActivity, fatura)
            withContext(Dispatchers.Main) {
                if (pdfFile != null) {
                    showToast("PDF gerado com sucesso!")
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(fatura.clienteNome)) // Enviar para o email do cliente (se disponível)
                        putExtra(Intent.EXTRA_SUBJECT, "Fatura ${fatura.numeroFatura}")
                        putExtra(Intent.EXTRA_TEXT, "Prezado(a) ${fatura.clienteNome},\n\nSegue em anexo a fatura ${fatura.numeroFatura}.\n\nAtenciosamente,\n${informacoesEmpresaDao.getInformacoesEmpresa().firstOrNull()?.nomeEmpresa ?: ""}")

                        val uri = FileProvider.getUriForFile(this@SecondScreenActivity, applicationContext.packageName + ".fileprovider", pdfFile)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_STREAM, uri)
                    }
                    startActivity(Intent.createChooser(emailIntent, "Enviar PDF via..."))

                    // Marcar fatura como enviada após a tentativa de envio
                    markFaturaAsSent(fatura.id)
                } else {
                    showToast("Falha ao gerar PDF.")
                }
            }
        }
    }

    private fun markFaturaAsSent(faturaId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                faturaDao.markFaturaAsSent(faturaId)
                isFaturaSent = true
                withContext(Dispatchers.Main) {
                    showToast("Fatura marcada como enviada.")
                    setEditability(true) // Desabilita edição após envio
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Erro ao marcar fatura como enviada: ${e.message}")
                    Log.e("SecondScreenActivity", "Erro ao marcar fatura como enviada: ${e.message}", e)
                }
            }
        }
    }

    private fun getFaturaFromUI(): Fatura {
        val numeroFatura = numeroFaturaEditText.text.toString().trim()
        val dataFatura = dataFaturaTextView.text.toString().trim()
        val clienteNome = clienteSpinner.selectedItem?.toString() ?: ""
        val clienteId = clienteSpinner.selectedItemId

        val subtotal = subtotalTextView.text.toString().toDoubleOrNull() ?: 0.0
        val desconto = descontoEditText.text.toString().toDoubleOrNull() ?: 0.0
        val descontoPercent = descontoPercentCheckBox.isChecked
        val taxaEntrega = taxaEntregaEditText.text.toString().toDoubleOrNull() ?: 0.0
        val saldoDevedor = saldoDevedorTextView.text.toString().toDoubleOrNull() ?: 0.0

        return Fatura(
            id = if (currentFaturaId == -1L) 0 else currentFaturaId,
            numeroFatura = numeroFatura,
            clienteNome = clienteNome,
            clienteId = clienteId,
            subtotal = subtotal,
            desconto = desconto,
            descontoPercent = descontoPercent,
            taxaEntrega = taxaEntrega,
            saldoDevedor = saldoDevedor,
            data = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()), // Use a data atual ao gerar o PDF
            observacao = null,
            foiEnviada = isFaturaSent
        )
    }

    private fun confirmDeleteFatura() {
        AlertDialog.Builder(this)
            .setTitle("Excluir Fatura")
            .setMessage("Tem certeza que deseja excluir esta fatura e movê-la para a Lixeira?")
            .setPositiveButton("Excluir") { dialog, _ ->
                deleteFatura()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteFatura() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val faturaOriginal = faturaDao.getFaturaById(currentFaturaId)
                if (faturaOriginal != null) {
                    // Mover para a Lixeira
                    val faturaParaLixeira = FaturaLixeira(
                        numeroFatura = faturaOriginal.numeroFatura,
                        clienteNome = faturaOriginal.clienteNome,
                        artigosJson = Gson().toJson(faturaItemsList), // Salva os itens como JSON
                        subtotal = faturaOriginal.subtotal,
                        desconto = faturaOriginal.desconto,
                        descontoPercent = faturaOriginal.descontoPercent,
                        taxaEntrega = faturaOriginal.taxaEntrega,
                        saldoDevedor = faturaOriginal.saldoDevedor,
                        data = faturaOriginal.data,
                        fotosJson = Gson().toJson(faturaFotoDao.getPhotosForFatura(currentFaturaId).firstOrNull()?.map { it.photoPath }), // Salva paths das fotos
                        notasJson = Gson().toJson(faturaNotaDao.getNotesForFatura(currentFaturaId).firstOrNull()?.map { it.notaConteudo }) // Salva conteúdo das notas
                    )

                    val newRowId = (application as MyApplication).database.faturaLixeiraDao().insert(faturaParaLixeira)

                    if (newRowId != -1L) {
                        // Deletar da tabela principal e tabelas relacionadas
                        faturaDao.deleteFaturaById(currentFaturaId)
                        faturaItemDao.deleteItemsForFatura(currentFaturaId)
                        faturaFotoDao.deletePhotosForFatura(currentFaturaId)
                        faturaNotaDao.deleteNotesForFatura(currentFaturaId)

                        withContext(Dispatchers.Main) {
                            showToast("Fatura movida para a Lixeira.")
                            setResult(Activity.RESULT_OK) // Notifica a MainActivity
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("Erro ao mover fatura para a Lixeira.")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Fatura não encontrada para exclusão.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Erro ao excluir fatura: ${e.message}")
                    Log.e("SecondScreenActivity", "Erro ao excluir fatura: ${e.message}", e)
                }
            }
        }
    }


    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    private fun dispatchTakePictureIntent() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e("SecondScreenActivity", "Erro ao criar arquivo de imagem: ${ex.message}")
            showToast("Erro ao criar arquivo de imagem.")
            null
        }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".fileprovider",
                it
            )
            takePictureLauncher.launch(photoURI)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
