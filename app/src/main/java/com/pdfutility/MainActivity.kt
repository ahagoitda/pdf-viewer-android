package com.pdfutility

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.pdfutility.presentation.ui.navigation.PdfUtilityNavHost
import com.pdfutility.presentation.ui.theme.PdfUtilityTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val pdfUriState = mutableStateOf<String?>(null)
    private val pendingConversionState = mutableStateOf<Pair<String, String?>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            PdfUtilityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val pdfUri by pdfUriState
                    val pendingConversion by pendingConversionState
                    PdfUtilityNavHost(
                        initialPdfUri = pdfUri,
                        onPdfUriHandled = { pdfUriState.value = null },
                        initialConversionRequest = pendingConversion,
                        onConversionRequestHandled = { pendingConversionState.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                tryPersistPermission(uri)
                val mimeType = intent.type ?: contentResolver.getType(uri)
                if (mimeType == "application/pdf") {
                    pdfUriState.value = uri.toString()
                } else {
                    pendingConversionState.value = Pair(uri.toString(), mimeType)
                }
            }
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val uri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let {
                    tryPersistPermission(it)
                    pendingConversionState.value = Pair(it.toString(), intent.type)
                }
            }
        }
    }

    private fun tryPersistPermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
    }
}
