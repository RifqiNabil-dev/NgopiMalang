# Direktori Cafe — Ossa Amelia Zuhra Lubis

Implementasi Progress 2 memakai Supabase Cloud, MVVM/StateFlow, form tambah/edit admin, foto Storage, arsip dan hapus. Daftar dibaca dari cafes_with_rating dan detail memuat UUID secara mandiri. Lihat PROGRESS2.md di folder ini untuk kontrak integrasi, batas fitur anggota lain, pemetaan materi praktikum, pengujian dan langkah demo.

Cafe masih memiliki properti kompatibilitas bagi modul Progress 1. Slot ReviewSection/BookmarkAction belum dihubungkan karena komponen anggota terkait belum tersedia di fondasi.

Pemeriksaan 24 November 2026: build debug, lima tes unit, dan dua skenario instrumentasi emulator lokal (admin dan user biasa) lulus. Pembatasan akses user juga terverifikasi melalui API Cloud; data fixture dibersihkan. Rekomendasi Home memakai UUID Cloud dan detail bersama mengikuti tata letak tim. Lihat PROGRESS2.md untuk cakupan serta batas pengujian. Belum di-push.
