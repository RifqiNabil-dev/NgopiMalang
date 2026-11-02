package com.ngopimalang.app.features.directory

import org.junit.Assert.*
import org.junit.Test

class DirectoryViewModelTest {
    @Test fun searchMatchesNameAndAddressIgnoringCaseAndOuterSpaces() {
        val model = DirectoryViewModel()
        model.onSearchChange(" SAE ")
        assertEquals(listOf("c1"), model.uiState.value.cafes.map { it.id })
        model.onSearchChange(" SEMARANG ")
        assertEquals(listOf("c3"), model.uiState.value.cafes.map { it.id })
    }

    @Test fun filtersCombineWithSearchAndResetRestoresAllCafes() {
        val model = DirectoryViewModel()
        model.onSearchChange("Ruang")
        model.togglePlugs()
        assertEquals(listOf("c3"), model.uiState.value.cafes.map { it.id })
        model.toggleWifi()
        assertTrue(model.uiState.value.cafes.isEmpty())
        model.resetFilters()
        assertEquals(6, model.uiState.value.cafes.size)
        assertEquals("", model.uiState.value.query)
        assertFalse(model.uiState.value.wifiOnly)
        assertFalse(model.uiState.value.plugsOnly)
    }

    @Test fun detailsAreIndependentOfActiveSearchAndHandleUnknownIds() {
        val model = DirectoryViewModel()
        model.onSearchChange("tidak ditemukan")
        assertTrue(model.uiState.value.cafes.isEmpty())
        assertEquals("Kopi Sae Malang", model.findCafe("c1")?.name)
        assertNull(model.findCafe("unknown"))
        assertNull(model.findCafe(null))
    }
}
