# Panduan Integrasi Supabase — Fitur 2: Ulasan & Rating (Shelfina)

Dokumen ini berisi **apa yang harus dibuat dan diubah** di kode fitur Ulasan & Rating supaya terhubung ke Supabase. Bukan SQL, bukan pembuatan tabel.

| Urusan | Dikerjakan oleh |
|---|---|
| Kode fitur Ulasan & Rating | **Shelfina** (dokumen ini) |
| Slot di layar detail cafe | **Ossa** memasang, **Shelfina** mengisi |

Kamu tidak perlu menjalankan SQL. Kalau ada kolom yang kurang, info saja di grub.

---

## 1. Yang harus sudah ada sebelum kamu mulai

| Prasyarat | Cara memastikan |
|---|---|
| `SupabaseClient.kt` dan `ApiError.kt` sudah ada | Bisa login dengan akun demo |
| `AuthUiState` menyimpan `userId` dan `role` | Label peran di profil tampil benar |
| Tabel `reviews` dan `profiles` sudah ada, view `cafes_with_rating` bisa dibaca | sudah ada |
| Nama slot `ReviewSection(cafeId)` sudah disepakati dengan Ossa | Bisa tanya ossa |

Slot butuh kesepakatan karena file `DirectoryDetailScreen.kt` milik Ossa. Kalau kamu menulis langsung ke file itu, konflik merge dijamin terjadi.

---

## 2. Peta file

| File | Status | Isi |
|---|---|---|
| `core/model/Review.kt` | **ubah** | Field menyesuaikan database |
| `core/components/ReviewCard.kt` | **ubah (koordinasi)** | Tanggal dari `createdAt`, penanda "ulasan saya" |
| `features/reviews/ReviewRow.kt` | **baru** | Bentuk baris dari Supabase + join profil |
| `features/reviews/ReviewMapper.kt` | **baru** | `ReviewRow` → `Review` |
| `features/reviews/ReviewRepository.kt` | **baru** | Satu-satunya tempat memanggil Supabase |
| `features/reviews/ReviewFormDialog.kt` | **baru** | Form bintang, komentar, menu favorit |
| `features/reviews/ReviewSection.kt` | **baru** | Composable mandiri untuk dipasang di detail cafe |
| `features/reviews/ReviewsViewModel.kt` | **ubah** | Ganti `DummyData`, hapus list ganda |
| `features/reviews/ReviewsScreen.kt` | **ubah** | Loading/empty/error, filter dari repository |

`DirectoryDetailScreen.kt` **bukan** milikmu. Yang kamu serahkan ke Ossa hanya satu baris pemanggilan `ReviewSection(cafeId = ...)`.

---

### 2.1 Skema tabel yang boleh kamu pakai

**`reviews`** — tabel utama fitur ini.

| Kolom | Tipe | Catatan |
|---|---|---|
| `id` | uuid | Dibuat database |
| `user_id` | uuid | FK ke `profiles`; harus sama dengan pengguna yang login |
| `cafe_id` | uuid | FK ke `cafes` |
| `rating` | smallint | 1–5, wajib |
| `comment` | text | Boleh kosong |
| `favorite_menu` | text | Boleh kosong |
| `created_at` | timestamptz | Diisi database |
| `updated_at` | timestamptz | Diisi trigger, jangan dikirim |

Aturan penting: pasangan `(user_id, cafe_id)` **unik**. Satu pengguna hanya punya satu ulasan per cafe, sehingga mengirim ulasan kedua harus memakai upsert, bukan insert biasa.

Hak akses yang berlaku:

| Operasi | Hasil untukmu |
|---|---|
| Baca semua ulasan | boleh |
| Insert ulasan sendiri (`user_id = auth.uid()`) | boleh |
| Insert ulasan atas nama orang lain | ditolak `42501` |
| Update/delete ulasan sendiri | boleh |
| Update/delete ulasan orang lain | **0 baris**, tanpa pesan error |

