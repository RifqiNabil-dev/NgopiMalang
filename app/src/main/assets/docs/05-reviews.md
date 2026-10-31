# Fitur Ulasan & Rating — bahan & tugas

> **Pemilik:** Shelfina Khayla Anindita — folder `features/reviews/` baru berisi `README.md`.
> **Panduan** yang isinya bahan yang sudah tersedia + daftar tugas. ikuti aturan di [08-komponen-bersama.md](08-komponen-bersama.md).

---

## 1. Bahan yang sudah disiapkan

### 1.1 Model — `core/model/Review.kt`

| Field | Tipe | Contoh isi |
|---|---|---|
| `id` | `String` | `"r1"` |
| `userName` | `String` | `"Dinda Ayu"` |
| `cafeName` | `String` | `"Bara Coffee House"` |
| `rating` | `Int` | `5` (skala 1–5, bukan Double) |
| `comment` | `String` | isi ulasan |
| `favoriteMenu` | `String` | `"Es Kopi Susu Gula Aren"` |
| `dateLabel` | `String` | `"2 hari lalu"` |

### 1.2 Data dummy — `core/data/DummyData.kt`

`DummyData.reviews` → **3 ulasan**. Rating-nya bervariasi (5, 4, ...) supaya filter
bintang terlihat bedanya.

### 1.3 Komponen siap pakai — `core/components/`

| Komponen | Untuk apa |
|---|---|
| `ReviewCard(review, modifier, onClick)` | kartu satu ulasan lengkap (nama, bintang, komentar, menu favorit) |
| `RatingStars(rating, showValue)` | baris bintang, `rating` bertipe `Int` 1–5 |
| `SectionHeader(title, actionLabel)` | judul bagian, misal "Ulasan Terbaru" |

Cara memanggil: `ReviewCard(review = item, modifier = Modifier.fillMaxWidth(), onClick = { ... })`

### 1.4 Navigasi

Route `NavRoutes.REVIEWS` sudah ada di `AppNavHost.kt` (dibuka dari Profil → "Ulasan Saya"),
tapi masih `FeaturePlaceholderScreen(...)`. Ganti blok itu dengan layarmu.

---

## 2. Yang perlu kamu buat

| # | File | Isi |
|---|---|---|
| 1 | `features/reviews/ReviewsViewModel.kt` | state + filter rating |
| 2 | `features/reviews/ReviewsScreen.kt` | daftar ulasan + tab filter |
| 3 | (opsional) `ReviewFormScreen.kt` | form tulis ulasan (belum ada backend, cukup dummy) |
| 4 | update `README.md` di folder fitur | catatan singkat apa yang sudah dibuat |

Rincian tugas:

1. **State** — daftar ulasan yang tampil, rating terpilih (`null` = semua), dan
   (kalau perlu) ringkasan rata-rata rating.
2. **Ambil data** dari `DummyData.reviews` di ViewModel.
3. **Filter rating** — pilihan "Semua / 5 / 4 / 3" dan sejenisnya; logika di ViewModel.
4. **Layar** — daftar vertikal (`LazyColumn`) berisi `ReviewCard`, tab/chip filter di atas.
5. **Form ulasan (opsional)** — kalau dibuat, cukup menambah ke list di state
   (belum disimpan ke database).

---

## 3. Petunjuk pengerjaan

- Komponen Compose yang perlu dibaca dokumentasinya: `LazyColumn`, `ScrollableTabRow`
  atau `FilterChip`, `Arrangement.spacedBy`, `TextField`/`OutlinedTextField` untuk komentar.
- `ReviewCard` di dalam `LazyColumn` cukup diberi `Modifier.fillMaxWidth()`.
- Rating bertipe **Int**, jadi jangan dibandingkan dengan `4.5` — pakai angka bulat.
- Rata-rata rating: hitung di ViewModel (`reviews.map { it.rating }.average()`),
  jangan di composable.
- Pola ViewModel: lihat bagian 5 di [08-komponen-bersama.md](08-komponen-bersama.md).

---

## 4. Jebakan

| Gejala | Sebab | Perbaikan |
|---|---|---|
| Semua ulasan hilang saat filter dipilih | filter menimpa list asli | simpan list asli di state terpisah / filter dari `DummyData.reviews` |
| Bintang tidak muncul | `rating` dikirim sebagai `Double` | `RatingStars` menerima `Int` |
| Tab filter tidak sinkron dengan list | tab disimpan di composable | simpan `selectedRating` di ViewModel |
| Layar tidak muncul | `AppNavHost` masih placeholder | ganti `composable(NavRoutes.REVIEWS)` |


---

📖 **Kembali ke indeks:** [README.md](README.md) · **Aturan umum:** [08-komponen-bersama.md](08-komponen-bersama.md)
