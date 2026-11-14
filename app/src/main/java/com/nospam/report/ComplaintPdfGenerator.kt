package com.nospam.report

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

/**
 * Rende l'esposto testuale in un PDF A4 impaginato, più presentabile per gli uffici.
 * Usa `android.graphics.pdf.PdfDocument` (nessuna dipendenza esterna). Il testo è
 * monospazio per preservare l'allineamento di hash e tabelle.
 */
class ComplaintPdfGenerator {

    private val pageWidth = 595   // A4 a 72 dpi, in punti
    private val pageHeight = 842
    private val margin = 28f
    private val lineHeight = 12f
    private val maxCharsPerLine = 92

    fun render(esposto: String, outputDir: File, fileName: String = "esposto_nospam.pdf"): File {
        val paint = Paint().apply {
            textSize = 8.5f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        // Spezza le righe troppo lunghe mantenendo i ritorni a capo originali.
        val lines = esposto.split("\n").flatMap { wrap(it) }

        val doc = PdfDocument()
        val linesPerPage = ((pageHeight - 2 * margin) / lineHeight).toInt()
        var index = 0
        var pageNumber = 1

        while (index < lines.size) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            var y = margin + lineHeight
            var drawn = 0
            while (index < lines.size && drawn < linesPerPage) {
                canvas.drawText(lines[index], margin, y, paint)
                y += lineHeight
                index++
                drawn++
            }
            doc.finishPage(page)
            pageNumber++
        }

        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, fileName)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun wrap(line: String): List<String> {
        if (line.length <= maxCharsPerLine) return listOf(line)
        val out = mutableListOf<String>()
        var rest = line
        while (rest.length > maxCharsPerLine) {
            out += rest.substring(0, maxCharsPerLine)
            rest = rest.substring(maxCharsPerLine)
        }
        if (rest.isNotEmpty()) out += rest
        return out
    }
}
