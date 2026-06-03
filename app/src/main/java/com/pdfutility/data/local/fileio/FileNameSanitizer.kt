package com.pdfutility.data.local.fileio

object FileNameSanitizer {
    private val INVALID_CHARS = Regex("""[\\/:*?"<>|]""")
    private val PATH_TRAVERSAL = Regex("""\.\.""")

    fun sanitize(name: String, defaultPrefix: String = "output"): String {
        var result = name.trim()
        if (result.isBlank()) return "${defaultPrefix}_${System.currentTimeMillis()}"
        result = PATH_TRAVERSAL.replace(result, "")
        result = INVALID_CHARS.replace(result, "_")
        result = result.trim('.')
        if (result.isBlank()) return "${defaultPrefix}_${System.currentTimeMillis()}"
        return result
    }
}