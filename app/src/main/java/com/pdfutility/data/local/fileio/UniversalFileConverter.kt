package com.pdfutility.data.local.fileio

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Xml
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.model.FileFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UniversalFileConverter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversionFileDataSource: ConversionFileDataSource,
) {
    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 40f
        private const val TEXT_SIZE = 12f
        private const val LINE_SPACING = 4f
        private const val MAX_ENTRY_BYTES = 10 * 1024 * 1024
    }

    suspend fun convertToPdf(uri: Uri, mimeType: String?): ConversionResult = withContext(Dispatchers.IO) {
        when (detectFormat(uri, mimeType)) {
            FileFormat.Pdf -> ConversionResult.Error("이미 PDF 파일입니다.")
            FileFormat.Image -> conversionFileDataSource.convertImagesToPdf(listOf(uri), "img_${System.currentTimeMillis()}")
            FileFormat.Text, FileFormat.Markdown -> convertText(uri)
            FileFormat.Csv -> convertCsv(uri)
            FileFormat.Html -> convertHtml(uri)
            FileFormat.Docx -> convertDocx(uri)
            FileFormat.Xlsx -> convertXlsx(uri)
            FileFormat.Unsupported -> ConversionResult.Error("지원하지 않는 파일 형식입니다.")
        }
    }

    fun detectFormat(uri: Uri, mimeType: String?): FileFormat {
        val ext = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase() ?: ""
        return when {
            mimeType == "application/pdf" || ext == "pdf" -> FileFormat.Pdf
            mimeType?.startsWith("image/") == true || ext in listOf("jpg", "jpeg", "png", "bmp", "webp", "gif") -> FileFormat.Image
            mimeType == "text/csv" || ext == "csv" || ext == "tsv" -> FileFormat.Csv
            mimeType == "text/html" || ext in listOf("html", "htm") -> FileFormat.Html
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    || mimeType == "application/msword" || ext == "docx" -> FileFormat.Docx
            mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    || mimeType == "application/vnd.ms-excel" || ext == "xlsx" -> FileFormat.Xlsx
            mimeType == "text/markdown" || ext == "md" || ext == "markdown" -> FileFormat.Markdown
            mimeType?.startsWith("text/") == true || ext == "txt" -> FileFormat.Text
            else -> FileFormat.Unsupported
        }
    }

    private fun outputFile(): File {
        val dir = File(context.cacheDir, "converted")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${System.currentTimeMillis()}.pdf")
    }

    private fun convertText(uri: Uri): ConversionResult {
        return try {
            val lines = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readLines() }
                ?: return ConversionResult.Error("파일을 읽을 수 없습니다.")
            val out = outputFile()
            renderTextToPdf(lines, out)
            ConversionResult.Success(out.absolutePath, out.name, 1, out.length())
        } catch (e: Exception) {
            ConversionResult.Error(e.message ?: "변환 중 오류가 발생했습니다.")
        }
    }

    private fun convertCsv(uri: Uri): ConversionResult {
        return try {
            val rawLines = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readLines() }
                ?: return ConversionResult.Error("파일을 읽을 수 없습니다.")

            val delimiter = if (rawLines.firstOrNull()?.contains('\t') == true) '\t' else ','
            val rows = rawLines.map { it.split(delimiter) }
            val colCount = rows.maxOfOrNull { it.size } ?: 0
            val colWidths = IntArray(colCount) { col ->
                rows.mapNotNull { row -> row.getOrNull(col)?.length }.maxOrNull()?.coerceAtMost(20) ?: 10
            }
            val formatted = rows.map { row ->
                row.mapIndexed { col, cell ->
                    cell.take(colWidths.getOrElse(col) { 10 }).padEnd(colWidths.getOrElse(col) { 10 })
                }.joinToString(" | ")
            }
            val out = outputFile()
            renderTextToPdf(formatted, out, typeface = Typeface.MONOSPACE, textSize = 10f)
            ConversionResult.Success(out.absolutePath, out.name, 1, out.length())
        } catch (e: Exception) {
            ConversionResult.Error(e.message ?: "변환 중 오류가 발생했습니다.")
        }
    }

    private fun convertHtml(uri: Uri): ConversionResult {
        return try {
            val raw = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: return ConversionResult.Error("파일을 읽을 수 없습니다.")

            val cleaned = raw
                .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</(p|div|h[1-6]|li|tr|blockquote)>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), "")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&nbsp;", " ").replace("&quot;", "\"").replace("&#39;", "'")

            val lines = cleaned.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            val out = outputFile()
            renderTextToPdf(lines, out)
            ConversionResult.Success(out.absolutePath, out.name, 1, out.length())
        } catch (e: Exception) {
            ConversionResult.Error(e.message ?: "변환 중 오류가 발생했습니다.")
        }
    }

    private fun convertDocx(uri: Uri): ConversionResult {
        return try {
            val lines = mutableListOf<String>()
            ZipInputStream(context.contentResolver.openInputStream(uri)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val parser = Xml.newPullParser()
                        parser.setInput(zis, "UTF-8")
                        val sb = StringBuilder()
                        var inTextTag = false
                        var eventType = parser.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    if (parser.name == "w:p") sb.clear()
                                    if (parser.name == "w:t") inTextTag = true
                                }
                                XmlPullParser.TEXT -> if (inTextTag) sb.append(parser.text)
                                XmlPullParser.END_TAG -> {
                                    if (parser.name == "w:t") inTextTag = false
                                    if (parser.name == "w:p") {
                                        val text = sb.toString()
                                        if (text.isNotBlank()) lines.add(text)
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            if (lines.isEmpty()) return ConversionResult.Error("문서에서 텍스트를 추출할 수 없습니다.")
            val out = outputFile()
            renderTextToPdf(lines, out)
            ConversionResult.Success(out.absolutePath, out.name, 1, out.length())
        } catch (e: Exception) {
            ConversionResult.Error(e.message ?: "변환 중 오류가 발생했습니다.")
        }
    }

    private fun convertXlsx(uri: Uri): ConversionResult {
        return try {
            var sharedStringsBytes: ByteArray? = null
            var sheetBytes: ByteArray? = null

            ZipInputStream(context.contentResolver.openInputStream(uri)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "xl/sharedStrings.xml" -> {
                            val bytes = zis.readBytes()
                            if (bytes.size <= MAX_ENTRY_BYTES) sharedStringsBytes = bytes
                        }
                        "xl/worksheets/sheet1.xml" -> {
                            val bytes = zis.readBytes()
                            if (bytes.size <= MAX_ENTRY_BYTES) sheetBytes = bytes
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val sharedStrings = mutableListOf<String>()
            sharedStringsBytes?.let { bytes ->
                val parser = Xml.newPullParser()
                parser.setInput(bytes.inputStream(), "UTF-8")
                val sb = StringBuilder()
                var inT = false
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> if (parser.name == "t") { inT = true; sb.clear() }
                        XmlPullParser.TEXT -> if (inT) sb.append(parser.text)
                        XmlPullParser.END_TAG -> if (parser.name == "t") {
                            inT = false
                            sharedStrings.add(sb.toString())
                        }
                    }
                    eventType = parser.next()
                }
            }

            val rows = mutableListOf<List<String>>()
            sheetBytes?.let { bytes ->
                val parser = Xml.newPullParser()
                parser.setInput(bytes.inputStream(), "UTF-8")
                var currentRow = mutableListOf<String>()
                var cellType = ""
                var cellValue = ""
                var inV = false
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> when (parser.name) {
                            "row" -> currentRow = mutableListOf()
                            "c" -> { cellType = parser.getAttributeValue(null, "t") ?: ""; cellValue = "" }
                            "v" -> inV = true
                        }
                        XmlPullParser.TEXT -> if (inV) cellValue += parser.text
                        XmlPullParser.END_TAG -> when (parser.name) {
                            "v" -> inV = false
                            "c" -> {
                                val display = if (cellType == "s") {
                                    val idx = cellValue.toIntOrNull() ?: -1
                                    sharedStrings.getOrElse(idx) { cellValue }
                                } else cellValue
                                currentRow.add(display)
                            }
                            "row" -> if (currentRow.isNotEmpty()) rows.add(currentRow.toList())
                        }
                    }
                    eventType = parser.next()
                }
            }

            if (rows.isEmpty()) return ConversionResult.Error("스프레드시트에서 데이터를 추출할 수 없습니다.")
            val colCount = rows.maxOf { it.size }
            val colWidths = IntArray(colCount) { col ->
                rows.mapNotNull { row -> row.getOrNull(col)?.length }.maxOrNull()?.coerceAtMost(20) ?: 8
            }
            val formatted = rows.map { row ->
                (0 until colCount).joinToString(" | ") { col ->
                    row.getOrElse(col) { "" }.take(colWidths[col]).padEnd(colWidths[col])
                }
            }
            val out = outputFile()
            renderTextToPdf(formatted, out, typeface = Typeface.MONOSPACE, textSize = 10f)
            ConversionResult.Success(out.absolutePath, out.name, 1, out.length())
        } catch (e: Exception) {
            ConversionResult.Error(e.message ?: "변환 중 오류가 발생했습니다.")
        }
    }

    private fun renderTextToPdf(
        lines: List<String>,
        outputFile: File,
        typeface: Typeface = Typeface.DEFAULT,
        textSize: Float = TEXT_SIZE,
    ) {
        val pdfDocument = PdfDocument()
        val paint = TextPaint().apply {
            this.typeface = typeface
            this.textSize = textSize
            color = Color.BLACK
            isAntiAlias = true
        }
        val contentWidth = (PAGE_WIDTH - 2 * MARGIN).toInt()
        var pageNumber = 1
        var page = startNewPage(pdfDocument, pageNumber)
        var canvas = page.canvas
        var y = MARGIN

        for (line in lines) {
            val displayLine = line.ifEmpty { " " }
            val layout = StaticLayout.Builder
                .obtain(displayLine, 0, displayLine.length, paint, contentWidth)
                .setLineSpacing(LINE_SPACING, 1f)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            if (y + layout.height > PAGE_HEIGHT - MARGIN) {
                pdfDocument.finishPage(page)
                pageNumber++
                page = startNewPage(pdfDocument, pageNumber)
                canvas = page.canvas
                y = MARGIN
            }

            canvas.save()
            canvas.translate(MARGIN, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + LINE_SPACING
        }

        pdfDocument.finishPage(page)
        FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
    }

    private fun startNewPage(pdfDocument: PdfDocument, pageNumber: Int): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        return pdfDocument.startPage(info)
    }
}
