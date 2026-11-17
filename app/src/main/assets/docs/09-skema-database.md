# Skema Database Supabase NgopiMalang

Dokumen ini adalah rancangan final database untuk aplikasi NgopiMalang pada Progress 2, sudah disesuaikan dengan keputusan kelompok sampai saat ini:

- Ditambah dua view untuk rating dan jumlah cafe per vibe, supaya tidak ada angka yang disimpan ganda.
- Hak akses memakai satu model: **admin satu-satunya penulis katalog**, **user memiliki data pribadinya sendiri** (ulasan dan bookmark).
- Sisi kode mengikuti [10-panduan-integrasi.md](10-panduan-integrasi.md): repository per fitur, satu `UiState`, detail cafe satu layar.

Dokumen ini tidak mengubah kode. Semua SQL di sini ditempel manual di Supabase Dashboard → SQL Editor oleh satu orang (ketua tim), lalu hasilnya diverifikasi anggota lain.

---

## 1. Ringkasan keputusan

| No | Keputusan | Alasan |
|---|---|---|
| 1 | Nama tabel dan kolom mengikuti PDF dosen: `event_promos`, `starts_at`, `ends_at`, `price_min`, `price_max`, `has_wifi`, `plug_count` | Tidak perlu berdebat nama; PDF adalah acuan penilaian |
| 2 | Role disimpan di tabel terpisah `user_roles`, **bukan** kolom di `profiles` | User tidak boleh mengubah rolenya sendiri; tabel tanpa policy tulis membuat itu mustahil |
| 3 | Rating **tidak** disimpan di `cafes`; dihitung dari `reviews` lewat view `cafes_with_rating` | Angka simpanan pasti pernah tidak sinkron dengan ulasan |
| 4 | Jumlah cafe per vibe dihitung lewat view `vibes_with_count` | Sama seperti rating: turunan, bukan data |
| 5 | Kepemilikan data katalog dicatat di `created_by`, tetapi **bukan** penentu keamanan | Penentu keamanan adalah role admin, bukan siapa yang menginput |
| 6 | Ulasan dan bookmark unik per `(user_id, cafe_id)` | Satu user satu ulasan per cafe; ulasan baru = upsert, bukan baris kedua |
| 7 | Semua foreign key memakai `ON DELETE RESTRICT` | Riwayat ulasan dan bookmark tidak boleh ikut terhapus |
| 8 | Hapus cafe dari UI = arsip `is_active = false` | Karena `RESTRICT`, hapus permanen akan gagal dan membingungkan pengguna |
| 9 | Login wajib untuk seluruh operasi (`to authenticated`) | Aplikasi tidak punya mode tamu; RLS tidak perlu aturan anonim |
| 10 | Bucket Storage: `cafe-photos` (5 MB) dan `avatars` (2 MB, disiapkan) | Foto cafe masuk ketentuan "minimal satu fitur upload gambar" |
| 11 | Trigram (`pg_trgm`) tidak dipakai dulu | Data awal ±12 baris; pencarian memakai `ilike` sudah cukup |
| 12 | Service role key **tidak** disimpan di repo; aplikasi hanya memakai anon key | Anon key aman karena keamanan dijaga RLS |

---

## 2. Peran dan akun

Dua peran, satu aplikasi.

| Peran | Cara mendapat | Yang bisa dilakukan |
|---|---|---|
| `user` | Register dari aplikasi | Melihat seluruh katalog; menulis ulasan dan bookmark miliknya; mengubah profil sendiri |
| `admin` | Dibuat manual lewat Dashboard (bagian 13) | Semua hak `user`, ditambah menulis katalog: cafe, vibe, relasi vibe, event/promo, foto |

Yang **tidak** bisa dilakukan admin (dan ini disengaja, supaya adil saat presentasi):

- Mengubah atau menghapus ulasan milik orang lain.
- Membaca bookmark milik orang lain.
- Mengubah role dirinya sendiri atau orang lain dari dalam aplikasi.

Akun demo yang disarankan:

| Keperluan | Akun | Dipakai untuk demo fitur |
|---|---|---|
| Admin | satu akun admin kelompok | Fitur 1 (Direktori), Fitur 4 (Vibe), Fitur 5 (Event & Promo) |
| User | satu akun user biasa | Fitur 2 (Ulasan), Fitur 3 (Bookmark), dan pembuktian penolakan akses |

