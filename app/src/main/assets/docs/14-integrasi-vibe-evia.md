# Panduan Integrasi Supabase — Fitur 4: Kategori Vibe (Evia)

Dokumen ini berisi **apa yang harus dibuat dan diubah** di kode fitur Kategori Vibe supaya terhubung ke Supabase. Bukan SQL, bukan pembuatan tabel.

| Urusan | Dikerjakan oleh |
|---|---|
| Membuat tabel, view, policy, data awal vibe | **Nabil** — lihat [09-skema-database.md](09-skema-database.md) |
| Dependency Gradle, `SupabaseClient`, `ApiError` | **Nabil** — sekali untuk semua fitur |
| Kode fitur Kategori Vibe | **Evia** (dokumen ini) |
| Layar detail cafe | **Ossa** pemilik; kamu hanya mengirim `cafeId` saat kartu diklik |

Kamu tidak perlu menjalankan SQL. Kalau ada kolom yang kurang, ajukan ke Nabil.

---

## 1. Yang harus sudah ada sebelum kamu mulai

| Prasyarat | Cara memastikan |
|---|---|
| `SupabaseClient.kt` dan `ApiError.kt` sudah ada | Bisa login dengan akun demo |
| `AuthUiState.role` terisi `"admin"`/`"user"` | Label peran di profil tampil benar |
| Tabel `vibes`, `cafe_vibes`, view `vibes_with_count` dan `cafes_with_rating` sudah ada | Dikonfirmasi Nabil |
| Tujuh vibe awal sudah terisi | Tab vibe muncul tanpa kamu mengisi data |

Tujuh vibe awal itu **isian awal (shortcut)**, bukan batas. Bila tabel masih kosong, minta Nabil menjalankan seed dulu supaya nama kategori sama di semua layar. Setelah itu kamu bebas menambah, mengubah, dan menghapus vibe lewat aplikasi — jumlah vibe tidak dibatasi.

---

## 2. Peta file

| File | Status | Isi |
|---|---|---|
| `core/model/Vibe.kt` | **ubah** | Tambah `iconKey`; `cafeCount` dari view |
| `features/vibe/VibeRow.kt` | **baru** | Bentuk baris `vibes` / `vibes_with_count` |
| `features/vibe/VibeMapper.kt` | **baru** | `VibeRow` → `Vibe` |
| `features/vibe/VibeRepository.kt` | **baru** | Satu-satunya tempat memanggil Supabase |
| `features/vibe/VibeViewModel.kt` | **ubah** | Hapus pencocokan lewat nama, hapus `cafeIdsByVibe` |
| `features/vibe/VibeScreen.kt` | **ubah** | Muat per halaman pager, loading/empty/error, tombol admin |
| `features/vibe/VibeDetailScreen.kt` | **ubah** | Terima `vibeId`, muat cafe dari relasi, kirim `cafeId` |
| `features/vibe/VibeFormScreen.kt` | **ubah** | Simpan vibe + relasi `cafe_vibes` |

Tiga file layar itu milikmu sendiri, jadi kamu bebas mengubahnya. Yang perlu koordinasi hanya bentuk pemanggilan `onCafeClick`.

---

### 2.1 Skema tabel yang boleh kamu pakai

**`vibes`** — tabel kategori.

| Kolom | Tipe | Catatan |
|---|---|---|
| `id` | uuid | Dibuat database; dipakai untuk relasi dan pager |
| `name` | text | Unik tanpa peduli huruf besar/kecil |
| `icon_key` | text | Kunci ikon **bebas**, default `'work'`. Tujuh kunci seed: `work`, `aesthetic`, `night`, `outdoor`, `vintage`, `rooftop`, `minimal`; kunci lain tetap boleh |
| `description` | text | Boleh kosong |
| `created_by`, `created_at`, `updated_at` | uuid, timestamptz | Diisi database; jangan dikirim |

`icon_key` bukan enum dan tidak unik: satu ikon boleh dipakai beberapa vibe, dan kunci baru tidak perlu persetujuan siapa pun. Tidak ada batas jumlah baris `vibes`.

Tidak ada kolom `is_active` di tabel ini. Vibe tidak diarsipkan, hanya dihapus. Menghapus vibe **ikut menghapus semua baris `cafe_vibes`** miliknya (FK `on delete cascade`); cafe-nya tetap ada, hanya kehilangan label vibe itu. Karena itu aplikasi wajib memperingatkan jumlah cafe sebelum menghapus, dan **tidak** mengharapkan error `23503` di sini.

