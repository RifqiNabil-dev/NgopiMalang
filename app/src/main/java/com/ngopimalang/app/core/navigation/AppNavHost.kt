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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.features.directory.DirectoryViewModel
import com.ngopimalang.app.features.directory.DirectoryScreen
import com.ngopimalang.app.features.directory.DirectoryDetailScreen
import com.ngopimalang.app.features.auth.AuthViewModel
import com.ngopimalang.app.features.auth.LoginScreen
import com.ngopimalang.app.features.auth.RegisterScreen
import com.ngopimalang.app.features.eventpromo.EventDetailScreen
import com.ngopimalang.app.features.eventpromo.EventFormScreen
import com.ngopimalang.app.features.eventpromo.EventPromoScreen
import com.ngopimalang.app.features.home.HomeScreen
import com.ngopimalang.app.features.profile.ProfileScreen
import com.ngopimalang.app.features.bookmark.BookmarkScreen
import com.ngopimalang.app.features.reviews.ReviewsScreen
import com.ngopimalang.app.features.vibe.VibeScreen

/**
 * Peta seluruh halaman aplikasi.
 *
 * Semua fitur anggota sudah terhubung dengan routing yang lengkap.
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
        // ==================== AUTH ====================
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

        // ==================== HOME & PROFILE ====================
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

        // ==================== DIREKTORI CAFE (Ossa) ====================
        composable(NavRoutes.DIRECTORY) {
            val directoryViewModel: DirectoryViewModel = viewModel()
            val state by directoryViewModel.uiState.collectAsStateWithLifecycle()
            
            DirectoryScreen(
                state = state,
                onSearchChange = directoryViewModel::onSearchChange,
                onToggleWifi = directoryViewModel::toggleWifi,
                onTogglePlugs = directoryViewModel::togglePlugs,
                onResetFilters = directoryViewModel::resetFilters,
                onCafeClick = { cafeId ->
                    navController.navigate("directory/cafe/$cafeId")
                }
            )
        }

        composable(
            route = NavRoutes.DIRECTORY_DETAIL,
            arguments = listOf(navArgument("cafeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cafeId = backStackEntry.arguments?.getString("cafeId")
            val directoryViewModel: DirectoryViewModel = viewModel()
            val cafe = directoryViewModel.findCafe(cafeId)
            
            DirectoryDetailScreen(
                cafe = cafe,
                onBack = { navController.popBackStack() }
            )
        }

        // ==================== ULASAN & RATING (Shelfina) ====================
        composable(NavRoutes.REVIEWS) {
            ReviewsScreen()
        }

        // ==================== SAVED SPOT / BOOKMARK (Shananda) ====================
        composable(NavRoutes.BOOKMARK) {
            BookmarkScreen()
        }

        // ==================== KATEGORI VIBE (Evia) ====================
        composable(NavRoutes.VIBE) {
            VibeScreen(
                onCafeClick = { cafe ->
                    navController.navigate("directory/cafe/${cafe.id}")
                },
                onVibeDetail = { vibeId ->
                    // Placeholder: bisa dikembangkan jadi detail vibe page
                    // Untuk Progress 1, cukup navigasi ke directory
                    navController.navigate(NavRoutes.DIRECTORY)
                },
                onAddVibe = {
                    // Placeholder: form tambah vibe (Progress 2)
                }
            )
        }

        // ==================== EVENT & PROMO (Rifqi/Nabil) ====================
        composable(NavRoutes.EVENT_PROMO) {
            EventPromoScreen(
                onEventClick = { eventId ->
                    navController.navigate(NavRoutes.eventDetailRoute(eventId))
                },
                onAddEvent = {
                    navController.navigate(NavRoutes.eventFormRoute())
                }
            )
        }

        composable(
            route = NavRoutes.EVENT_DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventDetailScreen(
                eventId = eventId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(NavRoutes.eventFormRoute(id))
                }
            )
        }

        composable(
            route = NavRoutes.EVENT_FORM,
            arguments = listOf(navArgument("eventId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            EventFormScreen(
                eventId = eventId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