Kata sandi akun tidak ditulis di repo. Simpan di catatan grup.

---

## 3. Daftar objek database

| Objek | Jenis | Fungsi |
|---|---|---|
| `profiles` | tabel | Nama tampilan, foto profil; satu baris per pengguna |
| `user_roles` | tabel | Peran `user`/`admin`; hanya bisa dibaca pemiliknya |
| `cafes` | tabel | Data cafe; sumber layar Direktori dan detail cafe |
| `vibes` | tabel | Kategori vibe; sumber tab pada fitur Vibe |
| `cafe_vibes` | tabel | Relasi banyak-ke-banyak cafe ↔ vibe |
| `event_promos` | tabel | Event dan promo, terhubung ke cafe |
| `reviews` | tabel | Ulasan dan rating per user per cafe |
| `bookmarks` | tabel | Cafe simpanan per user, dengan catatan pribadi |
| `cafes_with_rating` | view | `cafes` + `rating` + `review_count` |
| `vibes_with_count` | view | `vibes` + `cafe_count` |
| `public.is_admin()` | fungsi | Pemeriksa role untuk policy; `security definer` |
| `public.set_updated_at()` | fungsi + trigger | Mengisi `updated_at` otomatis |
| `public.handle_new_user()` | fungsi + trigger | Membuat baris `profiles` dan `user_roles` saat register |
| `cafe-photos` | bucket | Foto cafe, batas 5 MB |
| `avatars` | bucket | Foto profil, batas 2 MB, belum dipakai UI |

---

## 4. Matriks hak akses per fitur

| Fitur (pemilik) | Tabel yang dibaca | Tabel yang ditulis | Admin | User biasa |
|---|---|---|---|---|
| 1. Direktori Cafe (Ossa) | `cafes_with_rating`, `cafe_vibes`, `vibes` | `cafes`, `cafe_vibes`, `cafes.photo_path` via Storage | CRUD penuh | baca saja |
| 2. Ulasan & Rating (Shelfina) | `reviews`, `profiles`, `cafes_with_rating` | `reviews` | baca semua, tanpa hak tulis/hapus ulasan orang lain | CRUD ulasan miliknya |
| 3. Bookmark (Shananda) | `bookmarks`, `cafes_with_rating` | `bookmarks` | hanya miliknya sendiri | CRUD bookmark miliknya |
| 4. Kategori Vibe (Evia) | `vibes_with_count`, `cafe_vibes`, `cafes_with_rating` | `vibes`, `cafe_vibes` | CRUD penuh | baca saja |
| 5. Event & Promo (Nabil) | `event_promos`, `cafes` | `event_promos` | CRUD penuh | baca saja |
| Fondasi: Login & Profil | `profiles`, `user_roles` | `profiles` (miliknya) | sama | sama |

Catatan penting yang sering ditanyakan saat demo:

- Menekan tombol **bookmark** atau **beri rating** di layar detail cafe **bukan** menulis ke tabel `cafes`. Itu INSERT ke `bookmarks` atau upsert ke `reviews`. Karena itu user tetap "read-only" pada katalog.
- Admin yang membuka layar Ulasan hanya bisa membaca. Demo CRUD Fitur 2 memakai akun user.
- Bookmark sepenuhnya privat; admin tidak punya policy apa pun untuk membacanya.

---

## 5. Penyesuaian model Kotlin

Skema ini memaksa beberapa field model berubah. Perubahan ini **harus disepakati kelompok lebih dulu**, karena menyentuh layar semua orang.

### 5.1 `Cafe`

| Sekarang | Menjadi | Alasan |
|---|---|---|
| `priceRange: String` (mis. `"Rp15.000 – Rp30.000"`) | `priceMin: Int?`, `priceMax: Int?` | Rentang harga tersimpan sebagai angka; format rupiah dibuat di UI |
| `rating: Double` | `rating: Double?` | Bisa `null` bila belum ada ulasan; angka dari view |
| `vibes: List<String>` | `vibeNames: List<String>` (dari join) atau `vibeIds: List<String>` | Vibe adalah relasi, bukan teks bebas |
| `imageResId: Int?` | `photoPath: String?` | Foto dari Storage, bukan dari `res/drawable` |
| — (belum ada) | `isActive: Boolean` | Menampilkan badge arsip |
| — (belum ada) | `reviewCount: Int` | Ditampilkan di detail: "dari 8 ulasan" |
| — (belum ada) | `description: String`, `openHours: String` | Diisi dari kolom `description` dan `open_hours`; form edit membutuhkannya |

