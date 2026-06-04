package com.pdfutility.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun openPdfFile(context: Context, path: String) {
    val uri = shareableUri(context, path)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "PDF"))
}

fun sharePdfFile(context: Context, path: String) {
    val uri = shareableUri(context, path)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "PDF"))
}

private fun shareableUri(context: Context, path: String): Uri {
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        File(path),
    )
}
