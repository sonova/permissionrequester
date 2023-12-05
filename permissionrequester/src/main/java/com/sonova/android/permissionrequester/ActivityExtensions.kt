package com.sonova.android.permissionrequester

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal fun Activity.startActionApplicationDetailsSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

internal fun ComponentActivity.showMaterialDialog(
    @StringRes titleResId: Int?,
    @StringRes messageResId: Int?,
    @StringRes acceptButtonNameResId: Int,
    onAccept: DialogInterface.OnClickListener?,
    @StringRes declineButtonNameResId: Int? = null,
    onDecline: DialogInterface.OnClickListener? = null,
    @StringRes cancelButtonNameResId: Int? = null,
    onCancel: DialogInterface.OnClickListener? = null,
    isDialogCancelable: Boolean = true
): AlertDialog? {
    return MaterialAlertDialogBuilder(this).apply {
        titleResId?.let { setTitle(it) }
        messageResId?.let { setMessage(it) }
        setPositiveButton(acceptButtonNameResId, onAccept)
        declineButtonNameResId?.also { setNegativeButton(it, onDecline) }
        cancelButtonNameResId?.also { setNeutralButton(it, onCancel) }
        setCancelable(isDialogCancelable)
    }.show().also {
        lifecycle.addObserver(Observer(lifecycle, it))
    }
}

private class Observer(
    private val lifecycle: Lifecycle,
    private val alertDialog: AlertDialog
) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        lifecycle.removeObserver(this)
        alertDialog.dismiss()
    }
}
