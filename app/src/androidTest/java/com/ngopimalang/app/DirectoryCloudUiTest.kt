package com.ngopimalang.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngopimalang.app.features.directory.CafeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
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
}
