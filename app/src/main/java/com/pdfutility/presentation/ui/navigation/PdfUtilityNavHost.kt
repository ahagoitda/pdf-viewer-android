package com.pdfutility.presentation.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pdfutility.presentation.intent.DocumentListIntent
import com.pdfutility.presentation.ui.documentlist.DocumentListScreen
import com.pdfutility.presentation.ui.imagetopdf.ImageToPdfScreen
import com.pdfutility.presentation.ui.pdfviewer.PdfViewerScreen
import com.pdfutility.presentation.viewmodel.DocumentListViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object DocumentList : Screen("document_list")
    data object PdfViewer : Screen("pdf_viewer/{pdfUri}") {
        fun createRoute(pdfUri: String) = "pdf_viewer/${URLEncoder.encode(pdfUri, StandardCharsets.UTF_8.toString())}"
    }
    data object ImageToPdf : Screen("image_to_pdf")
}

@Composable
fun PdfUtilityNavHost(
    navController: NavHostController = rememberNavController(),
    initialPdfUri: String? = null,
    onPdfUriHandled: () -> Unit = {},
    initialConversionRequest: Pair<String, String?>? = null,
    onConversionRequestHandled: () -> Unit = {},
) {
    LaunchedEffect(initialPdfUri) {
        if (!initialPdfUri.isNullOrEmpty()) {
            navController.navigate(Screen.PdfViewer.createRoute(initialPdfUri)) {
                popUpTo(Screen.DocumentList.route)
            }
            onPdfUriHandled()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.DocumentList.route
    ) {
        composable(Screen.DocumentList.route) {
            val viewModel: DocumentListViewModel = hiltViewModel()
            LaunchedEffect(initialConversionRequest) {
                initialConversionRequest?.let { (uriString, mimeType) ->
                    viewModel.onIntent(
                        DocumentListIntent.ConvertAndOpenFile(Uri.parse(uriString), mimeType)
                    )
                    onConversionRequestHandled()
                }
            }
            DocumentListScreen(
                viewModel = viewModel,
                onDocumentClick = { document ->
                    navController.navigate(Screen.PdfViewer.createRoute(document.uri))
                },
                onImageToPdfClick = {
                    navController.navigate(Screen.ImageToPdf.route)
                }
            )
        }
        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(navArgument("pdfUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val pdfUri = backStackEntry.arguments?.getString("pdfUri") ?: ""
            PdfViewerScreen(
                pdfUri = pdfUri,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.ImageToPdf.route) {
            ImageToPdfScreen(
                onBackClick = { navController.popBackStack() },
                onConversionSuccess = { outputPath ->
                    // Navigate to viewer for the new PDF
                    navController.navigate(Screen.PdfViewer.createRoute("file://$outputPath")) {
                        popUpTo(Screen.DocumentList.route)
                    }
                }
            )
        }
    }
}
