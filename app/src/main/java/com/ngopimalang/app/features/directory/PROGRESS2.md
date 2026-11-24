# Progress 2 Direktori Cafe — Ossa

## Status dan ruang lingkup
Fondasi origin/progres2 digabung ke codex/direktori-cafe-ossa. Integrasi memakai Supabase Cloud kelompok. Tabel dan policy tidak diubah. Konfigurasi URL/publishable key hanya di local.properties yang diabaikan Git; password admin tidak disimpan di kode aplikasi.

## Implementasi
- CafeRepository menjadi satu pintu akses cafes_with_rating, cafes, vibes, cafe_vibes, dan bucket cafe-photos.
- CafeRow adalah DTO baca; CafeWrite hanya mengirim data yang boleh ditulis. ID, role pemilik, timestamp dan rating tidak dikirim.
- Daftar: StateFlow, pencarian dengan debounce 300 ms, filter Wi-Fi/colokan, loading/error/retry/empty, dan tombol tambah khusus admin. Maksimal 50 hasil; pencarian literal dilakukan pada hasil terbatas ini, sesuai volume awal proyek. Belum pagination.
- Detail dimuat dengan UUID secara mandiri. Bisa dibuka tanpa lebih dulu membuka daftar. Rating null ditampilkan sebagai Belum ada ulasan; arsip diberi label.
- Form tambah/edit: nama wajib, harga opsional berupa bilangan nonnegatif, rentang harga valid, jumlah colokan nonnegatif, pilihan vibe, pemilih foto dan pratinjau.
- Foto dibaca di Dispatchers.IO dengan batas 5 MB dan whitelist JPEG/PNG/WebP. URL bertanda tangan dibuat saat membaca; hanya path disimpan di tabel.
- Penyimpanan data cafe lebih dulu. ID hasil insert disimpan sebelum menyimpan relasi/foto agar retry tidak membuat baris baru. Operasi tabel dan Storage tidak dianggap transaksi atomik.
- Hapus memakai dialog. Jika database menolak karena FK (23503), pengguna ditawari arsip. Jumlah baris update/delete diverifikasi. Pembersihan foto lama bersifat best effort; kegagalan pembersihan dapat meninggalkan file yatim.

## Pemetaan materi praktikum
| Materi | Penerapan |
|---|---|
| Koneksi dan MVVM | Supabase singleton → Repository → ViewModel → StateFlow → Composable |
| Login/Register | AuthRepository dan AuthViewModel dari fondasi progres2, termasuk role dari user_roles |
| Event dan state | Input dan klik mengirim callback; ViewModel mengubah state; UI mengamati secara lifecycle-aware |
| Composable Layout dan Modifier | Column, Row, FlowRow, LazyVerticalGrid, ukuran/padding/imePadding, tema bersama |
| State hoisting | State form ada di ViewModel; kolom menerima value dan callback |
| Perubahan konfigurasi | ViewModel bertahan saat rotasi; SavedStateHandle menyimpan input/filter dan ID hasil insert untuk pemulihan proses |
| Foto setelah pemulihan proses | URI pratinjau dipulihkan, tetapi pengguna diminta memilih ulang foto sebelum menyimpan jika bytes sudah hilang |
| Activity lifecycle | Observer ON_RESUME memuat ulang daftar setelah kembali dari detail/form atau aplikasi peta |
| Side effect | LaunchedEffect memuat detail/form dan menavigasi setelah selesai; DisposableEffect melepas observer |
| Type-safe navigation | CafeFormRoute dan CafeDetailRoute @Serializable, navigate(route), composable<T>, toRoute |
| Navigasi detail bersama | Route lama directory/cafe/{cafeId} dipertahankan agar caller Vibe yang sudah ada tetap cocok |
| Implicit intent | ACTION_VIEW dengan geo URI untuk membuka alamat pada aplikasi peta; ActivityNotFoundException ditangani |

## Batas integrasi tim
Model Cafe mempertahankan priceRange, vibes dan imageResId agar modul Progress 1 lain tetap dapat dikompilasi. Data database memiliki priceMin/priceMax, rating nullable, reviewCount, photoPath, photoUrl, dan isActive. CafeCard sudah mendukung foto jaringan serta badge arsip.

Detail menyediakan slot composable reviewSection(cafeId) dan bookmarkAction(cafeId). Branch progres2 yang diperiksa belum menyediakan ReviewSection.kt dan BookmarkAction.kt; slot sengaja kosong sampai komponen anggota terkait tersedia. Tidak ada repository ulasan/bookmark buatan Direktori. Modul Vibe/Bookmark/Event masih perlu mengganti ID dummy dengan UUID database di branch masing-masing; ID seperti c1 tidak dapat menunjuk data Cloud.

## Pengujian database asli
Login admin dan pembacaan role berhasil. Uji REST dengan cafe sementara berhasil: insert → baca cafes_with_rating → update harga → upload PNG → signed URL → relasi vibe → arsip → hapus. Foto dan baris uji dibersihkan pada akhir tes. Ini menguji kontrak Cloud, belum menggantikan pengujian UI Android atau RLS dengan akun user biasa.