**`vibes_with_count`** — view; `vibes` ditambah `cafe_count`.

| Kolom | Tipe | Catatan |
|---|---|---|
| kolom `vibes` | — | Sama seperti tabel |
| `cafe_count` | integer | Hanya menghitung cafe yang `is_active` |

Angka jumlah cafe **wajib** diambil dari sini. Menghitung dari daftar cafe yang termuat akan salah begitu daftar dibatasi `limit`.

**`cafe_vibes`** — relasi cafe ↔ vibe.

| Kolom | Tipe | Catatan |
|---|---|---|
| `cafe_id` | uuid | FK ke `cafes` |
| `vibe_id` | uuid | FK ke `vibes` |
| `created_at` | timestamptz | Diisi database |

Pasangan `(cafe_id, vibe_id)` unik. Menyimpan relasi yang sama dua kali ditolak `23505`.

**`cafes_with_rating`** — baca saja, untuk kartu cafe di dalam tab vibe.

| Kolom | Tipe | Catatan |
|---|---|---|
| `id`, `name`, `address` | uuid, text | Isi kartu |
| `photo_path` | text | Path Storage, bukan URL |
| `price_min`, `price_max` | integer | Nullable |
| `rating`, `review_count` | numeric(2,1), integer | `rating` null bila belum ada ulasan |
| `is_active` | boolean | Cafe arsip tidak ditampilkan di tab vibe |

Hak akses:

| Objek | Admin | User biasa |
|---|---|---|
| `vibes` | baca, insert, update, delete | baca |
| `cafe_vibes` | baca, insert, delete | baca |
| `vibes_with_count`, `cafes_with_rating` | baca | baca |

User biasa yang memaksa menulis akan ditolak `42501`. Menghapus cafe ditolak `23503` bila cafe sudah diulas atau disimpan; menghapus vibe tidak ditolak, relasinya ikut terhapus.

---

## 3. Langkah 1 — sesuaikan model `Vibe`

```kotlin
data class Vibe(
    val id: String,
    val name: String,
    val description: String = "",
    val iconKey: String = "",
    val cafeCount: Int = 0,      // dari view, bukan hasil hitung aplikasi
)
```

| Lama | Baru | Kenapa |
|---|---|---|
| — (belum ada) | `iconKey: String` | Ikon tab dari kolom `icon_key`, bukan dari nama vibe; boleh kunci apa pun, yang tidak dikenal memakai ikon cadangan |
| `cafeCount: Int` | tetap, tetapi sumbernya view | Aplikasi tidak menghitung sendiri |
| `name` dipakai untuk mencocokkan cafe | `id` dipakai untuk relasi | Pencocokan lewat nama gagal saat nama vibe diubah |

Poin terakhir adalah kesalahan yang paling mahal di fitur ini. Kode sekarang mencocokkan `vibe.name in cafe.vibes`; itu hanya jalan karena data dummy memakai teks yang sama persis. Di database, hubungan disimpan sebagai `vibe_id`, bukan teks.

---

## 4. Langkah 2 — `VibeRow` dan `VibeMapper`

```kotlin
@Serializable
data class VibeRow(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("icon_key") val iconKey: String = "",
    @SerialName("cafe_count") val cafeCount: Int = 0,
)

data class VibeDraft(
    val name: String,
    val description: String,
    val iconKey: String,
    val cafeIds: List<String>,
)

object VibeMapper {
    fun toModel(row: VibeRow): Vibe
    fun draftToRow(draft: VibeDraft): VibeRow
}
```

Untuk query `vibes` biasa (tanpa view), `cafe_count` tidak ada; biarkan `0` dan jangan dipakai di layar. Layar memakai hasil dari `vibes_with_count`.

---

## 5. Langkah 3 — `VibeRepository`

