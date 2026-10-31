# Fitur Kategori Vibe Cafe — bahan & tugas

> **Pemilik:** Evia Vinata — folder `features/vibe/` baru berisi `README.md`.
> **Panduan** yang isinya bahan yang sudah tersedia + daftar tugas. ikuti aturan di [08-komponen-bersama.md](08-komponen-bersama.md).

---

## 1. Bahan yang sudah disiapkan

### 1.1 Model — `core/model/Vibe.kt`

| Field | Tipe | Contoh isi |
|---|---|---|
| `id` | `String` | `"v1"` |
| `name` | `String` | `"Nugas / WFC"` |
| `description` | `String` | `"Tenang, banyak colokan, cocok buat kerja"` |
| `cafeCount` | `Int` | `12` (jumlah cafe di vibe itu) |

### 1.2 Data dummy — `core/data/DummyData.kt`

`DummyData.vibes` → **4 vibe**: `Nugas / WFC`, `Aesthetic`, `24 Jam`, `Outdoor`.
`DummyData.cafes` (6 cafe) punya field `vibes: List<String>` yang isinya **sama persis**
dengan `Vibe.name`, contoh `listOf("Nugas / WFC", "Aesthetic")`.

> Ini kunci fiturmu: mencocokkan cafe ke vibe dilakukan dengan **membandingkan nama**.
> Kalau kamu mengubah teks nama vibe, cafe tidak akan ketemu.

### 1.3 Komponen siap pakai — `core/components/`

| Komponen | Untuk apa |
|---|---|
| `CafeCard(cafe, modifier, onClick)` | menampilkan cafe di dalam vibe terpilih |
| `SectionHeader(title, actionLabel)` | judul bagian |
| `RatingStars(rating, showValue)` | kalau mau menampilkan rating di header vibe |
| `InfoBadge(text, active)` | label kecil, misal jumlah cafe |

Cara memanggil: `CafeCard(cafe = cafe, modifier = Modifier.width(260.dp), onClick = { ... })`
(karena ditaruh di baris geser horizontal)

### 1.4 Navigasi

Route `NavRoutes.VIBE` sudah ada di `AppNavHost.kt` dan tampil di bottom bar, tapi isinya
masih `FeaturePlaceholderScreen(...)`. Ganti blok itu dengan layarmu.

---

## 2. Yang perlu kamu buat

| # | File | Isi |
|---|---|---|
| 1 | `features/vibe/VibeViewModel.kt` | state + vibe terpilih + daftar cafe per vibe |
| 2 | `features/vibe/VibeScreen.kt` | tab vibe + daftar cafe di dalamnya |
| 3 | (opsional) `VibeDetailScreen.kt` | detail satu vibe |
| 4 | update `README.md` di folder fitur | catatan singkat apa yang sudah dibuat |

Rincian tugas:

1. **State** — daftar vibe, index/`id` vibe terpilih, dan daftar cafe untuk vibe itu.
2. **Aksi** — `onVibeSelected(index)` yang mengganti vibe terpilih dan menghitung ulang
   daftar cafe.
3. **Layar** — tab vibe di atas (bisa digeser), daftar cafe di bawahnya.
4. **Kosong** — kalau satu vibe tidak punya cafe, tampilkan teks "Belum ada cafe di vibe ini".
5. **Klik cafe** — arahkan ke detail (koordinasi dengan Ossa kalau memakai layar detailnya).

---

## 3. Petunjuk pengerjaan

- Komponen Compose yang perlu kamu baca dokumentasinya: `ScrollableTabRow` (tab),
  `HorizontalPager` + `rememberPagerState` (halaman per vibe), `LazyColumn`,
  `LaunchedEffect` (menyamakan tab dengan halaman saat digeser).
- Kalau memakai pager + tab, kamu perlu dua arah sinkronisasi: tab diklik → pager pindah,
  pager digeser → tab ikut. Ini bagian tersulit fiturmu; kerjakan pelan-pelan dan uji
  dengan swipe.
- Versi lebih sederhana (boleh dipakai kalau pager terasa sulit): hanya `ScrollableTabRow`
  + `LazyColumn` yang isinya diganti sesuai vibe terpilih. Tidak perlu pager.
- Filter cafe per vibe: `DummyData.cafes.filter { vibe.name in it.vibes }` — kerjakan di
  ViewModel, bukan di composable.
- Pola ViewModel: lihat bagian 5 di [08-komponen-bersama.md](08-komponen-bersama.md).

---

## 4. Jebakan

| Gejala | Sebab | Perbaikan |
|---|---|---|
| Tab ada isinya kosong | nama vibe tidak sama dengan isi `cafe.vibes` | cek huruf besar/kecil & spasi, harus sama persis |
| Tab dan halaman tidak sinkron | hanya satu arah sinkronisasi | tambahkan `LaunchedEffect` untuk arah sebaliknya |
| Daftar cafe tidak berubah saat tab diganti | filter dilakukan sekali di awal | hitung ulang di ViewModel setiap vibe berubah |
| Kartu melar penuh di baris geser | lupa lebar | beri `Modifier.width(260.dp)` di pemanggil |
| Layar tidak muncul | `AppNavHost` masih placeholder | ganti `composable(NavRoutes.VIBE)` |


---

📖 **Kembali ke indeks:** [README.md](README.md) · **Aturan umum:** [08-komponen-bersama.md](08-komponen-bersama.md)
