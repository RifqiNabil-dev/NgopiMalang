# Home dan integrasi cafe

Tata letak Home mempertahankan komponen bersama dan bagian fitur anggota lain. Rekomendasi cafe kini menerima HomeCafeState dari HomeCafeViewModel, membaca cafe aktif melalui CafeRepository yang sama dengan Direktori, dan menyediakan loading/error/retry/empty state.

AppNavHost meneruskan UUID cafe ke CafeDetailRoute ketika kartu rekomendasi dipilih. ID dummy tidak digunakan untuk kartu rekomendasi cafe di Home. Data dimuat ulang saat Home kembali aktif.

Bagian Event/Promo, chip Vibe, Ulasan Terbaru, dan banner Saved Spot tetap mengikuti data dan alur modul anggota yang tersedia pada branch ini; ini bukan migrasi backend seluruh fitur tim.