Baris terakhir itu penting: tidak ada exception, tetapi tidak ada yang berubah. Periksa jumlah baris hasil, jangan menyimpulkan "berhasil" dari tidak adanya error.

**`profiles`** — hanya baca, untuk nama penulis.

| Kolom | Tipe | Catatan |
|---|---|---|
| `id` | uuid | Sama dengan `auth.uid()` |
| `display_name` | text | Ditampilkan sebagai nama penulis |
| `avatar_path` | text | Belum dipakai UI |

Email tidak ada di tabel ini. Jangan menampilkan email penulis.

**`cafes_with_rating`** — baca saja.

| Kolom | Tipe | Catatan |
|---|---|---|
| `id`, `name` | uuid, text | Nama cafe untuk judul |
| `rating` | numeric(2,1) | Rata-rata dari ulasan; **null** bila kosong |
| `review_count` | integer | Jumlah ulasan |

Jangan menghitung rata-rata sendiri dari daftar ulasan yang termuat. Daftar bisa terpotong `limit`, sedangkan view selalu menghitung seluruh ulasan.

Kolom yang **tidak boleh** kamu isi: `id`, `created_at`, `updated_at`, dan semua kolom di view.

---

## 3. Langkah 1 — sesuaikan model `Review`

```kotlin
data class Review(
    val id: String,
    val userId: String,
    val cafeId: String,
    val userName: String,        // dari profiles.display_name
    val cafeName: String,        // dari cafes.name
    val rating: Int,
    val comment: String,
    val favoriteMenu: String,
    val createdAt: Instant,
    val isMine: Boolean = false, // dihitung dari userId == pengguna login
)
```

| Lama | Baru | Kenapa |
|---|---|---|
| `dateLabel: String` | `createdAt: Instant` | Label tanggal dibuat di UI; data menyimpan waktu |
| — (belum ada) | `userId`, `cafeId` | Diperlukan untuk upsert dan menandai ulasan sendiri |
| — (belum ada) | `isMine` | Tombol edit/hapus hanya muncul pada ulasan sendiri |

`userName` dan `cafeName` tetap ada, tetapi **bukan** kolom tabel ulasan. Keduanya hasil join, jadi tidak pernah basi saat nama berubah.

Perubahan `Review` memengaruhi `ReviewCard` (dipakai layar Ulasan) dan beranda yang menampilkan ulasan terbaru. Umumkan di grup sebelum push.

---

## 4. Langkah 2 — `ReviewRow` dan `ReviewMapper`

Nama kolom database hanya boleh muncul di dua file ini dan `ReviewRepository`.

```kotlin
@Serializable
data class ReviewRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("cafe_id") val cafeId: String,
    val rating: Int,
    val comment: String = "",
    @SerialName("favorite_menu") val favoriteMenu: String = "",
    @SerialName("created_at") val createdAt: String,
    val profiles: ProfileNameRow? = null,   // hasil join
    val cafes: CafeNameRow? = null,         // hasil join
)

@Serializable data class ProfileNameRow(@SerialName("display_name") val displayName: String = "Pengguna")
@Serializable data class CafeNameRow(val name: String = "")

data class ReviewDraft(                     // isi form
    val rating: Int,
    val comment: String,
    val favoriteMenu: String,
)

object ReviewMapper {
    fun toModel(row: ReviewRow, currentUserId: String?): Review
}
```

Aturan mapper:

- `createdAt` dari ISO string diubah ke `Instant` di satu tempat; UI tidak mem-parsing string.
- `isMine = row.userId == currentUserId`.
- `display_name` kosong → `"Pengguna"`; jangan pernah menampilkan email.

---

## 5. Langkah 3 — `ReviewRepository`

```kotlin
class ReviewRepository(private val client: SupabaseClient = Supabase.client) {

    suspend fun observeReviews(cafeId: String? = null, limit: Int = 50): List<Review>

    suspend fun loadMyReview(cafeId: String): Review?

    suspend fun upsertReview(cafeId: String, draft: ReviewDraft): Review

    suspend fun deleteMyReview(cafeId: String)

    suspend fun cafeRating(cafeId: String): CafeRating
}

data class CafeRating(val rating: Double?, val reviewCount: Int)
```

