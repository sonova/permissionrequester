package com.sonova.android.permissionrequester

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Wrapper to allow overriding android version in testing
 */
public object VersionChecker {

    /**
     * Checks that the buildVersion is equal or higher than the sdk of the phone.
     *
     * @param buildVersion required API level is passed in first argument,
     * @return true if phone version >= buildVersion
     */
    @ChecksSdkIntAtLeast(parameter = 0)
    public fun isBuildVersionUpwards(buildVersion: Int): Boolean =
        Build.VERSION.SDK_INT >= buildVersion
}
