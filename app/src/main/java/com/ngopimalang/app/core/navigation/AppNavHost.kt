package com.ngopimalang.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ngopimalang.app.core.ui.FeaturePlaceholderScreen
import com.ngopimalang.app.features.auth.AuthViewModel
import com.ngopimalang.app.features.auth.LoginScreen
import com.ngopimalang.app.features.auth.RegisterScreen
import com.ngopimalang.app.features.home.HomeScreen
import com.ngopimalang.app.features.profile.ProfileScreen
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


        composable(NavRoutes.VIBE) {
            FeaturePlaceholderScreen(
                title = "Kategori Vibe Cafe",
                owner = "Evia Vinata",
                listComponent = "HorizontalPager + TabRow",
                flows = listOf("Daftar Vibe", "Detail Vibe", "Form Vibe")
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
