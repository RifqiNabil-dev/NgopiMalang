# Fitur Event & Promo Cafe — bahan & tugas

> **Pemilik:** Rifqi Nabil Al Fath — folder `features/eventpromo/` baru berisi `README.md`.
> **Panduan** yang isinya bahan yang sudah tersedia + daftar tugas. ikuti aturan di [08-komponen-bersama.md](08-komponen-bersama.md).

---

## 1. Bahan yang sudah disiapkan

### 1.1 Model — `core/model/EventPromo.kt`

| Field | Tipe | Contoh isi |
|---|---|---|
| `id` | `String` | `"e1"` |
| `title` | `String` | `"Live Acoustic Night"` |
| `cafeName` | `String` | `"Bara Coffee House"` |
| `category` | `EventCategory` | `EventCategory.EVENT` atau `EventCategory.PROMO` |
| `schedule` | `String` | `"Sab, 21 Des 2025 • 19.00 WIB"` |
| `description` | `String` | penjelasan singkat acara/promo |

`EventCategory` adalah enum dengan `label` siap tampil (`category.label`), jadi kamu tidak
perlu menerjemahkan sendiri.

### 1.2 Data dummy — `core/data/DummyData.kt`

`DummyData.events` → **4 event/promo** (campuran EVENT dan PROMO).

### 1.3 Komponen siap pakai — `core/components/`

| Komponen | Untuk apa |
|---|---|
| `EventCard(event, modifier, onClick)` | kartu satu event/promo, sudah berisi judul, cafe, jadwal, label kategori |
| `SectionHeader(title, actionLabel)` | judul bagian, misal "Event Mendatang" / "Promo Hari Ini" |
| `InfoBadge(text, active)` | label kecil, misal `EVENT` / `PROMO` |

Cara memanggil: `EventCard(event = item, modifier = Modifier.fillMaxWidth(), onClick = { ... })`

### 1.4 Navigasi

Route `NavRoutes.EVENT_PROMO` sudah ada di `AppNavHost.kt` dan tampil di bottom bar,
tapi isinya masih `FeaturePlaceholderScreen(...)`. Ganti blok itu dengan layarmu.

---

## 2. Yang perlu kamu buat

| # | File | Isi |
|---|---|---|
| 1 | `features/eventpromo/EventPromoViewModel.kt` | state + filter kategori |
| 2 | `features/eventpromo/EventPromoScreen.kt` | daftar event & promo |
| 3 | (opsional) `EventDetailScreen.kt` | detail satu event |
| 4 | update `README.md` di folder fitur | catatan singkat apa yang sudah dibuat |

Rincian tugas:

1. **State** — daftar event yang tampil + kategori terpilih (`null` = semua).
2. **Ambil data** dari `DummyData.events` di ViewModel.
3. **Filter kategori** — EVENT / PROMO / Semua, logika di ViewModel.
4. **Layar** — daftar vertikal (`LazyColumn`) berisi `EventCard` + tab/chip filter.
5. **Klik kartu** — arahkan ke layar detail (kalau dibuat).

---

## 3. Petunjuk pengerjaan

- Komponen Compose yang perlu kamu baca dokumentasinya: `LazyColumn`, `TabRow` atau
  `FilterChip`, `Arrangement.spacedBy`, `Text` dengan `maxLines` + `overflow` untuk
  deskripsi panjang.
- `EventCard` di `LazyColumn` cukup `Modifier.fillMaxWidth()`.
- Kalau memakai `TabRow`, index tab perlu dipetakan ke kategori — hati-hati urutannya
  (Semua, EVENT, PROMO) supaya tidak tertukar.
- Pola ViewModel: lihat bagian 5 di [08-komponen-bersama.md](08-komponen-bersama.md).

---

## 4. Jebakan

| Gejala | Sebab | Perbaikan |
|---|---|---|
| Tab "Promo" menampilkan event | index tab tidak dipetakan ke enum | buat list kategori eksplisit lalu ambil `list[index]` |
| Deskripsi kepanjangan merusak kartu | tidak dibatasi | pakai `maxLines = 2` + `TextOverflow.Ellipsis` |
| Filter mengosongkan semua | membandingkan `category` dengan `String` | bandingkan dengan `EventCategory.EVENT` / `.PROMO` |
| Layar tidak muncul | `AppNavHost` masih placeholder | ganti `composable(NavRoutes.EVENT_PROMO)` |

---


📖 **Kembali ke indeks:** [README.md](README.md) · **Aturan umum:** [08-komponen-bersama.md](08-komponen-bersama.md)
