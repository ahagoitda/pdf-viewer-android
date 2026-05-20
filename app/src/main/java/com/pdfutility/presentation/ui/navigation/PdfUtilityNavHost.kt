package com.pdfutility.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pdfutility.presentation.ui.documentlist.DocumentListScreen
import com.pdfutility.presentation.ui.imagetopdf.ImageToPdfScreen
import com.pdfutility.presentation.ui.pdfviewer.PdfViewerScreen
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
    onPdfUriHandled: () -> Unit = {}
) {
    LaunchedEffect(initialPdfUri) {
        if (!initialPdfUri.isNullOrEmpty()) {
            navController.navigate(Screen.PdfViewer.createRoute(initialPdfUri)) {
                // Ensure we go back to the document list screen when hitting back from viewer
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
            DocumentListScreen(
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
