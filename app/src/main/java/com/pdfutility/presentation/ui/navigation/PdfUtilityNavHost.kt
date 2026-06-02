package com.pdfutility.presentation.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.pdfutility.presentation.ui.reorderpages.ReorderPagesScreen
import com.pdfutility.presentation.ui.splitpdf.SplitPdfScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val ANIM_DURATION = 300

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
    data object SplitPdf : Screen("split_pdf")
    data object ReorderPages : Screen("reorder_pages")
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
        startDestination = Screen.DocumentList.route,
        enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        popEnterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        popExitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
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
                },
                onSplitPdfClick = {
                    navController.navigate(Screen.SplitPdf.route)
                },
                onReorderPagesClick = {
                    navController.navigate(Screen.ReorderPages.route)
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
        composable(Screen.SplitPdf.route) {
            SplitPdfScreen(
                onBackClick = { navController.popBackStack() },
                onSplitSuccess = { outputPath ->
                    navController.navigate(Screen.PdfViewer.createRoute("file://$outputPath")) {
                        popUpTo(Screen.DocumentList.route)
                    }
                }
            )
        }
        composable(Screen.ReorderPages.route) {
            ReorderPagesScreen(
                onBackClick = { navController.popBackStack() },
                onReorderSuccess = { outputPath ->
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