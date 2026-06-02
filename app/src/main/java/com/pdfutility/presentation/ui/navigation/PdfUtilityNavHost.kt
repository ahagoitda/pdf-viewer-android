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
import com.pdfutility.presentation.ui.hwpxviewer.HwpxViewerScreen
import com.pdfutility.presentation.ui.imagetopdf.ImageToPdfScreen
import com.pdfutility.presentation.ui.mergepdf.MergePdfScreen
import com.pdfutility.presentation.ui.pdfviewer.PdfViewerScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object DocumentList : Screen("document_list")
    data object PdfViewer : Screen("pdf_viewer/{pdfUri}") {
        fun createRoute(pdfUri: String) = "pdf_viewer/${URLEncoder.encode(pdfUri, StandardCharsets.UTF_8.toString())}"
    }
    data object HwpxViewer : Screen("hwpx_viewer/{hwpxUri}") {
        fun createRoute(hwpxUri: String) = "hwpx_viewer/${URLEncoder.encode(hwpxUri, StandardCharsets.UTF_8.toString())}"
    }
    data object ImageToPdf : Screen("image_to_pdf")
    data object MergePdf : Screen("merge_pdf")
}

@Composable
fun PdfUtilityNavHost(
    navController: NavHostController = rememberNavController(),
    initialDocumentUri: String? = null,
    initialDocumentMimeType: String? = null,
    onInitialDocumentHandled: () -> Unit = {}
) {
    LaunchedEffect(initialDocumentUri, initialDocumentMimeType) {
        if (!initialDocumentUri.isNullOrEmpty()) {
            val route = if (isHancomDocument(initialDocumentUri, initialDocumentMimeType)) {
                Screen.HwpxViewer.createRoute(initialDocumentUri)
            } else {
                Screen.PdfViewer.createRoute(initialDocumentUri)
            }
            navController.navigate(route) {
                popUpTo(Screen.DocumentList.route)
            }
            onInitialDocumentHandled()
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
                onHwpxDocumentClick = { uri ->
                    navController.navigate(Screen.HwpxViewer.createRoute(uri))
                },
                onImageToPdfClick = {
                    navController.navigate(Screen.ImageToPdf.route)
                },
                onMergePdfClick = {
                    navController.navigate(Screen.MergePdf.route)
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
        composable(
            route = Screen.HwpxViewer.route,
            arguments = listOf(navArgument("hwpxUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val hwpxUri = backStackEntry.arguments?.getString("hwpxUri") ?: ""
            HwpxViewerScreen(
                hwpxUri = hwpxUri,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.ImageToPdf.route) {
            ImageToPdfScreen(
                onBackClick = { navController.popBackStack() },
                onConversionSuccess = { outputPath ->
                    navController.navigate(Screen.PdfViewer.createRoute("file://$outputPath")) {
                        popUpTo(Screen.DocumentList.route)
                    }
                }
            )
        }
        composable(Screen.MergePdf.route) {
            MergePdfScreen(
                onBackClick = { navController.popBackStack() },
                onMergeSuccess = { outputPath ->
                    navController.navigate(Screen.PdfViewer.createRoute("file://$outputPath")) {
                        popUpTo(Screen.DocumentList.route)
                    }
                }
            )
        }
    }
}

private fun isHancomDocument(uri: String, mimeType: String?): Boolean {
    val isHancomMimeType = mimeType in setOf(
        "application/vnd.hancom.hwpx",
        "application/haansofthwp",
        "application/x-hwp",
    )
    val hasHancomExtension = uri.substringBefore('?').let {
        it.endsWith(".hwpx", ignoreCase = true) || it.endsWith(".hwp", ignoreCase = true)
    }
    return isHancomMimeType || hasHancomExtension
}
