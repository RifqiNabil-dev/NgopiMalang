package com.ngopimalang.app.features.directory

import org.junit.Assert.*
import org.junit.Test

class DirectoryViewModelTest {
    @Test fun validationRejectsInvalidPricesAndBlankName() {
        val errors = validateCafeForm(CafeFormState(name = " ", min = "30000", max = "20000", plugs = "-1"))
        assertTrue("name" in errors); assertTrue("max" in errors); assertTrue("plugs" in errors)
    }
    @Test fun optionalPricesStayNullAndNoReviewStaysNull() {
        val row = CafeRow(id = "id", name = "Cafe")
        val cafe = CafeMapper.toModel(row)
        assertNull(cafe.priceMin); assertNull(cafe.priceMax); assertNull(cafe.rating)
        assertEquals("Harga belum tersedia", priceLabel(null, null))
    }
    @Test fun partialPriceHasNoDanglingSeparator() {
        assertFalse(priceLabel(15000, null).endsWith("–"))
        assertTrue(priceLabel(null, 20000).startsWith("Hingga"))
    }
    @Test fun writePayloadPreservesNullablePricesAndRequiredTextColumns() {
        val write = CafeMapper.toWrite(CafeDraft(name = " Kopi "))
        assertEquals("Kopi", write.name); assertNull(write.priceMin); assertNull(write.priceMax)
        assertEquals("", write.address); assertEquals("", write.description); assertEquals("", write.openHours)
    }
    @Test fun validationAcceptsOptionalPricesButRejectsOverflow() {
        assertTrue(validateCafeForm(CafeFormState(name = "Kopi", loading = false)).isEmpty())
        assertTrue("min" in validateCafeForm(CafeFormState(name = "Kopi", min = "99999999999999")))
    }
}
