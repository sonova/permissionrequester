package com.sonova.android.permissionrequester

public fun interface PermissionSnapshotLogger {
    public fun log(granted: List<String>, denied: List<String>)
}

internal object NoLog : PermissionSnapshotLogger {
    override fun log(granted: List<String>, denied: List<String>) {
        // no logging desired
    }
}
