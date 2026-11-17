# Panduan Integrasi Supabase dan Standar Lapisan Aplikasi

Dokumen ini adalah lanjutan dari [09-skema-database.md](09-skema-database.md). Pembagian tugasnya tegas:

| Dokumen | Isi | Menang untuk |
|---|---|---|
| 09 | Tabel, kolom, relasi, hak akses, SQL | Nama tabel, nama kolom, tipe data |
| 10 (ini) | Cara kode disusun, tugas tiap anggota, elemen UI yang wajib ada | Struktur kode, penamaan kelas, urutan kerja |

Kalau isi keduanya bertabrakan, 09 menang untuk nama tabel/kolom, 10 menang untuk bentuk kode.

---

## 1. Untuk siapa dokumen ini

Untuk lima anggota yang mengerjakan fitur masing-masing. Setiap anggota memakai dokumen ini untuk dua hal:

1. Menyesuaikan **kode fiturnya sendiri** dengan pola yang sama (bukan menyalin kode orang lain).
2. Menambah **elemen UI** yang tercantum di bagian 7 pada layar miliknya.

Yang tidak ada di dokumen ini: kode layar jadi. Bagian 6 hanya menyebut elemen apa yang harus ada dan perilaku apa yang harus benar, bukan tata letaknya. Bentuk visual tetap milik masing-masing anggota, selama mengikuti [08-komponen-bersama.md](08-komponen-bersama.md).

---

## 2. Lima aturan yang tidak boleh dilanggar

| Kode | Aturan | Alasan |
|---|---|---|
| A1 | Composable hanya mengirim event ke ViewModel, tidak pernah mengambil data sendiri | Kalau layar yang mengambil data, jalur data bercabang dan tidak bisa dijelaskan |
| A2 | Satu `UiState` per layar; data tidak disimpan di `remember` atau di ViewModel kedua | Satu sumber kebenaran, supaya layar bisa digambar ulang dari state saja |
| A3 | Hanya repository yang memanggil Supabase | Satu tempat untuk 1 tabel + 1 query; mudah ditunjuk saat ditanya |
| A4 | Data turunan dihitung, tidak disimpan | Dua list yang disimpan bersamaan pasti pernah tidak sinkron |
| A5 | Tidak ada `DummyData` di dalam composable mana pun | Composable yang memegang data tidak bisa diuji dan tidak ikut berubah saat data pindah ke server |

Aturan A5 saat ini dilanggar di satu tempat: `HomeScreen.kt` membaca `DummyData.cafes`, `DummyData.vibes`, `DummyData.events`, dan `DummyData.reviews` langsung di dalam composable. Ini harus diperbaiki lebih dulu sebelum `DummyData.kt` dihapus.

---

## 3. Struktur folder target

Tidak ada folder yang dihapus. Yang berubah adalah isi dan tambahan file di dalamnya.

| Lokasi | Status | Isi setelah integrasi |
|---|---|---|
| `core/model/` | tetap, bertambah | Model UI (`Cafe`, `Review`, `Vibe`, `EventPromo`, `BookmarkItem`) tetap. Tambah `*Mapper.kt` per fitur untuk mengubah baris Supabase menjadi model |
| `core/data/` | tetap, isinya diganti | `SupabaseClient.kt` (URL, anon key, sesi) dan `ApiError.kt` (peta kode error ke pesan Indonesia). `DummyData.kt` dihapus paling akhir |
| `core/components/` | tetap | `CafeCard`, `RatingStars`, dan komponen bersama lain. Berubah hanya pada cara memuat foto |
| `core/navigation/` | tetap | `AppNavHost.kt`, `NavRoutes.kt`. Isinya hanya diganti cara ViewModel dibuat |
| `core/ui/` | 1 file dihapus | `FeaturePlaceholderScreen.kt` sudah tidak dipakai siapa pun |
| `features/<fitur>/` | bertambah 2 file | `<Fitur>Repository.kt` dan `<Fitur>Mapper.kt` di samping ViewModel dan Screen yang sudah ada |
| `res/drawable/` | 9 file dihapus | `cafe_*.jpg` tidak dipakai lagi setelah foto berasal dari bucket `cafe-photos` |
| `assets/docs/` | jangan disentuh | Dokumentasi tugas, bukan data aplikasi |

### File baru yang wajib ada

