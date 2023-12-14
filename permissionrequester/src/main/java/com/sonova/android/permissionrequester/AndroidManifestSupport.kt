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
public object AndroidManifestSupport {

    /**
     * Gathers permissions declared in AndroidManifest.xml that
     * have not been removed by the {@link android.os.Build.VERSION.SDK_INT}
     * and checks that permission is actually requested.
     *
     * @param context android context
     * @param permission going to be requested
     * @return permission is requested in AndroidManifest.xml
     */
    public fun isPermissionRequested(context: Context, permission: String): Boolean {
        return permission in getPermissionsRequested(context)
    }

    private fun getPermissionsRequested(context: Context): List<String> {
        return context.getPackageInfo(PackageManager.GET_PERMISSIONS)
            ?.requestedPermissions?.toList().orEmpty()
    }

    private fun Context.getPackageInfo(flags: Int = 0): PackageInfo? =
        if (VersionChecker.isBuildVersionUpwards(Build.VERSION_CODES.TIRAMISU)) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            packageManager.getPackageInfo(packageName, flags)
        }
}
