package com.example.myapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.application.MyApplication
import database.dao.FaturaDao
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ResumoFinanceiroActivity : AppCompatActivity() {

    private lateinit var voltarButton: TextView
    private lateinit var periodoSpinner: Spinner
    private lateinit var startDateButton: TextView
    private lateinit var endDateButton: TextView
    private lateinit var filterButton: Button
    private lateinit var barChart: BarChart
    private lateinit var pieChartClientes: PieChart
    private lateinit var pieChartArtigos: PieChart
    private lateinit var totalMesRecyclerView: RecyclerView
    private lateinit var resumoMensalAdapter: ResumoMensalAdapter
    private lateinit var dataRangeLayout: LinearLayout
    private lateinit var totalPeriodoTextView: TextView

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    // DAO do Room
    private lateinit var faturaDao: FaturaDao
    private val gson = Gson() // Para desserializar strings JSON

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resumo_financeiro)

        // Inicializa o DAO do Room
        val application = application as MyApplication
        faturaDao = application.database.faturaDao()

        initComponents()
        setupListeners()
        setupCharts()
        setupRecyclerView()
        setupPeriodSpinner()
    }

    private fun initComponents() {
        voltarButton = findViewById(R.id.voltarButton)
        periodoSpinner = findViewById(R.id.periodoSpinner)
        startDateButton = findViewById(R.id.startDateButton)
        endDateButton = findViewById(R.id.endDateButton)
        filterButton = findViewById(R.id.filterButton)
        barChart = findViewById(R.id.barChart)
        pieChartClientes = findViewById(R.id.pieChartClientes)
        pieChartArtigos = findViewById(R.id.pieChartArtigos)
        totalMesRecyclerView = findViewById(R.id.totalMesRecyclerView)
        dataRangeLayout = findViewById(R.id.dataRangeLayout)
        totalPeriodoTextView = findViewById(R.id.totalPeriodoTextView)
    }

    private fun setupListeners() {
        voltarButton.setOnClickListener {
            onBackPressed()
        }

        startDateButton.setOnClickListener {
            showDatePicker(true)
        }

        endDateButton.setOnClickListener {
            showDatePicker(false)
        }

        filterButton.setOnClickListener {
            applyFilters()
        }
    }

    private fun setupCharts() {
        // Configurações comuns para gráficos de pizza
        pieChartClientes.setUsePercentValues(true)
        pieChartClientes.description.isEnabled = false
        pieChartClientes.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChartClientes.dragDecelerationFrictionCoef = 0.95f
        pieChartClientes.isDrawHoleEnabled = true
        pieChartClientes.setHoleColor(resources.getColor(R.color.white))
        pieChartClientes.setTransparentCircleColor(resources.getColor(R.color.white))
        pieChartClientes.setTransparentCircleAlpha(110)
        pieChartClientes.holeRadius = 58f
        pieChartClientes.transparentCircleRadius = 61f
        pieChartClientes.setDrawCenterText(true)
        pieChartClientes.setEntryLabelColor(resources.getColor(R.color.black))
        pieChartClientes.setEntryLabelTextSize(TEXT_SIZE_NORMAL)

        pieChartArtigos.setUsePercentValues(true)
        pieChartArtigos.description.isEnabled = false
        pieChartArtigos.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChartArtigos.dragDecelerationFrictionCoef = 0.95f
        pieChartArtigos.isDrawHoleEnabled = true
        pieChartArtigos.setHoleColor(resources.getColor(R.color.white))
        pieChartArtigos.setTransparentCircleColor(resources.getColor(R.color.white))
        pieChartArtigos.setTransparentCircleAlpha(110)
        pieChartArtigos.holeRadius = 58f
        pieChartArtigos.transparentCircleRadius = 61f
        pieChartArtigos.setDrawCenterText(true)
        pieChartArtigos.setEntryLabelColor(resources.getColor(R.color.black))
        pieChartArtigos.setEntryLabelTextSize(TEXT_SIZE_NORMAL)

        // Configurações para gráfico de barras
        barChart.description.isEnabled = false
        barChart.setPinchZoom(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setFitBars(true)
        barChart.setDrawGridBackground(false)
        barChart.setNoDataText("Nenhum dado disponível para o período selecionado.")

        val xAxis = barChart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setGranularity(1f) // only 1 e.g. for month values
        xAxis.setLabelCount(5) // Max 5 labels visible
        xAxis.valueFormatter = MonthAxisValueFormatter()


        val leftAxis = barChart.axisLeft
        leftAxis.setLabelCount(8, false)
        leftAxis.setPosition(com.github.mikephil.charting.components.YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        leftAxis.setSpaceTop(15f)
        leftAxis.setAxisMinimum(0f) // this replaces setStartAtZero(true)

        val rightAxis = barChart.axisRight
        rightAxis.setDrawGridLines(false)
        rightAxis.setLabelCount(8, false)
        rightAxis.setPosition(com.github.mikephil.charting.components.YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        rightAxis.setSpaceTop(15f)
        rightAxis.setAxisMinimum(0f)

        barChart.animateY(1000)
        barChart.legend.isEnabled = false
    }

    private fun setupRecyclerView() {
        totalMesRecyclerView.layoutManager = LinearLayoutManager(this)
        resumoMensalAdapter = ResumoMensalAdapter { resumoMensalItem ->
            // Ao clicar, abrir a tela de detalhes das faturas do mês
            val intent = Intent(this, DetalhesFaturasMesActivity::class.java).apply {
                putExtra("MES", String.format(Locale.getDefault(), "%02d", resumoMensalItem.mes))
                putExtra("ANO", resumoMensalItem.ano.toString())
                putExtra("FATURAMENTO_TOTAL", resumoMensalItem.total)
            }
            startActivity(intent)
        }
        totalMesRecyclerView.adapter = resumoMensalAdapter

        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        totalMesRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))
    }

    private fun setupPeriodSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.periodo_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            periodoSpinner.adapter = adapter
        }

        periodoSpinner.setSelection(0, false) // Seleciona "Últimos 3 Meses" por padrão sem chamar o listener
        periodoSpinner.post { // Garante que o listener é anexado após a seleção inicial
            periodoSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                    when (position) {
                        0 -> { // Últimos 3 Meses
                            dataRangeLayout.visibility = View.GONE
                            val calendar = Calendar.getInstance()
                            endDate = calendar
                            calendar.add(Calendar.MONTH, -2) // 3 meses incluindo o atual
                            startDate = calendar
                            applyFilters()
                        }
                        1 -> { // Últimos 6 Meses
                            dataRangeLayout.visibility = View.GONE
                            val calendar = Calendar.getInstance()
                            endDate = calendar
                            calendar.add(Calendar.MONTH, -5) // 6 meses incluindo o atual
                            startDate = calendar
                            applyFilters()
                        }
                        2 -> { // Último Ano
                            dataRangeLayout.visibility = View.GONE
                            val calendar = Calendar.getInstance()
                            endDate = calendar
                            calendar.add(Calendar.YEAR, -1)
                            startDate = calendar
                            applyFilters()
                        }
                        3 -> { // Personalizado
                            dataRangeLayout.visibility = View.VISIBLE
                            // As datas já estarão definidas se o usuário as selecionou antes
                            // Caso contrário, elas serão nulas e a validação em applyFilters() irá lidar
                            startDate = null
                            endDate = null
                            startDateTextView.text = getString(R.string.data_inicio)
                            endDateTextView.text = getString(R.string.data_fim)
                            totalPeriodoTextView.text = getString(R.string.faturamento_total_periodo, 0.0) // Reset total
                        }
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                    // Do nothing
                }
            })
            // Dispara a seleção inicial manualmente para carregar os dados
            periodoSpinner.setSelection(0)
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
                    startDateTextView.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startDate!!.time)
                } else {
                    endDate = selectedDate
                    endDateTextView.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(endDate!!.time)
                }
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun applyFilters() {
        val selectedPeriodPosition = periodoSpinner.selectedItemPosition
        var startFilterDate: String? = null
        var endFilterDate: String? = null

        if (selectedPeriodPosition == 3) { // Personalizado
            if (startDate == null || endDate == null) {
                showToast("Por favor, selecione as datas de início e fim para o período personalizado.")
                totalPeriodoTextView.text = getString(R.string.faturamento_total_periodo, 0.0)
                resumoMensalAdapter.updateItems(emptyList())
                barChart.clear()
                pieChartClientes.clear()
                pieChartArtigos.clear()
                return
            }
            if (startDate!!.after(endDate)) {
                showToast("A data de início não pode ser posterior à data de fim.")
                totalPeriodoTextView.text = getString(R.string.faturamento_total_periodo, 0.0)
                resumoMensalAdapter.updateItems(emptyList())
                barChart.clear()
                pieChartClientes.clear()
                pieChartArtigos.clear()
                return
            }
            startFilterDate = SimpleDateFormat("yyyy-MM-dd 00:00:00", Locale.getDefault()).format(startDate!!.time)
            endFilterDate = SimpleDateFormat("yyyy-MM-dd 23:59:59", Locale.getDefault()).format(endDate!!.time)
        } else {
            // As datas já foram definidas no listener do spinner
            startDate?.let { startFilterDate = SimpleDateFormat("yyyy-MM-dd 00:00:00", Locale.getDefault()).format(it.time) }
            endDate?.let { endFilterDate = SimpleDateFormat("yyyy-MM-dd 23:59:59", Locale.getDefault()).format(it.time) }
        }

        loadDataForChartsAndRecyclerView(startFilterDate, endFilterDate)
    }


    private fun loadDataForChartsAndRecyclerView(startDateStr: String?, endDateStr: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Faturamento Mensal para o BarChart e RecyclerView
                faturaDao.getFaturamentoMensal(startDateStr, endDateStr, null)
                    .collectLatest { faturamentoMensalList ->
                        val barEntries = mutableListOf<BarEntry>()
                        val labels = mutableListOf<String>()
                        var totalPeriodo = 0.0
                        val resumoMensalItems = mutableListOf<ResumoMensalItem>()

                        // Ordenar a lista para garantir a ordem cronológica
                        val sortedList = faturamentoMensalList.sortedWith(compareBy({ it.ano }, { it.mes }))

                        sortedList.forEachIndexed { index, data ->
                            barEntries.add(BarEntry(index.toFloat(), data.total_mes.toFloat()))
                            labels.add(data.mes_ano_str)
                            totalPeriodo += data.total_mes

                            resumoMensalItems.add(
                                ResumoMensalItem(
                                    mes = data.mes,
                                    ano = data.ano,
                                    total = data.total_mes
                                )
                            )
                        }

                        withContext(Dispatchers.Main) {
                            // Atualiza o total do período
                            totalPeriodoTextView.text = getString(R.string.faturamento_total_periodo, totalPeriodo)

                            // Atualiza o RecyclerView
                            resumoMensalAdapter.updateItems(resumoMensalItems)

                            // Atualiza o BarChart
                            val barDataSet = BarDataSet(barEntries, "Faturamento Mensal")
                            barDataSet.color = ContextCompat.getColor(this@ResumoFinanceiroActivity,
                                R.color.colorPrimary
                            )
                            barDataSet.valueTextSize = TEXT_SIZE_NORMAL
                            val barData = BarData(barDataSet)
                            barChart.data = barData
                            barChart.xAxis.valueFormatter = MonthAxisValueFormatter(labels)
                            barChart.xAxis.setLabelCount(labels.size, true) // Force label count
                            barChart.invalidate() // refresh
                        }
                    }

                // Resumo por Cliente para o PieChart de Clientes
                faturaDao.getResumoPorCliente(startDateStr, endDateStr, null)
                    .collectLatest { resumoClientes ->
                        val pieEntries = mutableListOf<PieEntry>()
                        var totalGastoClientes = 0.0f
                        resumoClientes.forEach {
                            totalGastoClientes += it.total_gasto_cliente.toFloat()
                        }

                        resumoClientes.forEach {
                            pieEntries.add(PieEntry(it.total_gasto_cliente.toFloat(), it.cliente_nome))
                        }

                        withContext(Dispatchers.Main) {
                            if (pieEntries.isNotEmpty()) {
                                val pieDataSet = PieDataSet(pieEntries, "Gastos por Cliente")
                                pieDataSet.colors = Constants.CHART_COLORS // Use suas cores
                                pieDataSet.valueTextSize = TEXT_SIZE_NORMAL
                                pieDataSet.valueTextColor = ContextCompat.getColor(this@ResumoFinanceiroActivity,
                                    R.color.black
                                )
                                pieDataSet.sliceSpace = 2f
                                pieDataSet.valueFormatter = PercentFormatter(pieChartClientes)

                                val pieData = PieData(pieDataSet)
                                pieChartClientes.data = pieData
                                pieChartClientes.invalidate()
                            } else {
                                pieChartClientes.clear()
                                pieChartClientes.setNoDataText("Nenhum dado de cliente disponível para o período selecionado.")
                            }
                        }
                    }

                // Resumo por Artigo (extraindo de JSON) para o PieChart de Artigos
                faturaDao.getArtigosDataForAnalysis(startDateStr, endDateStr, null)
                    .collectLatest { artigosData ->
                        val artigoQuantities = mutableMapOf<String, Int>()
                        artigosData.forEach { result ->
                            result.artigos_json?.let { json ->
                                // Desserializar a string JSON de artigos (se for uma lista de FaturaItem)
                                try {
                                    val type = object : TypeToken<List<FaturaItem>>() {}.type
                                    val items: List<FaturaItem> = gson.fromJson(json, type)
                                    items.forEach { item ->
                                        artigoQuantities[item.nomeArtigo] = (artigoQuantities[item.nomeArtigo] ?: 0) + item.quantidade
                                    }
                                } catch (e: Exception) {
                                    Log.e("ResumoFinanceiroActivity", "Erro ao desserializar artigos_json: ${e.message}, JSON: $json")
                                }
                            }
                        }

                        val pieEntries = mutableListOf<PieEntry>()
                        var totalArtigos = 0f
                        artigoQuantities.forEach { (artigoNome, quantidade) ->
                            pieEntries.add(PieEntry(quantidade.toFloat(), artigoNome))
                            totalArtigos += quantidade
                        }

                        withContext(Dispatchers.Main) {
                            if (pieEntries.isNotEmpty()) {
                                val pieDataSet = PieDataSet(pieEntries, "Artigos Mais Vendidos")
                                pieDataSet.colors = Constants.CHART_COLORS
                                pieDataSet.valueTextSize = TEXT_SIZE_NORMAL
                                pieDataSet.valueTextColor = ContextCompat.getColor(this@ResumoFinanceiroActivity,
                                    R.color.black
                                )
                                pieDataSet.sliceSpace = 2f
                                pieDataSet.valueFormatter = object : PercentFormatter(pieChartArtigos) {
                                    override fun getFormattedValue(value: Float, entry: PieEntry?, dataSetIndex: Int, viewPortHandler: com.github.mikephil.charting.utils.ViewPortHandler?): String {
                                        return "${DecimalFormat("0").format(value)} (${super.getFormattedValue(value, entry, dataSetIndex, viewPortHandler)})"
                                    }
                                }


                                val pieData = PieData(pieDataSet)
                                pieChartArtigos.data = pieData
                                pieChartArtigos.invalidate()
                            } else {
                                pieChartArtigos.clear()
                                pieChartArtigos.setNoDataText("Nenhum dado de artigo disponível para o período selecionado.")
                            }
                        }
                    }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Erro ao carregar dados financeiros: ${e.message}")
                    Log.e("ResumoFinanceiroActivity", "Erro ao carregar dados financeiros: ${e.message}", e)
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Custom formatter for X-axis labels to display month names
    private class MonthAxisValueFormatter(private val labels: List<String> = emptyList()) : com.github.mikephil.charting.formatter.ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            return if (index >= 0 && index < labels.size) {
                labels[index]
            } else {
                ""
            }
        }
    }
}
