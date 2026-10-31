# Panduan Review Kode — Progress 1

Branch ini dipakai untuk **review kode**, bukan untuk develop fitur. Jangan push langsung
ke `main`; semua masukan lewat komentar di Pull Request atau di commit branch ini.

## Cara review

1. Buka tab **Files changed** pada Pull Request, atau telusuri folder
   `app/src/main/java/com/ngopimalang/app/` langsung di branch ini.
2. Beri komentar pada baris kode yang ingin ditanyakan/diubah.
3. Kalau setuju, tekan **Approve**. Kalau ada yang harus diperbaiki, **Request changes**.
4. Perbaikan dilakukan di branch fitur masing-masing, lalu PR baru ke `main`.

## Yang perlu dicek

**Semua orang**

- [ ] Nama file & package sesuai fitur (`features/<fitur>/`) — tidak ada file nyasar di root.
- [ ] Tidak ada warna hardcode (`Color(0xFF...)`, `Color.White`) di luar `ui/theme/`.
      Semua warna harus dari `MaterialTheme.colorScheme`.
- [ ] Teks yang tampil ke pengguna memakai `stringResource` atau minimal konsisten bahasa Indonesia.
- [ ] Composable tidak lebih dari ±250 baris; kalau lebih, pecah jadi komponen kecil.
- [ ] `./gradlew assembleDebug` masih sukses setelah perubahan.

**Pembagian per fitur**

| Penanggung jawab | File yang direview |
|---|---|
| Rifqi (auth, event) | `features/auth/LoginScreen.kt`, `RegisterScreen.kt`, `core/components/AppStyle.kt` |
| Ossa (direktori) | `core/components/CafeCard.kt`, `features/directory/`, `core/data/DummyData.kt` (bagian cafe) |
| Shelfina (ulasan) | `core/components/ReviewCard.kt`, `core/components/RatingStars.kt`, `features/reviews/` |
| Shananda (bookmark) | `features/bookmark/`, `core/components/AppBottomBar.kt`, `core/navigation/NavRoutes.kt` |
| Evia (vibe) | `features/vibe/`, `core/components/InfoBadge.kt`, `core/components/SectionHeader.kt` |
| Semua | `features/home/HomeScreen.kt`, `features/profile/ProfileScreen.kt`, `core/ui/FeaturePlaceholderScreen.kt` |

## Komponen bersama — jangan bikin ulang

Semua layar wajib memakai komponen di `core/components/AppStyle.kt` supaya tampilan rata:

| Komponen | Kegunaan |
|---|---|
| `GradientBackdrop` | latar gradient header, dipakai di semua layar |
| `AppCard` | kartu radius 24dp + border tipis, pengganti `Card` polos |
| `AppTextField` | input filled tanpa garis bawah |
| `PillButton` | tombol utama tinggi 56dp |
| `AppLogoMark` | logo kotak membulat di halaman auth |
| `AuthSwitchLink` | tautan pindah Login ↔ Daftar |
| `SectionLabel` | label kecil kapital di atas judul |

Kalau butuh komponen baru, tambahkan di file itu — jangan menyalin gaya ke layar masing-masing.

## Catatan Progress 1

- Semua data masih dummy (`core/data/DummyData.kt`), belum ada jaringan/Supabase.
- Lima fitur masih placeholder (`FeaturePlaceholderScreen`) — isinya rencana, bukan fitur jadi.
- Password hanya divalidasi minimal 6 karakter, belum ada penyimpanan sesi.
