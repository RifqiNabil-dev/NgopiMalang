package com.ngopimalang.app.core.data

import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.model.EventCategory
import com.ngopimalang.app.core.model.EventPromo
import com.ngopimalang.app.core.model.Review
import com.ngopimalang.app.core.model.Vibe

/**
 * Data dummy Progress 1 (UI belum terintegrasi Supabase).
 * Saat integrasi database masuk, isi file ini diganti oleh repository per fitur.
 */
object DummyData {

    val vibes = listOf(
        Vibe("v1", "Nugas / WFC", "Tenang, banyak colokan, cocok buat kerja", 12),
        Vibe("v2", "Aesthetic", "Interior instagramable dan pencahayaan hangat", 9),
        Vibe("v3", "24 Jam", "Buka sepanjang malam untuk begadang", 4),
        Vibe("v4", "Outdoor", "Area terbuka dengan taman dan udara segar", 7)
    )

    val cafes = listOf(
        Cafe(
            id = "c1",
            name = "Kopi Sae Malang",
            address = "Jl. Soekarno Hatta No. 12, Lowokwaru",
            priceRange = "Rp15.000 - Rp35.000",
            hasWifi = true,
            plugCount = 8,
            rating = 4.7,
            vibes = listOf("Nugas / WFC", "Aesthetic")
        ),
        Cafe(
            id = "c2",
            name = "Kedai Tepi Sawah",
            address = "Jl. Raya Tlogomas No. 45, Malang",
            priceRange = "Rp20.000 - Rp50.000",
            hasWifi = true,
            plugCount = 4,
            rating = 4.5,
            vibes = listOf("Outdoor", "Aesthetic")
        ),
        Cafe(
            id = "c3",
            name = "Ruang Seduh",
            address = "Jl. Semarang No. 8, Klojen",
            priceRange = "Rp18.000 - Rp40.000",
            hasWifi = false,
            plugCount = 2,
            rating = 4.3,
            vibes = listOf("Aesthetic")
        ),
        Cafe(
            id = "c4",
            name = "Ngopi Bareng 24",
            address = "Jl. Bendungan Sutami No. 3, Malang",
            priceRange = "Rp10.000 - Rp28.000",
            hasWifi = true,
            plugCount = 12,
            rating = 4.4,
            vibes = listOf("24 Jam", "Nugas / WFC")
        ),
        Cafe(
            id = "c5",
            name = "Bara Coffee House",
            address = "Jl. Ijen Boulevard No. 21, Malang",
            priceRange = "Rp25.000 - Rp60.000",
            hasWifi = true,
            plugCount = 6,
            rating = 4.8,
            vibes = listOf("Aesthetic", "Nugas / WFC")
        ),
        Cafe(
            id = "c6",
            name = "Teras Kopi Dago",
            address = "Jl. Dago Pojok No. 17, Malang",
            priceRange = "Rp12.000 - Rp30.000",
            hasWifi = false,
            plugCount = 0,
            rating = 4.1,
            vibes = listOf("Outdoor")
        )
    )

    val events = listOf(
        EventPromo(
            id = "e1",
            title = "Live Acoustic Night",
            cafeName = "Bara Coffee House",
            category = EventCategory.EVENT,
            schedule = "Sab, 21 Des 2025 • 19.00 WIB",
            description = "Penampilan akustik tiga musisi lokal Malang. Gratis untuk semua pengunjung."
        ),
        EventPromo(
            id = "e2",
            title = "Diskon 30% Semua Manual Brew",
            cafeName = "Ruang Seduh",
            category = EventCategory.PROMO,
            schedule = "Setiap hari • 14.00 - 17.00 WIB",
            description = "Nikmati potongan harga untuk seluruh menu manual brew di jam santai."
        ),
        EventPromo(
            id = "e3",
            title = "Pameran Foto Kopi Nusantara",
            cafeName = "Kopi Sae Malang",
            category = EventCategory.EVENT,
            schedule = "20 - 27 Des 2025 • 10.00 - 21.00 WIB",
            description = "Pameran mini 24 karya fotografi tentang perjalanan kopi dari petani ke cangkir."
        ),
        EventPromo(
            id = "e4",
            title = "Beli 1 Gratis 1 Kopi Susu",
            cafeName = "Ngopi Bareng 24",
            category = EventCategory.PROMO,
            schedule = "Min - Kam • 22.00 - 02.00 WIB",
            description = "Promo begadang: setiap pembelian kopi susu mendapat satu gelas gratis."
        )
    )

    val reviews = listOf(
        Review(
            id = "r1",
            userName = "Dinda Ayu",
            cafeName = "Bara Coffee House",
            rating = 5,
            comment = "Tempatnya tenang, colokan banyak, dan kopinya konsisten enak. Cocok banget buat ngerjain tugas.",
            favoriteMenu = "Es Kopi Susu Gula Aren",
            dateLabel = "2 hari lalu"
        ),
        Review(
            id = "r2",
            userName = "Bagas Pratama",
            cafeName = "Kedai Tepi Sawah",
            rating = 4,
            comment = "View sawahnya bikin betah. Agak ramai saat sore, tapi pelayanan cepat.",
            favoriteMenu = "Kopi Tubruk",
            dateLabel = "5 hari lalu"
        ),
        Review(
            id = "r3",
            userName = "Sari Wulandari",
            cafeName = "Ruang Seduh",
            rating = 5,
            comment = "Baristanya ramah dan mau menjelaskan karakter tiap biji kopi. Wajib coba manual brew-nya.",
            favoriteMenu = "V60 Gayo",
            dateLabel = "1 minggu lalu"
        )
    )
}
