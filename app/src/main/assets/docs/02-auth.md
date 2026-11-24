# Fitur Dasar Login & Register

Bagian ini **sudah dikerjakan** dan dipakai semua fitur. Kamu tidak perlu membuatnya,
cukup tahu cara kerjanya kalau layarmu butuh data pengguna.

---

## 1. Cara login saat demo (Progress 2)

Autentikasi sudah memakai Supabase Auth. Tidak ada lagi akun palsu.

| Peran | Akun | Cara masuk |
|---|---|---|
| Admin | dibuat lewat Dashboard → Authentication → Users → Add user (`admin@ngopimalang.app`) | Masuk dengan email + password itu |
| User biasa | hasil Register di aplikasi | Daftar → langsung masuk (bila "Confirm email" dimatikan) |

- Password minimal 6 karakter, sama seperti aturan Supabase.
- Sesi disimpan di perangkat: aplikasi ditutup lalu dibuka lagi masih tetap masuk sampai menekan **Keluar**.
- Tab **Profil** menampilkan nama + email dari sesi, plus lencana **ADMIN** bila `user_roles` akun itu berisi `role='admin'`. Lencana itu bukti role terbaca dari database; kalau akun admin tidak menampilkan lencana, baris `user_roles`-nya belum ada.
- Kalau "Confirm email" masih aktif, akun baru harus dikonfirmasi lewat email dulu; matikan di Authentication → Providers → Email supaya demo lancar. Bila masih aktif, aplikasi menampilkan "Akun dibuat. Cek email untuk konfirmasi".
- Register menolak email dengan domain rekaan (mis. `nama@ngopimalang.app`): Supabase memeriksa apakah domainnya bisa menerima email. Untuk akun user pakai email asli (Gmail dan sejenisnya); akun admin dibuat dari Dashboard sehingga tidak terkena aturan ini.

### Mengatur koneksi (sekali per orang)

1. Supabase Dashboard → **Project Settings → API**, salin **Project URL** dan **anon public key**.
2. Buka `local.properties` di root proyek (tidak ikut GitHub), isi:

   ```
   SUPABASE_URL=https://xxxxxxxx.supabase.co
   SUPABASE_ANON_KEY=eyJhbGciOi...
   ```

3. Build ulang. Kalau dua baris itu kosong, login menampilkan pesan "Supabase belum diatur",
   bukan crash.

Anon key memang dirancang publik dan tetap ada di dalam APK; yang tidak boleh beredar
adalah **service_role key**.

---

## 2. File yang terlibat

| File | Isi |
|---|---|
| `features/auth/AuthRepository.kt` | satu-satunya tempat yang memanggil Supabase Auth: `signIn`, `signUp`, `signOut`, `restoreSession` |
| `core/data/Supabase.kt` | objek client Supabase (URL + anon key dari `BuildConfig`) |
| `core/data/ApiError.kt` | `Throwable.toUserMessage()` — kode error Postgres/Auth jadi kalimat Indonesia |
| `features/auth/AuthViewModel.kt` | `AuthUiState` + fungsi `onEmailChange`, `onPasswordChange`, `onNameChange`, `login()`, `register()`, `logout()`; memegang `AuthRepository` |
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
| `userId` | UUID pengguna dari sesi; dipakai fitur lain saat menulis `reviews`/`bookmarks` |
| `role` | `"admin"` atau `"user"`, dibaca dari tabel `user_roles` |
| `isAdmin` | turunan `role == "admin"`, untuk menampilkan tombol tambah/edit |

Kalau layarmu butuh nama/email pengguna, ambil dari state ini — **jangan** menyimpan
sendiri di layar fiturmu.

---

## 3. Alur yang sudah terpasang

1. `MainActivity` → `NgopiMalangApp` membuat satu `AuthViewModel` untuk seluruh aplikasi.
2. `AuthViewModel.init` memanggil `repository.restoreSession()`: kalau sesi masih tersimpan,
   pengguna langsung masuk ke Beranda tanpa mengetik ulang.
3. Tekan **Masuk** → `repository.signIn(email, password)` → Supabase Auth mengembalikan sesi.
4. Setelah sesi ada, `accountFor()` membaca `profiles.display_name` dan `user_roles.role`,
   lalu mengisi `AuthUiState` (`name`, `email`, `userId`, `role`, `isLoggedIn`).
5. `AppNavHost` melihat `isLoggedIn` berubah → pindah ke Beranda.

Untuk anggota: **jangan** memanggil Supabase Auth dari layar fitur. Butuh `userId`?
Ambil dari `AuthUiState.userId`. Butuh nama penulis ulasan? Ambil dari `user_roles`/`profiles`
lewat query join di repositorymu, bukan dari state auth, karena state hanya berisi pengguna
yang sedang login.

### Kalau login gagal, pesannya dari mana?

`ApiError.toUserMessage()`: `Invalid login credentials` → "Email atau password salah",
`Email not confirmed` → pesan konfirmasi, jaringan mati → "Tidak bisa terhubung ke server".
Pesan ini yang tampil di bawah kolom password, jadi tidak perlu `try/catch` di layar.

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
