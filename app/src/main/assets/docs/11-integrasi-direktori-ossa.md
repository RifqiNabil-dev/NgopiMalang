# Panduan Integrasi Supabase — Fitur 1: Direktori Cafe (Ossa)

Dokumen ini berisi **apa yang harus dibuat dan diubah** di kode fitur Direktori supaya terhubung ke Supabase. Bukan SQL, bukan pembuatan tabel.

Pembagian tugasnya:

| Urusan | Dikerjakan oleh |
|---|---|
| Membuat tabel, view, policy, bucket, akun demo | **Nabil (ketua tim)** — lihat [09-skema-database.md](09-skema-database.md) |
| Menambah dependency Gradle, `SupabaseClient`, `ApiError` | **Nabil** — sekali untuk semua fitur |
| Kode fitur Direktori | **Ossa** (dokumen ini) |
| Kode fitur lain | anggota masing-masing, pola sama |

Kamu **tidak perlu** dan **tidak boleh** menjalankan SQL sendiri. Kalau ada kolom yang kurang, ajukan ke Nabil; jangan menambah tabel.

---

## 1. Yang harus sudah ada sebelum kamu mulai

Kalau salah satu belum ada, berhenti dan tunggu — kalau dipaksa, kamu akan menulis kode dua kali.

| Prasyarat | Cara memastikan |
|---|---|
| `core/data/SupabaseClient.kt` sudah ada dan bisa dipakai | Bisa login dari aplikasi dengan akun demo |
| `core/data/ApiError.kt` sudah ada | Sudah ada fungsi penerjemah kode error |
| `AuthUiState.role` sudah terisi `"admin"`/`"user"` | Buka profil, label peran tampil benar |
| Dependency Supabase, Ktor, serialization, Coil sudah masuk `build.gradle` | Project bisa `sync` tanpa error |
| Tabel `cafes`, `vibes`, `cafe_vibes` dan bucket `cafe-photos` sudah ada | Nabil sudah mengonfirmasi di grup |

Jangan menambah dependency sendiri. Semua kebutuhan pustaka masuk sekali di `gradle/libs.versions.toml` oleh Nabil, supaya tidak ada dua versi berbeda di branch yang berbeda.

---

## 2. Peta file

| File | Status | Isi |
|---|---|---|
| `core/model/Cafe.kt` | **ubah** | Field disesuaikan dengan database |
| `features/directory/CafeRow.kt` | **baru** | Bentuk baris dari Supabase (`@Serializable`) |
| `features/directory/CafeMapper.kt` | **baru** | `CafeRow` → `Cafe`, dan `CafeDraft` → baris |
| `features/directory/CafeRepository.kt` | **baru** | Satu-satunya tempat memanggil Supabase |
| `features/directory/CafeFormScreen.kt` | **baru** | Form tambah/edit |
| `features/directory/DirectoryViewModel.kt` | **ubah** | Ganti `DummyData` dengan repository, tambah state |
| `features/directory/DirectoryScreen.kt` | **ubah** | Tambah loading/empty/error, tombol admin |
| `features/directory/DirectoryDetailScreen.kt` | **ubah** | Muat data sendiri, badge arsip, slot, menu admin |
| `core/components/CafeCard.kt` | **ubah (koordinasi)** | Foto dari Storage, harga dari dua angka; dipakai beranda & bookmark |

`DummyData.kt` **tidak** kamu hapus. Nabil yang menghapus setelah semua fitur pindah.

### 2.1 Skema tabel yang boleh kamu pakai

Ini daftar objek yang sudah dibuat Nabil. Kamu **memakai**, bukan membuat. Nama kolom harus persis seperti di bawah — salah satu huruf saja membuat query gagal.

**`cafes_with_rating`** — view, dipakai untuk semua pembacaan daftar dan detail.