Ketentuan:

| Ketentuan | Alasan |
|---|---|
| Query memakai join `profiles:user_id(display_name)` | Nama penulis tidak diduplikasi di tabel ulasan |
| `cafeId = null` berarti semua ulasan (layar Ulasan) | Layar daftar menampilkan semua; detail menampilkan satu cafe |
| `upsertReview` memakai `onConflict = "user_id,cafe_id"` | Unique constraint; ulasan kedua akan ditolak bila memakai insert |
| `deleteMyReview` mengembalikan jumlah baris terpengaruh | Update/delete ulasan orang lain menghasilkan 0 baris tanpa error |
| `cafeRating` membaca view, bukan menghitung dari list | Daftar bisa terpotong `limit`, view tidak |
| `user_id` diambil dari sesi di dalam repository | Kolomnya tanpa nilai bawaan; policy menolak `user_id` yang bukan `auth.uid()` (`42501`) |
| Selalu `limit` | Jangan memuat seluruh tabel |

---

## 6. Langkah 4 — state

Dua state, satu layar dan satu slot. Hapus `allReviews` + `filteredReviews`; simpan satu list.

```kotlin
data class ReviewsUiState(
    val reviews: List<Review> = emptyList(),
    val ratingFilter: Int? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isAdmin: Boolean = false,
)

data class ReviewSectionUiState(
    val cafeId: String = "",
    val reviews: List<Review> = emptyList(),
    val myReview: Review? = null,
    val rating: Double? = null,        // dari view
    val reviewCount: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val canWrite: Boolean = false,     // login dan bukan admin
)

data class ReviewFormUiState(
    val rating: Int = 0,
    val comment: String = "",
    val favoriteMenu: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val validationError: String? = null,
)
```

`canWrite = (peran == "user")`. Admin boleh membaca, tetapi tidak menulis ulasan orang lain — jadi tombol tulis tidak ditampilkan untuk admin, bukan hanya disembunyikan setelah ditekan.

---

## 7. Langkah 5 — ViewModel

```kotlin
class ReviewsViewModel(private val repo: ReviewRepository = ReviewRepository()) : ViewModel() {
    val uiState: StateFlow<ReviewsUiState>

    fun onRatingFilterChange(rating: Int?)
    fun onRetry()
}

class ReviewSectionViewModel(private val repo: ReviewRepository = ReviewRepository()) : ViewModel() {
    val uiState: StateFlow<ReviewSectionUiState>

    fun load(cafeId: String)
    fun onSubmit(draft: ReviewDraft)
    fun onDeleteMine()
    fun onRetry()
}

class ReviewFormViewModel : ViewModel() {   // boleh digabung ke ReviewSectionViewModel
    val formState: StateFlow<ReviewFormUiState>

    fun onRatingChange(stars: Int)
    fun onCommentChange(text: String)
    fun onFavoriteMenuChange(text: String)
    fun onReset()                            // dipakai saat dialog dibuka ulang
}
```

Ketentuan:

- Validasi rating 1–5 diisi di aplikasi sebelum memanggil repository; database juga memeriksanya, tetapi pesan dari aplikasi lebih cepat.
- Setelah `upsertReview` berhasil, muat ulang daftar **dan** `cafeRating` supaya angka di header berubah.
- Kegagalan diubah `ApiError` lalu masuk `errorMessage`; jangan menampilkan pesan mentah Supabase.
- Jangan menyimpan `myReview` dan `reviews` dari dua sumber berbeda: `myReview` adalah `reviews.firstOrNull { it.isMine }` setelah daftar dimuat.

---

## 8. Langkah 6 — layar

### 8.1 `ReviewsScreen`

