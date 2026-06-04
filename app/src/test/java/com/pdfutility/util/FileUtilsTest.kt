package com.pdfutility.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileUtilsTest {

    @Test
    fun formatFileSize_bytes() {
        assertEquals("500 B", formatFileSize(500))
    }

    @Test
    fun formatFileSize_kilobytes() {
        assertEquals("1.0 KB", formatFileSize(1024))
        assertEquals("512.0 KB", formatFileSize(512 * 1024))
    }

    @Test
    fun formatFileSize_megabytes() {
        assertEquals("1.0 MB", formatFileSize(1024 * 1024))
        assertEquals("1.5 MB", formatFileSize((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun formatFileSize_zero() {
        assertEquals("0 B", formatFileSize(0))
    }
}