package com.sonova.android.permissionrequester

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sonova.android.permissionrequester.PermissionRequester.Builder
import com.sonova.android.permissionrequester.PermissionRequester.DialogConfigurationBuilder
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PermissionRequesterTest {
    private val permission1 = "permission1"
    private val permission2 = "permission2"
    private val permission3 = "permission3"

    private val permissionRequestLauncher = mockk<ActivityResultLauncher<Array<String>>>()
    private val settingActivityLauncher = mockk<ActivityResultLauncher<Intent>>()

    private val activity: ComponentActivity = mockk(relaxed = true)
    private val callback: () -> Unit = mockk(relaxed = true, relaxUnitFun = true)
    private val rationaleDialogConfig: DialogConfigurationBuilder.() -> Unit = {
        titleResId = R.string.test_rationale_title
        messageResId = R.string.test_rationale_description
    }
    private val settingsDialogConfig: DialogConfigurationBuilder.() -> Unit = {
        titleResId = R.string.test_settings_title
        messageResId = R.string.test_settings_description
    }
    private val logger = mockk<PermissionSnapshotLogger>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
        mockkStatic(ActivityCompat::class)
        mockkObject(VersionChecker)
        mockkStatic("com.sonova.android.permissionrequester.ActivityExtensionsKt")
        mockShowingDialog()
        mockManifestPermissionProvider(true)
        mockPermissionsLauncher()
    }

    @Test
    fun `test that only denied permissions get requested again`() {
        permission1.deny()
        permission2.grant()
        permission3.deny()

        Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1, permission2, permission3)
            ).build(activity, callback).request()

        verify { permissionRequestLauncher.launch(arrayOf(permission1, permission3)) }
    }

    @Test
    fun `test when no global location permission dialog config passed, then don't check location`() {
        permission1.deny()

        Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1)
            ).build(activity, callback).request()

        verify(inverse = true) {
            ContextCompat.getSystemService(
                any(),
                LocationManager::class.java
            )
        }
    }

    @SuppressLint("NewApi")
    @Test
    fun `test when global location permission dialog config passed, then check location`() {
        every { VersionChecker.isBuildVersionUpwards(any()) } returns true
        every { ContextCompat.getSystemService(any(), LocationManager::class.java) } returns mockk {
            every { isLocationEnabled } returns false
        }
        mockSettingsLauncher()
        permission1.deny()

        Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1)
            ).requireGlobalLocationEnabled {
                titleResId = R.string.test_settings_title
                messageResId = R.string.test_settings_description
                positiveButtonNameResId = R.string.default_settings_invitation_button_positive
                negativeButtonNameResId = R.string.default_rationale_dialog_button_negative
            }
            .build(activity, callback).request()

        verify {
            activity.showMaterialDialog(
                titleResId = R.string.test_settings_title,
                messageResId = R.string.test_settings_description,
                positiveButtonNameResId = R.string.default_settings_invitation_button_positive,
                onPositiveClick = any(),
                negativeButtonNameResId = R.string.default_rationale_dialog_button_negative
            )
        }
        verify(inverse = true) { permissionRequestLauncher.launch(any()) }
        verify { ContextCompat.getSystemService(any(), LocationManager::class.java) }
    }

    @Test
    fun `test if permission requires rationale, then show rationale first`() {
        permission1.grant()
        permission2.deny()
        permission3.deny()
        permission2.needsRationale()
        permission3.needsRationale()

        Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1, permission2, permission3)
            ).build(activity, callback).request()

        verifyRationaleShown()
        verify(inverse = true) { permissionRequestLauncher.launch(any()) }
    }

    @Test
    fun `test when all permissions granted then call callback directly`() {
        permission1.grant()
        permission2.grant()
        permission3.grant()

        Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1, permission2, permission3)
            ).build(activity, callback).request()

        verify { callback.invoke() }
        verify(inverse = true) { permissionRequestLauncher.launch(any()) }
    }

    @Test
    fun `test that if all permissions granted then allRequiredPermissionsGranted returns true`() {
        permission1.grant()
        permission2.grant()
        permission3.grant()

        val permissionRequester = Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1, permission2, permission3)
            ).build(activity, callback)

        assertTrue(permissionRequester.areAllRequiredPermissionsGranted())
    }

    @Test
    fun `test callback shows rationale when permission is not granted`() {
        permission1.grant()

        val slots = mutableListOf<ActivityResultCallback<Map<String, Boolean>>>()
        every {
            activity.registerForActivityResult(
                any<ActivityResultContracts.RequestMultiplePermissions>(),
                any(),
                capture(slots)
            )
        } returns permissionRequestLauncher

        Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1)
            ).build(activity, callback).request()

        permission1.deny()
        permission1.needsRationale()
        slots.first().onActivityResult(mapOf(permission1 to false))

        verifyRationaleShown()
        verify { logger.log(emptyList(), listOf(permission1)) }
    }

    @Test
    fun `test callback shows settings when permission is not granted`() {
        permission1.grant()

        val slots = mutableListOf<ActivityResultCallback<Map<String, Boolean>>>()
        every {
            activity.registerForActivityResult(
                any<ActivityResultContracts.RequestMultiplePermissions>(),
                any(),
                capture(slots)
            )
        } returns permissionRequestLauncher

        Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1)
            ).build(activity, callback).request()

        permission1.deny()
        slots.first().onActivityResult(mapOf(permission1 to false))

        verifySettingsDialogShown()
        verify { logger.log(emptyList(), listOf(permission1)) }
    }

    @Test
    fun `test permissions are not re-requested if callback did not get any response`() {
        val slots = mutableListOf<ActivityResultCallback<Map<String, Boolean>>>()
        every {
            activity.registerForActivityResult(
                any<ActivityResultContracts.RequestMultiplePermissions>(),
                any(),
                capture(slots)
            )
        } returns permissionRequestLauncher

        Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1, permission2)
            ).build(activity, callback).request()

        slots.first().onActivityResult(mapOf())

        verify(exactly = 2) { ContextCompat.checkSelfPermission(any(), permission1) }
        verify(exactly = 2) { ContextCompat.checkSelfPermission(any(), permission2) }
        verify(exactly = 1) { logger.log(any(), any()) }
    }

    @Test
    fun `test when user grants all permissions then invoke callback`() {
        permission1.grant()

        val slots = mutableListOf<ActivityResultCallback<Map<String, Boolean>>>()
        every {
            activity.registerForActivityResult(
                any<ActivityResultContracts.RequestMultiplePermissions>(),
                any(),
                capture(slots)
            )
        } returns permissionRequestLauncher

        Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1)
            ).build(activity, callback).request()
        slots.first().onActivityResult(mapOf(permission1 to true))

        verify(exactly = 2) { callback.invoke() }
        verify { logger.log(any(), any()) }
    }

    @Test
    fun `test when none of the permissions is required then invoke callback directly`() {
        mockManifestPermissionProvider(false)

        Builder(logger)
            .requirePermissions(
                rationaleDialogConfig,
                settingsDialogConfig,
                listOf(permission1, permission2, permission3)
            ).build(activity, callback).request()

        verify { callback.invoke() }
        verify(inverse = true) { permissionRequestLauncher.launch(any()) }
    }

    private fun String.grant() {
        every {
            ContextCompat.checkSelfPermission(
                activity,
                this@grant
            )
        } returns PackageManager.PERMISSION_GRANTED
    }

    private fun String.deny() {
        every {
            ContextCompat.checkSelfPermission(
                activity,
                this@deny
            )
        } returns PackageManager.PERMISSION_DENIED
    }

    private fun String.needsRationale() {
        every {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                this@needsRationale
            )
        } returns true
    }

    private fun verifyRationaleShown() {
        verify {
            activity.showMaterialDialog(
                titleResId = R.string.test_rationale_title,
                messageResId = R.string.test_rationale_description,
                positiveButtonNameResId = R.string.default_rationale_dialog_button_positive,
                onPositiveClick = any(),
                negativeButtonNameResId = R.string.default_rationale_dialog_button_negative,
                onNegativeClick = null,
                cancelButtonNameResId = null,
                onCancelClick = null
            )
        }
    }

    private fun verifySettingsDialogShown() {
        verify {
            activity.showMaterialDialog(
                titleResId = R.string.test_settings_title,
                messageResId = R.string.test_settings_description,
                positiveButtonNameResId = R.string.default_settings_invitation_button_positive,
                onPositiveClick = any(),
                negativeButtonNameResId = null,
                onNegativeClick = null,
                cancelButtonNameResId = null,
                onCancelClick = null
            )
        }
    }

    private fun mockSettingsLauncher() {
        val slot = slot<ActivityResultCallback<ActivityResult>>()
        every {
            activity.registerForActivityResult(
                any<ActivityResultContracts.StartActivityForResult>(),
                any(),
                capture(slot)
            )
        } returns settingActivityLauncher

        every { settingActivityLauncher.launch(any()) } answers {
            slot.captured.onActivityResult(ActivityResult(1, Intent()))
        }
    }

    private fun mockPermissionsLauncher() {
        every {
            activity.registerForActivityResult(
                any<ActivityResultContracts.RequestMultiplePermissions>(),
                any(),
                any()
            )
        } returns permissionRequestLauncher

        justRun { permissionRequestLauncher.launch(any()) }
    }

    private fun mockShowingDialog() {
        justRun {
            activity.showMaterialDialog(
                titleResId = any(),
                messageResId = any(),
                positiveButtonNameResId = any(),
                onPositiveClick = any(),
                negativeButtonNameResId = any(),
                onNegativeClick = any(),
                cancelButtonNameResId = any(),
                onCancelClick = any()
            )
        }
    }

    private fun mockManifestPermissionProvider(permissionRequired: Boolean) {
        mockkObject(AndroidManifestSupport)
        every {
            AndroidManifestSupport.isPermissionRequested(
                any(),
                any()
            )
        } returns permissionRequired
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

}
