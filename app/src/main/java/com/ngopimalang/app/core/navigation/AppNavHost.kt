package com.ngopimalang.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ngopimalang.app.core.ui.FeaturePlaceholderScreen
import com.ngopimalang.app.features.auth.AuthViewModel
import com.ngopimalang.app.features.auth.LoginScreen
import com.ngopimalang.app.features.auth.RegisterScreen
import com.ngopimalang.app.features.home.HomeScreen
import com.ngopimalang.app.features.profile.ProfileScreen
import com.ngopimalang.app.features.vibe.VibeDetailScreen
import com.ngopimalang.app.features.vibe.VibeFormScreen
import com.ngopimalang.app.features.vibe.VibeScreen
import com.ngopimalang.app.features.vibe.VibeViewModel
import com.ngopimalang.app.features.bookmark.BookmarkScreen

/**
 * Peta seluruh halaman aplikasi.
 *
 * Cara menambah screen fitur (contoh untuk Event & Promo milik Rifqi):
 * 1. Buat screen di package features.eventpromo.
 * 2. Ganti pemanggilan [FeaturePlaceholderScreen] pada route [NavRoutes.EVENT_PROMO]
 *    dengan screen milikmu.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    var hadSession by rememberSaveable { mutableStateOf(false) }

    // Login/logout mengubah root halaman tanpa menyisakan stack lama.
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn == hadSession) return@LaunchedEffect
        hadSession = authState.isLoggedIn
        val target = if (authState.isLoggedIn) NavRoutes.HOME else NavRoutes.LOGIN
        navController.navigate(target) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.LOGIN,
        modifier = modifier
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                state = authState,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onLoginClick = authViewModel::login,
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) }
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                state = authState,
                onNameChange = authViewModel::onNameChange,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onRegisterClick = authViewModel::register,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                userName = authState.name.ifBlank { "Ngopi Lover" },
                onOpenDirectory = { navController.navigate(NavRoutes.DIRECTORY) },
                onOpenVibe = { navController.navigate(NavRoutes.VIBE) },
                onOpenEventPromo = { navController.navigate(NavRoutes.EVENT_PROMO) },
                onOpenReviews = { navController.navigate(NavRoutes.REVIEWS) },
                onOpenBookmark = { navController.navigate(NavRoutes.BOOKMARK) }
            )
        }

        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                userName = authState.name.ifBlank { "Ngopi Lover" },
                userEmail = authState.email.ifBlank { "belum-ada-email@ngopimalang.app" },
                onOpenBookmark = { navController.navigate(NavRoutes.BOOKMARK) },
                onOpenReviews = { navController.navigate(NavRoutes.REVIEWS) },
                onLogout = authViewModel::logout
            )
        }

        // ---- Placeholder modul anggota, ganti dengan screen asli masing-masing ----
        composable(NavRoutes.DIRECTORY) {
            FeaturePlaceholderScreen(
                title = "Direktori Cafe",
                owner = "Ossa Amelia Zuhra Lubis",
                listComponent = "LazyVerticalGrid 2 kolom",
                flows = listOf("List/Grid", "Detail Cafe", "Form Tambah/Edit")
            )
        }

        composable(NavRoutes.REVIEWS) {
            FeaturePlaceholderScreen(
                title = "Ulasan & Rating",
                owner = "Shelfina Khayla Anindita",
                listComponent = "HorizontalPager + LazyColumn",
                flows = listOf("Daftar Ulasan", "Detail Ulasan", "Form Ulasan")
            )
        }


        composable(NavRoutes.BOOKMARK) {
            BookmarkScreen()
        }


        composable(NavRoutes.VIBE) { entry ->
            val vibeViewModel: VibeViewModel = viewModel(entry)
            VibeScreen(
                onCafeClick = { _ -> /* detail cafe menyusul */ },
                onVibeDetail = { id -> navController.navigate(NavRoutes.vibeDetail(id)) },
                onAddVibe = { navController.navigate(NavRoutes.vibeForm()) },
                vibeViewModel = vibeViewModel
            )
        }
        composable(
            route = NavRoutes.VIBE_DETAIL,
            arguments = listOf(navArgument("vibeId") { type = NavType.StringType })
        ) { entry ->
            val parentEntry = remember(entry) {
                navController.getBackStackEntry(NavRoutes.VIBE)
            }
            val vibeViewModel: VibeViewModel = viewModel(parentEntry)
            VibeDetailScreen(
                vibeId = entry.arguments?.getString("vibeId").orEmpty(),
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(NavRoutes.vibeForm(id)) },
                onCafeClick = { _ -> /* detail cafe menyusul */ },
                vibeViewModel = vibeViewModel
            )
        }

        composable(
            route = NavRoutes.VIBE_FORM,
            arguments = listOf(navArgument("vibeId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { entry ->
            val parentEntry = remember(entry) {
                navController.getBackStackEntry(NavRoutes.VIBE)
            }
            val vibeViewModel: VibeViewModel = viewModel(parentEntry)
            VibeFormScreen(
                vibeId = entry.arguments?.getString("vibeId"),
                onBack = { navController.popBackStack() },
                vibeViewModel = vibeViewModel
            )
        }
        composable(NavRoutes.EVENT_PROMO) {
            FeaturePlaceholderScreen(
                title = "Event & Promo Cafe",
                owner = "Rifqi Nabil Al Fath",
                listComponent = "LazyColumn",
                flows = listOf("List Event & Promo", "Detail", "Form Tambah/Edit")
            )
        }
    }
}
