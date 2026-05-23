package com.pdfutility.domain.model

sealed class FileFormat {
    data object Pdf : FileFormat()
    data object Image : FileFormat()
    data object Text : FileFormat()
    data object Markdown : FileFormat()
    data object Csv : FileFormat()
    data object Html : FileFormat()
    data object Docx : FileFormat()
    data object Xlsx : FileFormat()
    data object Unsupported : FileFormat()
}