| File | Isi minimal | Dibuat oleh |
|---|---|---|
| `core/data/SupabaseClient.kt` | `createSupabaseClient` dengan URL + anon key dari `BuildConfig`, satu instance dipakai bersama | satu orang saja |
| `core/data/ApiError.kt` | Fungsi yang menerjemahkan kode `42501`, `23503`, `23505`, `23514`, `PGRST116`, `401` menjadi kalimat yang bisa dibaca pengguna | satu orang saja |
| `features/<fitur>/<Fitur>Repository.kt` | Fungsi baca dan tulis untuk tabel milik fitur itu saja | Tiap anggota |
| `features/<fitur>/<Fitur>Mapper.kt` | `fromRow(...)` dan `toRow(...)` | Tiap anggota |
| `features/reviews/ReviewSection.kt` | Composable mandiri untuk dipasang di layar detail cafe | Shelfina |
| `features/bookmark/BookmarkAction.kt` | Composable mandiri untuk dipasang di layar detail cafe | Shananda |

Anon key boleh ada di `BuildConfig` karena memang dirancang publik; yang tidak boleh masuk repo adalah **service role key**. Keamanan tetap dipegang RLS, bukan dipegang kunci.

---

## 4. Jalur data standar

Satu jalur, dipakai semua fitur. Enam langkah, masing-masing punya nama file yang bisa dibuka saat demo.

| Langkah | Terjadi di | Yang dilakukan |
|---|---|---|
| 1 | `MainActivity` → `NgopiMalangApp` | Membuat `NavHostController` dan `AuthViewModel` |
| 2 | `AuthViewModel` → `AuthRepository` | Ambil sesi, lalu ambil role dari `user_roles` |
| 3 | `AuthUiState` | Menyimpan `userId`, `isLoggedIn`, `role` |
| 4 | `composable(...)` di `AppNavHost` | Membuat `<Fitur>ViewModel` dengan `viewModel()`, meng-`collect` state-nya |
| 5 | `<Fitur>ViewModel` → `<Fitur>Repository` | Memanggil query, hasilnya dipetakan mapper menjadi `List<Model>` |
| 6 | `LazyColumn` / `LazyVerticalGrid` / `HorizontalPager` | Menggambar daftar dengan `key = { it.id }` |

Aturan tambahan yang muncul dari jalur ini:

- **Filter dan pencarian milik repository, bukan layar.** Ketikan pengguna masuk ke `UiState.query`, lalu ViewModel memanggil ulang repository dengan parameter itu. Untuk data ~12 baris, menyaring di memori boleh, tetapi tempatnya tetap di ViewModel, bukan di composable.
- **Detail dibuka dengan id, bukan dengan objek.** `findCafe(id)` yang membaca list lokal akan gagal begitu list belum termuat. Ganti dengan `loadCafe(id)` yang memanggil repository.
- **Satu layar, satu ViewModel.** `viewModel()` di dalam `composable(...)` membuat instance baru per back stack entry. Itu benar selama data diambil ulang, dan salah kalau layar mengandalkan list yang sudah dimuat layar lain.

---

## 5. Standar UiState

Setiap `UiState` wajib punya empat kelompok field berikut.

| Kelompok | Nama field yang dipakai | Wajib |
|---|---|---|
| Data | `items` atau nama konkret (`cafes`, `reviews`, `events`, `vibes`) | ya |
| Status muat | `isLoading`, `isSaving` | ya |
| Kesalahan | `errorMessage: String?` | ya |
| Masukan pengguna | `query`, filter yang aktif | ya bila layar punya filter |

Yang **tidak** boleh ada di `UiState`: list hasil filter yang disimpan terpisah dari list aslinya.

### Yang perlu diperbaiki di UiState sekarang

| File | Kondisi sekarang | Yang harus jadi |
|---|---|---|
| `DirectoryViewModel.kt` | `cafes`, `query`, `wifiOnly`, `plugsOnly`; data dari `DummyData.cafes.toList()` | Tambah `isLoading`, `errorMessage`; hapus `allCafes`, ambil dari repository |
| `ReviewsViewModel.kt` | `allReviews` **dan** `filteredReviews` disimpan bersamaan | Simpan satu list; hasil filter dihitung lewat `val visible = ...` di dalam state atau di ViewModel |
| `BookmarkViewModel.kt` | `bookmarks` dan `cafes` disimpan terpisah, data dari `DummyData.cafes` | Simpan `bookmarks` berisi cafe lengkap; tambah `isLoading`, `errorMessage` |
| `VibeViewModel.kt` | `vibes`, `allCafes`, `cafeIdsByVibe` disimpan semua | `allCafes` dihapus dari state; relasi diambil per vibe dari repository, tambah `isLoading`, `errorMessage` |
| `EventPromoViewModel.kt` | `allEvents` **dan** `filteredEvents` | Simpan satu list; filter dihitung, bukan disimpan |
| `AuthViewModel.kt` | sudah punya `isLoading`, `errorMessage`, tetapi `role` belum ada | Tambah `role: String = "user"`; `delay(700)` diganti pemanggilan `signIn` sesungguhnya |

