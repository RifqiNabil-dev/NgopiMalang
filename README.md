# NgopiMalang ☕

Aplikasi katalog cafe & spot nongkrong di Malang. Dibuat untuk tugas akhir mata kuliah
**Pengembangan Aplikasi Perangkat Bergerak (PAPB)** — kelas PTI A.

Tahap saat ini: **Progress 1** — kerangka aplikasi, navigasi, dan UI dasar dengan data dummy.

## Fitur

| Fitur | Status Progress 1 |
|---|---|
| Autentikasi (Login & Daftar) | UI selesai, validasi dummy |
| Beranda | Selesai — sapaan, chip vibe, daftar cafe & event dummy |
| Direktori Cafe | Placeholder + rencana fitur |
| Vibe / Suasana | Placeholder + rencana fitur |
| Event & Promo | Placeholder + rencana fitur |
| Ulasan & Rating | Placeholder + rencana fitur |
| Bookmark | Placeholder + rencana fitur |
| Profil | Selesai — header akun, menu Saved Spot / Ulasan / Keluar |

## Teknologi

- Kotlin + Jetpack Compose (Material 3), MVVM + StateFlow
- Navigation Compose, satu activity
- Supabase (rencana Progress 2: auth & database)
- Data dummy lokal di `core/data/DummyData.kt`

## Struktur

```
app/src/main/java/com/ngopimalang/app/
├── core/
│   ├── components/    komponen UI bersama (kartu, field, tombol, bottom bar)
│   ├── data/          model + data dummy
│   ├── navigation/    NavRoutes & AppNavHost
│   └── ui/            layar placeholder fitur
├── features/
│   ├── auth/          Login & Daftar
│   ├── home/          Beranda
│   ├── profile/       Profil
│   ├── directory/     (rencana) direktori cafe
│   ├── reviews/       (rencana) ulasan
│   ├── bookmark/      (rencana) bookmark
│   ├── vibe/          (rencana) vibe
│   └── eventpromo/    (rencana) event & promo
└── ui/theme/          Theme.kt, Color.kt, Type.kt
```

Panduan pengerjaan tiap fitur ada di `app/src/main/assets/docs/` (dibuka dari menu
bantuan di aplikasi) — berisi bahan dan tugas, bukan kode jadi.

## Menjalankan

1. Buka folder ini di Android Studio (JDK 11+, Android SDK 29+).
2. Salin `local.properties.example` menjadi `local.properties` lalu isi `sdk.dir`.
   File `local.properties` tidak ikut di-commit karena berisi path lokal dan kredensial.
3. Jalankan `./gradlew assembleDebug` atau tekan Run.

Login dummy: email apa saja, password minimal 6 karakter.

## Tim

| Nama | NIM | Bagian |
|---|---|---|
| Rifqi Nabil Al Fath | 245150607111039 | Auth, Event & Promo |
| Ossa Amelia Zuhra Lubis | 245150600111022 | Direktori Cafe |
| Shelfina Khayla Anindita | 245150600111026 | Ulasan & Rating |
| Shananda Putri Aisyah | 245150601111013 | Bookmark |
| Evia Vinata | 245150607111009 | Vibe / Suasana |
