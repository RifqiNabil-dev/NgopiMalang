# Panduan Integrasi Supabase — Fitur 3: Bookmark / Saved Spot (Shananda)

Dokumen ini berisi **apa yang harus dibuat dan diubah** di kode fitur Bookmark supaya terhubung ke Supabase. Bukan SQL, bukan pembuatan tabel.

| Urusan | Dikerjakan oleh |
|---|---|
| Membuat tabel, view, policy, akun demo | **Nabil** — lihat [09-skema-database.md](09-skema-database.md) |
| Dependency Gradle, `SupabaseClient`, `ApiError` | **Nabil** — sekali untuk semua fitur |
| Kode fitur Bookmark | **Shananda** (dokumen ini) |
| Slot di layar detail cafe | **Ossa** memasang, **Shananda** mengisi |

Kamu tidak perlu menjalankan SQL. Kalau ada kolom yang kurang, ajukan ke Nabil.

---

## 1. Yang harus sudah ada sebelum kamu mulai

| Prasyarat | Cara memastikan |
|---|---|
| `SupabaseClient.kt` dan `ApiError.kt` sudah ada | Bisa login dengan akun demo |
| `AuthUiState` menyimpan `userId` | Dua akun berbeda melihat bookmark berbeda |
| Tabel `bookmarks` sudah ada, view `cafes_with_rating` bisa dibaca | Dikonfirmasi Nabil |
| Nama slot `BookmarkAction(cafeId)` sudah disepakati dengan Ossa | Tertulis di grup |

Bedanya dengan fitur lain: bookmark **tidak punya peran admin**. Yang menentukan isi daftar hanya `user_id = auth.uid()`. Admin tidak bisa melihat bookmark orang lain, dan itu memang disengaja.

---

## 2. Peta file

| File | Status | Isi |
|---|---|---|
| `features/bookmark/BookmarkItem.kt` | **ubah** | Membawa data cafe hasil join, bukan hanya nama |
| `features/bookmark/BookmarkRow.kt` | **baru** | Bentuk baris dari Supabase + join `cafes` |
| `features/bookmark/BookmarkMapper.kt` | **baru** | `BookmarkRow` → `BookmarkItem` |
| `features/bookmark/BookmarkRepository.kt` | **baru** | Satu-satunya tempat memanggil Supabase |
| `features/bookmark/BookmarkAction.kt` | **baru** | Tombol simpan/hapus untuk dipasang di detail cafe |
| `features/bookmark/BookmarkViewModel.kt` | **ubah** | Ganti `DummyData`, hapus dua list |
| `features/bookmark/BookmarkScreen.kt` | **ubah** | Loading/empty/error, catatan disimpan ke database |

`DirectoryDetailScreen.kt` bukan milikmu. Kamu hanya menyerahkan satu baris pemanggilan `BookmarkAction(cafeId = ...)`.

---

### 2.1 Skema tabel yang boleh kamu pakai

**`bookmarks`** — tabel utama fitur ini.

| Kolom | Tipe | Catatan |
|---|---|---|
| `id` | uuid | Dibuat database; dipakai untuk menghapus baris |
| `user_id` | uuid | FK ke `profiles`; harus sama dengan pengguna login |
| `cafe_id` | uuid | FK ke `cafes` |
| `note` | text | Catatan pribadi, boleh kosong (`''`, bukan null) |
| `created_at` | timestamptz | Diisi database; dipakai sebagai "disimpan pada" |
| `updated_at` | timestamptz | Diisi trigger saat catatan diubah |

Pasangan `(user_id, cafe_id)` **unik**: satu cafe hanya bisa disimpan sekali oleh satu pengguna. Insert kedua untuk cafe yang sama akan ditolak `23505`.

Hak akses yang berlaku:

| Operasi | Hasil |
|---|---|
| Baca bookmark sendiri | boleh |
| Baca bookmark pengguna lain (termasuk oleh admin) | **0 baris**, bukan error |
| Insert dengan `user_id` sendiri | boleh |
| Insert dengan `user_id` orang lain | ditolak `42501` |
| Update/delete bookmark sendiri | boleh |
| Update/delete bookmark orang lain | **0 baris**, tanpa error |

Baris terakhir berlaku juga untuk admin. Jangan membuat tampilan yang mengandaikan admin bisa melihat semua bookmark; hasilnya selalu kosong.

**`cafes_with_rating`** — baca saja, untuk isi kartu.

| Kolom | Tipe | Catatan |
|---|---|---|
| `id`, `name`, `address` | uuid, text | Ditampilkan di kartu |
| `photo_path` | text | Path Storage, bukan URL |
| `price_min`, `price_max` | integer | Nullable |
| `rating`, `review_count` | numeric(2,1), integer | `rating` null bila belum ada ulasan |
| `is_active` | boolean | `false` = cafe diarsipkan |

