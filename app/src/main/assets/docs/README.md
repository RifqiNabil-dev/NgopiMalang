# Panduan Proyek NgopiMalang

Aplikasi katalog cafe Malang — tugas akhir PAPB PTI A.
Tahap sekarang: **Progress 1 — halaman antarmuka, data masih dummy (belum ada database).**

> **Isi panduan ini: bahan + tugas, bukan solusi.**
> Setiap dokumen fitur hanya menjelaskan **apa yang sudah disediakan** (model, data dummy,
> komponen, route) dan **apa yang perlu kamu buat**. Kode layarnya kamu tulis sendiri —
> tidak ada yang bisa di-copy paste jadi. Kalau mentok, tanya pemilik fondasi (Rifqi) atau
> diskusikan di grup, jangan minta jawaban jadi.

> **Lokasi folder ini:** `app/src/main/assets/docs/`
> Di Android Studio, buka panel Project dalam tampilan **Android** → `app` → **`assets`** → **`docs`**.
> (Kalau `assets` belum kelihatan, klik kanan `app` → *Reload from Disk* atau lakukan Gradle Sync sekali.)
> Semua file panduan sengaja ditaruh di dalam modul `app` supaya ikut terlihat di tampilan Android.
> File `.md` ini ikut masuk ke dalam APK, tapi ukurannya kecil (~90 KB) dan tidak mengganggu aplikasi.

---

## 1. Cara menjalankan proyek

**Lewat Android Studio (paling mudah)**

1. Buka Android Studio → **Open** → pilih folder `NgopiMalang`.
2. Tunggu **Gradle sync** selesai (bar bawah hijau).
3. Pilih emulator di toolbar atas (AVD `Small_Phone`) atau colok HP Android.
4. Tekan tombol **Run ▶**.

**Lewat terminal**

```bash
cd ~/Projects/AndroidStudioProjects/NgopiMalang
export JAVA_HOME=/opt/android-studio/jbr
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug
```

Hasil APK: `app/build/outputs/apk/debug/app-debug.apk`

**Cara login:** pakai email dan password apa saja (password minimal 6 karakter).
Tidak perlu akun asli, karena autentikasi masih dummy (Progress 1).

---

## 2. Siapa mengerjakan apa

| Folder | Fitur | Pemilik | Panduan |
|---|---|---|---|
| `features/auth` | Login & Register | bersama (fondasi) | [02-auth.md](02-auth.md) |
| `features/home` | Beranda | bersama (fondasi) | [08-komponen-bersama.md](08-komponen-bersama.md) |
| `features/profile` | Profil & logout | bersama (fondasi) | [08-komponen-bersama.md](08-komponen-bersama.md) |
| `features/eventpromo` | Event & Promo Cafe | **Rifqi** | [03-eventpromo.md](03-eventpromo.md) |
| `features/directory` | Direktori Cafe | **Ossa** | [04-directory.md](04-directory.md) |
| `features/reviews` | Ulasan & Rating | **Shelfina** | [05-reviews.md](05-reviews.md) |
| `features/bookmark` | Saved Spot / Bookmark | **Shananda** | [06-bookmark.md](06-bookmark.md) |
| `features/vibe` | Kategori Vibe Cafe | **Evia** | [07-vibe.md](07-vibe.md) |

Yang **bukan** milik siapa-siapa (jangan diubah tanpa bicara dulu):
`core/` (model, data dummy, navigasi, komponen bersama) dan `ui/theme/` (tema & warna).

**Urutan membaca yang disarankan untuk setiap anggota:**

1. [08-komponen-bersama.md](08-komponen-bersama.md) — aturan warna & ukuran, daftar komponen siap pakai, pola ViewModel
2. Panduan fitur kalian sendiri di tabel atas — isinya bahan + daftar tugas

---

## 3. Tiga aturan wajib

### Aturan 1 — Warna selalu dari tema, jangan tulis hex di layar

❌ **Salah:**
```kotlin
Text(color = Color(0xFF6650A4))
Box(modifier = Modifier.background(Color.White))
```

✅ **Benar:**
```kotlin
Text(color = MaterialTheme.colorScheme.primary)
Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface))
```

Kenapa: kalau semua layar pakai `MaterialTheme.colorScheme.*`, warna aplikasi bisa diganti
dari satu tempat dan layar kalian tidak perlu disentuh. Satu-satunya tempat yang boleh
berisi kode warna hex adalah `ui/theme/Color.kt` — lihat
[08-komponen-bersama.md](08-komponen-bersama.md) bagian 1 dan 2.

