package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerationUtils {

    // Helper para redimensionar bitmap, evitando OutOfMemoryError com imagens grandes
    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    fun generatePdf(
        context: Context,
        faturaId: Long,
        clienteNome: String,
        faturaData: String,
        artigos: List<ArtigoItem>,
        observacoes: String,
        totalGeral: Double,
        caminhosFotos: List<String>?,
        modoSimplificado: Boolean,
        clienteCnpj: String? = null,
        clienteEndereco: String? = null,
        clienteTelefone: String? = null,
        clienteEmail: String? = null
    ): Uri? {
        val dbHelper = ClienteDbHelper(context)
        val informacoesEmpresa = dbHelper.getInformacoesEmpresa()
        val instrucoesPagamento = dbHelper.getInstrucoesPagamento()

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint()
        val textPaint = TextPaint()

        // Configurações de fonte padrão
        val typefaceBold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val typefaceNormal = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        var yPos = 40f
        val margin = 40f
        val lineHeight = 14f

        // --- Cabeçalho da Empresa (Informações e Logotipo) ---
        if (informacoesEmpresa != null) {
            textPaint.typeface = typefaceBold
            textPaint.textSize = 16f
            yPos += 10f // Espaço antes do cabeçalho

            // Logotipo
            val logoPath = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Constants.LOGO_PATH_KEY, null)
            if (!logoPath.isNullOrEmpty()) {
                try {
                    val logoFile = File(logoPath)
                    if (logoFile.exists()) {
                        val logoBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                        if (logoBitmap != null) {
                            val scaledLogo = getResizedBitmap(logoBitmap, 100) // Redimensiona para max 100px
                            val logoRect = RectF(margin, yPos, margin + scaledLogo.width, yPos + scaledLogo.height)
                            canvas.drawBitmap(scaledLogo, null, logoRect, paint)
                            yPos += scaledLogo.height + 10f // Ajusta yPos após o logo
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PdfGeneration", "Erro ao carregar logotipo: ${e.message}")
                    Toast.makeText(context, "Erro ao carregar logotipo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // Nome da Empresa
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(informacoesEmpresa.nome, margin, yPos, textPaint)
            yPos += lineHeight

            textPaint.typeface = typefaceNormal
            textPaint.textSize = 10f

            // Endereço da Empresa
            if (informacoesEmpresa.endereco?.isNotBlank() == true) {
                canvas.drawText(informacoesEmpresa.endereco, margin, yPos, textPaint)
                yPos += lineHeight
            }

            // Telefone da Empresa
            if (informacoesEmpresa.telefone?.isNotBlank() == true) {
                canvas.drawText(informacoesEmpresa.telefone, margin, yPos, textPaint)
                yPos += lineHeight
            }

            // Email da Empresa
            if (informacoesEmpresa.email?.isNotBlank() == true) {
                canvas.drawText(informacoesEmpresa.email, margin, yPos, textPaint)
                yPos += lineHeight
            }

            // CNPJ da Empresa
            if (informacoesEmpresa.cnpj?.isNotBlank() == true) {
                canvas.drawText("CNPJ: ${informacoesEmpresa.cnpj}", margin, yPos, textPaint)
                yPos += lineHeight
            }

            yPos += 20f // Espaço após as informações da empresa
        }


        // --- Informações da Fatura ---
        textPaint.typeface = typefaceBold
        textPaint.textSize = 14f
        canvas.drawText("Fatura Nº: ${Constants.DEFAULT_INVOICE_PREFIX}${faturaId}", margin, yPos, textPaint)
        yPos += lineHeight

        textPaint.typeface = typefaceNormal
        textPaint.textSize = 12f
        canvas.drawText("Data: $faturaData", margin, yPos, textPaint)
        yPos += lineHeight * 2

        // --- Informações do Cliente ---
        textPaint.typeface = typefaceBold
        textPaint.textSize = 12f
        canvas.drawText(context.getString(R.string.para_text), margin, yPos, textPaint)
        yPos += lineHeight

        textPaint.typeface = typefaceNormal
        textPaint.textSize = 12f
        canvas.drawText(clienteNome, margin, yPos, textPaint)
        yPos += lineHeight

        if (clienteCnpj?.isNotBlank() == true) {
            canvas.drawText("CPF/CNPJ: $clienteCnpj", margin, yPos, textPaint)
            yPos += lineHeight
        }
        if (clienteEndereco?.isNotBlank() == true) {
            canvas.drawText("Endereço: $clienteEndereco", margin, yPos, textPaint)
            yPos += lineHeight
        }
        if (clienteTelefone?.isNotBlank() == true) {
            canvas.drawText("Telefone: $clienteTelefone", margin, yPos, textPaint)
            yPos += lineHeight
        }
        if (clienteEmail?.isNotBlank() == true) {
            canvas.drawText("Email: $clienteEmail", margin, yPos, textPaint)
            yPos += lineHeight
        }

        yPos += 20f // Espaço após informações do cliente

        // --- Tabela de Artigos ---
        textPaint.typeface = typefaceBold
        textPaint.textSize = 12f
        canvas.drawText("Artigos:", margin, yPos, textPaint)
        yPos += lineHeight + 5f

        val tableLeft = margin
        val tableRight = pageInfo.pageWidth - margin
        val col1Width = 200f
        val col2Width = 80f
        val col3Width = 80f
        val col4Width = 80f

        // Cabeçalhos da tabela
        canvas.drawText("Nome do Artigo", tableLeft, yPos, textPaint)
        canvas.drawText("Preço Unit.", tableLeft + col1Width, yPos, textPaint)
        canvas.drawText("Qtd.", tableLeft + col1Width + col2Width, yPos, textPaint)
        canvas.drawText("Total", tableLeft + col1Width + col2Width + col3Width, yPos, textPaint)
        yPos += lineHeight

        // Linha divisória
        canvas.drawLine(tableLeft, yPos, tableRight, yPos, paint)
        yPos += 5f

        textPaint.typeface = typefaceNormal
        textPaint.textSize = 10f

        artigos.forEach { artigo ->
            // Quebra de página se necessário
            if (yPos + lineHeight * 3 > pageInfo.pageHeight - margin) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin + 20f // Reinicia yPos na nova página

                // Redesenha cabeçalhos da tabela na nova página
                textPaint.typeface = typefaceBold
                textPaint.textSize = 12f
                canvas.drawText("Nome do Artigo", tableLeft, yPos, textPaint)
                canvas.drawText("Preço Unit.", tableLeft + col1Width, yPos, textPaint)
                canvas.drawText("Qtd.", tableLeft + col1Width + col2Width, yPos, textPaint)
                canvas.drawText("Total", tableLeft + col1Width + col2Width + col3Width, yPos, textPaint)
                yPos += lineHeight
                canvas.drawLine(tableLeft, yPos, tableRight, yPos, paint)
                yPos += 5f
                textPaint.typeface = typefaceNormal
                textPaint.textSize = 10f
            }

            canvas.drawText(artigo.nome, tableLeft, yPos, textPaint)
            canvas.drawText(NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(artigo.precoUnitario), tableLeft + col1Width, yPos, textPaint)
            canvas.drawText(artigo.quantidade.toString(), tableLeft + col1Width + col2Width, yPos, textPaint)
            canvas.drawText(NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(artigo.total), tableLeft + col1Width + col2Width + col3Width, yPos, textPaint)
            yPos += lineHeight
            // Adicionar número de série e descrição se existirem
            if (artigo.numeroSerie?.isNotBlank() == true) {
                canvas.drawText("  Serial: ${artigo.numeroSerie}", tableLeft, yPos, textPaint)
                yPos += lineHeight
            }
            if (artigo.descricao?.isNotBlank() == true) {
                canvas.drawText("  Desc: ${artigo.descricao}", tableLeft, yPos, textPaint)
                yPos += lineHeight
            }
            yPos += 2f // Pequeno espaçamento entre itens
        }

        yPos += 10f
        canvas.drawLine(tableLeft, yPos, tableRight, yPos, paint)
        yPos += 10f

        // --- Totais ---
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.typeface = typefaceBold
        textPaint.textSize = 14f
        canvas.drawText("${context.getString(R.string.total_geral)}: ${NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(totalGeral)}", tableRight, yPos, textPaint)
        yPos += lineHeight * 2

        // --- Observações ---
        if (observacoes.isNotBlank()) {
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = typefaceBold
            textPaint.textSize = 12f
            canvas.drawText("Observações:", margin, yPos, textPaint)
            yPos += lineHeight

            textPaint.typeface = typefaceNormal
            textPaint.textSize = 10f
            // Quebra de texto para observações
            val staticLayout = StaticLayout.Builder.obtain(
                observacoes,
                textPaint,
                (pageInfo.pageWidth - 2 * margin).toInt()
            )
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()

            canvas.save()
            canvas.translate(margin, yPos)
            staticLayout.draw(canvas)
            canvas.restore()
            yPos += staticLayout.height + 20f
        }

        // --- Instruções de Pagamento ---
        if (instrucoesPagamento?.isNotBlank() == true) {
            // Quebra de página se necessário
            if (yPos + lineHeight * 3 > pageInfo.pageHeight - margin) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin + 20f // Reinicia yPos na nova página
            }

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = typefaceBold
            textPaint.textSize = 12f
            canvas.drawText(context.getString(R.string.payment_instructions_title), margin, yPos, textPaint)
            yPos += lineHeight

            textPaint.typeface = typefaceNormal
            textPaint.textSize = 10f

            val staticLayout = StaticLayout.Builder.obtain(
                instrucoesPagamento,
                textPaint,
                (pageInfo.pageWidth - 2 * margin).toInt()
            )
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()

            canvas.save()
            canvas.translate(margin, yPos)
            staticLayout.draw(canvas)
            canvas.restore()
            yPos += staticLayout.height + 20f
        }


        // --- Fotos (apenas no modo completo) ---
        if (!modoSimplificado && caminhosFotos != null && caminhosFotos.isNotEmpty()) {
            val photoHeight = 100f
            val photosPerRow = 4 // 4 fotos por linha
            val photoMargin = 10f
            val totalPhotoWidth = (pageInfo.pageWidth - 2 * margin - (photosPerRow - 1) * photoMargin) / photosPerRow

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = typefaceBold
            textPaint.textSize = 12f

            // Quebra de página se necessário para o título "Fotos"
            if (yPos + lineHeight * 2 > pageInfo.pageHeight - margin) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin + 20f
            }
            canvas.drawText(context.getString(R.string.galeria_fotos), margin, yPos, textPaint) // Usando string de recurso
            yPos += lineHeight + 5f

            var currentX = margin
            caminhosFotos.forEachIndexed { index, path ->
                // Quebra de página para as fotos
                if (yPos + photoHeight + photoMargin > pageInfo.pageHeight - margin) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = margin + 20f // Reinicia yPos na nova página
                    currentX = margin // Reinicia X na nova página
                }

                try {
                    val file = File(path)
                    if (file.exists()) {
                        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (originalBitmap != null) {
                            // Redimensiona a foto para caber na tabela de fotos
                            val scaledBitmap = getResizedBitmap(originalBitmap, photoHeight.toInt())
                            val photoRect = RectF(currentX, yPos, currentX + scaledBitmap.width, yPos + scaledBitmap.height)
                            canvas.drawBitmap(scaledBitmap, null, photoRect, paint)

                            currentX += scaledBitmap.width + photoMargin
                            if ((index + 1) % photosPerRow == 0) { // Próxima linha
                                currentX = margin
                                yPos += photoHeight + photoMargin
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PdfGeneration", "Erro ao carregar foto $path: ${e.message}")
                    Toast.makeText(context, "Erro ao carregar foto ${file.name}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            yPos += photoHeight + 20f // Ajusta yPos após a última linha de fotos
        }

        document.finishPage(page)

        // Salvar PDF
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Fatura_${clienteNome.replace(" ", "_")}_${timeStamp}.pdf"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDir = File(downloadsDir, Constants.APP_DIR_NAME)
        val invoicesDir = File(appDir, Constants.INVOICES_DIR_NAME)

        if (!invoicesDir.exists()) {
            invoicesDir.mkdirs()
        }

        val file = File(invoicesDir, fileName)
        return try {
            val fileOutputStream = FileOutputStream(file)
            document.writeTo(fileOutputStream)
            fileOutputStream.close()
            document.close()
            Toast.makeText(context, context.getString(R.string.generate_pdf_success), Toast.LENGTH_LONG).show()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: IOException) {
            Log.e("PdfGeneration", "Erro ao gerar PDF: ${e.message}", e)
            Toast.makeText(context, context.getString(R.string.generate_pdf_error, e.message), Toast.LENGTH_LONG).show()
            null
        } finally {
            document.close() // Garante que o documento seja fechado mesmo em caso de erro
        }
    }
}