Kolom yang **tidak boleh** kamu isi: `id`, `created_at`, `updated_at`, dan semua kolom view.

---

## 3. Langkah 1 — sesuaikan model `BookmarkItem`

Sekarang `BookmarkItem` hanya menyimpan `cafeName` dan `savedAtLabel`. Kartu bookmark butuh foto, alamat, dan rating, sehingga datanya diambil lewat join dan disimpan di model.

```kotlin
data class BookmarkItem(
    val id: String,               // id baris bookmarks
    val cafeId: String,
    val note: String = "",
    val savedAt: Instant,
    val cafeName: String,
    val address: String = "",
    val photoPath: String? = null,
    val priceMin: Int? = null,
    val priceMax: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val isActive: Boolean = true,
)
```

| Lama | Baru | Kenapa |
|---|---|---|
| `cafeName: String` saja | ditambah `address`, `photoPath`, `rating`, `isActive` | Kartu tidak perlu query cafe satu per satu |
| `savedAtLabel: String` | `savedAt: Instant` | Label "Baru disimpan" dibuat di UI dari selisih waktu |
| `id: String` (buatan sendiri) | `id` dari tabel `bookmarks` | Harus id baris database supaya bisa dihapus |

`BookmarkItem` hanya dipakai fitur Bookmark, jadi perubahannya tidak memaksa anggota lain mengubah kode. Tetap umumkan di grup supaya tidak ada yang terkejut.

---

## 4. Langkah 2 — `BookmarkRow` dan `BookmarkMapper`

```kotlin
@Serializable
data class BookmarkRow(
    val id: String,
    val note: String = "",
    @SerialName("cafe_id") val cafeId: String,
    @SerialName("created_at") val createdAt: String,
    val cafes: BookmarkCafeRow? = null,   // hasil join
)

@Serializable
data class BookmarkCafeRow(
    val id: String,
    val name: String,
    val address: String = "",
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("price_min") val priceMin: Int? = null,
    @SerialName("price_max") val priceMax: Int? = null,
    val rating: Double? = null,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
)

object BookmarkMapper {
    fun toModel(row: BookmarkRow): BookmarkItem
}
```

Aturan mapper:

- `cafes` bisa `null` bila cafe sudah hilang dari hasil join; perlakukan sebagai data tidak lengkap dan **jangan** menampilkan kartu kosong tanpa nama.
- `createdAt` ISO diubah ke `Instant` hanya di sini.
- Nama kolom `snake_case` tidak boleh bocor ke layar.

---

## 5. Langkah 3 — `BookmarkRepository`

```kotlin
class BookmarkRepository(private val client: SupabaseClient = Supabase.client) {

    suspend fun observeBookmarks(limit: Int = 50): List<BookmarkItem>

    suspend fun isBookmarked(cafeId: String): Boolean

    suspend fun add(cafeId: String): Unit

    suspend fun remove(cafeId: String): Int      // jumlah baris terhapus

    suspend fun updateNote(cafeId: String, note: String): Int

    suspend fun toggle(cafeId: String): Boolean  // true = sekarang tersimpan
}
```

Ketentuan:

| Ketentuan | Alasan |
|---|---|
| Query memakai join `cafes:cafe_id(...)` | Satu permintaan untuk daftar, tanpa query per kartu |
| `add` mengabaikan konflik `(user_id, cafe_id)` bila baris sudah ada | Menekan tombol dua kali tidak boleh menghasilkan error `23505` |
| `remove` mengembalikan jumlah baris | 0 baris berarti bukan milikmu atau sudah terhapus |
| `updateNote` mengembalikan jumlah baris | Sama: catatan orang lain tidak akan berubah |
| `toggle` membaca `isBookmarked` lalu memilih tambah/hapus | Satu sumber kebenaran, bukan menebak dari state UI |
| `limit` selalu dipakai | Jangan memuat seluruh tabel |

`user_id` **wajib dikirim**, karena kolomnya tidak punya nilai bawaan. Ambil dari sesi di dalam repository (`client.auth.currentUserOrNull()?.id`), jangan dari parameter layar. Policy memeriksa `user_id = auth.uid()`; nilai yang tidak cocok ditolak `42501`. Kalau null, hentikan dan tampilkan "Silakan login" — jangan mengirim string kosong.

---

## 6. Langkah 4 — state

Sekarang `BookmarkUiState` menyimpan `bookmarks` **dan** `cafes` — dua sumber untuk data yang sama. Setelah integrasi, cukup satu list karena data cafe sudah menempel di `BookmarkItem`.

