package com.ngopimalang.app.core.data

import com.ngopimalang.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Satu koneksi Supabase untuk seluruh aplikasi.
 *
 * URL dan anon key dibaca dari `local.properties` (tidak ikut git) lalu masuk
 * ke `BuildConfig`. Anon key memang dirancang publik; keamanan data dipegang
 * RLS di database, bukan dipegang kunci ini.
 */
object Supabase {

    /** False kalau `local.properties` belum diisi, supaya layar bisa memberi pesan jelas. */
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val client: SupabaseClient by lazy {
        check(isConfigured) {
            "SUPABASE_URL / SUPABASE_ANON_KEY belum diisi di local.properties"
        }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