### 5.2 `Review`

| Sekarang | Menjadi | Alasan |
|---|---|---|
| `userName: String` | tetap, diisi dari `profiles.display_name` lewat join | Tidak diduplikasi di tabel ulasan |
| `cafeName: String` | tetap, diisi dari `cafes.name` lewat join | Sama |
| `dateLabel: String` | `createdAt: Instant`, label dibuat di UI | Label tanggal tidak boleh jadi data |
| `favoriteMenu: String` | tetap | Sesuai PDF |

### 5.3 `Vibe`

| Sekarang | Menjadi | Alasan |
|---|---|---|
| `cafeCount: Int` | tetap, diisi dari `vibes_with_count` | Turunan, bukan kolom |
| — (belum ada) | `iconKey: String` | Ikon dipilih dari daftar tetap |

### 5.4 `EventPromo`

| Sekarang | Menjadi | Alasan |
|---|---|---|
| `schedule: String` | `startsAt: Instant`, `endsAt: Instant` | Badge status dihitung dari waktu, bukan teks |
| `cafeName: String` | tetap, dari join + `cafeId: String?` | Bisa dibuka ke detail cafe |
| — (belum ada) | `priceMin: Int?`, `priceMax: Int?` | Sesuai PDF |
| — (belum ada) | `isPublished: Boolean` | Event bisa disembunyikan tanpa dihapus |

### 5.5 `BookmarkItem`

| Sekarang | Menjadi | Alasan |
|---|---|---|
| `cafeName: String` | tetap (dari join) atau ganti `cafe: Cafe` | Menampilkan kartu cafe tersimpan |
| `savedAtLabel: String` | `createdAt: Instant` | Sama seperti ulasan |

### 5.6 Dampak ke layar

| Layar | Yang berubah | Pemilik |
|---|---|---|
| `DirectoryScreen`, `CafeCard`, `DirectoryDetailScreen` | `priceRange` → `priceMin`/`priceMax`, `imageResId` → `photoPath`, `rating` boleh kosong | Ossa |
| `ReviewsScreen`, `ReviewCard` | tanggal dari `createdAt`; nama dari join | Shelfina |
| `VibeScreen`, `VibeDetailScreen` | `cafeCount` dari view; `iconKey` | Evia |
| `EventPromoScreen` | `schedule` → dua waktu; badge status; harga | Nabil |
| `BookmarkScreen` | tanggal dari `createdAt`; kartu memakai `Cafe` | Shananda |
| `HomeScreen` | memakai model yang sama; tidak lagi membaca `DummyData` | bersama |
---

## 6. Spesifikasi tabel

Ringkasan kolom penting. DDL lengkap ada di bagian 7, jadi tidak ada dua sumber yang bisa berbeda.

| Tabel | Kolom kunci | Aturan kunci |
|---|---|---|
| `profiles` | `id` (PK, = `auth.users.id`), `display_name`, `avatar_path`, `created_at`, `updated_at` | Tidak menyimpan email; email tetap di `auth.users` |
| `user_roles` | `user_id` (PK, FK `profiles`), `role`, `granted_at` | `role in ('user','admin')`; tanpa policy tulis |
| `cafes` | `id`, `name`, `address`, `description`, `open_hours`, `price_min`, `price_max`, `has_wifi`, `plug_count`, `photo_path`, `is_active`, `created_by`, `created_at`, `updated_at` | `price_min <= price_max`; nama unik tanpa peduli huruf besar/kecil |
| `vibes` | `id`, `name`, `icon_key`, `description`, `created_by`, `created_at`, `updated_at` | `lower(btrim(name))` unik; `icon_key` teks bebas, **tidak** unik (satu ikon boleh dipakai beberapa vibe) |
| `cafe_vibes` | `cafe_id`, `vibe_id`, `created_at` | PK gabungan; kedua FK `ON DELETE CASCADE` (relasi, bukan data utama) |
| `event_promos` | `id`, `cafe_id`, `title`, `description`, `category`, `starts_at`, `ends_at`, `price_min`, `price_max`, `is_published`, `created_by`, `created_at`, `updated_at` | `category in ('event','promo')`; `ends_at > starts_at`; `cafe_id` `ON DELETE RESTRICT` |
| `reviews` | `id`, `user_id`, `cafe_id`, `rating`, `comment`, `favorite_menu`, `created_at`, `updated_at` | `rating between 1 and 5`; unik `(user_id, cafe_id)`; kedua FK `RESTRICT` |
| `bookmarks` | `id`, `user_id`, `cafe_id`, `note`, `created_at`, `updated_at` | unik `(user_id, cafe_id)`; kedua FK `RESTRICT` |