```kotlin
data class BookmarkUiState(
    val items: List<BookmarkItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val editingCafeId: String? = null,
    val deletingCafeId: String? = null,
)

data class BookmarkActionUiState(
    val cafeId: String = "",
    val isBookmarked: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val requiresLogin: Boolean = false,
)
```

`editingCafeId` dan `deletingCafeId` menyimpan **id**, bukan objek. Dialog yang menyimpan objek akan menampilkan data basi setelah daftar dimuat ulang.

---

## 7. Langkah 5 — ViewModel

```kotlin
class BookmarkViewModel(private val repo: BookmarkRepository = BookmarkRepository()) : ViewModel() {
    val uiState: StateFlow<BookmarkUiState>

    fun load()
    fun onRemoveClick(cafeId: String)
    fun onRemoveConfirm()
    fun onRemoveDismiss()
    fun onEditNoteClick(cafeId: String)
    fun onNoteChange(text: String)
    fun onNoteSave()
    fun onEditNoteDismiss()
    fun onCafeClick(cafeId: String)
    fun onRetry()
}

class BookmarkActionViewModel(private val repo: BookmarkRepository = BookmarkRepository()) : ViewModel() {
    val uiState: StateFlow<BookmarkActionUiState>

    fun load(cafeId: String)
    fun onToggle()
}
```

Ketentuan:

- `load()` dipanggil saat layar dibuka **dan** setelah setiap perubahan; jangan mengubah list di memori secara manual.
- `onToggle` menonaktifkan tombol selama `isSaving`; dua tekanan cepat tidak boleh menghasilkan dua baris.
- Bila pengguna belum login, `requiresLogin = true` dan tombol meminta login, bukan diam saja.
- Kegagalan diubah `ApiError` lalu masuk `errorMessage`.
- Setelah catatan disimpan, tutup dialog dan muat ulang daftar.

---

## 8. Langkah 6 — layar

### 8.1 `BookmarkScreen`

| Yang ditambahkan/diubah | Perilaku benar |
|---|---|
| Indikator muat | Muncul saat `isLoading`; daftar lama tidak dikosongkan |
| Empty state | Muncul saat kosong dan tidak memuat; beri petunjuk "simpan cafe dari Direktori" |
| Error + coba lagi | Pesan dari `errorMessage`, tombol memanggil `onRetry` |
| `LazyColumn` + `key = { it.id }` | Wajib, supaya kartu tidak dibuat ulang saat satu item dihapus |
| Hapus | Dialog konfirmasi, lalu panggil repository; jangan hanya menghapus dari list |
| Edit catatan | Simpan ke kolom `note`; setelah berhasil muat ulang |
| Cafe diarsipkan | Tetap ditampilkan dengan penanda **Tidak aktif**; jangan disembunyikan diam-diam |
| Rating | `null` → "Belum ada ulasan", bukan 0 bintang |
| Foto | Dari `photoUrl(path)`; tanpa foto pakai placeholder |
| Klik kartu | `onCafeClick(cafeId)` — id saja, detail memuat sendiri |

### 8.2 `BookmarkAction.kt` (slot untuk layar detail cafe)

| Ketentuan | Alasan |
|---|---|
| Parameter hanya `cafeId: String` | Ossa tidak perlu tahu model atau ViewModel-mu |
| ViewModel sendiri lewat `viewModel()` | Tidak menerima ViewModel dari layar induk |
| Ikon terisi bila tersimpan, garis bila belum | Status jelas tanpa perlu teks panjang |
| `LaunchedEffect(cafeId)` memuat status | Detail bisa dibuka dari Vibe, Event, atau Bookmark |
| Menampilkan pesan singkat setelah berhasil | "Disimpan ke Saved Spot" / "Dihapus dari Saved Spot" |
| Belum login → minta login | Jangan biarkan tombol tampak berhasil padahal tidak menyimpan |
| Tidak mengubah apa pun di luar composable ini | File layar tetap milik Ossa |

Slot ini boleh dipakai admin juga: bookmark bersifat pribadi, jadi admin menyimpan daftarnya sendiri dan tetap tidak bisa melihat daftar orang lain.

---

## 9. Alur simpan dan hapus

| Situasi | Yang dijalankan | Hasil yang benar |
|---|---|---|
| Menekan simpan pada cafe yang belum tersimpan | `add(cafeId)` | Satu baris baru |
| Menekan simpan dua kali cepat | `add` dengan konflik diabaikan | Tetap satu baris |
| Menekan hapus | `remove(cafeId)` | Satu baris hilang, kartu hilang dari daftar |
| Mengubah catatan | `updateNote(cafeId, note)` | Catatan tersimpan; setelah aplikasi dibuka ulang tetap ada |
| Akun lain membuka daftarnya | `observeBookmarks()` | Hanya baris miliknya; bookmark akun pertama tidak terlihat |
| Admin membuka daftarnya | sama | Hanya miliknya sendiri |

