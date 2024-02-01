package com.sonova.android.permissionrequester.test

/**
 * Permission configuration for mocking
 *
 * @property permission the permission that shall be mocked
 * @property initiallyGranted if the permission is already granted or not.
 *                           No further checks will be done if set to true
 * @property userGranted if the user would click on "Allow" in the system permission dialog
 * @property shouldShowRationale in case that is is not initially granted, shall a rationale dialog show.
 */
data class PermissionTestConfiguration(
    val permission: String,
    val initiallyGranted: Boolean,
    val userGranted: Boolean = initiallyGranted,
    val shouldShowRationale: Boolean = false
)