Index yang dibuat (selain PK dan unique):

| Index | Untuk query |
|---|---|
| `cafes_is_active_idx` pada `(is_active)` | Daftar cafe aktif |
| `cafes_name_idx` pada `lower(name)` | Pencarian nama |
| `reviews_cafe_id_idx` pada `(cafe_id)` | Ulasan per cafe |
| `bookmarks_user_id_idx` pada `(user_id)` | Daftar simpanan |
| `event_promos_ends_at_idx` pada `(ends_at)` | Filter event yang masih berlaku |
| `cafe_vibes_vibe_id_idx` pada `(vibe_id)` | Cafe per vibe |

---

### 7.1 Profil otomatis saat register

Tanpa trigger ini, setiap pengguna baru harus membuat baris `profiles` sendiri, dan aplikasi akan gagal menampilkan nama.

```sql
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, display_name)
  values (
    new.id,
    coalesce(nullif(new.raw_user_meta_data ->> 'display_name', ''), split_part(new.email, '@', 1))
  )
  on conflict (id) do nothing;

  insert into public.user_roles (user_id, role)
  values (new.id, 'user')
  on conflict (user_id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();
```

Trigger ini hanya jalan untuk pengguna baru. Akun yang sudah terdaftar sebelum SQL ini dijalankan perlu diisi manual sekali (lihat bagian 13).

---

## 8. SQL: dua view

```sql
-- Rating dan jumlah ulasan per cafe
create or replace view public.cafes_with_rating
with (security_invoker = true) as
select
  c.*,
  round(avg(r.rating)::numeric, 1)::numeric(2,1) as rating,
  count(r.id)::integer                             as review_count
from public.cafes c
left join public.reviews r on r.cafe_id = c.id
group by c.id;

-- Jumlah cafe aktif per vibe
create or replace view public.vibes_with_count
with (security_invoker = true) as
select
  v.*,
  count(c.id)::integer as cafe_count
from public.vibes v
left join public.cafe_vibes cv on cv.vibe_id = v.id
left join public.cafes c on c.id = cv.cafe_id and c.is_active
group by v.id;
```

Kenapa `security_invoker = true`:

- View tetap menghormati RLS tabel di bawahnya. Tanpa opsi ini, view berjalan sebagai pemiliknya dan bisa membocorkan baris yang seharusnya tidak terlihat.
- Semua fitur membaca angka yang sama. Tidak ada lagi perhitungan rating di ViewModel masing-masing.

Yang perlu diingat saat memakai view:

- `select *` pada `cafes_with_rating` mengembalikan seluruh kolom `cafes` **plus** `rating` dan `review_count`. Untuk hemat data, sebut kolomnya.
- `rating` bernilai `null` bila belum ada ulasan. UI wajib menampilkan "belum ada ulasan", bukan `0.0`.
- Kolom view tidak bisa ditulis. INSERT/UPDATE tetap ke tabel aslinya.
---

## 9. SQL: pemeriksa role dan RLS

### 9.1 Fungsi `is_admin()`

```sql
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.user_roles
    where user_id = auth.uid() and role = 'admin'
  );
$$;

revoke all on function public.is_admin() from anon;
grant execute on function public.is_admin() to authenticated;
```

`security definer` diperlukan karena policy pada tabel lain tidak boleh bergantung pada policy tabel `user_roles` milik pengguna itu sendiri.

### 9.2 Aktifkan RLS

```sql
alter table public.profiles    enable row level security;
alter table public.user_roles  enable row level security;
alter table public.cafes       enable row level security;
alter table public.vibes       enable row level security;
alter table public.cafe_vibes  enable row level security;
alter table public.event_promos enable row level security;
alter table public.reviews     enable row level security;
alter table public.bookmarks   enable row level security;
```

