package com.beast.app.diagnostics

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestPermissionsTest {
    @Test
    fun `manifest does not request internet permission`() {
        val manifestPath = Paths.get("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml should exist", Files.exists(manifestPath))
        val manifestText = manifestPath.readText()
        assertFalse(
            "INTERNET permission must remain absent to guarantee offline-first behavior",
            manifestText.contains("android.permission.INTERNET")
        )
    }
}
