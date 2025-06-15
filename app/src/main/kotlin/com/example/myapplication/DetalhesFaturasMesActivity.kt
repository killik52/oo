package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.application.MyApplication
import database.dao.FaturaDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DetalhesFaturasMesActivity : AppCompatActivity() {

    private lateinit var mesAnoTextView: TextView
    private lateinit var faturamentoTotalTextView: TextView
    private lateinit var faturasRecyclerView: RecyclerView
    private lateinit var faturaAdapter: FaturaAdapter
    private lateinit var voltarButton: TextView

    private var selectedMonth: String? = null
    private var selectedYear: String? = null

    // DAO do Room
    private lateinit var faturaDao: FaturaDao

    private val SECOND_SCREEN_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_faturas_mes)

        // Inicializa o DAO do Room
        val application = application as MyApplication
        faturaDao = application.database.faturaDao()

        selectedMonth = intent.getStringExtra("MES")
        selectedYear = intent.getStringExtra("ANO")
        val faturamentoTotal = intent.getDoubleExtra("FATURAMENTO_TOTAL", 0.0)

        initComponents()
        setupListeners()
        setupRecyclerView()
        displayData(faturamentoTotal)
        loadFaturasDoMes()
    }

    private fun initComponents() {
        mesAnoTextView = findViewById(R.id.mesAnoTextView)
        faturamentoTotalTextView = findViewById(R.id.faturamentoTotalTextView)
        faturasRecyclerView = findViewById(R.id.faturasRecyclerView)
        voltarButton = findViewById(R.id.voltarButton)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        faturasRecyclerView.layoutManager = LinearLayoutManager(this)
        faturaAdapter = FaturaAdapter(
            onItemClick = { fatura ->
                val intent = Intent(this, SecondScreenActivity::class.java).apply {
                    putExtra("fatura_id", fatura.id)
                    putExtra("foi_enviada", fatura.foiEnviada)
                }
                startActivityForResult(intent, SECOND_SCREEN_REQUEST_CODE)
            },
            onItemLongClick = { fatura ->
                showToast("Opções de fatura (Long Click): ${fatura.numeroFatura}")
                // Implementar opções como exclusão, envio, etc.
            }
        )
        faturasRecyclerView.adapter = faturaAdapter

        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        faturasRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))
    }

    private fun displayData(faturamentoTotal: Double) {
        val mesNome = selectedMonth?.let { getMonthName(it.toInt()) } ?: "Mês Desconhecido"
        mesAnoTextView.text = "$mesNome / $selectedYear"
        faturamentoTotalTextView.text = String.format(Locale.getDefault(), "Total: R$ %.2f", faturamentoTotal)
    }

    private fun loadFaturasDoMes() {
        selectedMonth ?: return
        selectedYear ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Formate as datas para o padrão YYYY-MM-DD para a query do Room
                val startDate = "$selectedYear-${selectedMonth}-01 00:00:00"
                val endDate = "$selectedYear-${selectedMonth}-${getLastDayOfMonth(selectedMonth!!.toInt(), selectedYear!!.toInt())} 23:59:59"

                val faturas = faturaDao.getFaturasInDateRange(startDate, endDate)
                val faturaItens = faturas.map { fatura ->
                    FaturaResumidaItem(
                        id = fatura.id,
                        numeroFatura = fatura.numeroFatura ?: "",
                        clienteNome = fatura.clienteNome,
                        artigosSerial = emptyList(), // Você precisará carregar isso da tabela FaturaItem
                        saldoDevedor = fatura.saldoDevedor,
                        data = fatura.data,
                        foiEnviada = fatura.foiEnviada
                    )
                }
                withContext(Dispatchers.Main) {
                    faturaAdapter.updateFaturas(faturaItens)
                    Log.d("DetalhesFaturasMes", "Faturas do mês carregadas: ${faturaItens.size}")
                    if (faturaItens.isEmpty()) {
                        showToast("Nenhuma fatura encontrada para este mês.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Erro ao carregar faturas do mês: ${e.message}")
                    Log.e("DetalhesFaturasMes", "Erro ao carregar faturas: ${e.message}", e)
                }
            }
        }
    }

    private fun getMonthName(monthNumber: Int): String {
        return when (monthNumber) {
            1 -> "Janeiro"
            2 -> "Fevereiro"
            3 -> "Março"
            4 -> "Abril"
            5 -> "Maio"
            6 -> "Junho"
            7 -> "Julho"
            8 -> "Agosto"
            9 -> "Setembro"
            10 -> "Outubro"
            11 -> "Novembro"
            12 -> "Dezembro"
            else -> "Mês Inválido"
        }
    }

    private fun getLastDayOfMonth(month: Int, year: Int): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month - 1, 1) // month - 1 because Calendar.MONTH is 0-indexed
        return calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SECOND_SCREEN_REQUEST_CODE) {
            // Se a SecondScreenActivity retornou, recarregue as faturas para refletir possíveis mudanças
            loadFaturasDoMes()
        }
    }
}
