package com.ngopimalang.app.core.data

/**
 * Menerjemahkan error Supabase/Postgres menjadi kalimat yang bisa dibaca pengguna.
 *
 * Dipakai semua repository: `catch (e: Exception) { e.toUserMessage() }`.
 * Kode error database dijelaskan di dokumen 09 bagian 12.
 */
fun Throwable.toUserMessage(): String {
    val raw = message.orEmpty()
    val text = listOf(raw, cause?.message.orEmpty()).joinToString(" ").lowercase()

    return when {
        // --- Auth ---
        "invalid login credentials" in text || "invalid_credentials" in text ->
            "Email atau password salah"

        "email not confirmed" in text || "email_not_confirmed" in text ->
            "Email belum dikonfirmasi. Cek kotak masuk email kamu"

        "user already registered" in text || "already been registered" in text ->
            "Email ini sudah terdaftar. Silakan masuk"

        "password should be at least" in text || "weak_password" in text ->
            "Password terlalu lemah. Gunakan minimal 6 karakter"

        "is invalid" in text || "unable to validate email" in text || "invalid email" in text ->
            "Email tidak valid atau domainnya tidak bisa menerima email. Pakai email asli, mis. Gmail"

        "rate limit" in text || "too many requests" in text ->
            "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi"

        // --- Jaringan ---
        "unable to resolve host" in text || "failed to connect" in text ||
            "connectexception" in text || "unknownhost" in text ->
            "Tidak bisa terhubung ke server. Periksa koneksi internet"

        "timeout" in text || "timed out" in text ->
            "Server terlalu lama merespons. Coba lagi"

        // --- Postgres / PostgREST (dokumen 09 bagian 12) ---
        "42501" in text || "row-level security" in text || "permission denied" in text ->
            "Kamu tidak punya hak untuk tindakan ini"

        "23505" in text || "duplicate key" in text || "already exists" in text ->
            "Data ini sudah ada"

        "23503" in text || "foreign key" in text ->
            "Data ini masih dipakai di tabel lain. Arsipkan saja, jangan dihapus"

        "23514" in text || "violates check constraint" in text ->
            "Isian tidak valid. Periksa kembali tanggal dan angkanya"

        "23502" in text || "not-null" in text || "null value in column" in text ->
            "Ada isian wajib yang masih kosong"

        "pgrst116" in text || "json object requested" in text ->
            "Data tidak ditemukan"

        "pgrst301" in text || "jwt" in text || "401" in text ->
            "Sesi sudah berakhir. Silakan masuk lagi"

        raw.isBlank() -> "Terjadi kesalahan. Coba lagi"
        else -> "Gagal memuat data: ${raw.take(120)}"
    }
}