| Kolom | Tipe | Catatan |
|---|---|---|
| `id` | uuid | Kunci utama; dipakai untuk membuka detail |
| `name` | text | Unik tanpa peduli huruf besar/kecil |
| `address` | text | Boleh kosong |
| `description` | text | Boleh kosong |
| `open_hours` | text | Teks bebas, mis. "08.00 – 22.00" |
| `price_min` | integer | Nullable; angka, bukan teks |
| `price_max` | integer | Nullable; `price_min <= price_max` |
| `has_wifi` | boolean | Untuk chip filter |
| `plug_count` | integer | Untuk chip filter colokan |
| `photo_path` | text | Path di bucket `cafe-photos`, bukan URL |
| `is_active` | boolean | `false` = arsip |
| `rating` | numeric(2,1) | Dari ulasan; **null** bila belum ada ulasan |
| `review_count` | integer | Jumlah ulasan |
| `created_by`, `created_at`, `updated_at` | uuid, timestamptz | Diisi database; jangan dikirim dari aplikasi |

**`cafes`** — tabel asli. Dipakai untuk menulis; membaca tetap dari view.

| Operasi | Hak | Catatan |
|---|---|---|
| Insert | admin | Isi kolom data saja; `id` dibuat database |
| Update | admin | Termasuk arsip: `update is_active = false` |
| Delete | admin | Hanya berhasil bila cafe belum dipakai ulasan/bookmark/event |

**`vibes`** — tabel kategori. Ossa hanya membaca, untuk pilihan vibe di form.

| Kolom | Tipe | Catatan |
|---|---|---|
| `id` | uuid | Dipakai saat menyimpan relasi |
| `name` | text | Ditampilkan di chip/detail |
| `icon_key` | text | Kunci ikon, mis. `work`, `aesthetic` |
| `description` | text | Boleh kosong |

**`vibes_with_count`** — view; sama seperti `vibes` ditambah `cafe_count`. Dipakai fitur Vibe, bukan fitur Direktori. Jangan dipakai untuk form.

**`cafe_vibes`** — relasi cafe ↔ vibe.

| Kolom | Tipe | Catatan |
|---|---|---|
| `cafe_id` | uuid | FK ke `cafes` |
| `vibe_id` | uuid | FK ke `vibes` |
| `created_at` | timestamptz | Diisi database |

Menulis relasi: hapus baris yang tidak dipilih lagi, lalu insert yang baru. Jangan lupa `cafe_id` dan `vibe_id` bersama merupakan kunci unik — insert ganda akan ditolak.

**Storage `cafe-photos`** — bukan tabel, tetapi perlu diketahui bentuknya.

| Hal | Nilai |
|---|---|
| Jalur | `{cafe_id}/{uuid}.jpg` |
| Batas ukuran | 5 MB |
| Jenis | JPEG, PNG, WebP |
| Baca | semua pengguna login |
| Tulis | admin |

Kolom yang **tidak boleh** kamu isi dari aplikasi: `id`, `created_by`, `created_at`, `updated_at`, `rating`, `review_count`. Empat yang pertama diisi database; dua yang terakhir milik view dan tidak bisa ditulis sama sekali.

---

## 3. Langkah 1 — sesuaikan model `Cafe`

Field mengikuti kolom database. Kalau kamu mempertahankan `priceRange: String`, kamu akan menulis ulang form dua kali.

```kotlin
data class Cafe(
    val id: String,
    val name: String,
    val address: String,
    val description: String = "",     // dipakai form edit
    val openHours: String = "",       // dipakai form edit & detail
    val priceMin: Int? = null,
    val priceMax: Int? = null,
    val hasWifi: Boolean = false,
    val plugCount: Int = 0,
    val rating: Double? = null,       // null = belum ada ulasan
    val reviewCount: Int = 0,
    val vibeNames: List<String> = emptyList(),
    val photoPath: String? = null,    // path di bucket cafe-photos
    val isActive: Boolean = true,
)
```

Yang hilang dari model lama dan penggantinya:

| Lama | Baru | Kenapa |
|---|---|---|
| `priceRange: String` | `priceMin`, `priceMax` | Database menyimpan angka; format "Rp15.000 – Rp30.000" dibuat di UI |
| `rating: Double` | `rating: Double?` | Cafe tanpa ulasan bukan berarti rating 0 |
| `vibes: List<String>` | `vibeNames: List<String>` | Nama vibe datang dari relasi, bukan teks bebas |
| `imageResId: Int?` | `photoPath: String?` | Foto dari Storage, bukan `res/drawable` |

Perubahan ini **wajib diumumkan di grup** sebelum kamu push, karena `Cafe` dipakai beranda, Vibe, dan Bookmark. Kalau langsung push, tiga branch lain gagal compile.

---

## 4. Langkah 2 — `CafeRow` dan `CafeMapper`

`CafeRow` adalah bentuk mentah dari Supabase: nama kolom `snake_case`, semua field yang bisa kosong dibuat nullable dengan nilai awal. Jangan pakai `Cafe` langsung sebagai tipe hasil query.

```kotlin
@Serializable
data class CafeRow(
    val id: String,
    val name: String,
    val address: String = "",
    val description: String = "",
    @SerialName("open_hours") val openHours: String = "",
    @SerialName("price_min") val priceMin: Int? = null,
    @SerialName("price_max") val priceMax: Int? = null,
    @SerialName("has_wifi") val hasWifi: Boolean = false,
    @SerialName("plug_count") val plugCount: Int = 0,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val rating: Double? = null,
    @SerialName("review_count") val reviewCount: Int = 0,
)

@Serializable
data class CafeVibeRow(
    @SerialName("cafe_id") val cafeId: String,
    @SerialName("vibes") val vibe: VibeNameRow? = null,
)

@Serializable
data class VibeNameRow(val name: String)
```

Mapper:

```kotlin
object CafeMapper {
    fun toModel(row: CafeRow, vibeNames: List<String> = emptyList()): Cafe
    fun draftToRow(draft: CafeDraft): CafeRow
}

data class CafeDraft(            // isi form, bukan model tampilan
    val name: String,
    val address: String,
    val description: String,
    val openHours: String,
    val priceMin: Int?,
    val priceMax: Int?,
    val hasWifi: Boolean,
    val plugCount: Int,
    val vibeIds: List<String>,
)
```

Aturan mapper:

- Nama kolom `snake_case` **hanya** boleh muncul di file ini.
- `null` dari database tetap `null`; jangan diganti `0` atau `""` diam-diam.
- Format rupiah, format jam, dan label "belum ada ulasan" dibuat di UI, bukan di mapper.

---

## 5. Langkah 3 — `CafeRepository`

Satu file, satu tabel utama. Semua fungsi `suspend`, semua memakai `SupabaseClient` yang sudah disiapkan Nabil.

```kotlin
class CafeRepository(private val client: SupabaseClient = Supabase.client) {

    suspend fun observeCafes(
        query: String = "",
        wifiOnly: Boolean = false,
        plugsOnly: Boolean = false,
        limit: Int = 50,
    ): List<Cafe>

    suspend fun loadCafe(id: String): Cafe?

    suspend fun create(draft: CafeDraft, photo: ByteArray?): Cafe

    suspend fun update(id: String, draft: CafeDraft, newPhoto: ByteArray?): Cafe

    suspend fun archive(id: String)          // is_active = false

    suspend fun deleteOrArchive(id: String): DeleteOutcome   // DELETE, kalau 23503 → archive

    suspend fun uploadPhoto(cafeId: String, bytes: ByteArray): String  // mengembalikan path

    fun photoUrl(path: String?): String?     // publicUrl bucket cafe-photos
}

enum class DeleteOutcome { DELETED, ARCHIVED }
```

Ketentuan yang tidak boleh dilanggar:

| Ketentuan | Alasan |
|---|---|
| Baca daftar dari `cafes_with_rating`, bukan `cafes` | Rating dan jumlah ulasan sudah dihitung di view; jangan hitung ulang di aplikasi |
| Detail memakai `loadCafe(id)` | Detail bisa dibuka dari Vibe, Bookmark, dan Event — daftar Direktori belum tentu termuat |
| `deleteOrArchive` menangkap kode `23503` lalu memanggil `archive` | Hapus cafe yang sudah diulas akan ditolak database |
| `uploadPhoto` mengembalikan path, bukan URL | Yang disimpan di kolom `photo_path` adalah path; URL dibuat saat menampilkan |
| Update dan delete memeriksa jumlah baris yang berubah | Policy hanya menyaring baris; operasi bisa "berhasil" dengan 0 baris |

### 5.1 Cara mengambil nama vibe

`Cafe` membawa `vibeNames`, tetapi **jangan** memaksakan satu query untuk semuanya.

| Layar | Cara mengambil | Alasan |
|---|---|---|
| Daftar Direktori | `vibeNames` dibiarkan kosong | Kartu hanya butuh nama, foto, rating, harga; menghindari query tambahan per kartu |
| Detail cafe | Satu query ke `cafe_vibes` dengan `select("vibes:vibe_id(name)")` untuk `cafe_id` itu | Satu kali per layar, bukan per baris |
| Form edit | Query yang sama dengan detail, untuk menandai vibe yang sudah dipilih | Form butuh id, bukan nama |

Jangan memanggil query vibe di dalam `items(...)`. Itu N+1 yang membuat daftar terasa lambat dan sulit dijelaskan saat demo.

Foto: alur ganti foto ada di bagian 8.

---

## 6. Langkah 4 — state

Satu state untuk daftar, satu untuk detail. Jangan menyimpan list hasil filter terpisah dari list aslinya.

```kotlin
data class DirectoryUiState(
    val cafes: List<Cafe> = emptyList(),
    val query: String = "",
    val wifiOnly: Boolean = false,
    val plugsOnly: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isAdmin: Boolean = false,
)

data class CafeDetailUiState(
    val cafe: Cafe? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isAdmin: Boolean = false,
    val notFound: Boolean = false,
)
```

`isAdmin` diambil dari `AuthUiState.role`, diteruskan dari `AppNavHost`. Jangan query `user_roles` lagi di fitur Direktori — satu sumber kebenaran untuk role.

---

## 7. Langkah 5 — ViewModel

Daftar fungsi yang harus ada. Nama boleh beda, perilakunya jangan.

```kotlin
class DirectoryViewModel(private val repo: CafeRepository = CafeRepository()) : ViewModel() {
    val uiState: StateFlow<DirectoryUiState>

    fun onSearchChange(query: String)      // tunggu 300 ms sebelum query, bukan tiap ketukan
    fun onToggleWifi()
    fun onTogglePlugs()
    fun onResetFilters()
    fun onRetry()
    fun onCafeClick(cafeId: String)        // cukup id
}

class CafeDetailViewModel(private val repo: CafeRepository = CafeRepository()) : ViewModel() {
    val uiState: StateFlow<CafeDetailUiState>

    fun load(cafeId: String)
    fun onSave(draft: CafeDraft, photo: ByteArray?)   // insert atau update
    fun onDelete()                                    // delete atau arsip
    fun onRetry()
}
```

Ketentuan:

- Setiap pemanggilan repository dibungkus `runCatching`; kegagalan diubah `ApiError` lalu masuk `errorMessage`.
- Setelah insert/update/delete berhasil, daftar **dimuat ulang**; jangan mengubah list di memori secara manual.
- `isSaving` menonaktifkan tombol simpan; jangan biarkan pengguna menekan dua kali.
- `findCafe` dihapus. Pemanggilan query di dalam `composable` juga dihapus.

---

## 8. Langkah 6 — layar

### 8.1 `DirectoryScreen`