### Aturan 2 — Satu fitur, satu package

❌ Jangan taruh semua layar di `ui/screens/` atau di `features/` langsung.
✅ Buat semua file fitur kalian di dalam folder fitur masing-masing, misalnya semua file
Ossa di `features/directory/`.

Kalau butuh data yang dipakai bersama (misal daftar cafe), pakai model di `core/model/`
dan data dummy di `core/data/DummyData.kt`. Jangan buat model duplikat di folder fitur.

### Aturan 3 — Pakai ViewModel + StateFlow (MVVM)

Layar (Composable) hanya menampilkan data. Logika dan data ada di ViewModel.

Polanya: `data class XxxUiState` (semua field punya nilai default) → `MutableStateFlow` di
ViewModel → layar membaca `viewModel.uiState.collectAsStateWithLifecycle()`.
Penjelasan 4 langkahnya ada di [08-komponen-bersama.md](08-komponen-bersama.md) bagian 5.
Contoh nyata yang sudah jadi (boleh dibaca sebagai referensi pola): `features/auth/AuthViewModel.kt`.

---

## 4. Alur menyambungkan layarmu (3 langkah, tanpa kode jadi)

**Langkah 1 — buat layar di folder fitur kalian.**
Nama file: `features/<fitur>/<Nama>Screen.kt`, dengan satu `@Composable fun <Nama>Screen(...)`
yang menerima data/state dan lambda untuk aksi (misal `onCafeClick: (String) -> Unit`).
Layar **tidak boleh** memanggil `navController` langsung — cukup panggil lambda, supaya
layar bisa di-preview di Android Studio tanpa crash.

**Langkah 2 — (hanya kalau butuh halaman baru) tambah route.**
Tambahkan satu `const val` di `core/navigation/NavRoutes.kt`. Kalau halaman itu harus jadi
tab di bottom bar, tambahkan juga ke `bottomBarRoutes`; kalau tidak, biarkan.

**Langkah 3 — ganti placeholder di `AppNavHost.kt`.**
Cari blok `composable(NavRoutes.<FITURMU>)`, hapus `FeaturePlaceholderScreen(...)` di dalamnya,
ganti dengan layarmu. Bentuk akhirnya hanya selebar ini:

```kotlin
composable(NavRoutes.DIRECTORY) { DirectoryScreen(onCafeClick = { id -> ... }) }
```

Sisanya (isi layar, ViewModel, filter, state) kamu yang tulis.

---

## 5. Daftar panduan

| Dokumen | Isi |
|---|---|
| [02-auth.md](02-auth.md) | Fondasi login/register yang sudah jadi + cara pindah ke Supabase nanti |
| [03-eventpromo.md](03-eventpromo.md) | Bahan & tugas fitur Event & Promo — **Rifqi** |
| [04-directory.md](04-directory.md) | Bahan & tugas fitur Direktori Cafe — **Ossa** |
| [05-reviews.md](05-reviews.md) | Bahan & tugas fitur Ulasan & Rating — **Shelfina** |
| [06-bookmark.md](06-bookmark.md) | Bahan & tugas fitur Saved Spot — **Shananda** |
| [07-vibe.md](07-vibe.md) | Bahan & tugas fitur Kategori Vibe — **Evia** |
| [08-komponen-bersama.md](08-komponen-bersama.md) | Aturan warna & ukuran, katalog komponen siap pakai, pola ViewModel, jebakan umum |

---

## 6. Kalau ada error

| Gejala | Penyebab & solusi |
|---|---|
| `Unresolved reference: MaterialTheme` | Import belum ada. Tekan `Alt+Enter` pada teks merah. |
| `Cannot find a parameter with this name` | Nama parameter salah. Cek panduan komponen di [08-komponen-bersama.md](08-komponen-bersama.md). |
| Layar putih/kosong | Lupa `modifier.fillMaxSize()` atau lupa daftarkan `composable(...)` di `AppNavHost`. |
| Build gagal setelah pull | Jalankan **Build → Clean Project**, lalu **Rebuild**. |
| Tema tidak berubah | Pastikan warna di layar diambil dari `MaterialTheme.colorScheme.*`, bukan hex. |
