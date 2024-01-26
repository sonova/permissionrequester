package com.sonova.android.permissionrequester.test

data class PermissionTestConfiguration(
    val permission: String,
    val initiallyGranted: Boolean,
    val userGranted: Boolean = initiallyGranted,
    val shouldShowRationale: Boolean = false
)