| Yang ditambahkan/diubah | Perilaku benar |
|---|---|
| Indikator muat | Muncul saat `isLoading`; daftar lama tetap tampil |
| Empty state | Muncul saat kosong dan tidak memuat; bedakan "belum ada ulasan" vs "tidak ada ulasan untuk rating ini" |
| Error + coba lagi | Pesan dari `errorMessage`, tombol memanggil `onRetry` |
| Filter rating | Satu angka di state; hasil dari repository, bukan dua list |
| Ringkasan rata-rata | Dari view (`rating`, `reviewCount`), bukan rata-rata list yang termuat |
| `LazyColumn` + `key = { it.id }` | Wajib, supaya daftar tidak dibuat ulang |
| Tombol tulis | Boleh tetap ada di layar ini, tetapi mengarahkan pengguna memilih cafe dulu — ulasan selalu terikat `cafe_id` |

### 8.2 `ReviewFormDialog`

| Bagian | Isi |
|---|---|
| Bintang | Pilihan 1–5, wajib; tombol simpan nonaktif selama rating 0 |
| Komentar | Multiline, boleh kosong |
| Menu favorit | Satu baris, boleh kosong |
| Field yang **dihapus** | "Nama Anda" dan "Nama Cafe" — dua-duanya berasal dari sesi dan `cafeId`, bukan diketik |
| Mode | Judul berubah "Beri ulasan" atau "Edit ulasan"; satu form untuk keduanya |
| Proses simpan | Tombol nonaktif saat `isSaving`; setelah berhasil dialog tertutup dan daftar diperbarui |

Field "Nama Anda" pada versi sekarang adalah sumber data palsu. Nama penulis datang dari `profiles.display_name`; kalau dibiarkan bisa diketik, ulasan orang bisa memakai nama siapa saja.

### 8.3 `ReviewSection.kt` (slot untuk layar detail cafe)

Composable ini dipasang Ossa di `DirectoryDetailScreen`. Ketentuannya:

