package com.sonova.android.permissionrequester

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Wrapper to allow overriding android version in testing
 */
public object VersionChecker {
    @ChecksSdkIntAtLeast(parameter = 0)
    public fun isBuildVersionUpwards(buildVersion: Int): Boolean =
        Build.VERSION.SDK_INT >= buildVersion
}
