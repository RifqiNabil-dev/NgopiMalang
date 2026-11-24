# Home dan integrasi cafe/ulasan

Home mempertahankan tata letak dan komponen bersama tim. HomeCafeViewModel membaca rekomendasi cafe aktif melalui CafeRepository dan enam ulasan terbaru melalui ReviewRepository, dengan loading/error/retry/empty state yang terpisah. Kedua permintaan berjalan independen sehingga kegagalan salah satunya tidak menutupi hasil yang lain.

AppNavHost meneruskan UUID cafe ke CafeDetailRoute ketika kartu rekomendasi dipilih. Data dimuat ulang saat Home kembali aktif, termasuk setelah pengguna menulis ulasan di detail. ReviewCard memakai model Review dari modul ulasan shel-branch; rating dan jumlah ulasan cafe berasal dari cafes_with_rating.

Bagian Event/Promo, chip Vibe, dan Saved Spot tetap mengikuti alur modul pemiliknya pada branch ini. Integrasi ini mencakup direktori dan ulasan; migrasi backend seluruh fitur tim tetap menjadi tanggung jawab masing-masing anggota.