Hak akses tabel (GRANT) di Supabase sudah diberikan otomatis kepada role `authenticated`. Blok berikut aman dijalankan ulang dan berguna bila project dibuat manual atau role-nya terhapus — tanpa GRANT, policy tidak pernah dievaluasi karena akses tabelnya sendiri sudah ditolak lebih dulu.

```sql
grant usage on schema public to authenticated;
grant select, insert, update, delete on all tables in schema public to authenticated;
grant execute on all functions in schema public to authenticated;
```

### 9.3 Policy per operasi

Satu tabel satu blok. Policy dipisah baca dan tulis, bukan satu `for all`, supaya hak baca tidak ikut terbuka saat menambah hak tulis.

```sql
-- ================= profiles =================
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles
  for select to authenticated using (true);

drop policy if exists profiles_insert_self on public.profiles;
create policy profiles_insert_self on public.profiles
  for insert to authenticated with check (id = auth.uid());

drop policy if exists profiles_update_self on public.profiles;
create policy profiles_update_self on public.profiles
  for update to authenticated
  using (id = auth.uid()) with check (id = auth.uid());

-- ================= user_roles =================
-- hanya baca milik sendiri; tidak ada policy tulis sama sekali
drop policy if exists user_roles_select_self on public.user_roles;
create policy user_roles_select_self on public.user_roles
  for select to authenticated using (user_id = auth.uid());

-- ================= cafes =================
-- user melihat cafe aktif; admin melihat semua termasuk arsip
drop policy if exists cafes_select on public.cafes;
create policy cafes_select on public.cafes
  for select to authenticated using (is_active or public.is_admin());

drop policy if exists cafes_insert_admin on public.cafes;
create policy cafes_insert_admin on public.cafes
  for insert to authenticated with check (public.is_admin());

drop policy if exists cafes_update_admin on public.cafes;
create policy cafes_update_admin on public.cafes
  for update to authenticated
  using (public.is_admin()) with check (public.is_admin());

drop policy if exists cafes_delete_admin on public.cafes;
create policy cafes_delete_admin on public.cafes
  for delete to authenticated using (public.is_admin());

-- ================= vibes =================
drop policy if exists vibes_select on public.vibes;
create policy vibes_select on public.vibes
  for select to authenticated using (true);

drop policy if exists vibes_write_admin on public.vibes;
create policy vibes_write_admin on public.vibes
  for insert to authenticated with check (public.is_admin());

drop policy if exists vibes_update_admin on public.vibes;
create policy vibes_update_admin on public.vibes
  for update to authenticated
  using (public.is_admin()) with check (public.is_admin());

drop policy if exists vibes_delete_admin on public.vibes;
create policy vibes_delete_admin on public.vibes
  for delete to authenticated using (public.is_admin());

-- ================= cafe_vibes =================
drop policy if exists cafe_vibes_select on public.cafe_vibes;
create policy cafe_vibes_select on public.cafe_vibes
  for select to authenticated using (true);

drop policy if exists cafe_vibes_insert_admin on public.cafe_vibes;
create policy cafe_vibes_insert_admin on public.cafe_vibes
  for insert to authenticated with check (public.is_admin());

drop policy if exists cafe_vibes_delete_admin on public.cafe_vibes;
create policy cafe_vibes_delete_admin on public.cafe_vibes
  for delete to authenticated using (public.is_admin());

-- ================= event_promos =================
drop policy if exists event_promos_select on public.event_promos;
create policy event_promos_select on public.event_promos
  for select to authenticated using (is_published or public.is_admin());

drop policy if exists event_promos_insert_admin on public.event_promos;
create policy event_promos_insert_admin on public.event_promos
  for insert to authenticated with check (public.is_admin());

drop policy if exists event_promos_update_admin on public.event_promos;
create policy event_promos_update_admin on public.event_promos
  for update to authenticated
  using (public.is_admin()) with check (public.is_admin());

drop policy if exists event_promos_delete_admin on public.event_promos;
create policy event_promos_delete_admin on public.event_promos
  for delete to authenticated using (public.is_admin());

-- ================= reviews =================
-- semua yang login boleh membaca ulasan; menulis hanya ulasannya sendiri
drop policy if exists reviews_select on public.reviews;
create policy reviews_select on public.reviews
  for select to authenticated using (true);

drop policy if exists reviews_insert_self on public.reviews;
create policy reviews_insert_self on public.reviews
  for insert to authenticated with check (user_id = auth.uid());

drop policy if exists reviews_update_self on public.reviews;
create policy reviews_update_self on public.reviews
  for update to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists reviews_delete_self on public.reviews;
create policy reviews_delete_self on public.reviews
  for delete to authenticated using (user_id = auth.uid());

-- ================= bookmarks =================
-- tidak ada satu pun policy yang membuka bookmark orang lain, termasuk untuk admin
drop policy if exists bookmarks_select_self on public.bookmarks;
create policy bookmarks_select_self on public.bookmarks
  for select to authenticated using (user_id = auth.uid());

drop policy if exists bookmarks_insert_self on public.bookmarks;
create policy bookmarks_insert_self on public.bookmarks
  for insert to authenticated with check (user_id = auth.uid());

drop policy if exists bookmarks_update_self on public.bookmarks;
create policy bookmarks_update_self on public.bookmarks
  for update to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists bookmarks_delete_self on public.bookmarks;
create policy bookmarks_delete_self on public.bookmarks
  for delete to authenticated using (user_id = auth.uid());
```

