# features/directory — Direktori Cafe

**Pemilik:** Ossa Amelia Zuhra Lubis (245150600111022)

Implementasi Progress 1: `DirectoryScreen` (LazyVerticalGrid 2 kolom) → `DirectoryDetailScreen`, dengan `DirectoryViewModel` (StateFlow).

- Pencarian nama/alamat, tanpa membedakan kapital dan spasi luar.
- Filter Wi-Fi dan colokan bisa digabung dengan pencarian.
- Jumlah hasil, pesan hasil kosong, tombol reset, dan penanganan ID detail tidak valid.
- Menggunakan CafeCard dan tema bersama; data contoh hanya diakses ViewModel.
- Route detail: `directory/cafe/{cafeId}`. Navigasi disambungkan di AppNavHost.

## Pengujian manual

1. Login contoh, buka Direktori: enam cafe dalam dua kolom.
2. Cari `SAE`, lalu ` semarang `: hasil Kopi Sae dan Ruang Seduh.
3. Aktifkan Wi-Fi: Ruang Seduh dan Teras Kopi Dago tidak tampil.
4. Gabungkan pencarian dan filter; kata kunci tidak cocok menampilkan pesan dan reset.
5. Buka detail lalu kembali: pencarian dan filter tetap aktif.
6. Periksa mode gelap dan ukuran huruf besar.

## Pekerjaan versi akhir

CafeFormScreen, CRUD Supabase dan unggah/hapus foto belum dibuat. Data belum persisten.
Foto masih placeholder bawaan CafeCard; model bersama belum punya URL foto atau kualitas Wi-Fi.
Integrasi perlu skema cafes, hak akses pengguna, Storage bucket, konfigurasi Supabase,
dan kesepakatan perubahan model bersama dengan tim. Jangan menyimpan service-role key di aplikasi.

Branch lokal: `codex/direktori-cafe-ossa`, belum di-push.

- Model bersama: `com.ngopimalang.app.core.model.Cafe`
- Dummy data sementara: `com.ngopimalang.app.core.data.DummyData.cafes`
- Route: `NavRoutes.DIRECTORY` (ganti placeholder di `AppNavHost`)

---

📖 **Panduan lengkap:** [`docs/04-directory.md`](../../../../../../assets/docs/04-directory.md)
