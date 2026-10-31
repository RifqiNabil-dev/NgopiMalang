# Fitur Dasar Login & Register

Bagian ini **sudah dikerjakan** dan dipakai semua fitur. Kamu tidak perlu membuatnya,
cukup tahu cara kerjanya kalau layarmu butuh data pengguna.

---

## 1. Cara login saat demo (Progress 1)

- Email: **apa saja** yang formatnya mirip email (contoh `nama@ngopimalang.app`)
- Password: **minimal 6 karakter**
- Register: isi nama, email, password → langsung masuk

Belum ada database. Semua masih data sementara di memori (hilang kalau aplikasi ditutup).

---

## 2. File yang terlibat

| File | Isi |
|---|---|
| `features/auth/AuthViewModel.kt` | `AuthUiState` + fungsi `onEmailChange`, `onPasswordChange`, `onNameChange`, `login()`, `register()`, `logout()` |
| `features/auth/LoginScreen.kt` | form login (menerima state + lambda, tanpa logika) |
| `features/auth/RegisterScreen.kt` | form daftar |
| `core/navigation/AppNavHost.kt` | mengatur perpindahan Login → Beranda setelah berhasil masuk |
| `NgopiMalangApp.kt` | menyimpan satu `AuthViewModel` untuk seluruh aplikasi |

### Isi `AuthUiState`

| Field | Arti |
|---|---|
| `name` | nama pengguna (dipakai di Beranda & Profil) |
| `email` | email yang sedang login |
| `password` | isi kolom password |
| `isLoading` | true saat proses masuk berjalan |
| `errorMessage` | pesan kesalahan yang ditampilkan di form |
| `isLoggedIn` | penentu pindah ke Beranda |

Kalau layarmu butuh nama/email pengguna, ambil dari state ini — **jangan** menyimpan
sendiri di layar fiturmu.

---

## 3. Kalau mau mengganti ke Supabase (Progress 2)

Yang perlu diubah hanya `AuthViewModel.kt`:

1. Tambahkan dependency Supabase di `gradle/libs.versions.toml` dan `app/build.gradle.kts`.
2. Buat objek client Supabase (URL + anon key diambil dari `local.properties`,
   **jangan** ditulis langsung di kode).
3. Di `login()` / `register()`, ganti pengecekan lokal dengan panggilan
   `supabase.auth.signInWith(...)` / `signUpWith(...)`.
4. `isLoggedIn` diisi dari hasil panggilan tersebut, bukan dari pengecekan manual.
5. Layar Login/Register **tidak perlu diubah**, karena hanya menerima state dan lambda.

---

## 4. Yang perlu dijaga

| Hal | Aturan |
|---|---|
| Password | jangan pernah ditampilkan/di-log |
| Kunci Supabase | simpan di `local.properties`, jangan di kode atau GitHub |
| Logout | panggil `authViewModel.logout()`, jangan mengubah state dari layar |
| Navigasi | tetap lewat `AppNavHost`, jangan pindah layar manual dari dalam composable |

---

📖 **Kembali ke indeks:** [README.md](README.md)