## Pengujian demo Android
1. Login admin dan buka Direktori; tambah cafe tanpa foto, kemudian dengan foto.
2. Edit harga, Wi-Fi, colokan dan pilihan vibe. Kembali ke daftar, pastikan data dimuat ulang.
3. Putar emulator saat form terisi; input tetap ada. Coba pilih gambar >5 MB dan format selain yang diizinkan.
4. Matikan jaringan; error dan retry harus terlihat. Saat simpan foto gagal setelah insert, retry tidak membuat cafe ganda.
5. Hapus cafe tanpa relasi; untuk cafe dengan relasi ulasan/bookmark/event, pilih arsip ketika ditawarkan.
6. Login user biasa; tombol admin tidak tampil dan cafe arsip tidak muncul dari view karena RLS.
7. Buka detail dari fitur lain setelah caller memakai UUID asli. Coba Buka di peta.

## Build
Fondasi memakai AGP 8.7.3 dan Kotlin 2.0.21. Wrapper diselaraskan ke Gradle 8.9 dan JVM daemon 21. Android Studio perlu Gradle JDK 21, bukan JBR 25 untuk kombinasi ini. Storage mengikuti versi Supabase fondasi 3.0.3; Coil Compose dipatok 2.7.0.

Implementasi sudah disalin dan diterapkan pada proyek utama D:\NgopiMalang, branch direktori-cafe-ossa. Perubahan belum di-push. Salinan lama di C sudah dicadangkan dan dihapus; proyek aktif tetap di D.

## Hasil build 23 November 2026
assembleDebug dan testDebugUnitTest berhasil dengan Gradle 8.9 / JDK 21. Lima tes unit lulus tanpa failure/error. APK berhasil dibuat. Pengujian visual pada emulator serta login akun user biasa belum dilakukan karena akses perangkat virtual Android tidak tersedia dari sesi ini.

## Pemeriksaan ulang pada proyek utama di D

- Build APK dari D berhasil setelah salinan C dibersihkan. Perintah testDebugUnitTest juga berhasil; hasil lima tes direktori memiliki 0 failure dan 0 error. Cakupan lima tes ini adalah validasi form dan mapping/format harga, bukan pengujian interaksi UI atau jaringan.
- Login Supabase Cloud dengan akun admin berhasil dan user_roles mengembalikan admin.
- Uji API Cloud ulang berhasil untuk insert cafe, baca detail dari cafes_with_rating, update harga, simpan relasi vibe, upload PNG, signed URL foto, dan arsip. Foto, relasi, serta cafe verifikasi dibersihkan di akhir pengujian.
- Jumlah baris yang terlihat dengan sesi admin sebelum uji: cafes 0, cafes_with_rating 0, vibes 8, cafe_vibes 0, reviews 0, bookmarks 0, event_promos 0. Ini hasil akses aplikasi dengan RLS, bukan pemeriksaan superuser. Data cafe contoh di DummyData belum dimigrasikan ke Cloud.
- Direktori memakai repository Cloud. Beranda, Vibe, Ulasan, Bookmark, dan Event masih memiliki penggunaan DummyData pada branch ini. Slot ulasan/bookmark di detail belum dihubungkan ke komponen anggota tim.
- Android Studio sudah membuka D:\NgopiMalang dengan cache, konfigurasi, dan file sementara di D. Belum ada perangkat terhubung; percobaan emulator CLI ditolak karena akselerasi VM tidak tersedia. Pengujian tampilan, rotasi, pemilih foto, intent peta, dan akun user biasa masih belum dinyatakan lulus.
- Kesimpulan: implementasi Direktori dan kontrak API Cloud tersedia; seluruh fitur aplikasi dan seluruh data proyek belum bisa disebut lengkap atau teruji menyeluruh.

## Impor data demo dan penyiapan uji layar

Atas permintaan untuk mengisi database, 12 cafe dari DummyData Progress 1 sudah dimasukkan permanen ke Supabase Cloud: nama, alamat, rentang harga, Wi-Fi, colokan, foto, dan relasi vibe. Setiap baris baru diberi deskripsi bahwa sumbernya data demo. Ini dataset demo proyek, bukan informasi cafe yang telah diverifikasi di dunia nyata.

Hasil verifikasi setelah impor: 12 cafe, 12 photo_path, 8 kategori vibe, dan 28 cafe_vibes. Seluruh 12 foto berhasil diunduh melalui signed URL. Rating contoh tidak diimpor: semua cafe memiliki rating null dan review_count 0 sampai ada ulasan asli. Tidak ada tabel, policy, atau skema yang diubah.

Pemetaan ID lama ke UUID Cloud disimpan secara lokal di D:\NgopiMalang\.codex-work\demo-import-manifest.json. Script impor lokal menggunakan kredensial dari environment dan memeriksa cafe berdasarkan nama supaya pengulangan tidak membuat duplikat. Source DummyData anggota lain tidak diubah.