---

## 6. Konvensi penamaan

| Hal | Pola | Contoh |
|---|---|---|
| Repository | `<Fitur>Repository` | `CafeRepository`, `ReviewRepository` |
| Fungsi baca | `observe...` untuk daftar, `load...` untuk satu baris | `observeCafes(query)`, `loadCafe(id)` |
| Fungsi tulis | `save...`, `delete...`, `toggle...` | `saveCafe(cafe)`, `deleteReview(id)`, `toggleBookmark(cafeId)` |
| Mapper | `fromRow(...)`, `toRow(...)` | `CafeMapper.fromRow(row)` |
| State | `<Fitur>UiState` | `DirectoryUiState` |
| Event | `on...Change`, `on...Click`, `onSubmit` | `onSearchChange(q)` |

Semua fungsi repository `suspend` dan mengembalikan `Result<T>` atau melempar exception yang ditangkap ViewModel dengan `runCatching`. Repository tidak pernah menyentuh `Context` dan tidak pernah memanggil navigasi.
---

## 7. Elemen UI yang wajib ditambahkan

Bagian ini adalah daftar tugas. Kolom "perilaku" adalah syarat benar, bukan saran tampilan. Bentuk visualnya bebas, selama memakai komponen bersama dari [08-komponen-bersama.md](08-komponen-bersama.md).

### 7.1 Wajib di semua layar (lima elemen)

| Elemen | Perilaku yang benar | Bukti saat demo |
|---|---|---|
| Indikator muat | Tampil selama `isLoading`; daftar lama tidak dikosongkan saat memuat ulang | Putus koneksi, tekan ulang, indikator muncul |
| Empty state | Muncul saat daftar kosong **dan** `isLoading = false`; beri kalimat penyebab | Hapus filter sampai hasil kosong |
| Kesalahan + tombol coba lagi | Pesan dari `ApiError`, bukan pesan mentah Supabase; tombol memicu ulang pemuatan | Matikan Wi-Fi, buka layar |
| Umpan balik simpan | Tombol nonaktif + `isSaving` selama proses; berhasil → daftar ikut berubah | Tambah satu data, lihat daftar |
| Daftar berat-key | `items(list, key = { it.id })` pada LazyColumn/Grid, `key` pada pager | Filter berubah, posisi kartu stabil |

### 7.2 Fitur 1 — Direktori Cafe (Ossa)

| Elemen | Letak | Perilaku yang benar |
|---|---|---|
| Kolom pencarian | Atas daftar | Teks masuk `UiState.query`, hasil dari repository |
| Chip filter Wi-Fi / colokan | Bawah pencarian | Sama seperti pencarian; kombinasi filter tetap satu state |
| Tombol **Tambah Cafe** | FAB di daftar, hanya bila `role == "admin"` | Membuka form; user biasa tidak melihat tombol ini |
| Menu **Edit** dan **Hapus** | `TopAppBar` layar detail, hanya admin | Edit membuka form terisi; Hapus memunculkan dialog konfirmasi |
| Form tambah/edit | Layar terpisah | Field: nama, alamat, jam buka, harga minimum, harga maksimum, deskripsi, pilihan vibe (boleh lebih dari satu), foto |
| Pemilih foto + pratinjau | Di form | Mengambil gambar dari galeri, menampilkan pratinjau sebelum disimpan |
| Indikator unggah | Di dalam form | Tampil selama foto diunggah; bila unggah gagal, data tidak tersimpan setengah jalan |
| Badge **Tidak aktif** | Kartu dan detail | Tampil bila `is_active = false`; kartu arsip tetap terlihat oleh admin |
| Aksi **Arsipkan** | Menu detail | Mengganti Hapus saat data sudah dipakai fitur lain (error `23503`); menjelaskan sebabnya ke pengguna |
| Foto cadangan | Kartu dan detail | Bila `photoPath` kosong, tampil placeholder, bukan kotak kosong |

