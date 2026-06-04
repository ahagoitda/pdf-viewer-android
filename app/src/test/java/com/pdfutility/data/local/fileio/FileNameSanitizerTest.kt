package com.pdfutility.data.local.fileio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameSanitizerTest {

    @Test
    fun sanitize_normalFilename() {
        assertEquals("document.pdf", FileNameSanitizer.sanitize("document.pdf"))
    }

    @Test
    fun sanitize_pathTraversal() {
        val result = FileNameSanitizer.sanitize("../../../etc/passwd")
        assertTrue(result.contains("passwd"))
        assertEquals(result, result.replace("../", ""))
    }

    @Test
    fun sanitize_absolutePath() {
        val result = FileNameSanitizer.sanitize("/etc/shadow")
        assertEquals("shadow", result)
    }

    @Test
    fun sanitize_windowsPath() {
        val result = FileNameSanitizer.sanitize("C:\\Users\\test\\file.pdf")
        assertEquals("file.pdf", result)
    }

    @Test
    fun sanitize_emptyString() {
        assertEquals("document", FileNameSanitizer.sanitize(""))
    }

    @Test
    fun sanitize_nullChars() {
        val result = FileNameSanitizer.sanitize("file\u0000name.pdf")
        assertEquals("filename.pdf", result)
    }

    @Test
    fun sanitize_reservedNames() {
        val result = FileNameSanitizer.sanitize("CON")
        assertEquals("_CON", result)
    }

    @Test
    fun sanitize_longFilename() {
        val longName = "a".repeat(300) + ".pdf"
        val result = FileNameSanitizer.sanitize(longName)
        assertTrue(result.length <= 255)
    }

    @Test
    fun sanitize_hiddenFile() {
        assertEquals("_hidden", FileNameSanitizer.sanitize(".hidden"))
    }

    @Test
    fun sanitize_remainingDots() {
        val result = FileNameSanitizer.sanitize("...hidden")
        assertTrue(!result.startsWith("."))
    }
}