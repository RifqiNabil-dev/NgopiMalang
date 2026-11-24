# Deskripsi Direktori Cafe — Ossa

Bagian **Tentang cafe** pada 12 cafe awal sudah diisi dengan deskripsi masing-masing. Kontennya mencakup suasana, alamat, fasilitas, dan kisaran harga berdasarkan kolom serta relasi vibe yang sudah tersimpan di database.

Salinan konten tersedia dalam `directory-cafe-descriptions.json` di folder ini agar perubahan teks dapat ditinjau melalui Git. Aplikasi tetap membaca `cafes.description` dari Supabase Cloud melalui `CafeRepository`; JSON ini merupakan referensi konten dan tidak otomatis menulis database ketika aplikasi dijalankan.

Pembaruan Cloud hanya menyentuh kolom `description` pada cafe yang dituju. Nama, ID, alamat, harga, fasilitas, jam buka, foto, status aktif, dan relasi fitur lain tetap mengikuti data yang sudah ada. Perubahan berikutnya dapat dilakukan admin melalui **Direktori → Detail Cafe → Edit cafe → Deskripsi → Simpan cafe**.

Sumber awal katalog adalah dataset proyek Progress 1. Deskripsi ditulis ulang dari atribut katalog tersebut, bukan hasil verifikasi lapangan atau ulasan pengguna. Tidak ditambahkan klaim menu, promo, sertifikasi, kecepatan internet, maupun jam operasional yang belum tersedia. Label kategori 24 Jam tidak digunakan untuk mengisi jam buka secara otomatis.

Sebelum pembaruan, nilai lama dicadangkan secara lokal. Setelah pembaruan, seluruh deskripsi dibaca ulang dari tabel `cafes` dan view `cafes_with_rating`, termasuk menggunakan sesi user biasa. Skenario emulator untuk login user biasa, membuka detail Cloud dari Home, dan pembatasan tombol admin juga berhasil dijalankan. Halaman detail yang sudah terbuka perlu dibuka ulang agar memuat deskripsi terbaru.
