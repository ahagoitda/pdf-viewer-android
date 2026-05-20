package com.pdfutility.presentation.ui.imagetopdf

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.pdfutility.presentation.state.ImageItem

@Composable
fun rememberImagePicker(
    onImagesSelected: (List<ImageItem>) -> Unit
): ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>> {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val items = uris.map { uri ->
                val metadata = getMetadata(context, uri)
                ImageItem(
                    uri = uri,
                    displayName = metadata.first,
                    size = metadata.second
                )
            }
            onImagesSelected(items)
        }
    }

    return launcher
}

private fun getMetadata(context: android.content.Context, uri: Uri): Pair<String, Long> {
    var name = "Unknown"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
            size = cursor.getLong(sizeIndex)
        }
    }
    return Pair(name, size)
}