Catatan tersimpan di database, bukan di memori. Uji dengan menutup aplikasi lalu membukanya lagi: kalau catatan kembali kosong, berarti masih disimpan di state saja.

---

## 10. Daftar periksa sebelum push

- [ ] `BookmarkItem` membawa data cafe hasil join dan `savedAt: Instant`.
- [ ] Tidak ada `DummyData` di file fitur Bookmark.
- [ ] Tidak ada pemanggilan Supabase di luar `BookmarkRepository`.
- [ ] `BookmarkUiState` hanya punya satu list (tidak ada `cafes` terpisah).
- [ ] Dialog menyimpan `cafeId`, bukan objek `BookmarkItem`.
- [ ] `add` tidak gagal saat ditekan dua kali (konflik `(user_id, cafe_id)` diabaikan).
- [ ] Hapus dan ubah catatan memeriksa jumlah baris yang terpengaruh.
- [ ] Catatan tetap ada setelah aplikasi ditutup dan dibuka lagi.
- [ ] Dua akun berbeda melihat daftar berbeda.
- [ ] `LazyColumn` memakai `key = { it.id }`.
- [ ] Loading, empty, dan error+retry diuji dengan Wi-Fi mati.
- [ ] Cafe arsip tampil dengan penanda, tidak disembunyikan.
- [ ] `BookmarkAction` hanya menerima `cafeId` dan punya ViewModel sendiri.
- [ ] Slot sudah disepakati dengan Ossa dan dipasang di `progres2`.
- [ ] Tidak ada kode yang mengandaikan admin bisa melihat bookmark orang lain.

---

## 11. Bukti demo (5 langkah, ±30 detik)

| No | Yang dilakukan | Berhasil bila |
|---|---|---|
| 1 | Login user A, simpan satu cafe dari detail | Ikon berubah, kartu muncul di Saved Spot |
| 2 | Tekan simpan lagi pada cafe yang sama | Tidak ada baris ganda, tidak ada error |
| 3 | Ubah catatan, tutup aplikasi, buka lagi | Catatan masih ada |
| 4 | Login user B, buka Saved Spot | Daftar kosong; bookmark user A tidak terlihat |
| 5 | Hapus satu bookmark | Kartu hilang dan tetap hilang setelah muat ulang |

---

## 12. Kesalahan yang sering terjadi

| Gejala | Penyebab | Perbaikan |
|---|---|---|
| `duplicate key ... bookmarks_one_per_user_cafe` | Insert tanpa menangani konflik | Abaikan konflik atau periksa dulu |
| Daftar kosong padahal sudah menyimpan | `userId` tidak terkirim sebagai sesi, atau policy memfilter | Pastikan login; jangan mengirim `user_id` manual |
| Hapus terasa berhasil, kartu kembali setelah muat ulang | Hanya menghapus dari list di memori | Panggil `remove` lalu `load()` |
| Catatan hilang setelah aplikasi dibuka ulang | Catatan disimpan di state saja | Simpan ke kolom `note` |
| Tombol tampak berhasil padahal belum login | `requiresLogin` tidak ditangani | Minta login lebih dulu |
| Semua akun melihat daftar yang sama | Bukan dari database, atau memakai akun yang sama saat uji | Uji dengan dua akun berbeda |

---

## 13. Yang tidak perlu dikerjakan

- Pagination; cukup `limit(50)`.
- Realtime subscription; muat ulang setelah perubahan.
- Room / cache offline.
- Dependency injection framework.
- Folder atau koleksi bookmark bertingkat — tidak diminta ketentuan tugas.
- Sinkronisasi bookmark antar perangkat secara langsung; database sudah menanganinya.

---

## 14. Batas dengan anggota lain

| Hal | Aturan |
|---|---|
| `DirectoryDetailScreen.kt` | milik Ossa; kamu hanya menyerahkan `BookmarkAction(cafeId)` |
| `Cafe`, `CafeCard` | milik Ossa; jangan diubah dari fitur Bookmark |
| `Review`, `ReviewCard`, `ReviewSection` | milik Shelfina; jangan dipanggil dari fitur ini |
| Tabel `bookmarks` dan policy | jangan diubah; ajukan ke Nabil |
| Tabel `reviews` | baca hanya lewat `cafes_with_rating`; jangan query langsung |