### 7.3 Fitur 2 — Ulasan & Rating (Shelfina)

| Elemen | Letak | Perilaku yang benar |
|---|---|---|
| Tombol **Beri ulasan** | Detail cafe | Hanya untuk pengguna login; admin tidak melihat tombol tulis/hapus milik orang lain |
| Form ulasan | Dialog atau layar | Bintang 1–5, kolom komentar, penanda menu favorit |
| Status **sudah diulas** | Detail cafe | Bila pengguna sudah mengulas cafe itu, tombol berubah menjadi **Edit ulasan** (upsert, bukan insert kedua) |
| Daftar ulasan | Di layar ulasan dan detail cafe | Nama penulis, tanggal, jumlah bintang; memakai `key = { it.id }` |
| Filter rating | Atas daftar | Hasil dari repository dengan parameter rating, bukan dua list yang disimpan |
| Ringkasan rating | Header detail cafe | Angka rata-rata dan jumlah ulasan dari view `cafes_with_rating`; tampil "belum ada ulasan" bila kosong |
| Hapus ulasan | Pada ulasan sendiri | Hanya muncul untuk ulasan milik pengguna yang sedang login |

### 7.4 Fitur 3 — Bookmark / Saved Spot (Shananda)

| Elemen | Letak | Perilaku yang benar |
|---|---|---|
| Ikon bookmark | Kartu cafe dan detail (komponen bersama) | Menyala bila sudah tersimpan; menekan sekali menyimpan, sekali lagi melepas |
| Dialog catatan | Setelah menyimpan | Catatan pribadi disimpan bersama bookmark, bisa diubah kemudian |
| Daftar simpanan | Layar bookmark | Menampilkan cafe tersimpan beserta catatannya, memakai `LazyColumn` + `key` |
| Hapus simpanan | Geser atau menu item | Menghapus baris bookmark saja, bukan cafe-nya |
| Empty state khusus | Layar bookmark | Mengarahkan pengguna membuka Direktori, bukan sekadar "kosong" |

Catatan penting: tombol bookmark dan tombol rating dipasang di layar milik Ossa, tetapi **repository-nya milik fitur masing-masing**. Cara memasangnya diatur di bagian 7.8: Ossa menyediakan slot, Shananda dan Shelfina mengisi slot itu dengan composable milik mereka sendiri. Fitur 3 tidak menyalin query ulasan, dan fitur 2 tidak menyalin query bookmark.

### 7.5 Fitur 4 — Kategori Vibe (Evia)

| Elemen | Letak | Perilaku yang benar |
|---|---|---|
| `TabRow` vibe | Atas layar | Sumber tab dari tabel `vibes`, bukan list hardcode |
| `HorizontalPager` | Isi layar | Satu halaman per vibe; halaman memuat cafe milik vibe itu dari repository |
| Jumlah cafe per vibe | Di tab atau header halaman | Angka dari view `vibes_with_count`; ikut berubah setelah cafe diarsipkan |
| Tombol **Tambah Vibe** | FAB, hanya admin | Form: nama vibe dan `icon_key` |
| Pemilih ikon | Di form vibe | `icon_key` dipetakan ke ikon yang tersedia; bila key tidak dikenal, pakai ikon cadangan |
| Kelola isi vibe | Di halaman vibe, hanya admin | Memilih cafe yang masuk vibe tersebut (menulis `cafe_vibes`), tanpa mengubah data cafe |

### 7.6 Fitur 5 — Event & Promo (Nabil)

| Elemen | Letak | Perilaku yang benar |
|---|---|---|
| `LazyColumn` + filter kategori | Layar daftar | Filter kategori masuk state, hasil dari repository |
| Tombol **Tambah Event** | FAB, hanya admin | Membuka form event/promo |
| Form event/promo | Layar terpisah | Judul, deskripsi, tanggal & jam mulai, tanggal & jam selesai, harga minimum, harga maksimum, kategori, cafe terkait |
| Validasi waktu | Di form | Waktu selesai harus setelah waktu mulai; pesan kesalahan tampil di field, bukan toast |
| Badge status | Kartu dan detail | **Akan datang** / **Berlangsung** / **Selesai**, dihitung dari `starts_at` dan `ends_at` saat ini |
| Tautan ke cafe | Detail event | Membuka detail cafe terkait; tombol disembunyikan bila `cafe_id` kosong |
| Menu **Edit** dan **Hapus** | Detail, hanya admin | Hapus memakai dialog konfirmasi |

