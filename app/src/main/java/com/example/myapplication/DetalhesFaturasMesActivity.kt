package com.example.myapplication

import android.os.Bundle
import android.view.View // Adicionado import
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityDetalhesFaturasMesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat // Adicionado import
import java.util.Locale // Adicionado import

class DetalhesFaturasMesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalhesFaturasMesBinding
    private lateinit var dbHelper: ClienteDbHelper
    private lateinit var faturaAdapter: FaturaAdapter // Usando FaturaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalhesFaturasMesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detalhes do Mês" // String resource

        val mesAno = intent.getStringExtra("MES_ANO")
        val valorTotal = intent.getDoubleExtra("VALOR_TOTAL", 0.0)
        val totalFaturas = intent.getIntExtra("TOTAL_FATURAS", 0)

        if (mesAno == null) {
            Toast.makeText(this, "Dados do mês não encontrados.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.textMesAno.text = mesAno
        binding.textValorTotalMes.text = getString(R.string.total_receita, NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valorTotal))
        binding.textTotalFaturasMes.text = getString(R.string.total_faturas, totalFaturas)


        setupRecyclerView()
        loadFaturasByMesAno(mesAno)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        faturaAdapter = FaturaAdapter( // Usando FaturaAdapter
            onItemClick = { faturaId ->
                val intent = Intent(this, SecondScreenActivity::class.java).apply {
                    putExtra("FATURA_ID", faturaId)
                }
                startActivity(intent)
            },
            onItemLongClick = { faturaId ->
                // Implementar clique longo se necessário (ex: deletar fatura)
                Toast.makeText(this, "Fatura $faturaId clicada longo", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerViewFaturasMes.apply { // Corrigido o nome do RecyclerView
            layoutManager = LinearLayoutManager(this@DetalhesFaturasMesActivity)
            adapter = faturaAdapter
            addItemDecoration(VerticalSpaceItemDecoration(16))
        }
    }

    private fun loadFaturasByMesAno(mesAno: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val faturas = dbHelper.getAllFaturasResumidas() // Você precisaria filtrar por mesAno aqui no DBHelper
            // Para um filtro preciso, ClienteDbHelper.getFaturasByMesAno(mesAno) seria ideal.
            // Por simplicidade, filtraremos na memória por enquanto.
            val filteredFaturas = faturas.filter { it.data.startsWith(mesAno) } // Filtra na memória

            withContext(Dispatchers.Main) {
                if (filteredFaturas.isNotEmpty()) {
                    faturaAdapter.submitList(filteredFaturas)
                    binding.textNoFaturasFound.visibility = View.GONE
                    binding.recyclerViewFaturasMes.visibility = View.VISIBLE
                } else {
                    binding.textNoFaturasFound.visibility = View.VISIBLE
                    binding.recyclerViewFaturasMes.visibility = View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val mesAno = intent.getStringExtra("MES_ANO")
        if (mesAno != null) {
            loadFaturasByMesAno(mesAno) // Recarrega sempre que a tela volta ao foco
        }
    }
}