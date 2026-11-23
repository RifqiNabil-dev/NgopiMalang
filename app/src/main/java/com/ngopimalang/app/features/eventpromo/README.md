# features/eventpromo — Event dan Promo Cafe

**Pemilik:** Rifqi Nabil Al Fath (245150607111039)

## Progress

### ✅ Sudah dibuat (Progress 1)
- **EventPromoViewModel.kt** — State management dengan filter kategori + fungsi CRUD
  - `addEvent()` - tambah event baru
  - `updateEvent()` - edit event
  - `deleteEvent()` - hapus event
  - `getEventById()` - ambil detail event
- **EventPromoScreen.kt** — Daftar event/promo
  - LazyColumn dengan EventCard
  - FilterChip untuk kategori (Semua/Event/Promo)
  - FAB untuk tambah event baru
- **EventDetailScreen.kt** — Detail event/promo
  - Info lengkap event (judul, cafe, jadwal, deskripsi, kategori)
  - Tombol Edit dan Hapus
  - Dialog konfirmasi hapus
- **EventFormScreen.kt** — Form tambah/edit event
  - Input: judul, nama cafe, kategori, jadwal, deskripsi
  - Validasi form
  - Mode tambah dan edit
- **Navigasi** — List → Detail → Form sudah terhubung

### 📝 Catatan teknis
- Data disimpan di memory (mutableList) untuk Progress 1
- Progress 2: ganti dengan Supabase (tabel `events`)
- CRUD sudah lengkap: Create, Read, Update, Delete

- Model bersama: `com.ngopimalang.app.core.model.EventPromo`
- Dummy data sementara: `com.ngopimalang.app.core.data.DummyData.events`
- Route: `NavRoutes.EVENT_PROMO`

---

📖 **Panduan lengkap:** [`docs/03-eventpromo.md`](../../../../../../assets/docs/03-eventpromo.md)