```kotlin
class VibeRepository(private val client: SupabaseClient = Supabase.client) {

    suspend fun observeVibes(): List<Vibe>                       // dari vibes_with_count

    suspend fun loadVibe(id: String): Vibe?

    suspend fun cafesForVibe(vibeId: String, limit: Int = 50): List<Cafe>

    suspend fun create(draft: VibeDraft): Vibe

    suspend fun update(id: String, draft: VibeDraft): Vibe

    suspend fun delete(id: String)                                // relasi cafe_vibes ikut terhapus

    suspend fun setCafeRelations(vibeId: String, cafeIds: List<String>)

    suspend fun cafesForPicker(limit: Int = 100): List<Cafe>     // untuk form tambah/edit
}
```

Ketentuan:

| Ketentuan | Alasan |
|---|---|
| Daftar vibe dari `vibes_with_count` | `cafe_count` sudah dihitung database |
| Cafe per vibe diambil dari `cafe_vibes` dengan join ke `cafes_with_rating` | Relasi, bukan pencocokan nama |
| Filter `c.is_active` | Cafe arsip tidak muncul di tab vibe |
| `setCafeRelations` = hapus yang dilepas, insert yang baru | Cara paling sederhana yang tetap konsisten |
| Insert relasi memakai `onConflict = "cafe_id,vibe_id"` | Mencegah `23505` saat menyimpan ulang |
| `delete` menghapus baris `vibes`; relasi `cafe_vibes` ikut terhapus | Tidak ada error `23503` di sini; peringatan harus datang dari aplikasi |
| `cafesForPicker` membaca `cafes_with_rating` | Form butuh daftar cafe untuk dipilih |

Contoh bentuk query relasi: `from("cafe_vibes").select(Columns.raw("cafes!inner(id, name, address, photo_path, price_min, price_max, rating, review_count, is_active)"))` dengan filter `vibe_id` dan `cafes.is_active = true`. Nama tabel dan kolom diambil dari bagian 2.1; jangan mengarang kolom baru.

---

## 6. Langkah 4 — state

State sekarang menyimpan `allCafes` dan `cafeIdsByVibe` — dua salinan data yang harus dijaga tetap sinkron. Setelah integrasi, simpan hasil per vibe saja.

```kotlin
data class VibeUiState(
    val vibes: List<Vibe> = emptyList(),
    val selectedIndex: Int = 0,
    val cafesByVibe: Map<String, List<Cafe>> = emptyMap(),  // hanya yang sudah dimuat
    val loadingVibeIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAdmin: Boolean = false,
)

data class VibeDetailUiState(
    val vibe: Vibe? = null,
    val cafes: List<Cafe> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val notFound: Boolean = false,
)

data class VibeFormUiState(
    val name: String = "",
    val description: String = "",
    val iconKey: String = "",
    val selectedCafeIds: Set<String> = emptySet(),
    val availableCafes: List<Cafe> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val validationError: String? = null,
)
```

`cafesByVibe` hanya berisi tab yang sudah dibuka. Jangan memuat ketujuh vibe sekaligus saat layar dibuka; muat saat halaman pager aktif.

---

## 7. Langkah 5 — ViewModel

```kotlin
class VibeViewModel(private val repo: VibeRepository = VibeRepository()) : ViewModel() {
    val uiState: StateFlow<VibeUiState>

    fun load()
    fun onVibeSelected(index: Int)
    fun loadCafesFor(vibeId: String)      // dipanggil saat halaman pager aktif
    fun onRetry()
    fun onDeleteVibe(id: String)          // admin
}

class VibeDetailViewModel(private val repo: VibeRepository = VibeRepository()) : ViewModel() {
    val uiState: StateFlow<VibeDetailUiState>

    fun load(vibeId: String)
    fun onRetry()
}

class VibeFormViewModel(private val repo: VibeRepository = VibeRepository()) : ViewModel() {
    val formState: StateFlow<VibeFormUiState>

    fun loadForEdit(vibeId: String?)      // null = tambah
    fun onNameChange(text: String)
    fun onDescriptionChange(text: String)
    fun onIconChange(key: String)
    fun onToggleCafe(cafeId: String)
    fun onSave()
}
```

Ketentuan:

- `loadCafesFor` dipanggil dari `LaunchedEffect(pagerState.currentPage)` atau padanan; jangan di dalam `items(...)`.
- Vibe yang sudah ada di `cafesByVibe` tidak dimuat ulang, kecuali setelah tambah/edit/hapus.
- Setelah menyimpan vibe, muat ulang daftar vibe supaya `cafe_count` ikut berubah.
- `onDeleteVibe`: dialog konfirmasi wajib menyebut jumlah cafe yang akan kehilangan label (`cafe_count`), karena relasinya ikut terhapus. Jangan menghapus tanpa angka itu.

