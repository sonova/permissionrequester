package com.sonova.android.permissionrequester

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Wrapper object for reading the permissions declared in the manifest.
 * Useful when requesting permissions that are not necessary for the
 * current API level (e.g. {@link android.Manifest.permission.BLUETOOTH_CONNECT})
 */
public object ManifestPermissionsProvider {

    /**
     * Gathers permissions declared in AndroidManifest.xml that
     * have not been removed by the {@link android.os.Build.VERSION.SDK_INT}.
     *
     * @param context android context
     * @return list of the declared permissions
     */
    public fun getRequestedPermissions(context: Context): List<String> {
        return context.getPackageInfo()
            ?.requestedPermissions?.toList().orEmpty()
    }

    private fun Context.getPackageInfo(): PackageInfo? =
        if (VersionChecker.isBuildVersionUpwards(Build.VERSION_CODES.TIRAMISU)) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }
}
