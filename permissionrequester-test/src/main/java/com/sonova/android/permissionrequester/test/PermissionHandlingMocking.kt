package com.sonova.android.permissionrequester.test

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.sonova.android.permissionrequester.AndroidManifestSupport
import com.sonova.android.permissionrequester.PermissionRequester
import com.sonova.android.permissionrequester.VersionChecker
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot

/**
 * Mocks permission grant status for the configured permissions.
 * Depending on the buildVersion some checks will not be done.
 *
 * @param permissionTestConfig permissions that will be specifically mocked
 * @param buildVersion for specific permissions that appear in a newer android version
 * @param globalLocationPermissionEnabled if global location permission shall be enabled or not
 * @param defaultPermissionStatusGranted per default all permission are not granted
 */
fun mockPermissions(
    permissionTestConfig: List<PermissionTestConfiguration> = emptyList(),
    buildVersion: Int = Build.VERSION.SDK_INT,
    globalLocationPermissionEnabled: Boolean = true,
    defaultPermissionStatusGranted: Boolean = false
) {
    mockkStatic(ContextCompat::class)
    mockkStatic(ActivityCompat::class)
    mockkObject(AndroidManifestSupport)

    every { AndroidManifestSupport.isPermissionRequested(any(), any()) } returns true

    mockBuildVersion(buildVersion)
    mockGlobalLocationStatus(globalLocationPermissionEnabled)
    mockDefaultGrantPermissionStatus(defaultPermissionStatusGranted)
    permissionTestConfig.forEach { it.mockkPermission() }
    mockPermissionRequester(permissionTestConfig)
}

private fun mockDefaultGrantPermissionStatus(defaultPermissionStatusGranted: Boolean) {
    every {
        ContextCompat.checkSelfPermission(
            any(),
            any()
        )
    } returns defaultPermissionStatusGranted.toGrantStatus()
}

private fun mockBuildVersion(currentBuildVersion: Int) {
    mockkObject(VersionChecker)
    every { VersionChecker.isBuildVersionUpwards(any()) } answers {
        currentBuildVersion >= firstArg<Int>()
    }
}

@SuppressLint("NewApi")
private fun mockGlobalLocationStatus(globalLocationPermissionEnabled: Boolean) {
    every {
        ContextCompat.getSystemService(any(), LocationManager::class.java)
    } returns mockk { every { isLocationEnabled } returns globalLocationPermissionEnabled }
}

private fun PermissionTestConfiguration.mockkPermission() {
    val returnValuesForPermission = listOf(
        initiallyGranted,
        userGranted,
    ).map { it.toGrantStatus() }
    every {
        ContextCompat.checkSelfPermission(
            any(),
            permission
        )
    } returnsMany returnValuesForPermission
    every {
        ActivityCompat.shouldShowRequestPermissionRationale(any(), permission)
    } returns shouldShowRationale
}

private fun mockPermissionRequester(config: List<PermissionTestConfiguration>) {
    with(PermissionRequester.Builder()) {
        val expectedResult = config.associate { it.permission to it.userGranted }
        registerRegistry(PermissionMockRegistry(expectedResult))
        mockkConstructor(PermissionRequester.Builder::class)
        returnThisWhenRequiringGlobalLocationStatus()
        returnThisWhenRequestingPermissions()
    }
}

private class PermissionMockRegistry(private val expectedResult: Map<String, Boolean>) :
    ActivityResultRegistry() {
    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?
    ) {
        when (contract) {
            is ActivityResultContracts.RequestMultiplePermissions -> dispatchResult(
                requestCode,
                expectedResult
            )

            is ActivityResultContracts.StartActivityForResult -> {
                if (input is Intent) {
                    ApplicationProvider.getApplicationContext<Context>()
                        .startActivity(input.apply { addFlags(FLAG_ACTIVITY_NEW_TASK) })
                    dispatchResult(requestCode, ActivityResult(Activity.RESULT_OK, Intent()))
                }
            }
        }
    }
}

private fun Boolean.toGrantStatus() =
    if (this) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED

private fun PermissionRequester.Builder.returnThisWhenRequestingPermissions() {
    val dialogConfiguration =
        mutableListOf<PermissionRequester.DialogConfigurationBuilder.() -> Unit>()
    val multiplePermissions = slot<List<String>>()
    every {
        anyConstructed<PermissionRequester.Builder>()
            .requirePermissions(
                capture(dialogConfiguration),
                capture(dialogConfiguration),
                capture(multiplePermissions)
            )
    } answers {
        requirePermissions(
            dialogConfiguration[dialogConfiguration.lastIndex - 1],
            dialogConfiguration.last(),
            multiplePermissions.captured
        )
    }
}

private fun PermissionRequester.Builder.returnThisWhenRequiringGlobalLocationStatus() {
    val dialogConfiguration =
        mutableListOf<PermissionRequester.DialogConfigurationBuilder.() -> Unit>()
    every {
        anyConstructed<PermissionRequester.Builder>()
            .requireGlobalLocationEnabled(capture(dialogConfiguration))
    } answers {
        requireGlobalLocationEnabled(dialogConfiguration.last())
    }
}