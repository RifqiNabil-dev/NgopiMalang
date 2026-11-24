package com.ngopimalang.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import com.ngopimalang.app.features.directory.CafeFormScreen
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
import com.ngopimalang.app.features.home.HomeCafeViewModel
import com.ngopimalang.app.features.profile.ProfileScreen
import com.ngopimalang.app.features.vibe.VibeDetailScreen
import com.ngopimalang.app.features.vibe.VibeFormScreen
import com.ngopimalang.app.features.vibe.VibeScreen
import com.ngopimalang.app.features.vibe.VibeViewModel
import com.ngopimalang.app.features.bookmark.BookmarkScreen
import com.ngopimalang.app.features.reviews.ReviewsScreen

@Serializable
data class CafeFormRoute(val cafeId: String? = null)
@Serializable
data class CafeDetailRoute(val cafeId: String)

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
            val homeViewModel: HomeCafeViewModel = viewModel()
            val cafeState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val owner = LocalLifecycleOwner.current
            DisposableEffect(owner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) homeViewModel.refresh()
                }
                owner.lifecycle.addObserver(observer)
                onDispose { owner.lifecycle.removeObserver(observer) }
            }
            HomeScreen(
                userName = authState.name.ifBlank { "Ngopi Lover" },
                onOpenDirectory = { navController.navigate(NavRoutes.DIRECTORY) },
                onOpenVibe = { navController.navigate(NavRoutes.VIBE) },
                onOpenEventPromo = { navController.navigate(NavRoutes.EVENT_PROMO) },
                onOpenReviews = { navController.navigate(NavRoutes.REVIEWS) },
                onOpenBookmark = { navController.navigate(NavRoutes.BOOKMARK) },
                cafeState = cafeState,
                onRetryCafes = homeViewModel::refresh,
                onCafeClick = { navController.navigate(CafeDetailRoute(it)) }
            )
        }

        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                userName = authState.name.ifBlank { "Ngopi Lover" },
                userEmail = authState.email.ifBlank { "belum-ada-email@ngopimalang.app" },
                isAdmin = authState.isAdmin,
                onOpenBookmark = { navController.navigate(NavRoutes.BOOKMARK) },
                onOpenReviews = { navController.navigate(NavRoutes.REVIEWS) },
                onLogout = authViewModel::logout
            )
        }

        // ==================== DIREKTORI CAFE (Ossa) ====================
        composable(NavRoutes.DIRECTORY) {
            val directoryViewModel: DirectoryViewModel = viewModel()
            val state by directoryViewModel.uiState.collectAsStateWithLifecycle()
            val owner = LocalLifecycleOwner.current
            DisposableEffect(owner, authState.isAdmin) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) directoryViewModel.configure(authState.isAdmin)
                }
                owner.lifecycle.addObserver(observer)
                directoryViewModel.configure(authState.isAdmin)
                onDispose { owner.lifecycle.removeObserver(observer) }
            }
            
            DirectoryScreen(
                state = state,
                onSearchChange = directoryViewModel::onSearchChange,
                onToggleWifi = directoryViewModel::toggleWifi,
                onTogglePlugs = directoryViewModel::togglePlugs,
                onResetFilters = directoryViewModel::resetFilters,
                onRetry = { directoryViewModel.refresh() },
                onAdd = { navController.navigate(CafeFormRoute()) },
                onCafeClick = { cafeId ->
                    navController.navigate(CafeDetailRoute(cafeId))
                }
            )
        }

        composable(
            route = NavRoutes.DIRECTORY_DETAIL,
            arguments = listOf(navArgument("cafeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cafeId = backStackEntry.arguments?.getString("cafeId")
            DirectoryDetailScreen(
                cafeId = cafeId.orEmpty(),
                isAdmin = authState.isAdmin,
                onEdit = { navController.navigate(CafeFormRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<CafeFormRoute> { entry ->
            val route = entry.toRoute<CafeFormRoute>()
            CafeFormScreen(cafeId = route.cafeId, isAdmin = authState.isAdmin,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(NavRoutes.DIRECTORY, inclusive = false)
                    if (navController.currentDestination?.route != NavRoutes.DIRECTORY)
                        navController.navigate(NavRoutes.DIRECTORY)
                })
        }

        // Typed entry point for new modules. Legacy string route above remains an adapter.
        composable<CafeDetailRoute> { entry ->
            val route = entry.toRoute<CafeDetailRoute>()
            DirectoryDetailScreen(cafeId = route.cafeId, isAdmin = authState.isAdmin,
                onEdit = { navController.navigate(CafeFormRoute(it)) },
                onBack = { navController.popBackStack() })
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
        composable(NavRoutes.VIBE) { entry ->
            val vibeViewModel: VibeViewModel = viewModel(entry)
            VibeScreen(
                onCafeClick = { cafe -> navController.navigate("directory/cafe/${cafe.id}") },
                onVibeDetail = { id -> navController.navigate(NavRoutes.vibeDetail(id)) },
                onAddVibe = { navController.navigate(NavRoutes.vibeForm()) },
                vibeViewModel = vibeViewModel
            )
        }
        composable(
            route = NavRoutes.VIBE_DETAIL,
            arguments = listOf(navArgument("vibeId") { type = NavType.StringType })
        ) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(NavRoutes.VIBE) }
            val vibeViewModel: VibeViewModel = viewModel(parentEntry)
            VibeDetailScreen(
                vibeId = entry.arguments?.getString("vibeId").orEmpty(),
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(NavRoutes.vibeForm(id)) },
                onCafeClick = { cafe -> navController.navigate("directory/cafe/${cafe.id}") },
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
            val parentEntry = remember(entry) { navController.getBackStackEntry(NavRoutes.VIBE) }
            val vibeViewModel: VibeViewModel = viewModel(parentEntry)
            VibeFormScreen(
                vibeId = entry.arguments?.getString("vibeId"),
                onBack = { navController.popBackStack() },
                vibeViewModel = vibeViewModel
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