| Yang ditambahkan/diubah | Perilaku benar |
|---|---|
| Indikator muat | Muncul saat `isLoading`, daftar lama tidak dikosongkan |
| Empty state | Muncul saat list kosong **dan** tidak sedang memuat; sebutkan filternya |
| Error + tombol coba lagi | Pesan dari `errorMessage`, tombol memanggil `onRetry` |
| FAB **Tambah Cafe** | Hanya bila `isAdmin` |
| Pencarian | Teks → `onSearchChange`, hasil dari repository |
| Chip Wi-Fi / colokan | `onToggleWifi`, `onTogglePlugs` |
| `items(key = { it.id })` | Wajib, supaya kartu tidak dibuat ulang saat filter berubah |
| Klik kartu | `onCafeClick(cafe.id)` — id saja |

### 8.2 `DirectoryDetailScreen`

| Yang diubah | Perilaku benar |
|---|---|
| Parameter layar | `cafeId: String`, bukan `Cafe?` |
| Sumber data | `CafeDetailViewModel.load(cafeId)` di `LaunchedEffect` |
| Status tidak ditemukan | `notFound` → tampilkan pesan, bukan layar kosong |
| Badge **Tidak aktif** | Tampil bila `isActive == false`, hanya admin yang melihat kartu arsip |
| Menu **Edit** dan **Hapus** | Hanya admin; Hapus memakai dialog konfirmasi |
| Slot ulasan dan bookmark | Sudah dipasang Ossa: `ReviewSection(cafeId)` dan `BookmarkAction(cafeId)` — jangan diubah isinya |
| Rating | Bila `null`, tampilkan "Belum ada ulasan", bukan bintang kosong |
| Harga | Dari `priceMin`/`priceMax`; bila salah satu `null`, tampilkan seadanya tanpa tanda hubung menggantung |

### 8.3 `CafeFormScreen` (baru)

| Bagian | Isi |
|---|---|
| Field | Nama, alamat, jam buka, harga minimum, harga maksimum, deskripsi, pilihan vibe (boleh lebih dari satu) |
| Pemilih foto | Ambil dari galeri, tampilkan pratinjau sebelum disimpan |
| Validasi | Nama wajib; `priceMin <= priceMax`; pesan kesalahan di field, bukan toast |
| Tombol simpan | Nonaktif saat `isSaving`; setelah berhasil kembali ke daftar |
| Mode | Satu layar untuk tambah dan edit; bedanya hanya data awal dan tombol hapus |

---

## 9. Langkah 7 — alur simpan yang menyertakan foto

Storage dan tabel tidak punya transaksi bersama. Ikuti urutan ini agar tidak ada data setengah jadi.

| Langkah | Aksi | Bila gagal |
|---|---|---|
| 1 | Simpan/ubah data cafe **tanpa** foto baru | Tampilkan error, tetap di form |
| 2 | Bila ada foto baru: unggah ke `cafe-photos/{cafe_id}/{uuid}.jpg` | Tampilkan error; data cafe sudah tersimpan, foto lama tetap dipakai |
| 3 | Simpan `photo_path` hasil unggah ke tabel | Bila gagal, hapus file baru lalu tampilkan error |
| 4 | Setelah berhasil, muat ulang daftar dan detail | — |
| 5 | Hapus file foto lama (opsional) | Boleh diabaikan; file yatim tidak merusak data |

Karena langkah 1 dan 2 dipisah, pengguna tidak pernah kehilangan isi form hanya karena unggah foto gagal.

---

## 10. Langkah 8 — peran admin dan user

| Aksi | Admin | User biasa |
|---|---|---|
| Melihat daftar dan detail cafe aktif | ya | ya |
| Melihat cafe arsip | ya | tidak |
| Tambah, edit, arsip, hapus | ya | tombol tidak ditampilkan |
| Unggah/ganti foto | ya | tidak |

Menyembunyikan tombol **bukan** pengamanan. Pengamanan ada di policy database; menyembunyikan tombol hanya supaya tampilan tidak membingungkan. Kalau user memaksa menulis, database menolak dengan `42501`, dan aplikasi harus menampilkan pesan yang bisa dibaca.

---

## 11. Daftar periksa sebelum push

