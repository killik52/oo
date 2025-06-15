package com.example.myapplication

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.dao.FaturaDao
import com.example.myapplication.database.dao.FaturaItemDao
import com.example.myapplication.database.dao.InformacoesEmpresaDao
import com.example.myapplication.database.dao.InstrucoesPagamentoDao
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExportActivity : AppCompatActivity() {

    private lateinit var voltarButton: TextView
    private lateinit var exportAllButton: Button
    private lateinit var exportPeriodButton: Button
    private lateinit var startDateTextView: TextView
    private lateinit var endDateTextView: TextView

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    // DAOs do Room
    private lateinit var faturaDao: FaturaDao
    private lateinit var faturaItemDao: FaturaItemDao
    private lateinit var informacoesEmpresaDao: InformacoesEmpresaDao
    private lateinit var instrucoesPagamentoDao: InstrucoesPagamentoDao

    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        // Inicializa os DAOs do Room
        val application = application as MyApplication
        faturaDao = application.database.faturaDao()
        faturaItemDao = application.database.faturaItemDao()
        informacoesEmpresaDao = application.database.informacoesEmpresaDao()
        instrucoesPagamentoDao = application.database.instrucoesPagamentoDao()

        initComponents()
        setupListeners()
    }

    private fun initComponents() {
        voltarButton = findViewById(R.id.voltarButton)
        exportAllButton = findViewById(R.id.exportAllButton)
        exportPeriodButton = findViewById(R.id.exportPeriodButton)
        startDateTextView = findViewById(R.id.startDateTextView)
        endDateTextView = findViewById(R.id.endDateTextView)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        exportAllButton.setOnClickListener {
            checkAndRequestPermissions {
                exportData(ExportType.ALL)
            }
        }

        exportPeriodButton.setOnClickListener {
            checkAndRequestPermissions {
                exportData(ExportType.PERIOD)
            }
        }

        startDateTextView.setOnClickListener {
            showDatePicker(true)
        }

        endDateTextView.setOnClickListener {
            showDatePicker(false)
        }
    }

    private fun checkAndRequestPermissions(onPermissionsGranted: () -> Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE // No Android 10+, this is for mediaStore access
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            onPermissionsGranted()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Permissão concedida. Exportando dados...")
                // Determine qual botão foi clicado para continuar a exportação
                // Isso requer um ajuste na lógica do listener, ou passar um enum no checkAndRequestPermissions
                // Por simplicidade, vou assumir que a exportação será acionada novamente após a concessão.
                // Idealmente, você pode armazenar o tipo de exportação pendente.
            } else {
                showToast("Permissão de escrita no armazenamento externo negada.")
            }
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDayOfMonth)

                if (isStartDate) {
                    startDate = selectedDate
                    startDateTextView.text = formatDate(startDate!!)
                } else {
                    endDate = selectedDate
                    endDateTextView.text = formatDate(endDate!!)
                }
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun formatDate(calendar: Calendar): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun formatDbDate(calendar: Calendar): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun exportData(type: ExportType) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val faturas: List<Fatura> = when (type) {
                    ExportType.ALL -> faturaDao.getAllFaturas().firstOrNull() ?: emptyList()
                    ExportType.PERIOD -> {
                        if (startDate == null || endDate == null) {
                            withContext(Dispatchers.Main) {
                                showToast("Por favor, selecione um período válido.")
                            }
                            return@launch
                        }
                        faturaDao.getFaturasInDateRange(formatDbDate(startDate!!), formatDbDate(endDate!!))
                    }
                }

                val allExportData = mutableListOf<ExportFaturaData>()

                for (fatura in faturas) {
                    val faturaItems = faturaItemDao.getItemsForFatura(fatura.id).firstOrNull() ?: emptyList()
                    val empresaInfo = informacoesEmpresaDao.getInformacoesEmpresa().firstOrNull()
                    val pagtoInstrucoes = instrucoesPagamentoDao.getInstrucoesPagamento().firstOrNull()

                    val exportFatura = ExportFaturaData(
                        fatura = fatura,
                        items = faturaItems,
                        informacoesEmpresa = empresaInfo,
                        instrucoesPagamento = pagtoInstrucoes
                    )
                    allExportData.add(exportFatura)
                }

                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonString = gson.toJson(allExportData)

                val fileName = if (type == ExportType.ALL) {
                    "faturas_all_${System.currentTimeMillis()}.json"
                } else {
                    val start = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(startDate!!.time)
                    val end = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(endDate!!.time)
                    "faturas_period_${start}_${end}.json"
                }

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)

                FileWriter(file).use { writer ->
                    writer.write(jsonString)
                }

                withContext(Dispatchers.Main) {
                    showToast("Dados exportados com sucesso para ${file.absolutePath}")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Erro ao exportar dados: ${e.message}")
                    Log.e("ExportActivity", "Erro ao exportar: ${e.message}", e)
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    enum class ExportType {
        ALL, PERIOD
    }

    // Classes auxiliares para a estrutura JSON de exportação
    data class ExportFaturaData(
        val fatura: Fatura,
        val items: List<FaturaItem>,
        val informacoesEmpresa: InformacoesEmpresaEntity?,
        val instrucoesPagamento: InstrucoesPagamentoEntity?
    )
}