---

## 8. Langkah 6 — layar

### 8.1 `VibeScreen`

| Yang ditambahkan/diubah | Perilaku benar |
|---|---|
| Indikator muat | Muncul saat daftar vibe dimuat; pager tidak dirender dengan 0 halaman |
| `pageCount` | Dari `vibes.size`; jaga agar tidak negatif saat kosong |
| Empty state | Muncul saat tidak ada vibe sama sekali, dengan tombol **Tambah vibe** untuk admin |
| Error + coba lagi | Pesan dari `errorMessage` |
| Isi tab | `LazyColumn` cafe untuk vibe itu; `key = { it.id }` |
| Memuat per tab | Tab yang dibuka memuat cafe-nya sendiri, tab lain belum |
| Judul tab | `name` dari database, `cafe_count` dari view |
| Ikon tab | Dari `iconKey`, dengan ikon cadangan bila kuncinya tidak dikenal |
| FAB Tambah Vibe | Hanya untuk admin |
| Menu edit/hapus | Hanya untuk admin, di dalam tab yang aktif |

### 8.2 `VibeDetailScreen`

| Yang diubah | Perilaku benar |
|---|---|
| Parameter | `vibeId: String`, bukan objek `Vibe?` |
| Sumber data | `VibeDetailViewModel.load(vibeId)` |
| Daftar cafe | Dari relasi `cafe_vibes`, bukan `cafesFor(vibe.id)` pada state induk |
| Klik kartu | `onCafeClick(cafe.id)` — **`String`**, bukan `Cafe` |
| Vibe tidak ditemukan | `notFound` → pesan, bukan layar kosong |
| Rating null | "Belum ada ulasan", bukan 0 |

Perubahan `onCafeClick` dari `(Cafe) -> Unit` menjadi `(String) -> Unit` wajib, karena layar detail cafe memuat datanya sendiri berdasarkan `cafeId`. Kalau masih mengirim objek, dua sumber data akan berbeda dan detail bisa menampilkan data basi.

### 8.3 `VibeFormScreen`

| Bagian | Isi |
|---|---|
| Field | Nama, deskripsi, ikon |
| Pilihan ikon | Tujuh tombol cepat (`work`, `aesthetic`, `night`, `outdoor`, `vintage`, `rooftop`, `minimal`) **plus** kolom teks untuk kunci lain. Kosong berarti `work`. Jangan ditutup hanya untuk tujuh kunci itu |
| Ikon tidak dikenal | `when (iconKey)` dengan cabang `else` → ikon cadangan; jangan crash atau tampil kotak kosong |
| Pemilih cafe | Daftar cafe dengan centang; memakai `Set<cafeId>` di state |
| Validasi | Nama wajib; nama unik (tangkapan `23505` → "Nama vibe sudah dipakai"); `icon_key` dirapikan `trim().lowercase()`, maksimal 24 karakter |
| Simpan | Simpan vibe dulu, lalu `setCafeRelations` |
| Mode | Satu layar untuk tambah dan edit |
| Hapus | Hanya di mode edit; dialog menyebut `cafe_count` karena relasi ikut terhapus |

---

## 9. Alur menyimpan vibe dan relasinya

| Langkah | Aksi | Bila gagal |
|---|---|---|
| 1 | Insert/update baris `vibes` | Tampilkan error, tetap di form |
| 2 | Hapus relasi `cafe_vibes` yang tidak dicentang lagi | Tampilkan error; data vibe sudah tersimpan |
| 3 | Insert relasi baru dengan konflik diabaikan | Sama; centang yang gagal bisa diulang |
| 4 | Muat ulang daftar vibe dan isi tab aktif | — |

Relasi ditulis setelah vibe punya `id`. Untuk data baru, `id` diambil dari hasil insert, bukan dibuat di aplikasi.

---

## 10. Daftar periksa sebelum push

