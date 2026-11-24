package com.ngopimalang.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngopimalang.app.features.directory.CafeRepository
import com.ngopimalang.app.features.reviews.ReviewRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Opt-in test: credentials come from instrumentation arguments, never from source control. */
@RunWith(AndroidJUnit4::class)
class DirectoryCloudUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        compose.waitUntil(60_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForSavedReview(id: String, comment: String) {
        // The dialog and saved card contain the same text during the async save.
        // The card's identity distinguishes a completed reload from the editor.
        val card = hasTestTag("cafe_review_$id") and hasText(comment)
        compose.waitUntil(60_000) {
            compose.onAllNodes(card).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(card).performScrollTo().assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "$name.png")
        file.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun saveForm() {
        compose.onNodeWithText("Simpan cafe").performScrollTo().assertIsEnabled()
        // Focus/IME bring-into-view can move a long form during injected pointer events.
        // Invoke the button's accessibility action to test the actual save callback deterministically.
        compose.onNodeWithText("Simpan cafe").performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun signInThroughUi(email: String, password: String) {
        compose.waitUntil(60_000) {
            compose.onAllNodesWithText("Masuk").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Profil").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Profil").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Profil").performClick()
            waitForText("Keluar")
            compose.onNodeWithText("Keluar").performScrollTo().performClick()
            waitForText("Masuk")
        }
        compose.onNode(hasText("Email") and hasSetTextAction()).performTextReplacement(email)
        compose.onNode(hasText("Password") and hasSetTextAction()).performTextReplacement(password)
        compose.onNodeWithText("Masuk").performClick()
        waitForText("Direktori")
    }

    @Test fun adminCanReadSearchOpenFormsAndCreateDeleteCafe() {
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString("qaEmail").orEmpty()
        val password = args.getString("qaPassword").orEmpty()
        assumeTrue("Supply qaEmail and qaPassword to run the live Cloud test", email.isNotBlank() && password.isNotBlank())
        signInThroughUi(email, password)
        val temporaryName = "QA UI Ossa ${UUID.randomUUID().toString().take(8)}"
        try {
            waitForText("Direktori")
            compose.onNodeWithText("Direktori").performClick()
            waitForText("12 cafe ditemukan")
            compose.onNodeWithText("Tambah cafe").assertIsDisplayed()
            screenshot("qa-directory-cloud")

            compose.onNode(hasSetTextAction()).performTextReplacement("Kopi Sae Malang")
            waitForText("1 cafe ditemukan")
            compose.onNode(hasText("Kopi Sae Malang") and !hasSetTextAction()).performClick()
            waitForText("Buka di peta")
            compose.onNodeWithContentDescription("Foto Kopi Sae Malang").assertExists()
            screenshot("qa-detail-cloud")
            compose.onNodeWithText("Edit cafe").performScrollTo().performClick()
            waitForText("Edit Cafe")
            compose.waitUntil(180_000) {
                compose.onAllNodes(hasText("Kopi Sae Malang") and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
            }
            val unsavedName = "Kopi Sae Malang uji pemulihan"
            compose.onNode(hasText("Nama cafe *") and hasSetTextAction()).performTextReplacement(unsavedName)
            compose.activityRule.scenario.recreate()
            waitForText("Edit Cafe")
            compose.onNode(hasText("Nama cafe *") and hasSetTextAction()).assertTextContains(unsavedName)
            screenshot("qa-edit-restored")
            compose.onNodeWithText("Kembali").performScrollTo().performClick()
            waitForText("Buka di peta")
            compose.onNodeWithText("Kembali").performScrollTo().performClick()
            waitForText("Direktori Cafe")
            compose.onNode(hasSetTextAction()).performTextReplacement("")
            waitForText("12 cafe ditemukan")

            compose.onNodeWithText("Ada Wi-Fi").performClick()
            waitForText("10 cafe ditemukan")
            compose.onNodeWithText("Ada Wi-Fi").performClick()
            waitForText("12 cafe ditemukan")
            compose.onNodeWithText("Tambah cafe").performClick()
            waitForText("Tambah Cafe")
            compose.waitUntil(180_000) {
                compose.onAllNodes(hasText("Nama cafe *") and hasSetTextAction() and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            saveForm()
            waitForText("Nama cafe wajib diisi")
            compose.onNode(hasText("Nama cafe *") and hasSetTextAction()).performScrollTo().performTextReplacement(temporaryName)
            screenshot("qa-add-form")
            saveForm()
            waitForText("Direktori Cafe")
            compose.onNode(hasSetTextAction()).performTextReplacement(temporaryName)
            waitForText("1 cafe ditemukan")
            compose.onNode(hasText(temporaryName) and !hasSetTextAction()).performClick()
            waitForText("Edit cafe")
            compose.onNodeWithText("Edit cafe").performScrollTo().performClick()
            waitForText("Edit Cafe")
            val note = "Catatan uji layar Ossa"
            compose.waitUntil(60_000) {
                compose.onAllNodes(hasText("Deskripsi") and hasSetTextAction() and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(hasText("Deskripsi") and hasSetTextAction()).performScrollTo().performTextReplacement(note)
            saveForm()
            waitForText("Direktori Cafe")
            waitForText("1 cafe ditemukan")
            compose.onNode(hasText(temporaryName) and !hasSetTextAction()).performClick()
            waitForText("Edit cafe")
            waitForText(note)
            compose.onNodeWithText("Hapus cafe").performScrollTo().performClick()
            waitForText("Hapus cafe?")
            compose.onNodeWithText("Hapus", substring = false).performClick()
            waitForText("Direktori Cafe")
            waitForText("0 cafe ditemukan")
            compose.onNode(hasSetTextAction()).performTextReplacement("")
            waitForText("12 cafe ditemukan")
        } finally {
            // Only this test's uniquely named fixture is eligible for cleanup.
            runBlocking {
                val repo = CafeRepository()
                repo.list(temporaryName, false, false, true).filter { it.name == temporaryName }.forEach {
                    repo.delete(it.id, it.photoPath)
                }
            }
        }
    }

    @Test fun ordinaryUserCanOpenCloudCafeFromHomeAndCannotSeeAdminActions() {
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString("qaUserEmail").orEmpty()
        val password = args.getString("qaUserPassword").orEmpty()
        assumeTrue("Supply ordinary test user credentials", email.isNotBlank() && password.isNotBlank())
        signInThroughUi(email, password)
        compose.onNodeWithTag("home_feed").performScrollToNode(hasTestTag("home_cafes"))
        val recommendedCafe = hasText("Bara Coffee House") and hasAnyAncestor(hasTestTag("home_cafes"))
        compose.waitUntil(60_000) { compose.onAllNodes(recommendedCafe).fetchSemanticsNodes().isNotEmpty() }
        screenshot("qa-user-home")
        compose.onNode(recommendedCafe).performClick()
        waitForText("Detail Cafe")
        waitForText("Buka di peta")
        compose.onAllNodesWithText("Edit cafe").assertCountEquals(0)
        compose.onAllNodesWithText("Hapus cafe").assertCountEquals(0)
        screenshot("qa-user-detail")
        compose.onNodeWithText("Kembali").performScrollTo().performClick()
        waitForText("Direktori")
        compose.onNodeWithText("Direktori").performClick()
        waitForText("12 cafe ditemukan")
        compose.onAllNodesWithText("Tambah cafe").assertCountEquals(0)
        screenshot("qa-user-directory")
    }

    @Test fun ordinaryUserReviewPersistsAndAppearsOnHomeAndDetail() {
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString("qaUserEmail").orEmpty()
        val password = args.getString("qaUserPassword").orEmpty()
        assumeTrue("Supply ordinary test user credentials", email.isNotBlank() && password.isNotBlank())
        signInThroughUi(email, password)
        val repo = ReviewRepository()
        val cafe = runBlocking { CafeRepository().list("Bara Coffee House", false, false, false).single() }
        // This opt-in test never overwrites an existing review, even for the QA account.
        assertNull(runBlocking { repo.loadMyReview(cafe.id) })
        val baseline = runBlocking { repo.cafeRating(cafe.id) }.reviewCount
        val comment = "QA ulasan Ossa ${UUID.randomUUID().toString().take(8)}"
        try {
            compose.onNodeWithTag("home_feed").performScrollToNode(hasTestTag("home_cafes"))
            val recommended = hasText(cafe.name) and hasAnyAncestor(hasTestTag("home_cafes"))
            compose.waitUntil(60_000) { compose.onAllNodes(recommended).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(recommended).performClick()
            waitForText("Tulis Ulasan")
            compose.onNodeWithText("Tulis Ulasan").performScrollTo().performClick()
            waitForText("Beri ulasan")
            compose.onNodeWithText("Simpan").assertIsNotEnabled()
            compose.onNodeWithContentDescription("5 Bintang").performClick()
            compose.onNode(hasText("Menu Favorit (opsional)") and hasSetTextAction()).performTextReplacement("Kopi susu QA")
            compose.onNode(hasText("Komentar (opsional)") and hasSetTextAction()).performTextReplacement(comment)
            compose.onNodeWithText("Simpan").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(60_000) {
                compose.onAllNodesWithContentDescription("Hapus ulasan saya").fetchSemanticsNodes().isNotEmpty()
            }
            val savedReview = requireNotNull(runBlocking { repo.loadMyReview(cafe.id) })
            assertEquals(comment, savedReview.comment)
            waitForSavedReview(savedReview.id, comment)
            assertEquals(baseline + 1, runBlocking { repo.cafeRating(cafe.id) }.reviewCount)
            screenshot("qa-review-created")

            compose.onNodeWithText("Kembali").performScrollTo().performClick()
            compose.onNodeWithTag("home_feed").performScrollToNode(hasTestTag("home_reviews"))
            val homeReview = hasText(comment) and hasAnyAncestor(hasTestTag("home_reviews"))
            compose.waitUntil(60_000) { compose.onAllNodes(homeReview).fetchSemanticsNodes().isNotEmpty() }
            val expectedRating = runBlocking { repo.cafeRating(cafe.id) }.rating
            val homeRating = hasText(" $expectedRating") and hasAnyAncestor(hasTestTag("home_cafes"))
            compose.waitUntil(60_000) { compose.onAllNodes(homeRating).fetchSemanticsNodes().isNotEmpty() }
            screenshot("qa-home-real-review")
            compose.onNode(homeReview).performClick()
            waitForText("Ulasan Saya")
            waitForText(comment)
            compose.onNodeWithContentDescription("Kembali").performClick()

            compose.onNodeWithTag("home_feed").performScrollToNode(hasTestTag("home_cafes"))
            compose.onNode(recommended).performClick()
            waitForText(comment)
            compose.onAllNodesWithText("Edit ulasan")[0].performScrollTo().performClick()
            waitForText("Komentar (opsional)")
            compose.onNodeWithContentDescription("3 Bintang").performClick()
            compose.onNode(hasText("Komentar (opsional)") and hasSetTextAction()).performTextReplacement("$comment diedit")
            compose.onNodeWithText("Simpan").performSemanticsAction(SemanticsActions.OnClick) { it() }
            waitForSavedReview(savedReview.id, "$comment diedit")
            assertEquals(3, runBlocking { repo.loadMyReview(cafe.id) }?.rating)
            assertEquals(baseline + 1, runBlocking { repo.cafeRating(cafe.id) }.reviewCount)
            screenshot("qa-review-edited")
            compose.onNodeWithContentDescription("Hapus ulasan saya").performScrollTo().performClick()
            waitForText("Tulis Ulasan")
            assertNull(runBlocking { repo.loadMyReview(cafe.id) })
            assertEquals(baseline, runBlocking { repo.cafeRating(cafe.id) }.reviewCount)
        } finally {
            runBlocking {
                val mine = repo.loadMyReview(cafe.id)
                if (mine != null && mine.comment.startsWith(comment)) repo.deleteMyReview(cafe.id)
            }
        }
    }
}