- [ ] `Cafe` sudah memakai `priceMin`/`priceMax`, `photoPath`, `rating` nullable, `isActive`, `reviewCount`.
- [ ] Tidak ada `DummyData` di file fitur Direktori (`grep` bersih).
- [ ] Tidak ada pemanggilan Supabase di luar `CafeRepository`.
- [ ] Nama kolom `snake_case` hanya ada di `CafeMapper`/`CafeRow`.
- [ ] Daftar dibaca dari `cafes_with_rating`, bukan `cafes`.
- [ ] Detail memakai `loadCafe(id)`, bukan `findCafe`.
- [ ] `items(key = { it.id })` ada di grid.
- [ ] Indikator muat, empty state, dan error+retry tampil benar (uji dengan Wi-Fi mati).
- [ ] Tombol tambah/edit/hapus hanya muncul untuk admin.
- [ ] Simpan dua kali cepat tidak menghasilkan dua baris.
- [ ] Hapus cafe yang sudah diulas → aplikasi menawarkan arsip, bukan berhenti.
- [ ] Foto tampil dari Storage; kartu tanpa foto memakai placeholder.
- [ ] Pagar model: perubahan `Cafe` sudah diumumkan ke grup.
- [ ] `CafeCard.kt` diubah hanya setelah berkoordinasi (dipakai beranda dan bookmark).

---

## 12. Bukti demo (5 langkah, ±30 detik)

| No | Yang dilakukan | Berhasil bila |
|---|---|---|
| 1 | Login admin, buka Direktori | Tombol tambah terlihat |
| 2 | Tambah cafe + foto | Kartu muncul tanpa membuka ulang aplikasi |
| 3 | Edit harga cafe | Daftar dan detail langsung berubah |
| 4 | Hapus cafe yang sudah diulas | Muncul tawaran arsip; ulasan tetap ada |
| 5 | Login user, buka Direktori | Tombol tambah tidak ada; data tetap terbaca |

---

## 13. Kesalahan yang sering terjadi

| Gejala | Penyebab | Perbaikan |
|---|---|---|
| `Cannot find a parameter with this name` | Parameter `Cafe` berubah, layar lain belum diperbarui | Umumkan perubahan; perbarui pemanggil |
| Daftar kosong padahal data ada | RLS atau filter salah; cek `is_active` dan sesi | Pastikan login; cek pesan error dari `ApiError` |
| Foto tidak muncul | Kolom berisi URL, bukan path, atau bucket salah | Simpan path; URL dibuat lewat `photoUrl(path)` |
| Hapus gagal tanpa pesan jelas | Kode `23503` tidak ditangani | Tangkap lalu tawarkan arsip |
| Simpan terasa berhasil tapi data tidak berubah | Update kena policy (0 baris) | Periksa jumlah baris hasil update |
| Rating tampil 0.0 | `null` diubah jadi 0 | Biarkan `null`, tampilkan "Belum ada ulasan" |

---

## 14. Yang tidak perlu dikerjakan

Supaya tidak menambah beban presentasi:

- Pagination atau infinite scroll (data ±12 baris; cukup `limit(50)`).
- Cache offline, Room, atau WorkManager.
- Dependency injection (Hilt/Koin); cukup nilai default di konstruktor ViewModel.
- Realtime subscription; muat ulang setelah simpan sudah cukup.
- Animasi transisi khusus.
- Penulisan SQL atau perubahan tabel — itu milik Nabil.

---

## 15. Batas dengan anggota lain

| Hal | Aturan |
|---|---|
| `Cafe`, `CafeCard` | dipakai beranda, Vibe, Bookmark; perubahannya diumumkan dulu |
| `DirectoryDetailScreen` | milikmu; slot `ReviewSection` (Shelfina) dan `BookmarkAction` (Shananda) dipasang sekali, jangan diubah isinya |
| Repository ulasan / bookmark | jangan dipanggil dari fitur Direktori; itu milik fitur masing-masing |
| Tabel dan policy | jangan diubah; ajukan ke Nabil |
| `DummyData.kt` | jangan dihapus; Nabil menghapusnya setelah semua fitur pindah |
