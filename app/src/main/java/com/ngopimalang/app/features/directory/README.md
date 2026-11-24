# Direktori Cafe — Ossa Amelia Zuhra Lubis

Proyek utama: D:\NgopiMalang, branch direktori-cafe-ossa. Progress 2 memakai Supabase Cloud, MVVM/StateFlow, tambah/edit khusus admin, foto Storage, arsip, dan hapus. Daftar memakai cafes_with_rating; detail menerima UUID Cloud melalui navigasi bertipe maupun adapter jalur lama.

ReviewSection dan ReviewSectionViewModel dari branch shel-branch sudah terhubung ke detail bersama. Rating hero mengikuti agregat database setelah ulasan disimpan/dihapus. Pengguna biasa dapat menulis/mengedit/menghapus ulasannya; admin memoderasi. Role diambil dari user_roles. Ulasan contoh tidak ditambahkan ketika database kosong.

Home memakai cafe dan ulasan Cloud. BookmarkAction tetap menjadi slot integrasi milik anggota terkait. Model Cafe mempertahankan properti kompatibilitas bagi modul Progress 1. Lihat PROGRESS2.md untuk hasil pengujian dan batas cakupan.
