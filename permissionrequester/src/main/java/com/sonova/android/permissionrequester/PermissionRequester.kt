package com.sonova.android.permissionrequester

import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

public typealias PermissionRequestResult = Map<String, Boolean>

public class PermissionRequester private constructor(
    private val activity: AppCompatActivity,
    private val permissionsBeingRequested: List<PermissionRequestInformation>,
    private val globalLocationSettingEnableDialog: DialogConfiguration?,
    private val permissionGrantStatusCallback: () -> Unit,
    activityResultRegistry: ActivityResultRegistry
) {
    private val notGrantedPermissions: List<PermissionRequestInformation>
        get() = permissionsBeingRequested.filterNot { isPermissionGranted(it.permission) }

    private val permissionRequestCallback: (PermissionRequestResult) -> Unit =
        { permissionsResult ->
            val permissionRequestedAndNotGranted = permissionsBeingRequested
                .firstOrNull { permissionsResult[it.permission] == false }

            if (permissionRequestedAndNotGranted != null) {
                showRationalForPermission(permissionRequestedAndNotGranted)
            } else if (permissionsResult.isNotEmpty()) {
                request()
            }
        }

    private val permissionRequest = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        activityResultRegistry,
        permissionRequestCallback
    )

    private val navigateToGlobalLocationSetting =
        activity.registerForActivityResult(StartActivityForResult(), activityResultRegistry) {
            if (isGlobalLocationPermissionEnabled) request()
        }

    /**
     * Requests the permissions passed in the configuration.
     * If global location setting is set, then it will be handled first.
     * Afterwards, if the ignoreRationale flag is not set, the rationales
     * for already requested permissions are shown.
     * Finally, the permissions will be requested
     *
     * @param ignoreRationale disables showing the rationale before requesting the permission
     */
    @MainThread
    public fun request(ignoreRationale: Boolean = false) {
        if (globalLocationSettingEnableDialog != null && !isGlobalLocationPermissionEnabled) {
            showGlobalLocationPermissionDialog(globalLocationSettingEnableDialog)
        } else if (ignoreRationale) {
            requestPermissions()
        } else {
            val rationaleToShow = notGrantedPermissions
                .firstOrNull { needsRationale(it) }?.rationaleDialog

            if (rationaleToShow != null) {
                showRationaleDialog(rationaleToShow)
            } else {
                requestPermissions()
            }
        }
    }

    /**
     * Checks if all permissions requested have also been granted.
     *
     * @return true if all permissions are granted
     */
    public fun areAllRequiredPermissionsGranted(): Boolean {
        return notGrantedPermissions.isEmpty()
    }

    private fun showRationalForPermission(permission: PermissionRequestInformation) {
        if (needsRationale(permission)) {
            showRationaleDialog(permission.rationaleDialog)
        } else {
            showInvitationToSettingsDialog(permission.settingsInvitationDialog)
        }
    }

    private fun needsRationale(permissionRequest: PermissionRequestInformation): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            permissionRequest.permission
        )
    }

    private fun requestPermissions() {
        val permissionsToRequest = notGrantedPermissions.map { it.permission }
        if (permissionsToRequest.isEmpty()) {
            permissionGrantStatusCallback.invoke()
        } else {
            permissionRequest.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun showRationaleDialog(dialogConfig: DialogConfiguration) {
        activity.showMaterialDialog(
            titleResId = dialogConfig.titleResId,
            messageResId = dialogConfig.messageResId,
            acceptButtonNameResId = dialogConfig.positiveButtonNameResId,
            onAccept = { _, _ -> requestPermissions() },
            declineButtonNameResId = dialogConfig.negativeButtonNameResId,
            isDialogCancelable = false
        )
    }

    private fun showInvitationToSettingsDialog(dialogConfig: DialogConfiguration) {
        activity.showMaterialDialog(
            titleResId = dialogConfig.titleResId,
            messageResId = dialogConfig.messageResId,
            acceptButtonNameResId = dialogConfig.positiveButtonNameResId,
            onAccept = { dialog, _ ->
                dialog.dismiss()
                activity.startActionApplicationDetailsSettings()
            },
            cancelButtonNameResId = dialogConfig.negativeButtonNameResId
                .takeIf { dialogConfig.hasCancelButton },
            isDialogCancelable = false
        )
    }

    private fun showGlobalLocationPermissionDialog(dialogConfig: DialogConfiguration) {
        activity.showMaterialDialog(
            titleResId = dialogConfig.titleResId,
            messageResId = dialogConfig.messageResId,
            acceptButtonNameResId = dialogConfig.positiveButtonNameResId,
            onAccept = { _, _ ->
                navigateToGlobalLocationSetting.launch(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            },
            declineButtonNameResId = dialogConfig.negativeButtonNameResId
        )
    }

    private val isGlobalLocationPermissionEnabled: Boolean
        get() = !BuildVersionProvider.isPOrAbove || ContextCompat.getSystemService(
            activity,
            LocationManager::class.java
        )?.isLocationEnabled == true

    private fun isPermissionGranted(permission: String): Boolean {
        return !isPermissionRequired(permission) ||
            ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isPermissionRequired(permission: String): Boolean {
        return permission in ManifestPermissionsProvider.getRequestedPermissions(activity)
    }

    public class Builder {
        private var globalLocationPermissionEnableDialog: DialogConfiguration? = null
        private val permissionsToRequest = mutableListOf<PermissionRequestInformation>()
        private var activityResultRegistry: ActivityResultRegistry? = null

        /**
         * Builder for permission request
         *
         * @param rationaleBuilder dialog configuration when showing a rationale
         * @param settingsInvitationBuilder dialog configuration when inviting to allow the permission in settings
         * @param permissions list of permissions applicable for dialog configuration
         * @return b®uilder
         */
        public fun requirePermissions(
            rationaleBuilder: DialogConfigurationBuilder.() -> Unit,
            settingsInvitationBuilder: DialogConfigurationBuilder.() -> Unit,
            permissions: List<String>
        ): Builder {
            val rationaleDialog = DialogConfigurationBuilder(
                positiveButtonNameResId = R.string.default_rationale_dialog_button_accept,
                negativeButtonNameResId = R.string.default_rationale_dialog_button_decline,
                hasCancelButton = true
            ).apply(rationaleBuilder).build()
            val settingsInvitationDialog = DialogConfigurationBuilder(
                positiveButtonNameResId = R.string.default_settings_invitation_button_accept,
                negativeButtonNameResId = R.string.default_settings_invitation_button_cancel,
                hasCancelButton = false
            ).apply(settingsInvitationBuilder).build()
            permissions.mapTo(permissionsToRequest) { permission ->
                PermissionRequestInformation(permission, rationaleDialog, settingsInvitationDialog)
            }
            return this
        }

        public fun requireGlobalLocationEnabled(builder: DialogConfigurationBuilder.() -> Unit): Builder {
            globalLocationPermissionEnableDialog =
                DialogConfigurationBuilder().apply(builder).build()
            return this
        }

        /**
         * For testing purposes only
         *
         * @param activityResultRegistry
         * @return current builder
         */
        public fun registerRegistry(activityResultRegistry: ActivityResultRegistry): Builder {
            this.activityResultRegistry = activityResultRegistry
            return this
        }

        public fun build(
            activity: AppCompatActivity,
            allPermissionGrantedCallback: () -> Unit
        ): PermissionRequester = PermissionRequester(
            activity,
            permissionsToRequest.toList(),
            globalLocationPermissionEnableDialog,
            allPermissionGrantedCallback,
            activityResultRegistry ?: activity.activityResultRegistry
        )

        public fun build(activity: AppCompatActivity): PermissionRequester = build(activity) {}
    }

    private data class PermissionRequestInformation(
        val permission: String,
        val rationaleDialog: DialogConfiguration,
        val settingsInvitationDialog: DialogConfiguration
    )

    /**
     * Builder to create a {@link DialogConfiguration}
     */
    public class DialogConfigurationBuilder(
        @StringRes public var positiveButtonNameResId: Int,
        @StringRes public var negativeButtonNameResId: Int,
        var hasCancelButton: Boolean
    ) {
        public constructor() : this(-1, -1, true)

        @StringRes
        public var titleResId: Int? = null

        @StringRes
        public var messageResId: Int? = null

        public fun build(): DialogConfiguration =
            DialogConfiguration(
                titleResId,
                messageResId,
                positiveButtonNameResId,
                negativeButtonNameResId,
                hasCancelButton
            )
    }

    /**
     * Dialog configuration for rationale
     * and settings invitation dialogs.
     */
    public data class DialogConfiguration(
        val titleResId: Int?,
        val messageResId: Int?,
        val positiveButtonNameResId: Int,
        val negativeButtonNameResId: Int?,
        val hasCancelButton: Boolean = true
    )
}