### 7.7 Fondasi — Login & Profil (bersama)

| Elemen | Letak | Perilaku yang benar |
|---|---|---|
| Login & Register | Layar awal | Pesan kesalahan dari `ApiError` (mis. sandi salah), bukan pesan teknis |
| Sesi tersimpan | Saat aplikasi dibuka ulang | Bila sesi masih berlaku, langsung masuk tanpa login ulang |
| Nama tampilan | Layar profil | Bisa diubah, tersimpan di `profiles` |
| Email | Layar profil | Hanya baca |
| Foto profil | Layar profil | Opsional; bila tidak diisi, pakai inisial nama |
| Label peran | Layar profil | Menampilkan "Admin" bila `role == "admin"`, dan label itu **tidak bisa diubah dari aplikasi** |
| Tombol keluar | Layar profil | Mengakhiri sesi dan menghapus state login |

### 7.8 Layar milik bersama: siapa memasang, siapa mengisi

Beberapa elemen dari satu fitur harus muncul di layar milik anggota lain. Contohnya tombol **Beri ulasan** (fitur 2) dan tombol **Bookmark** (fitur 3) yang harus berada di `DirectoryDetailScreen` — layar milik Ossa.

Aturannya satu: **pemilik layar menyediakan slot, pemilik fitur mengisi slot.** Anggota lain tidak mengedit file orang lain.

| Layar | Pemilik file | Slot yang disediakan | Diisi oleh |
|---|---|---|---|
| `DirectoryDetailScreen.kt` | Ossa | `ReviewSection(cafeId)` | Shelfina — `features/reviews/ReviewSection.kt` |
| `DirectoryDetailScreen.kt` | Ossa | `BookmarkAction(cafeId)` | Shananda — `features/bookmark/BookmarkAction.kt` |
| `DirectoryDetailScreen.kt` | Ossa | Tautan ke event cafe | bila disepakati |
| `CafeCard.kt` (`core/components/`) | bersama | Tidak ada slot; perubahan di sini menyentuh beranda, direktori, dan bookmark sekaligus | hanya dengan kesepakatan tim |

Ketentuan teknis slot:

- Composable slot **mandiri**: membuat ViewModel-nya sendiri dengan `viewModel()`, tidak menerima ViewModel dari luar, tidak menerima `Context` dari layar induk.
- Masukannya hanya id: `cafeId: String`. Bukan objek `Cafe`, supaya fitur pemanggil tidak bergantung pada bentuk model Ossa.
- Tidak mengambil ruang bila tidak berhak: `ReviewSection` tidak menampilkan tombol tulis untuk peran admin, `BookmarkAction` tidak tampil untuk pengguna yang belum login.
- Slot dipasang **sekali** oleh pemilik layar, dan penggabungannya dilakukan di branch `progres2`, bukan di branch pribadi. Setelah itu file `DirectoryDetailScreen.kt` tidak diubah lagi oleh anggota lain.

Urutan kerja yang aman, karena slot butuh nama fungsi yang sudah disepakati:

1. Ossa menambahkan pemanggilan slot dengan tanda `TODO` dan menyepakati nama fungsi serta parameter di grup.
2. Shelfina dan Shananda mengerjakan composable-nya di folder masing-masing.
3. Setelah keduanya ada, Ossa menghapus `TODO`; tidak ada perubahan lain di file itu.

Kalau urutan ini dibalik — Shelfina lebih dulu menulis tombol di dalam `DirectoryDetailScreen.kt` — maka Ossa akan menerima konflik merge di setiap perubahan layar detail, dan saat presentasi kepemilikan layar tidak lagi jelas.

---

## 8. Yang dilarang

