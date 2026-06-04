package com.pdfutility.data.local.fileio

object FileNameSanitizer {
    private const val MAX_FILENAME_LENGTH = 255
    private val INVALID_CHARS = Regex("""[/:*?"<>|]""")
    private val CONTROL_CHARS = Regex("""[\u0000-\u001F]""")
    private val PATH_TRAVERSAL = Regex("""\.\.""")
    private val WINDOWS_RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
    )

    fun sanitize(name: String, defaultPrefix: String = "document"): String {
        var result = name.trim().substringAfterLast('/').substringAfterLast('\\')
        if (result.isBlank()) return defaultPrefix
        val wasHiddenFile = result.startsWith(".") && !result.startsWith("..")
        result = PATH_TRAVERSAL.replace(result, "")
        result = CONTROL_CHARS.replace(result, "")
        result = INVALID_CHARS.replace(result, "_")
        result = result.trim('.')
        if (result.isBlank()) return defaultPrefix
        if (wasHiddenFile) result = "_$result"
        val baseName = result.substringBeforeLast('.', result)
        if (baseName.uppercase() in WINDOWS_RESERVED_NAMES) {
            result = "_$result"
        }
        return result.takeMaxFilenameLength()
    }

    private fun String.takeMaxFilenameLength(): String {
        if (length <= MAX_FILENAME_LENGTH) return this
        val dotIndex = lastIndexOf('.')
        if (dotIndex <= 0 || dotIndex == lastIndex) return take(MAX_FILENAME_LENGTH)
        val extension = substring(dotIndex)
        val baseMaxLength = (MAX_FILENAME_LENGTH - extension.length).coerceAtLeast(1)
        return take(baseMaxLength) + extension
    }
}