| Ketentuan | Alasan |
|---|---|
| Parameter hanya `cafeId: String` | Ossa tidak perlu tahu model atau ViewModel-mu |
| Membuat ViewModel sendiri dengan `viewModel()` | Tidak menerima ViewModel dari layar induk |
| Memuat data di `LaunchedEffect(cafeId)` | Detail bisa dibuka dari Vibe atau Bookmark |
| Menampilkan: ringkasan rating, tombol **Beri ulasan**/**Edit ulasan**, daftar ulasan cafe itu | Satu blok utuh, bukan potongan yang tersebar |
| Admin: tanpa tombol tulis | Admin hanya membaca |
| Tidak mengubah apa pun di luar composable ini | File layar tetap milik Ossa |

Sebelum Ossa memasang slotnya, kamu bisa menguji `ReviewSection` di layar sendiri dengan `cafeId` contoh yang diambil dari daftar cafe.

---

## 9. Alur menulis ulasan

| Situasi | Yang dijalankan | Hasil yang benar |
|---|---|---|
| Belum pernah mengulas cafe ini | `upsertReview` dengan rating baru | Satu baris baru |
| Sudah pernah mengulas, menekan **Edit ulasan** | `upsertReview` cafe yang sama | Baris yang sama berubah, bukan baris kedua |
| Menghapus ulasan sendiri | `deleteMyReview` | Satu baris hilang, angka rating cafe berubah |
| Admin menekan hapus pada ulasan orang lain | tombol tidak ada | — |
| Pengguna lain mengubah ulasan yang bukan miliknya | tombol tidak ada; bila dipaksa, 0 baris | Data tidak berubah |

Setelah setiap operasi tulis, `rating` dan `review_count` dari view ikut berubah tanpa aplikasi menghitung apa pun.

---

## 10. Daftar periksa sebelum push

- [ ] `Review` memakai `userId`, `cafeId`, `createdAt`, `isMine`.
- [ ] Tidak ada `DummyData` di file fitur Ulasan.
- [ ] Tidak ada pemanggilan Supabase di luar `ReviewRepository`.
- [ ] `reviews` dibaca dengan join `profiles` untuk nama penulis.
- [ ] `ReviewsUiState` tidak lagi menyimpan `allReviews` + `filteredReviews`.
- [ ] Rata-rata rating diambil dari `cafes_with_rating`.
- [ ] Simpan ulasan memakai upsert dengan konflik `(user_id, cafe_id)`.
- [ ] Field "Nama Anda" dan "Nama Cafe" sudah dihapus dari form.
- [ ] Tombol tulis tidak muncul untuk admin.
- [ ] Tombol edit/hapus hanya pada ulasan sendiri (`isMine`).
- [ ] Setelah simpan/hapus, daftar dan angka rating dimuat ulang.
- [ ] `LazyColumn` memakai `key = { it.id }`.
- [ ] Loading, empty, dan error+retry diuji dengan Wi-Fi mati.
- [ ] `ReviewSection` hanya menerima `cafeId`, punya ViewModel sendiri.
- [ ] Slot sudah disepakati dengan Ossa dan dipasang di `progres2`.

---

## 11. Bukti demo (5 langkah, ±30 detik)

| No | Yang dilakukan | Berhasil bila |
|---|---|---|
| 1 | Login user, buka detail cafe | Tombol **Beri ulasan** terlihat, ringkasan rating tampil |
| 2 | Beri rating 5 + komentar | Ulasan muncul, angka rating cafe berubah |
| 3 | Buka lagi, tekan **Edit ulasan** | Form terisi data lama; simpan tidak menambah baris |
| 4 | Hapus ulasan sendiri | Ulasan hilang, angka rating turun |
| 5 | Login admin, buka detail cafe yang sama | Ulasan terbaca, tidak ada tombol tulis/hapus |

---

## 12. Kesalahan yang sering terjadi

| Gejala | Penyebab | Perbaikan |
|---|---|---|
| `duplicate key value ... reviews_one_per_user_cafe` | Memakai insert, bukan upsert | Gunakan `onConflict = "user_id,cafe_id"` |
| Ulasan tersimpan tetapi nama penulis kosong | Join tidak memakai alias `profiles:user_id(...)` | Perbaiki query |
| Rating tampil 0.0 | `null` diubah jadi 0 | Biarkan `null`, tampilkan "Belum ada ulasan" |
| Rata-rata berbeda dengan hitungan manual | Rata-rata dihitung dari list yang terpotong `limit` | Ambil dari view |
| Tombol hapus tidak berefek | Update/delete 0 baris karena bukan milik sendiri | Periksa `isMine` sebelum menampilkan tombol |
| Tanggal tampil aneh | String ISO dipakai langsung | Ubah ke `Instant` di mapper, format di UI |
| Daftar berkedip saat filter | List ganda atau `key` tidak ada | Satu list + `key = { it.id }` |

---

## 13. Yang tidak perlu dikerjakan

- Pagination atau infinite scroll; cukup `limit(50)`.
- Realtime subscription; muat ulang setelah simpan.
- Room / cache offline.
- Dependency injection framework; nilai default di konstruktor cukup.
- Fitur "balas ulasan" atau "suka ulasan" — tidak diminta ketentuan tugas.
- Menghitung ulang rata-rata di aplikasi.

---

## 14. Batas dengan anggota lain

| Hal | Aturan |
|---|---|
| `DirectoryDetailScreen.kt` | milik Ossa; kamu hanya menyerahkan `ReviewSection(cafeId)` |
| `Cafe`, `CafeCard` | milik Ossa; jangan diubah dari fitur Ulasan |
| `ReviewCard` | dipakai bersama (layar Ulasan dan beranda); perubahan bentuknya diumumkan |
| Tabel `reviews` dan policy | jangan diubah |
| Bookmark | milik Shananda; jangan membaca `bookmarks` dari fitur ini |
| Tabel `cafes` | baca saja; jangan menulis dari fitur Ulasan |