DirectoryCloudUiTest ditambahkan sebagai uji instrumentasi opt-in. Kredensial disuplai melalui argumen qaEmail dan qaPassword, bukan disimpan dalam source. Paket assembleDebugAndroidTest berhasil dibangun. Skenarionya mencakup daftar Cloud, pencarian, filter Wi-Fi, detail, form edit dan pemulihan Activity, serta tambah/hapus cafe uji bernama unik dengan cleanup terbatas pada fixture tersebut. Status build paket tes tidak berarti skenario tersebut sudah lulus: eksekusinya masih menunggu perangkat Android yang selesai boot/terhubung.

## Koreksi kontrak tulis database saat uji Android — 24 November 2026

Uji pada emulator lokal menemukan bahwa address, description, dan open_hours di tabel Cloud memiliki constraint NOT NULL. Form membolehkan pengguna mengosongkan isinya, tetapi payload sebelumnya mengubah teks kosong menjadi null sehingga insert ditolak dengan 23502. CafeWrite dan CafeMapper sekarang mengirim ketiga kolom sebagai String (kosong bila tidak diisi); price_min dan price_max tetap nullable. Payload minimal yang diperbaiki berhasil diuji langsung ke Cloud dan baris verifikasinya dihapus. Tes unit mencakup regresi ini dan kelima tes direktori lulus.

Emulator lokal Ossa UI Android 10 (API 29) berhasil dijalankan dari Android Studio dengan WHPX aktif. Data emulator berada di D:\NgopiMalang\.codex-work\avd; file kecil pendaftarannya berada di folder avd Android pengguna. APK utama dan APK tes berhasil dipasang ke emulator-5554. Koneksi melalui Firebase Device Streaming tidak dipakai.

Hasil akhir uji instrumentasi adminCanReadSearchOpenFormsAndCreateDeleteCafe: OK (1 test), 29,615 detik. Skenario yang lulus: sesi admin, daftar 12 cafe dari Cloud, pencarian, filter Wi-Fi (10 hasil), detail, form edit, pemulihan input setelah Activity recreation, validasi nama kosong, tambah cafe, edit deskripsi lalu baca ulang detail, dan hapus dengan konfirmasi. Cafe sementara dihapus; dataset demo tetap 12 cafe. Bukti keluaran tes disimpan secara lokal di .codex-work/evidence/directory-ui-test.txt.

Pengujian ini belum mencakup akun user biasa, pemilih foto sistem, aplikasi peta eksternal, pemulihan setelah process death, atau seluruh skenario kegagalan jaringan. Upload/akses foto serta arsip sebelumnya terverifikasi melalui API Cloud; jangan menyamakan pengujian API dengan uji interaksi UI untuk bagian tersebut.

## Penyesuaian Home/detail dan akun user biasa — 24 November 2026

DirectoryDetailScreen mengikuti referensi tata letak tim: header bersama, hero foto membulat dengan badge rating, informasi lokasi, chip vibe, dan AppCard untuk fasilitas/deskripsi/ringkasan ulasan. Foto Cloud, pembacaan UUID, rating nullable, loading/error/retry, intent peta, dan kontrol admin tetap dipertahankan. Nama fungsi serta jalur detail lama/bertipe tetap tersedia bagi caller lain. Slot reviewSection dan bookmarkAction dipertahankan; kelas ReviewSection dan ReviewSectionViewModel pada referensi belum tersedia dalam branch aktif maupun ref shel-branch yang diperiksa, sehingga tidak diimpor sebagai dependensi yang belum ada.

Rekomendasi cafe Home memakai HomeCafeViewModel dan CafeRepository; klik cafe mengarah langsung ke detail bersama dengan UUID Cloud. Preview Event/Vibe/Ulasan lain tetap mengikuti kode modul pemiliknya.

Akun QA non-admin dibuat melalui pendaftaran biasa, dengan role user dari user_roles. Kredensial hanya disimpan lokal dalam .codex-work/ordinary-user.json yang diabaikan Git. Pemeriksaan API nyata lulus untuk baca 12 cafe aktif, akses signed URL foto, penolakan insert/update/delete dan upload foto, serta penyembunyian cafe arsip. Fixture pemeriksaan dibersihkan. Bukti lokal: .codex-work/evidence/ordinary-user-rls.json.

Build debug, lima tes unit direktori, serta dua skenario UI pada emulator lokal API 29 lulus. Hasil instrumentasi: OK (2 tests), 41,316 detik. Admin tetap dapat mengelola cafe pada tata letak baru. User biasa diuji login, rekomendasi Cloud di Home, navigasi kartu ke detail bersama, ketiadaan tombol edit/hapus di detail, serta ketiadaan tombol tambah di Direktori. Bukti lokal: .codex-work/evidence/admin-and-user-ui-test.txt dan qa-user-home.png, qa-user-detail.png, qa-user-directory.png. Akun user biasa tidak lagi termasuk bagian yang belum diuji; batas lain pada catatan sebelumnya (pemilih foto sistem, intent aplikasi peta, process death dan kegagalan jaringan) tetap berlaku.

