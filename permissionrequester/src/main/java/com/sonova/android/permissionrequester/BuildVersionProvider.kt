package com.sonova.android.permissionrequester

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Helper function for managing android versions
 */
object BuildVersionProvider {
    val isMOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.M)
    val isNOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.N)
    val isOOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.O)
    val isPOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.P)
    val isQOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.Q)
    val isROrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.R)
    val isSOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.S)
    val isTOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.TIRAMISU)
    val isUOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

    val isROrBelow: Boolean get() = isBuildVersionDownwards(Build.VERSION_CODES.R)

    @ChecksSdkIntAtLeast(parameter = 0)
    private fun isBuildVersionUpwards(buildVersion: Int) = Build.VERSION.SDK_INT >= buildVersion

    @ChecksSdkIntAtLeast(parameter = 0)
    private fun isBuildVersionDownwards(buildVersion: Int) = Build.VERSION.SDK_INT <= buildVersion
}
