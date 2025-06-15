package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.application.MyApplication
import com.example.myapplication.database.AppDatabase // Importe seu AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoadingLoginActivity : AppCompatActivity() {

    private lateinit var loadingImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_login)

        loadingImage = findViewById(R.id.loadingImage)

        // Inicie a animação ou lógica de carregamento
        // Por exemplo, uma rotação simples se a imagem for um ícone de carregamento
        // loadingImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_indefinitely))

        // Inicia o processo de inicialização do banco de dados Room em uma coroutine
        GlobalScope.launch(Dispatchers.IO) {
            // Acessa a instância do banco de dados. Isso aciona a criação do banco de dados
            // e a execução de quaisquer migrações se o banco de dados não existir ou estiver desatualizado.
            val db = (application as MyApplication).database // Apenas acessá-lo é suficiente para inicializá-lo

            // Exemplo de como você poderia usar um DAO para uma verificação inicial
            // val clienteCount = db.clienteDao().getClienteCount() // Se você tivesse um método getClienteCount()
            // Log.d("LoadingLoginActivity", "Número de clientes no banco: $clienteCount")

            withContext(Dispatchers.Main) {
                // Simula um tempo de carregamento mínimo
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this@LoadingLoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }, 2000) // 2 segundos de atraso
            }
        }
    }
}
