package com.ngopimalang.app.core.data

import com.ngopimalang.app.R
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
        Vibe("v1", "Nugas / WFC", "Tenang, banyak colokan, cocok buat kerja & kuliah", 16),
        Vibe("v2", "Aesthetic", "Interior instagramable dan pencahayaan hangat", 12),
        Vibe("v3", "24 Jam", "Buka sepanjang malam untuk begadang & nugas", 6),
        Vibe("v4", "Outdoor", "Area terbuka dengan taman dan udara segar khas Malang", 9),
        Vibe("v5", "Vintage / Retro", "Nuansa nostalgia dengan dekorasi klasik dan musik santai", 5),
        Vibe("v6", "Rooftop & View", "Pemandangan citylight dan pegunungan dari ketinggian", 4),
        Vibe("v7", "Minimalist Modern", "Desain bersih, tenang, dan pencahayaan alami", 8)
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
            vibes = listOf("Nugas / WFC", "Aesthetic", "Minimalist Modern"),
            imageResId = R.drawable.cafe_glasshouse
        ),
        Cafe(
            id = "c2",
            name = "Kedai Tepi Sawah",
            address = "Jl. Raya Tlogomas No. 45, Malang",
            priceRange = "Rp20.000 - Rp50.000",
            hasWifi = true,
            plugCount = 4,
            rating = 4.5,
            vibes = listOf("Outdoor", "Aesthetic", "Vintage / Retro"),
            imageResId = R.drawable.cafe_garden_outdoor
        ),
        Cafe(
            id = "c3",
            name = "Ruang Seduh",
            address = "Jl. Semarang No. 8, Klojen",
            priceRange = "Rp18.000 - Rp40.000",
            hasWifi = false,
            plugCount = 2,
            rating = 4.3,
            vibes = listOf("Aesthetic", "Minimalist Modern"),
            imageResId = R.drawable.cafe_boho_interior
        ),
        Cafe(
            id = "c4",
            name = "Ngopi Bareng 24",
            address = "Jl. Bendungan Sutami No. 3, Malang",
            priceRange = "Rp10.000 - Rp28.000",
            hasWifi = true,
            plugCount = 12,
            rating = 4.4,
            vibes = listOf("24 Jam", "Nugas / WFC"),
            imageResId = R.drawable.cafe_night_beanbag
        ),
        Cafe(
            id = "c5",
            name = "Bara Coffee House",
            address = "Jl. Ijen Boulevard No. 21, Malang",
            priceRange = "Rp25.000 - Rp60.000",
            hasWifi = true,
            plugCount = 6,
            rating = 4.8,
            vibes = listOf("Aesthetic", "Nugas / WFC", "Minimalist Modern"),
            imageResId = R.drawable.cafe_simpang_luwe
        ),
        Cafe(
            id = "c6",
            name = "Teras Kopi Dago",
            address = "Jl. Dago Pojok No. 17, Malang",
            priceRange = "Rp12.000 - Rp30.000",
            hasWifi = false,
            plugCount = 0,
            rating = 4.1,
            vibes = listOf("Outdoor"),
            imageResId = R.drawable.cafe_joglo_wooden
        ),
        Cafe(
            id = "c7",
            name = "Latar Ijen Roastery",
            address = "Jl. Besar Ijen No. 88, Klojen",
            priceRange = "Rp30.000 - Rp75.000",
            hasWifi = true,
            plugCount = 10,
            rating = 4.9,
            vibes = listOf("Aesthetic", "Minimalist Modern", "Outdoor"),
            imageResId = R.drawable.cafe_patisserie_white
        ),
        Cafe(
            id = "c8",
            name = "Lapan Puluh Coffee",
            address = "Jl. MT Haryono No. 160, Dinoyo",
            priceRange = "Rp12.000 - Rp25.000",
            hasWifi = true,
            plugCount = 15,
            rating = 4.6,
            vibes = listOf("Nugas / WFC", "24 Jam"),
            imageResId = R.drawable.cafe_alura_minimalist
        ),
        Cafe(
            id = "c9",
            name = "Oemah Heretan Vintage",
            address = "Jl. Retawu No. 5, Malang",
            priceRange = "Rp18.000 - Rp45.000",
            hasWifi = true,
            plugCount = 5,
            rating = 4.5,
            vibes = listOf("Vintage / Retro", "Outdoor"),
            imageResId = R.drawable.cafe_joglo_wooden
        ),
        Cafe(
            id = "c10",
            name = "Senja Coffee & View",
            address = "Jl. Raya Dau No. 102, Batu-Malang",
            priceRange = "Rp22.000 - Rp55.000",
            hasWifi = true,
            plugCount = 6,
            rating = 4.8,
            vibes = listOf("Rooftop & View", "Outdoor", "Aesthetic"),
            imageResId = R.drawable.cafe_rooftop_white
        ),
        Cafe(
            id = "c11",
            name = "Kopi Studio 24 Suhat",
            address = "Jl. Soekarno Hatta No. 99, Lowokwaru",
            priceRange = "Rp15.000 - Rp30.000",
            hasWifi = true,
            plugCount = 14,
            rating = 4.4,
            vibes = listOf("24 Jam", "Nugas / WFC"),
            imageResId = R.drawable.cafe_night_beanbag
        ),
        Cafe(
            id = "c12",
            name = "Griya Coffee & Bites",
            address = "Jl. Jakarta No. 14, Malang",
            priceRange = "Rp20.000 - Rp48.000",
            hasWifi = true,
            plugCount = 8,
            rating = 4.6,
            vibes = listOf("Minimalist Modern", "Outdoor"),
            imageResId = R.drawable.cafe_boho_interior
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
        ),
        EventPromo(
            id = "e5",
            title = "Barista Workshop: Latte Art 101",
            cafeName = "Latar Ijen Roastery",
            category = EventCategory.EVENT,
            schedule = "Ming, 28 Des 2025 • 10.00 WIB",
            description = "Pelajari teknik frothing susu dan pouring latte art bersama Champion Barista Malang."
        ),
        EventPromo(
            id = "e6",
            title = "Diskon Student 20% Pakai KTM",
            cafeName = "Lapan Puluh Coffee",
            category = EventCategory.PROMO,
            schedule = "Setiap Hari • 08.00 - 16.00 WIB",
            description = "Tunjukkan KTM UB/UM/UMM aktif untuk menikmati potongan 20% pada semua menu."
        ),
        EventPromo(
            id = "e7",
            title = "Sunset Chill Beats & Coffee",
            cafeName = "Senja Coffee & View",
            category = EventCategory.EVENT,
            schedule = "Jum, 26 Des 2025 • 16.30 WIB",
            description = "Sesi ngopi sore menikmati sunset dengan live DJ set santai khas pegunungan Malang."
        ),
        EventPromo(
            id = "e8",
            title = "Buy 1 Get 1 Pastry Choice",
            cafeName = "Griya Coffee & Bites",
            category = EventCategory.PROMO,
            schedule = "Sab - Ming • 08.00 - 11.00 WIB",
            description = "Beli 1 croissant atau pastry manis gratis 1 pastry segar pilihan hari ini."
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
        ),
        Review(
            id = "r4",
            userName = "Fajar Hidayat",
            cafeName = "Kopi Studio 24 Suhat",
            rating = 5,
            comment = "Tempat andalan buat ngerjain skripsi malam-malam. WiFi kenceng banget dan colokan aman.",
            favoriteMenu = "Es Kopi Aren 24",
            dateLabel = "3 hari lalu"
        ),
        Review(
            id = "r5",
            userName = "Nabila Putri",
            cafeName = "Senja Coffee & View",
            rating = 5,
            comment = "Gak pernah bosan ke sini! Pemandangan sore pas sunset keren banget, adem dan romantis.",
            favoriteMenu = "Matcha Cream Latte",
            dateLabel = "4 hari lalu"
        ),
        Review(
            id = "r6",
            userName = "Rizky Ramadhan",
            cafeName = "Latar Ijen Roastery",
            rating = 5,
            comment = "Coffee shop paling estetis di Malang saat ini. Espresso blend-nya acid-nya pas dan wangi.",
            favoriteMenu = "Piccolo Latte",
            dateLabel = "1 minggu lalu"
        ),
        Review(
            id = "r7",
            userName = "Maya Indah",
            cafeName = "Oemah Heretan Vintage",
            rating = 4,
            comment = "Suasananya sangat homey kayak rumah nenek. Pisang goreng kayumanisnya enak parah!",
            favoriteMenu = "Kopi Tubruk Rempah",
            dateLabel = "2 minggu lalu"
        ),
        Review(
            id = "r8",
            userName = "Andi Wijaya",
            cafeName = "Lapan Puluh Coffee",
            rating = 4,
            comment = "Lokasi strategis dekat kampus UB & UMM. Tempat luas dan harga mahasiswa.",
            favoriteMenu = "Es Kopi Susu Kampus",
            dateLabel = "2 minggu lalu"
        )
    )
}