---

## 10. Storage

| Bucket | Publik? | Batas | Jenis | Jalur |
|---|---|---|---|---|
| `cafe-photos` | ya (baca) | 5 MB | JPEG, PNG, WebP | `{cafe_id}/{uuid}.jpg` |
| `avatars` | ya (baca) | 2 MB | JPEG, PNG | `{user_id}/avatar.jpg` |


### 10.1 Alur ganti foto cafe

Storage dan tabel tidak punya transaksi bersama, jadi urutannya harus jelas dan bisa diulang:

| Langkah | Aksi | Bila gagal |
|---|---|---|
| 1 | Unggah file baru ke `cafe-photos/{cafe_id}/{uuid}.jpg` | Batalkan, tampilkan pesan; `photo_path` lama tetap dipakai |
| 2 | Simpan `photo_path` baru ke tabel `cafes` | Hapus file yang baru diunggah, lalu tampilkan pesan |
| 3 | Hapus file lama di Storage | Boleh diabaikan bila gagal; file yatim tidak merusak data |
| 4 | Muat ulang `cafes_with_rating` | Data tetap benar karena langkah 2 sudah tersimpan |
| 5 | Bila langkah 2 gagal dua kali, tawarkan "pakai foto lama" | Pengguna tidak terjebak di form |

---


### 11.1 Detail cafe dipakai bersama

Skema ini sengaja hanya membutuhkan satu kunci untuk membuka detail: `cafe_id`. Karena itu layar detail cukup ada **satu**, yaitu route `directory/cafe/{cafeId}` milik Ossa, dan fitur lain menumpang ke sana:

| Pemanggil | Yang dikirim | Yang dibaca |
|---|---|---|
| Direktori (Ossa) | `cafeId` | `cafes_with_rating` |
| Vibe (Evia) | `cafeId` dari `cafe_vibes` | `cafes_with_rating` |
| Bookmark (Shananda) | `cafeId` dari `bookmarks` | `cafes_with_rating` |
| Event & Promo (Nabil) | `cafeId` dari `event_promos` | `cafes_with_rating` |

Konsekuensi yang perlu disepakati:

- Detail memuat datanya sendiri lewat `loadCafe(id)`, bukan mengambil dari daftar yang sudah dimuat layar pemanggil. Ini yang membuat detail tetap benar ketika dibuka dari Vibe atau Bookmark, yang tidak memuat daftar Direktori.
- Bagian ulasan dan bookmark di dalam detail dipasang sebagai **slot**: `ReviewSection(cafeId)` milik Shelfina dan `BookmarkAction(cafeId)` milik Shananda. Masing-masing punya ViewModel sendiri dan hanya menerima `cafeId`, sehingga file `DirectoryDetailScreen.kt` tetap milik Ossa dan tidak diedit anggota lain.
- Tabel database tidak perlu dipisah per fitur. Yang dipisah adalah **policy** (bagian 9) dan **repository** di sisi kode, bukan tabelnya.

Aturan query:

- Selalu `limit` atau `range` untuk daftar; jangan `select()` tanpa batas.
- Jangan menulis nama tabel milik fitur lain di repository sendiri.
- Kolom view dipakai untuk baca; tulis selalu ke tabel aslinya.
---

