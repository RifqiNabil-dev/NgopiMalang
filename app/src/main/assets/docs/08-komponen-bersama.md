# Komponen Bersama — bahan untuk menyusun fitur

Dokumen ini isinya: aturan main, daftar komponen yang sudah
tersedia, dan pola yang dipakai proyek ini.

---

## 1. Aturan warna

**Jangan pernah menulis warna langsung di layar.** Selalu ambil dari tema:

| ❌ Salah | ✅ Benar |
|---|---|
| `Color(0xFF6750A4)` | `MaterialTheme.colorScheme.primary` |
| `Color.White` | `MaterialTheme.colorScheme.surface` |
| `Color.Black` | `MaterialTheme.colorScheme.onSurface` |
| `Color.Gray` | `MaterialTheme.colorScheme.onSurfaceVariant` |

Kenapa: warna aplikasi bisa diganti dari satu tempat (`ui/theme/`), dan otomatis ikut
mode gelap HP. Hex warna **hanya** boleh ada di `ui/theme/Color.kt`.

### Arti tiap role warna

| Role | Dipakai untuk |
|---|---|
| `primary` / `onPrimary` | tombol utama, ikon aktif, teks di atas warna primary |
| `primaryContainer` / `onPrimaryContainer` | chip terpilih, badge, kartu sorotan |
| `secondaryContainer` | latar chip netral, label kecil |
| `surface` / `onSurface` | latar kartu & layar, teks utama |
| `surfaceVariant` / `onSurfaceVariant` | teks sekunder, keterangan, ikon pasif |
| `outline` / `outlineVariant` | garis tepi, pemisah |
| `error` / `onError` | pesan gagal |

Butuh warna lain? Cek dulu di `ui/theme/Theme.kt` role apa yang tersedia. Kalau memang
tidak ada, tanya Rifqi — jangan bikin warna sendiri.

---

## 2. Aturan ukuran

- **Lebar kartu diatur oleh pemanggil, bukan oleh komponennya.**
  - Di dalam `LazyRow` (baris geser horizontal): `Modifier.width(260.dp)`
  - Di dalam `LazyColumn` / `LazyVerticalGrid`: `Modifier.fillMaxWidth()`
- Padding layar: `Modifier.padding(horizontal = 16.dp, vertical = 12.dp)`.
- Jarak antar item list: `Arrangement.spacedBy(12.dp)`.
- Jangan menulis `Modifier.width(...)` di dalam file `core/components/` — nanti rusak
  saat dipakai di grid/kolom.

---

## 3. Katalog komponen yang sudah tersedia

Semua di `core/components/`. **Jangan menyalin ulang isinya ke folder fiturmu**, cukup panggil.

| Komponen | Parameter | Dipakai untuk |
|---|---|---|
| `CafeCard(cafe, modifier, onClick)` | `cafe: Cafe` | kartu cafe (nama, alamat, harga, rating, badge fasilitas) |
| `EventCard(event, modifier, onClick)` | `event: EventPromo` | kartu event/promo (judul, cafe, jadwal, label kategori) |
| `ReviewCard(review, modifier, onClick)` | `review: Review` | kartu ulasan (nama, rating bintang, komentar, menu favorit) |
| `RatingStars(rating, modifier, showValue)` | `rating: Int` (1–5) | baris bintang; `showValue = true` menampilkan angka |
| `InfoBadge(text, modifier, active)` | `text: String`, `active: Boolean` | label kecil (WiFi, colokan, kategori) |
| `SectionHeader(title, modifier, actionLabel, onActionClick)` | `actionLabel` opsional | judul bagian + tombol "Lihat semua" |
| `AppBottomBar(currentRoute, onNavigate)` | — | sudah dipasang di `NgopiMalangApp`, tidak perlu disentuh |

Cara memanggil (satu baris, bukan layar penuh):

```kotlin
CafeCard(cafe = cafe, modifier = Modifier.fillMaxWidth(), onClick = { onCafeClick(cafe.id) })
```

Komponen di atas sudah memakai `MaterialTheme.colorScheme.*`, jadi otomatis cocok di
mode terang maupun gelap.

---

## 4. Model & data dummy yang sudah ada

| Model | File | Data dummy | Dipakai fitur |
|---|---|---|---|
| `Cafe` | `core/model/Cafe.kt` | `DummyData.cafes` (6 cafe) | Direktori, Vibe, Bookmark, Beranda |
| `EventPromo` + `EventCategory` | `core/model/EventPromo.kt` | `DummyData.events` (4 event) | Event & Promo, Beranda |
| `Review` | `core/model/Review.kt` | `DummyData.reviews` (3 ulasan) | Ulasan, Beranda |
| `Vibe` | `core/model/Vibe.kt` | `DummyData.vibes` (4 vibe) | Kategori Vibe |

