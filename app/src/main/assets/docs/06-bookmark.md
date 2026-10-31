# Fitur Saved Spot / Bookmark — bahan & tugas

> **Pemilik:** Shananda Putri Aisyah — folder `features/bookmark/` baru berisi `README.md`.
> **Panduan** yang isinya bahan yang sudah tersedia + daftar tugas. ikuti aturan di [08-komponen-bersama.md](08-komponen-bersama.md).

---

## 1. Bahan yang sudah disiapkan

### 1.1 Model

Belum ada model bookmark, dan **memang kamu yang membuatnya** di folder fitur sendiri.
Contoh bentuk data yang masuk akal (boleh kamu sesuaikan):

| Field | Tipe | Keterangan |
|---|---|---|
| `id` | `String` | id bookmark |
| `cafeId` | `String` | id cafe yang disimpan (ambil dari `Cafe.id`) |
| `cafeName` | `String` | nama cafe (untuk ditampilkan) |
| `note` | `String` | catatan pribadi, misal "enak buat nugas sore" |
| `savedAtLabel` | `String` | `"Disimpan 3 hari lalu"` |

Aturan proyek: model milik fitur ditaruh di `features/bookmark/`, **bukan** di `core/model/`.

### 1.2 Data yang bisa dipakai

- `core/data/DummyData.kt` → `DummyData.cafes` (6 cafe) untuk mengambil id & nama cafe.
- Belum ada data bookmark dummy. Buat 2–3 data awal di dalam `BookmarkViewModel`
  supaya layar tidak kosong saat didemokan.

### 1.3 Komponen siap pakai — `core/components/`

| Komponen | Untuk apa |
|---|---|
| `CafeCard(cafe, modifier, onClick)` | menampilkan cafe yang disimpan (butuh objek `Cafe`, cari lewat `cafeId`) |
| `SectionHeader(title, actionLabel)` | judul bagian, misal "Tersimpan" |
| `InfoBadge(text, active)` | label kecil seperti `WiFi` |

### 1.4 Navigasi

Route `NavRoutes.BOOKMARK` sudah ada di `AppNavHost.kt` dan **tidak tampil di bottom bar** —
dibuka dari Profil → "Saved Spot". Isinya masih `FeaturePlaceholderScreen(...)`.
Ganti blok itu dengan layarmu.

---

## 2. Yang perlu kamu buat

| # | File | Isi |
|---|---|---|
| 1 | `features/bookmark/BookmarkItem.kt` | model bookmark (tabel di atas) |
| 2 | `features/bookmark/BookmarkViewModel.kt` | state + aksi simpan/hapus/ubah catatan |
| 3 | `features/bookmark/BookmarkScreen.kt` | daftar saved spot + empty state |
| 4 | update `README.md` di folder fitur | catatan singkat apa yang sudah dibuat |

Rincian tugas:

1. **State** — daftar bookmark dan status kosong/terisi.
2. **Aksi** — `toggleBookmark(cafe)`, `removeBookmark(id)`, `updateNote(id, note)`.
   Semua mengubah state di ViewModel (belum perlu database).
3. **Layar** — daftar vertikal (`LazyColumn`) berisi kartu saved spot + tombol hapus.
4. **Empty state** — kalau daftar kosong, tampilkan teks ajakan + ikon, jangan layar putih.
5. **Opsional** — tombol "Simpan" di layar detail cafe (koordinasi dengan Ossa/Rifqi kalau
   ingin menyambung ke layar mereka).

---

## 3. Petunjuk pengerjaan

- Komponen Compose yang perlu dibaca dokumentasinya: `LazyColumn`, `Card`,
  `IconButton`, `SwipeToDismissBox` (kalau mau hapus dengan geser), `AlertDialog`
  (konfirmasi hapus).
- Untuk menampilkan `CafeCard` kamu butuh objek `Cafe`, bukan cuma id:
  cari dengan `DummyData.cafes.firstOrNull { it.id == item.cafeId }`.
  Kalau hasilnya `null`, tampilkan kartu sederhana buatan sendiri.
- Simpan daftar di `_uiState.update { state -> state.copy(bookmarks = ...) }` —
  jangan pakai `mutableListOf` yang diubah langsung, layar tidak akan ikut berubah.
- Pola ViewModel: lihat bagian 5 di [08-komponen-bersama.md](08-komponen-bersama.md).

---

## 4. Jebakan

| Gejala | Sebab | Perbaikan |
|---|---|---|
| Klik hapus tapi list tidak berubah | mengubah list lama, bukan membuat list baru | buat list baru di `copy(...)` / `filterNot` |
| Layar putih saat belum ada bookmark | tidak ada empty state | tambahkan tampilan khusus untuk list kosong |
| Crash saat ambil cafe | `first { }` untuk id yang tidak ada | pakai `firstOrNull { }` dan tangani `null` |
| Catatan hilang setelah pindah tab | catatan disimpan di composable | simpan di state ViewModel |



---

📖 **Kembali ke indeks:** [README.md](README.md) · **Aturan umum:** [08-komponen-bersama.md](08-komponen-bersama.md)
