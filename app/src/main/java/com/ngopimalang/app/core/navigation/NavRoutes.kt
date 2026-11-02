package com.ngopimalang.app.core.navigation

/**
 * Daftar route aplikasi. Satu route per fitur, dipetakan di [AppNavHost].
 * Anggota yang menambah screen baru cukup menambah const di sini lalu daftarkan composable-nya.
 */
object NavRoutes {
    // Fondasi aplikasi
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val HOME = "home"
    const val PROFILE = "profile"

    // Fitur anggota
    const val DIRECTORY = "directory"      // Ossa - Direktori Cafe
    const val DIRECTORY_DETAIL = "directory/cafe/{cafeId}"
    const val REVIEWS = "reviews"          // Shelfina - Ulasan & Rating
    const val BOOKMARK = "bookmark"        // Shananda - Saved Spot / Bookmark
    const val VIBE = "vibe"                // Evia - Kategori Vibe Cafe
    const val EVENT_PROMO = "event_promo"  // Rifqi - Event & Promo Cafe

    /** Route yang tampil sebagai tab pada bottom navigation. */
    val bottomBarRoutes = setOf(HOME, DIRECTORY, VIBE, EVENT_PROMO, PROFILE)
}