Kalau butuh model sendiri (misal `BookmarkItem`), buat di **folder fiturmu**, bukan di
`core/model/`.

---

## 5. Pola ViewModel (MVVM + StateFlow)

Semua fitur memakai pola yang sama. Empat langkah, tanpa perlu lihat kode siapa pun:

1. Buat `data class XxxUiState(...)` berisi semua yang ditampilkan layar, semua field
   diberi nilai default (supaya state awal valid).
2. Di `class XxxViewModel : ViewModel()`:
   - `private val _uiState = MutableStateFlow(XxxUiState())`
   - `val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()`
3. Setiap aksi pengguna jadi **fungsi** di ViewModel (`onSearchChange`, `onTabSelected`,
   `toggleBookmark`, ...) yang mengubah `_uiState` lewat `_uiState.update { ... }`.
4. Layar **tidak menyimpan data sendiri**: ambil state dengan
   `val state by viewModel.uiState.collectAsStateWithLifecycle()`, lalu tampilkan.
   Aksi pengguna dipanggil sebagai lambda ke ViewModel.

Aturan tambahan:
- Jangan taruh `DummyData` langsung di layar; ambil lewat ViewModel supaya nanti mudah
  diganti Supabase.
- Filter/pencarian/urutan dikerjakan di ViewModel, bukan di dalam composable.
- Satu fitur = satu ViewModel (boleh lebih kalau memang butuh layar detail terpisah).

---

## 6. Navigasi

- Semua route ada di `core/navigation/NavRoutes.kt` dan sudah didaftarkan di
  `AppNavHost.kt`.
- Route fiturmu saat ini masih memakai `FeaturePlaceholderScreen(...)` — ganti blok itu
  dengan layarmu, contoh: `DirectoryScreen(onCafeClick = { ... })`.
- Untuk membuka layar detail, tambahkan route baru di `NavRoutes` lalu daftarkan
  `composable(...)` di `AppNavHost`. Kabari Rifqi kalau menambah route supaya tidak bentrok.

---

## 7. Struktur file per fitur

```
features/<fitur>/
├── XxxScreen.kt        ← tampilan (composable)
├── XxxViewModel.kt     ← state + logika
└── README.md           ← catatan fitur (sudah ada, boleh diupdate)
```

Kalau ada layar detail: `XxxDetailScreen.kt`. Jangan taruh file fitur di `core/`.

---

## 8. Jebakan yang sering terjadi

| Gejala | Sebab | Perbaikan |
|---|---|---|
| `Unresolved reference: MaterialTheme` | import belum ada | `import androidx.compose.material3.MaterialTheme` |
| `Unresolved reference: dp` | import belum ada | `import androidx.compose.ui.unit.dp` |
| `by remember` error "no getValue" | kurang import | `import androidx.compose.runtime.getValue` |
| Layar tidak berubah saat tombol ditekan | mengubah variabel lokal, bukan state ViewModel | panggil fungsi ViewModel yang mengubah `_uiState` |
| Kartu melebar penuh di `LazyRow` | lupa lebar | tambahkan `Modifier.width(260.dp)` di pemanggil |
| Kartu sempit di grid | lupa `fillMaxWidth()` | tambahkan `Modifier.fillMaxWidth()` |
| Teks tidak terbaca di mode gelap HP | warna ditulis manual | ganti ke `MaterialTheme.colorScheme.*` |
| Aplikasi crash saat buka layar | `viewModel()` dipanggil di layar yang tidak punya ViewModelStore | pastikan layar dibuka lewat `composable(...)` di NavHost |


---

## 9. Kalau mau mengganti warna aplikasi

Warna sekarang ikut wallpaper HP (dynamic color, Android 12+). Untuk warna brand sendiri:

1. Di `ui/theme/Theme.kt`, ubah `dynamicColor: Boolean = true` menjadi `false`.
2. Isi warna di `ui/theme/Color.kt`, lalu pasang di `LightColorScheme` dan
   `DarkColorScheme` pada file yang sama.
3. Tidak ada layar yang perlu diubah, karena semua memakai `MaterialTheme.colorScheme.*`.

Hex tetap hanya boleh ada di `Color.kt`.

---

📖 **Kembali ke indeks:** [README.md](README.md)
