package com.ngopimalang.app.features.auth

import com.ngopimalang.app.core.data.Supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Baris tabel `profiles` yang dipakai aplikasi. */
@Serializable
data class UserProfile(
    val id: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_path") val avatarPath: String? = null
)

/** Baris tabel `user_roles`. Role tidak disimpan di `profiles`. */
@Serializable
data class UserRole(val role: String = "user")

/** Data akun yang sudah siap dipakai layar: sesi + profil + role. */
data class Account(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String
) {
    val isAdmin: Boolean get() = role == "admin"
}

/**
 * Satu pintu ke Supabase Auth dan tabel profil.
 *
 * ViewModel tidak pernah memanggil `client` langsung, jadi kalau alur auth
 * berubah, hanya file ini yang disentuh.
 */
class AuthRepository {

    private val client get() = Supabase.client

    /**
     * Mengembalikan akun yang sesinya masih tersimpan di perangkat,
     * atau null kalau belum pernah login.
     */
    suspend fun restoreSession(): Account? {
        client.auth.awaitInitialization()
        val user = client.auth.currentUserOrNull() ?: return null
        return accountFor(user.id, user.email.orEmpty())
    }

    suspend fun signIn(email: String, password: String): Account {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        val user = client.auth.currentUserOrNull()
            ?: throw IllegalStateException("Sesi tidak terbentuk setelah login")
        return accountFor(user.id, user.email.orEmpty())
    }

    /**
     * Mendaftar akun baru. Trigger `handle_new_user` di database yang membuat
     * baris `profiles` dan `user_roles`, jadi aplikasi tidak perlu INSERT manual.
     *
     * @return akun bila langsung mendapat sesi, atau null bila email masih
     *         menunggu konfirmasi (fitur "Confirm email" aktif di Supabase).
     */
    suspend fun signUp(displayName: String, email: String, password: String): Account? {
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject {
                put("display_name", displayName.trim())
            }
        }
        val user = client.auth.currentUserOrNull() ?: return null
        return accountFor(user.id, user.email.orEmpty())
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    private suspend fun accountFor(userId: String, email: String): Account {
        val profile = client.postgrest["profiles"]
            .select {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeSingleOrNull<UserProfile>()

        val role = client.postgrest["user_roles"]
            .select {
                filter { eq("user_id", userId) }
                limit(1)
            }
            .decodeSingleOrNull<UserRole>()

        return Account(
            id = userId,
            email = email,
            displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@"),
            role = role?.role ?: "user"
        )
    }
}
