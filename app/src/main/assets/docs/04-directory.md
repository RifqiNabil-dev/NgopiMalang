# Fitur Direktori Cafe — bahan & tugas

> **Pemilik:** Ossa Amelia Zuhra Lubis — folder `features/directory/` baru berisi `README.md`.
> **Panduan** yang isinya bahan yang sudah tersedia + daftar tugas. ikuti aturan di [08-komponen-bersama.md](08-komponen-bersama.md).

---

## 1. Bahan yang sudah disiapkan

### 1.1 Model — `core/model/Cafe.kt`

| Field | Tipe | Contoh isi |
|---|---|---|
| `id` | `String` | `"c1"` |
| `name` | `String` | `"Kopi Sae Malang"` |
| `address` | `String` | `"Jl. Soekarno Hatta No. 12, Lowokwaru"` |
| `priceRange` | `String` | `"Rp15.000 - Rp35.000"` |
| `hasWifi` | `Boolean` | `true` |
| `plugCount` | `Int` | `8` (jumlah colokan) |
| `rating` | `Double` | `4.7` |
| `vibes` | `List<String>` | `listOf("Nugas / WFC", "Aesthetic")` |

### 1.2 Data dummy — `core/data/DummyData.kt`

`DummyData.cafes` → **6 cafe** (Kopi Sae Malang, Kedai Tepi Sawah, Bara Coffee House,
Ruang Seduh, dan lainnya). Isinya sudah lengkap, tidak perlu menambah data.

### 1.3 Komponen siap pakai — `core/components/`

| Komponen | Untuk apa |
|---|---|
| `CafeCard(cafe, modifier, onClick)` | kartu satu cafe, sudah berisi nama/alamat/harga/rating/badge |
| `RatingStars(rating, showValue)` | bintang rating, `showValue = true` untuk menampilkan angka |
| `InfoBadge(text, active)` | label kecil seperti `WiFi`, `8 colokan` |
| `SectionHeader(title, actionLabel)` | judul bagian + tombol aksi opsional |

Cara memanggil: `CafeCard(cafe = item, modifier = Modifier.fillMaxWidth(), onClick = { ... })`

### 1.4 Navigasi

Route `NavRoutes.DIRECTORY` sudah ada di `AppNavHost.kt`, tapi isinya masih
`FeaturePlaceholderScreen(...)`. Ganti blok itu dengan layarmu.

---

## 2. Yang perlu kamu buat

| # | File | Isi |
|---|---|---|
| 1 | `features/directory/DirectoryViewModel.kt` | state + logika pencarian & filter |
| 2 | `features/directory/DirectoryScreen.kt` | layar grid daftar cafe |
| 3 | (opsional) `DirectoryDetailScreen.kt` + route baru | detail satu cafe |
| 4 | update `README.md` di folder fitur | catatan singkat apa yang sudah dibuat |

Rincian tugas:

1. **State** — minimal berisi daftar cafe yang ditampilkan, kata kunci pencarian, dan
   (kalau perlu) filter "ada WiFi" / "banyak colokan".
2. **Ambil data** dari `DummyData.cafes` di dalam ViewModel, bukan di composable.
3. **Pencarian** — filter nama/alamat berdasarkan kata kunci; kerjakan di ViewModel.
4. **Layar** — grid 2 kolom berisi `CafeCard`, plus kolom pencarian di atas.
5. **Klik kartu** — arahkan ke layar detail (kalau dibuat) lewat `onCafeClick(cafe.id)`.

---

## 3. Petunjuk pengerjaan

- Komponen Compose yang perlu kamu baca dokumentasinya:
  `LazyVerticalGrid`, `GridCells.Fixed(2)`, `contentPadding`, `Arrangement.spacedBy`,
  `OutlinedTextField` (pencarian), `FilterChip` (filter).
- Grid 2 kolom: item di dalamnya **tidak perlu** diberi lebar, cukup
  `Modifier.fillMaxWidth()` — `CafeCard` sudah mengikuti lebar sel.
- Kalau ingin kartu terlihat seragam, beri `Modifier.fillMaxWidth()` saja; jangan
  `width(200.dp)` karena akan merusak grid.
- Pola ViewModel: lihat bagian 5 di [08-komponen-bersama.md](08-komponen-bersama.md).
- Hasil filter sebaiknya berupa list baru di state, bukan mengubah `DummyData`.

---

## 4. Jebakan

| Gejala | Sebab | Perbaikan |
|---|---|---|
| Kartu tidak sejajar tinggi | isi kartu beda panjang | pastikan `CafeCard` memakai `fillMaxWidth()` dan grid `spacedBy` |
| Pencarian tidak jalan | query disimpan di composable | simpan di ViewModel, panggil `onSearchChange(query)` |
| Layar kosong | list diambil dari `DummyData` di layar tapi salah nama properti | cek nama: `DummyData.cafes` |
| Layar tidak muncul saat tab Direktori diklik | `AppNavHost` masih memakai placeholder | ganti blok `composable(NavRoutes.DIRECTORY)` |



---

📖 **Kembali ke indeks:** [README.md](README.md) · **Aturan umum:** [08-komponen-bersama.md](08-komponen-bersama.md)
