package com.pdfutility.data.local.fileio

import android.content.Context
import android.net.Uri
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object HwpxTextExtractor {
    private const val MAX_ENTRY_SIZE = 10 * 1024 * 1024L

    fun extract(context: Context, uri: Uri): String {
        val paragraphs = extractParagraphs(context, uri)
        return paragraphs.joinToString("\n\n").ifBlank { "문서 텍스트가 비어 있습니다." }
    }

    fun extractParagraphs(context: Context, uri: Uri): List<String> {
        val parts = mutableListOf<Pair<String, ByteArray>>()

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.startsWith("Contents/section") && entry.name.endsWith(".xml")) {
                        if (entry.size > MAX_ENTRY_SIZE) {
                            zip.closeEntry()
                            continue
                        }
                        parts += entry.name to zip.readBytes(MAX_ENTRY_SIZE)
                    }
                    zip.closeEntry()
                }
            }
        }

        require(parts.isNotEmpty()) { "HWPX 본문 XML을 찾지 못했습니다." }

        val paragraphs = mutableListOf<String>()
        parts.sortedBy { it.first }.forEach { (_, bytes) ->
            appendSectionParagraphs(bytes, paragraphs)
        }

        return paragraphs.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun ZipInputStream.readBytes(maxSize: Long): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var totalRead = 0L
        while (true) {
            val read = this.read(buf)
            if (read == -1) break
            totalRead += read
            if (totalRead > maxSize) throw java.util.zip.ZipException("엔트리 크기가 제한을 초과합니다.")
            buffer.write(buf, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun appendSectionParagraphs(xmlBytes: ByteArray, paragraphs: MutableList<String>) {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }

        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        collectParagraphs(document.documentElement, paragraphs)
    }

    private fun collectParagraphs(node: Node, paragraphs: MutableList<String>) {
        val localName = node.localName ?: node.nodeName.substringAfter(':')
        if (localName == "p") {
            val sb = StringBuilder()
            walkForParagraphText(node, sb)
            val text = sb.toString().trim()
            if (text.isNotEmpty()) {
                paragraphs.add(text)
            }
            return
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            collectParagraphs(children.item(i), paragraphs)
        }
    }

    private fun walkForParagraphText(node: Node, output: StringBuilder) {
        val localName = node.localName ?: node.nodeName.substringAfter(':')
        when (localName) {
            "t" -> {
                output.append(node.textContent)
                return
            }
            "lineBreak" -> {
                output.append('\n')
                return
            }
            "tab" -> {
                output.append('\t')
                return
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            walkForParagraphText(children.item(i), output)
        }
    }
}
