package com.sonova.android.permissionrequester

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Helper function for managing android versions
 */
public object BuildVersionProvider {
    public val isMOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.M)
    public val isNOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.N)
    public val isOOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.O)
    public val isPOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.P)
    public val isQOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.Q)
    public val isROrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.R)
    public val isSOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.S)
    public val isTOrAbove: Boolean get() = isBuildVersionUpwards(Build.VERSION_CODES.TIRAMISU)
    public val isUOrAbove: Boolean get() = isBuildVersionUpwards(
        Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    )

    public val isROrBelow: Boolean get() = isBuildVersionDownwards(Build.VERSION_CODES.R)

    @ChecksSdkIntAtLeast(parameter = 0)
    private fun isBuildVersionUpwards(buildVersion: Int): Boolean =
        Build.VERSION.SDK_INT >= buildVersion

    @ChecksSdkIntAtLeast(parameter = 0)
    private fun isBuildVersionDownwards(buildVersion: Int): Boolean =
        Build.VERSION.SDK_INT <= buildVersion
}