- [ ] `Vibe` memakai `iconKey`; `cafeCount` dari view; kunci ikon tak dikenal memakai ikon cadangan, bukan crash.
- [ ] Tidak ada `DummyData` di file fitur Vibe.
- [ ] Tidak ada pemanggilan Supabase di luar `VibeRepository`.
- [ ] Pencocokan cafe ke vibe memakai `vibe_id`, bukan nama vibe.
- [ ] `cafeIdsByVibe` dan `allCafes` dihapus dari state.
- [ ] `cafe_count` di tab berasal dari `vibes_with_count`.
- [ ] Cafe arsip tidak muncul di tab vibe.
- [ ] `VibeDetailScreen` menerima `vibeId` dan memuat sendiri datanya.
- [ ] `onCafeClick` mengirim `String` (cafeId), bukan objek `Cafe`.
- [ ] Tombol tambah/edit/hapus hanya muncul untuk admin.
- [ ] Hapus vibe memperingatkan jumlah cafe; relasi `cafe_vibes` ikut terhapus tanpa error.
- [ ] Simpan relasi dua kali tidak menghasilkan `23505`.
- [ ] `LazyColumn` memakai `key = { it.id }`; pager memakai `key` per vibe.
- [ ] Loading, empty, dan error+retry diuji dengan Wi-Fi mati.
- [ ] `pageCount` tidak membuat crash saat daftar vibe kosong.

---

## 11. Bukti demo (5 langkah, ±30 detik)

| No | Yang dilakukan | Berhasil bila |
|---|---|---|
| 1 | Login admin, buka Kategori Vibe | Tujuh tab dengan jumlah cafe masing-masing |
| 2 | Tambah vibe baru, tulis kunci ikon baru (mis. `boardgame`), centang dua cafe | Tab baru muncul dengan jumlah 2 dan ikon cadangan (tidak error) |
| 3 | Buka tab itu, klik satu kartu | Masuk detail cafe yang benar |
| 4 | Edit vibe, lepas satu cafe | Jumlah turun jadi 1; cafe hilang dari tab |
| 5 | Hapus vibe yang masih punya cafe | Dialog menyebut jumlah cafe; cafe tetap ada, hanya label vibe itu yang hilang |

---

## 12. Kesalahan yang sering terjadi

| Gejala | Penyebab | Perbaikan |
|---|---|---|
| Tab vibe kosong padahal relasi ada | Masih mencocokkan nama vibe | Pakai `vibe_id` lewat `cafe_vibes` |
| Jumlah cafe tidak cocok isi tab | Angka dihitung dari list terbatas `limit` | Ambil dari `vibes_with_count` |
| `duplicate key ... cafe_vibes_pkey` | Insert relasi tanpa menangani konflik | Pakai `onConflict` atau hapus dulu |
| Vibe baru tidak bisa disimpan | Kunci ikon dibatasi daftar tertutup | Biarkan kolom teks bebas + ikon cadangan |
| Cafe kehilangan label tanpa disadari | Vibe dihapus tanpa peringatan (relasi ikut terhapus) | Tampilkan `cafe_count` di dialog konfirmasi |
| Detail cafe menampilkan data lama | Mengirim objek `Cafe` dari tab | Kirim `cafeId` saja |
| Crash saat daftar vibe kosong | `pageCount` negatif atau `lastIndex` dipakai langsung | Lindungi dengan `coerceAtLeast(0)` |
| Cafe arsip tetap muncul | Filter `is_active` tidak dipakai | Tambahkan pada query relasi |

---

## 13. Yang tidak perlu dikerjakan

- Pagination; cukup `limit(50)` per tab.
- Realtime subscription; muat ulang setelah simpan.
- Room / cache offline.
- Dependency injection framework.
- Pengurutan vibe manual (drag and drop) — tidak diminta ketentuan tugas.
- Unggah berkas ikon sendiri; cukup kunci teks + ikon cadangan bawaan.

---

## 14. Batas dengan anggota lain

| Hal | Aturan |
|---|---|
| `DirectoryDetailScreen.kt` | milik Ossa; kamu hanya mengirim `cafeId` |
| `Cafe`, `CafeCard` | milik Ossa; jangan diubah dari fitur Vibe |
| Tabel `cafes` | baca saja; jangan menulis dari fitur Vibe |
| `cafe_vibes` | milikmu untuk ditulis, tetapi bentuk kolomnya jangan diubah |
| Data awal vibe | milik Nabil; jangan menambah lewat SQL |
| `ReviewSection`, `BookmarkAction` | milik Shelfina dan Shananda; jangan dipasang di layar Vibe |