| Larangan | Alasan | Contoh yang ada sekarang |
|---|---|---|
| Membaca `DummyData` di composable | Melanggar A1 dan A5 | `HomeScreen.kt` memakai empat list `DummyData` langsung |
| Menyimpan dua versi list | Melanggar A4 | `ReviewsViewModel.kt` (`allReviews` + `filteredReviews`), `EventPromoViewModel.kt` (`allEvents` + `filteredEvents`) |
| Memanggil query di dalam `composable` | Jalur data jadi tidak terlacak | `findCafe(cafeId)` dipanggil saat komposisi di `AppNavHost.kt` |
| Menaruh `delay(...)` sebagai pengganti pemanggilan jaringan | Menyembunyikan bahwa belum ada integrasi | `AuthViewModel.kt` |
| Menampilkan pesan error mentah | Tidak bisa dibaca pengguna | Belum ada penanganan; perlu `ApiError` |
| Menaruh service role key di repo | Memberi akses penuh ke seluruh database | Tidak ada, dan jangan sampai ada |
| Menulis policy RLS dari sisi aplikasi | Policy hanya dibuat di dashboard SQL | Tidak ada, dan jangan sampai ada |
| Menyalin layar anggota lain | Melanggar ketentuan tugas independen | — |

---

## 9. Urutan kerja tiap anggota

Kerjakan berurutan. Jangan melompat ke UI sebelum langkah 3 selesai, karena UI yang dibuat di atas data palsu akan ditulis dua kali.

| Langkah | Pekerjaan | Selesai bila |
|---|---|---|
| 1 | Buat `<Fitur>Repository` untuk tabel milik fitur sendiri | Ada fungsi baca dan tulis; tidak ada nama tabel orang lain di file itu |
| 2 | Buat `<Fitur>Mapper` (`fromRow`, `toRow`) | Nama kolom database hanya muncul di file mapper |
| 3 | Rapikan `UiState` sesuai bagian 5 | Ada `isLoading`, `errorMessage`, tanpa list ganda |
| 4 | Ganti isi ViewModel: panggil repository, tangani error dengan `ApiError` | Tidak ada `DummyData` di ViewModel |
| 5 | Tambah elemen UI dari bagian 7 pada layar sendiri | Semua baris tabel fitur sendiri terpenuhi |
| 6 | Hapus sisa referensi data palsu | `grep -rn "DummyData"` kosong untuk file milik sendiri |

### Definisi selesai (per anggota)

- Fitur bisa dibaca (Read) dari Supabase, tidak ada data hardcode.
- Minimal satu Insert, satu Update, satu Delete berhasil lewat aplikasi.
- Loading, empty state, dan error tampil benar saat jaringan dimatikan.
- Tombol tulis hanya muncul untuk peran yang berhak.
- Foto diunggah ke Storage, bukan disimpan sebagai base64 di tabel.

---

## 10. Urutan menghapus file lama

Jangan dihapus lebih awal, karena akan memutus build anggota lain yang belum selesai.

| Urutan | Yang dihapus | Syarat |
|---|---|---|
| 1 | `core/ui/FeaturePlaceholderScreen.kt` | Aman sekarang; tidak dipakai siapa pun |
| 2 | `imageResId` di `Cafe.kt` dan 9 `cafe_*.jpg` di `drawable/` | `CafeCard` dan `DirectoryDetailScreen` sudah memakai foto dari Storage |
| 3 | `DummyData.kt` | `grep -rn "DummyData"` kosong di seluruh `app/src/main/java` |

`core/data/` **tidak** dihapus. Setelah `DummyData.kt` hilang, folder itu justru berisi `SupabaseClient.kt` dan `ApiError.kt`.

---

## 11. Checklist demo Progress 2

| No | Yang ditunjukkan | Berhasil bila |
|---|---|---|
| 1 | Login dengan akun user | Masuk ke beranda, nama tampil di profil |
| 2 | Login dengan akun admin | Tombol tambah/edit/hapus muncul di katalog |
| 3 | Tambah cafe + unggah foto (admin) | Data muncul di daftar tanpa membuka ulang aplikasi |
| 4 | Edit cafe | Perubahan langsung terlihat di daftar dan detail |
| 5 | Hapus cafe yang sudah punya ulasan | Muncul tawaran arsip, bukan aplikasi berhenti |
| 6 | Beri ulasan (akun user) | Rating cafe berubah, tombol berubah jadi Edit ulasan |
| 7 | Simpan bookmark + catatan | Muncul di layar Saved Spot |
| 8 | Buka detail dari bookmark | Detail terisi walau daftar direktori belum dibuka |
| 9 | Matikan jaringan | Muncul pesan kesalahan yang bisa dibaca dan tombol coba lagi |
| 10 | Akun user mencoba menambah cafe | Ditolak database, aplikasi menampilkan pesan yang jelas |
