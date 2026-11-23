
package com.ngopimalang.app.features.bookmark

data class BookmarkItem(
    val id: String,
    val cafeId: String,
    val cafeName: String,
    val note: String = "",
    val savedAtLabel: String = "Baru disimpan"
)