## 12. Arsip, hapus, dan pesan kesalahan

### 12.1 Kenapa arsip

Karena `reviews.cafe_id` dan `bookmarks.cafe_id` memakai `ON DELETE RESTRICT`, menghapus cafe yang sudah pernah diulas akan ditolak database (kode `23503`). Itu bukan bug, itu perlindungan riwayat.

| Aksi pengguna | Yang dijalankan aplikasi | Yang dilihat pengguna |
|---|---|---|
| Hapus cafe yang belum punya ulasan/bookmark/event | `delete` ke `cafes` | Konfirmasi, data hilang dari daftar |
| Hapus cafe yang sudah dipakai | `update cafes set is_active = false` | Badge "Tidak aktif", cafe hilang dari daftar user, tetap terlihat admin |
| Hapus vibe yang masih dipakai cafe | `delete` ditolak `23503` | Pesan: lepaskan cafe dari vibe ini dulu |

Cafe yang diarsipkan tetap muncul di query admin (`is_active or is_admin()`), jadi bisa diaktifkan kembali tanpa mengisi ulang data.

### 12.2 Peta kode kesalahan

Kode dari PostgREST/Postgres diterjemahkan sekali di `core/data/ApiError.kt`, bukan di setiap layar.

| Kode | Arti | Pesan ke pengguna | Tindakan aplikasi |
|---|---|---|---|
| `42501` | Ditolak RLS | "Kamu tidak punya hak untuk aksi ini." | Sembunyikan tombol tulis; periksa role |
| `23503` | Melanggar foreign key | "Data ini masih dipakai fitur lain." | Tawarkan arsip, bukan hapus |
| `23505` | Melanggar unique | "Data ini sudah ada." | Arahkan ke edit/upsert |
| `23514` | Melanggar check constraint | Pesan spesifik field (mis. "Harga minimum tidak boleh lebih besar dari maksimum") | Sorot field di form |
| `PGRST116` | Tidak ada baris | "Data tidak ditemukan." | Tampilkan empty state |
| `PGRST301` / `401` | Sesi tidak valid | "Sesi berakhir, silakan masuk lagi." | Bersihkan sesi, ke login |
| `23502` | Kolom wajib kosong | "Ada isian yang belum lengkap." | Sorot field kosong |

### 12.3 Operasi yang "berhasil" tetapi tidak mengubah apa pun

Policy `using` bekerja dengan menyaring baris, bukan selalu melempar kesalahan. Hasil uji di database:

| Operasi | Hasil nyata | Yang harus dilakukan aplikasi |
|---|---|---|
| User meng-UPDATE ulasan milik orang lain | Tidak ada error, **0 baris** terpengaruh | Periksa jumlah baris hasil; tampilkan "tidak punya hak" bila 0 |
| User meng-UPDATE role sendiri di `user_roles` | Tidak ada error, 0 baris | Sembunyikan aksi ini dari UI sama sekali |
| User meng-INSERT ke tabel katalog | Error `42501` | Tangkap lewat `ApiError` |
| User membaca bookmark orang lain | 0 baris, tanpa error | Tampilkan empty state, bukan "kesalahan" |

Karena itu, untuk UPDATE dan DELETE, repository sebaiknya memakai `.select()` setelah operasi agar jumlah baris yang berubah bisa diperiksa. Menyimpulkan "berhasil" hanya dari tidak adanya exception adalah sumber bug yang sulit dilacak.

---

## 13. Membuat akun admin dan data awal

### 13.1 Akun admin

Urutannya penting: akun dibuat lewat Auth lebih dulu, baru role-nya dinaikkan.

| Langkah | Tempat | Aksi |
|---|---|---|
| 1 | Dashboard → Authentication → Users → Add user | Isi email dan kata sandi; centang **Auto Confirm User** |
| 2 | SQL Editor | Pastikan profilnya ada (trigger sudah membuatnya; berikut hanya jaring pengaman) |
| 3 | SQL Editor | Naikkan role menjadi `admin` |
| 4 | Aplikasi | Login dengan akun itu, tombol tulis katalog muncul |



Jangan menulis kata sandi di repo, README, atau dokumen ini. Simpan di catatan grup.

### 13.2 Seed vibe

Tujuh vibe awal, supaya tab pada fitur Vibe tidak kosong saat pertama dibuka.

