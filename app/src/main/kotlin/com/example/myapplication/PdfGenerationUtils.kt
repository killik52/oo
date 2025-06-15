package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.myapplication.database.AppDatabase
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerationUtils {

    private const val PAGE_WIDTH = 595 // A4 width in points (approx 210mm)
    private const val PAGE_HEIGHT = 842 // A4 height in points (approx 297mm)
    private const val MARGIN_HORIZONTAL = 30
    private const val MARGIN_TOP = 30
    private const val MARGIN_BOTTOM = 30
    private const val TEXT_SIZE_NORMAL = 10f
    private const val TEXT_SIZE_LARGE = 12f
    private const val TEXT_SIZE_XLARGE = 14f
    private const val TEXT_SIZE_XXLARGE = 16f

    suspend fun generatePdf(
        context: Context,
        fatura: Fatura
    ): File? {
        val faturaItemDao = com.example.myapplication.database.AppDatabase.getDatabase(context).faturaItemDao()
        val faturaNotaDao = com.example.myapplication.database.AppDatabase.getDatabase(context).faturaNotaDao()
        val informacoesEmpresaDao = com.example.myapplication.database.AppDatabase.getDatabase(context).informacoesEmpresaDao()
        val instrucoesPagamentoDao = com.example.myapplication.database.AppDatabase.getDatabase(context).instrucoesPagamentoDao()

        val faturaItems = faturaItemDao.getItemsForFatura(fatura.id).firstOrNull() ?: emptyList()
        val faturaNotas = faturaNotaDao.getNotesForFatura(fatura.id).firstOrNull() ?: emptyList()
        val empresaInfo = informacoesEmpresaDao.getInformacoesEmpresa().firstOrNull()
        val pagtoInstrucoes = instrucoesPagamentoDao.getInstrucoesPagamento().firstOrNull()

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        var yPos = MARGIN_TOP

        // Load logo
        val logoUriString = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("logo_uri", null)
        var logoBitmap: Bitmap? = null
        if (logoUriString != null) {
            try {
                val uri = Uri.parse(logoUriString)
                if (uri.scheme == "file") {
                    val file = File(uri.path)
                    if (file.exists()) {
                        logoBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        logoBitmap = BitmapFactory.decodeStream(inputStream)
                    }
                }
            } catch (e: Exception) {
                Log.e("PdfGenerationUtils", "Erro ao carregar logo: ${e.message}", e)
                logoBitmap = null // Reset if error
            }
        }

        // Header - Company Info (Top Right)
        paint.color = ContextCompat.getColor(context, R.color.black)
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = TEXT_SIZE_NORMAL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        yPos = MARGIN_TOP

        if (empresaInfo != null) {
            empresaInfo.nomeEmpresa?.let {
                paint.textSize = TEXT_SIZE_XXLARGE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(it, (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(), yPos.toFloat(), paint)
                yPos += 20
            }
            paint.textSize = TEXT_SIZE_NORMAL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            empresaInfo.informacoesAdicionais?.let {
                canvas.drawText(it, (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            empresaInfo.cnpj?.let {
                canvas.drawText("CNPJ: $it", (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            empresaInfo.email?.let {
                canvas.drawText("Email: $it", (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            empresaInfo.telefone?.let {
                canvas.drawText("Tel: $it", (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            val enderecoEmpresa = "${empresaInfo.cidade.orEmpty()}${if (empresaInfo.cidade?.isNotEmpty() == true && empresaInfo.estado?.isNotEmpty() == true) ", " else ""}${empresaInfo.estado.orEmpty()}"
            if (enderecoEmpresa.isNotBlank()) {
                canvas.drawText(enderecoEmpresa, (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            empresaInfo.cep?.let {
                canvas.drawText("CEP: $it", (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
        }

        // Draw logo (left side, vertically centered with company info)
        if (logoBitmap != null) {
            paint.textAlign = Paint.Align.LEFT
            val logoHeight = 80
            val logoWidth = (logoBitmap.width * (logoHeight.toFloat() / logoBitmap.height)).toInt()
            val startYForLogo = MARGIN_TOP.toFloat() + (yPos - MARGIN_TOP - logoHeight) / 2
            canvas.drawBitmap(logoBitmap, MARGIN_HORIZONTAL.toFloat(), startYForLogo, paint)
        }

        // Reset yPos after header for content below
        yPos = MARGIN_TOP + 80 // Some space after header, adjust as needed. Max yPos from company info.
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = TEXT_SIZE_NORMAL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        yPos += 20 // Extra space after company info block

        // Line separator
        paint.color = ContextCompat.getColor(context, R.color.light_gray)
        canvas.drawLine(
            MARGIN_HORIZONTAL.toFloat(),
            yPos.toFloat(),
            (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(),
            yPos.toFloat(),
            paint
        )
        yPos += 20 // Space after line

        // Fatura Details (Left)
        paint.color = ContextCompat.getColor(context, R.color.black)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Fatura Nº: ${fatura.numeroFatura}", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
        yPos += 15

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val formattedDate = try {
            val date = inputFormat.parse(fatura.data)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            fatura.data
        }
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Data: $formattedDate", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
        yPos += 20

        // Client Details (Left)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Detalhes do Cliente:", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
        yPos += 15

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Nome: ${fatura.clienteNome}", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
        yPos += 15

        val cliente = com.example.myapplication.database.AppDatabase.getDatabase(context).clienteDao().getClienteById(fatura.clienteId ?: -1)
        if (cliente != null) {
            cliente.telefone?.let {
                canvas.drawText("Telefone: $it", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            cliente.email?.let {
                canvas.drawText("Email: $it", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            val cpfCnpj = if (!cliente.cpf.isNullOrEmpty()) "CPF: ${cliente.cpf}" else if (!cliente.cnpj.isNullOrEmpty()) "CNPJ: ${cliente.cnpj}" else null
            cpfCnpj?.let {
                canvas.drawText(it, MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            val enderecoCliente = "${cliente.logradouro.orEmpty()}${if (cliente.logradouro?.isNotEmpty() == true && cliente.numero?.isNotEmpty() == true) ", " else ""}${cliente.numero.orEmpty()}" +
                    "${if (enderecoCliente.isNotEmpty() && cliente.complemento?.isNotEmpty() == true) ", " else ""}${cliente.complemento.orEmpty()}" +
                    "${if (enderecoCliente.isNotEmpty() && cliente.bairro?.isNotEmpty() == true) ", " else ""}${cliente.bairro.orEmpty()}"
            if (enderecoCliente.isNotBlank()) {
                canvas.drawText(enderecoCliente, MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            val cidadeEstadoCep = "${cliente.municipio.orEmpty()}${if (cliente.municipio?.isNotEmpty() == true && cliente.uf?.isNotEmpty() == true) ", " else ""}${cliente.uf.orEmpty()}${if (cliente.cep?.isNotEmpty() == true) " - CEP: ${cliente.cep}" else ""}"
            if (cidadeEstadoCep.isNotBlank()) {
                canvas.drawText(cidadeEstadoCep, MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 20
            }
        } else {
            yPos += 15 // Add some space if client details are missing
        }

        // Items Header
        yPos += 15
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Itens da Fatura:", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
        yPos += 20

        // Table Header
        paint.color = ContextCompat.getColor(context, R.color.black)
        paint.textSize = TEXT_SIZE_LARGE
        val col1X = MARGIN_HORIZONTAL
        val col2X = col1X + 180
        val col3X = col2X + 70
        val col4X = col3X + 70
        val col5X = col4X + 70

        canvas.drawText("Nome do Artigo", col1X.toFloat(), yPos.toFloat(), paint)
        canvas.drawText("Preço Un.", col2X.toFloat(), yPos.toFloat(), paint)
        canvas.drawText("Qtd.", col3X.toFloat(), yPos.toFloat(), paint)
        canvas.drawText("Desc.", col4X.toFloat(), yPos.toFloat(), paint)
        canvas.drawText("Total", col5X.toFloat(), yPos.toFloat(), paint)
        yPos += 5 // Space below header
        paint.color = ContextCompat.getColor(context, R.color.light_gray)
        canvas.drawLine(
            MARGIN_HORIZONTAL.toFloat(),
            yPos.toFloat(),
            (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(),
            yPos.toFloat(),
            paint
        )
        yPos += 10 // Space after line

        // Items List
        paint.color = ContextCompat.getColor(context, R.color.black)
        paint.textSize = TEXT_SIZE_NORMAL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val decimalFormat = DecimalFormat("0.00")

        for (item in faturaItems) {
            // Check for page break
            if (yPos + 30 > PAGE_HEIGHT - MARGIN_BOTTOM) {
                document.finishPage(page)
                document.startPage(pageInfo)
                canvas.drawColor(ContextCompat.getColor(context, R.color.white)) // Clear page
                yPos = MARGIN_TOP // Reset yPos for new page
            }

            canvas.drawText(item.nomeArtigo, col1X.toFloat(), yPos.toFloat(), paint)
            canvas.drawText(decimalFormat.format(item.precoUnitario), col2X.toFloat(), yPos.toFloat(), paint)
            canvas.drawText(item.quantidade.toString(), col3X.toFloat(), yPos.toFloat(), paint)
            // Calculate item total considering discount
            val itemTotalBeforeDiscount = item.quantidade * item.precoUnitario
            val itemTotalAfterDiscount = itemTotalBeforeDiscount // Assume discount handled at fatura level for simplicity or need more specific item discount field
            // Se houver um campo de desconto por item na FaturaItem, use-o aqui
            canvas.drawText("0.00", col4X.toFloat(), yPos.toFloat(), paint) // Placeholder for item discount
            canvas.drawText(decimalFormat.format(itemTotalAfterDiscount), col5X.toFloat(), yPos.toFloat(), paint)
            yPos += 15
            item.descricao?.let {
                if (it.isNotEmpty()) {
                    canvas.drawText("  ${it}", col1X.toFloat(), yPos.toFloat(), paint)
                    yPos += 15
                }
            }
            item.numeroSerie?.let {
                if (it.isNotEmpty()) {
                    canvas.drawText("  SN: ${it}", col1X.toFloat(), yPos.toFloat(), paint)
                    yPos += 15
                }
            }
            yPos += 5 // Extra space between items
        }

        yPos += 20
        paint.color = ContextCompat.getColor(context, R.color.light_gray)
        canvas.drawLine(
            MARGIN_HORIZONTAL.toFloat(),
            yPos.toFloat(),
            (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(),
            yPos.toFloat(),
            paint
        )
        yPos += 10

        // Totals
        paint.color = ContextCompat.getColor(context, R.color.black)
        paint.textSize = TEXT_SIZE_LARGE
        paint.textAlign = Paint.Align.RIGHT

        val totalX = (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat()
        val labelX = totalX - 150 // Adjust as needed

        canvas.drawText("Subtotal:", labelX, yPos.toFloat(), paint)
        canvas.drawText(decimalFormat.format(fatura.subtotal), totalX, yPos.toFloat(), paint)
        yPos += 20

        if (fatura.desconto > 0) {
            val discountLabel = if (fatura.descontoPercent) "Desconto (${decimalFormat.format(fatura.desconto)}%):" else "Desconto:"
            canvas.drawText(discountLabel, labelX, yPos.toFloat(), paint)
            val descontoValor = if (fatura.descontoPercent) {
                fatura.subtotal * (fatura.desconto / 100)
            } else {
                fatura.desconto
            }
            canvas.drawText("-${decimalFormat.format(descontoValor)}", totalX, yPos.toFloat(), paint)
            yPos += 20
        }

        if (fatura.taxaEntrega > 0) {
            canvas.drawText("Taxa de Entrega:", labelX, yPos.toFloat(), paint)
            canvas.drawText(decimalFormat.format(fatura.taxaEntrega), totalX, yPos.toFloat(), paint)
            yPos += 20
        }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = TEXT_SIZE_XXLARGE
        canvas.drawText("Saldo Devedor:", labelX, yPos.toFloat(), paint)
        canvas.drawText(decimalFormat.format(fatura.saldoDevedor), totalX, yPos.toFloat(), paint)
        yPos += 30

        // Notes
        if (faturaNotas.isNotEmpty()) {
            yPos += 20
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = TEXT_SIZE_LARGE
            canvas.drawText("Notas:", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
            yPos += 15

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = TEXT_SIZE_NORMAL
            for (nota in faturaNotas) {
                // Check for page break
                if (yPos + 20 > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    document.finishPage(page)
                    document.startPage(pageInfo)
                    canvas.drawColor(ContextCompat.getColor(context, R.color.white)) // Clear page
                    yPos = MARGIN_TOP // Reset yPos for new page
                }
                canvas.drawText("- ${nota.notaConteudo}", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
        }

        // Payment Instructions
        if (pagtoInstrucoes != null) {
            yPos += 20
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = TEXT_SIZE_LARGE
            canvas.drawText("Instruções de Pagamento:", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
            yPos += 15

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = TEXT_SIZE_NORMAL

            pagtoInstrucoes.pix?.let {
                // Check for page break
                if (yPos + 15 > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    document.finishPage(page)
                    document.startPage(pageInfo)
                    canvas.drawColor(ContextCompat.getColor(context, R.color.white)) // Clear page
                    yPos = MARGIN_TOP // Reset yPos for new page
                }
                canvas.drawText("PIX: $it", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            pagtoInstrucoes.banco?.let {
                // Check for page break
                if (yPos + 15 > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    document.finishPage(page)
                    document.startPage(pageInfo)
                    canvas.drawColor(ContextCompat.getColor(context, R.color.white)) // Clear page
                    yPos = MARGIN_TOP // Reset yPos for new page
                }
                canvas.drawText("Banco: $it", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            pagtoInstrucoes.agencia?.let {
                // Check for page break
                if (yPos + 15 > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    document.finishPage(page)
                    document.startPage(pageInfo)
                    canvas.drawColor(ContextCompat.getColor(context, R.color.white)) // Clear page
                    yPos = MARGIN_TOP // Reset yPos for new page
                }
                canvas.drawText("Agência: $it", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            pagtoInstrucoes.conta?.let {
                // Check for page break
                if (yPos + 15 > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    document.finishPage(page)
                    document.startPage(pageInfo)
                    canvas.drawColor(ContextCompat.getColor(context, R.color.white)) // Clear page
                    yPos = MARGIN_TOP // Reset yPos for new page
                }
                canvas.drawText("Conta: $it", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
            pagtoInstrucoes.outrasInstrucoes?.let {
                // Check for page break
                if (yPos + 15 > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    document.finishPage(page)
                    document.startPage(pageInfo)
                    canvas.drawColor(ContextCompat.getColor(context, R.color.white)) // Clear page
                    yPos = MARGIN_TOP // Reset yPos for new page
                }
                canvas.drawText("Outras: $it", MARGIN_HORIZONTAL.toFloat(), yPos.toFloat(), paint)
                yPos += 15
            }
        }

        document.finishPage(page)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val fileName = "Fatura_${fatura.numeroFatura}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        val file = File(downloadsDir, fileName)

        return try {
            document.writeTo(FileOutputStream(file))
            document.close()
            Log.d("PdfGenerationUtils", "PDF gerado com sucesso: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("PdfGenerationUtils", "Erro ao gerar PDF: ${e.message}", e)
            null
        }
    }
}