```sql
insert into public.vibes (name, icon_key, description, created_by)
select v.name, v.icon_key, v.description,
       (select user_id from public.user_roles
         where role = 'admin' order by granted_at limit 1)
from (values
  ('Nugas / WFC',      'work',      'Colokan banyak, Wi-Fi kencang, meja panjang'),
  ('Aesthetic',        'aesthetic', 'Interior rapi, cocok untuk foto'),
  ('24 Jam',           'night',     'Buka sepanjang hari'),
  ('Outdoor',          'outdoor',   'Area terbuka, taman, atau rooftop'),
  ('Vintage / Retro',  'vintage',   'Perabot kayu, nuansa lama'),
  ('Rooftop & View',   'rooftop',   'Pemandangan kota dari ketinggian'),
  ('Minimalist Modern','minimal',   'Putih, bersih, tanpa banyak ornamen')
) as v(name, icon_key, description)
on conflict do nothing;
```

Cafe, relasi vibe, dan event **tidak** di-seed lewat SQL. Isi dari aplikasi dengan akun admin, sekalian menguji Insert dan unggah foto.

---

## 15. Uji manual sebelum integrasi dianggap selesai

Dijalankan setelah SQL selesai dan aplikasi sudah memakai repository. Kolom "hasil" diisi saat rapat.

| No | Yang diuji | Cara | Hasil yang benar | Hasil |
|---|---|---|---|---|
| 1 | Register baru | Daftar dari aplikasi | Baris `profiles` dan `user_roles` (`user`) otomatis ada | |
| 2 | Login admin | Masuk dengan akun admin | Tombol tambah/edit/hapus muncul | |
| 3 | Insert cafe | Admin menambah cafe | Muncul di Direktori; `created_by` terisi | |
| 4 | Upload foto | Unggah foto saat insert | File di `cafe-photos/{cafe_id}/`, `photo_path` terisi | |
| 5 | Update cafe | Ubah harga dan nama | Daftar dan detail ikut berubah | |
| 6 | Arsip cafe | Hapus cafe yang sudah diulas | `is_active = false`, ulasan tetap ada | |
| 7 | Ulasan | User memberi rating | `reviews` bertambah, `rating` view berubah | |
| 8 | Upsert ulasan | User mengulas cafe yang sama | Tidak ada baris kedua; data berubah | |
| 9 | Hapus ulasan orang lain | Admin mencoba lewat aplikasi | Ditolak `42501`, tombol tidak tersedia | |
| 10 | Bookmark | User menyimpan cafe + catatan | Muncul di Saved Spot, `note` tersimpan | |
| 11 | Bookmark privat | Admin membuka Saved Spot | Hanya melihat miliknya sendiri | |
| 12 | Detail dari Vibe | Buka detail dari fitur Vibe | Detail terisi walau Direktori belum dibuka | |
| 13 | Filter event | Buka Event & Promo | Hanya event `is_published` dan `ends_at > now` | |
| 14 | Jaringan mati | Matikan Wi-Fi, buka layar | Pesan dari `ApiError` + tombol coba lagi | |

---


## Lampiran A — Peta objek database ke kode

| Objek database | Model Kotlin | File model | Dipakai layar |
|---|---|---|---|
| `cafes_with_rating` | `Cafe` | `core/model/Cafe.kt` | Direktori, detail cafe, Vibe, Bookmark, Beranda |
| `reviews` + join `profiles` | `Review` | `core/model/Review.kt` | Ulasan, detail cafe |
| `vibes_with_count` | `Vibe` | `core/model/Vibe.kt` | Vibe, Beranda |
| `event_promos` + join `cafes` | `EventPromo` | `core/model/EventPromo.kt` | Event & Promo, Beranda |
| `bookmarks` + join `cafes_with_rating` | `BookmarkItem` | `features/bookmark/BookmarkItem.kt` | Bookmark |
| `profiles` + `user_roles` | `AuthUiState` (bukan model tabel) | `features/auth/AuthViewModel.kt` | Login, Profil |

Aturan pemetaan:

- Nama kolom database hanya muncul di file `*Mapper.kt`. Layar tidak pernah melihat `snake_case`.
- `null` dari database tidak diubah menjadi `0` atau `""` secara diam-diam; layar yang memutuskan tampilannya.
- Waktu selalu `Instant` di model, label dibuat di UI.

---