package com.sonova.android.permissionrequester

import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

public typealias PermissionRequestResult = Map<String, Boolean>

public class PermissionRequester private constructor(
    private val activity: ComponentActivity,
    private val permissionsBeingRequested: List<PermissionRequestInformation>,
    private val globalLocationSettingEnableDialog: DialogConfiguration?,
    private val permissionGrantStatusCallback: () -> Unit,
    private val permissionSnapshotLogger: PermissionSnapshotLogger,
    activityResultRegistry: ActivityResultRegistry
) {
    private val missingPermissions: List<PermissionRequestInformation>
        get() = permissionsBeingRequested.filter { isPermissionMissing(it.permission) }

    private val permissionRequestCallback: (PermissionRequestResult) -> Unit =
        { permissionsResult ->
            val (granted, denied) = permissionsResult.toList().partition { it.second }
            permissionSnapshotLogger.log(granted.map { it.first }, denied.map { it.first })
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
            if (isGlobalLocationPermissionEnabled()) request()
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
        if (globalLocationSettingEnableDialog != null && !isGlobalLocationPermissionEnabled()) {
            showGlobalLocationPermissionDialog(globalLocationSettingEnableDialog)
        } else if (ignoreRationale) {
            requestPermissions()
        } else {
            val rationaleToShow = missingPermissions
                .firstOrNull { needsRationale(it) }
                ?.rationaleDialog

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
        return missingPermissions.isEmpty()
    }

    /**
     * When permission has been denied by the user, a dialog will be shown. If the user
     * has denied the permission for the first time, the rationale dialog will shown
     * otherwise an invitation to allow the permission in settings dialog.
     *
     * @param permission configuration with dialogs
     */
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
        val permissionsToRequest = missingPermissions.map { it.permission }
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
            positiveButtonNameResId = dialogConfig.positiveButtonNameResId,
            onPositiveClick = { _, _ -> requestPermissions() },
            negativeButtonNameResId = dialogConfig.negativeButtonNameResId
        )
    }

    private fun showInvitationToSettingsDialog(dialogConfig: DialogConfiguration) {
        activity.showMaterialDialog(
            titleResId = dialogConfig.titleResId,
            messageResId = dialogConfig.messageResId,
            positiveButtonNameResId = dialogConfig.positiveButtonNameResId,
            onPositiveClick = { dialog, _ ->
                dialog.dismiss()
                activity.startActionApplicationDetailsSettings()
            },
            cancelButtonNameResId = dialogConfig.negativeButtonNameResId
                .takeIf { dialogConfig.hasCancelButton }
        )
    }

    private fun showGlobalLocationPermissionDialog(dialogConfig: DialogConfiguration) {
        activity.showMaterialDialog(
            titleResId = dialogConfig.titleResId,
            messageResId = dialogConfig.messageResId,
            positiveButtonNameResId = dialogConfig.positiveButtonNameResId,
            onPositiveClick = { _, _ ->
                navigateToGlobalLocationSetting.launch(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            },
            negativeButtonNameResId = dialogConfig.negativeButtonNameResId
        )
    }

    private fun isGlobalLocationPermissionEnabled(): Boolean {
        return if (VersionChecker.isBuildVersionUpwards(Build.VERSION_CODES.P)) {
            ContextCompat.getSystemService(
                activity,
                LocationManager::class.java
            )?.isLocationEnabled == true
        } else {
            true
        }
    }

    private fun isPermissionMissing(permission: String): Boolean {
        return AndroidManifestSupport.isPermissionRequired(permission) &&
            ContextCompat.checkSelfPermission(
            activity,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    public class Builder(private val permissionSnapshotLogger: PermissionSnapshotLogger = NoLog) {
        private var globalLocationPermissionEnableDialog: DialogConfiguration? = null
        private val permissionsToRequest = mutableListOf<PermissionRequestInformation>()
        private var activityResultRegistry: ActivityResultRegistry? = null

        /**
         * Builder for permission request
         *
         * @param rationaleBuilder dialog configuration when showing a rationale
         * @param settingsInvitationBuilder dialog configuration when inviting to allow the permission in settings
         * @param permissions list of permissions applicable for dialog configuration
         * @return builder
         */
        public fun requirePermissions(
            rationaleBuilder: DialogConfigurationBuilder.() -> Unit,
            settingsInvitationBuilder: DialogConfigurationBuilder.() -> Unit,
            permissions: List<String>
        ): Builder {
            val rationaleDialog = DialogConfigurationBuilder(
                positiveButtonNameResId = R.string.default_rationale_dialog_button_positive,
                negativeButtonNameResId = R.string.default_rationale_dialog_button_negative,
                hasCancelButton = true
            ).apply(rationaleBuilder).build()
            val settingsInvitationDialog = DialogConfigurationBuilder(
                positiveButtonNameResId = R.string.default_settings_invitation_button_positive,
                negativeButtonNameResId = R.string.default_settings_invitation_button_negative,
                hasCancelButton = false
            ).apply(settingsInvitationBuilder).build()
            permissions.mapTo(permissionsToRequest) { permission ->
                PermissionRequestInformation(permission, rationaleDialog, settingsInvitationDialog)
            }
            return this
        }

        public fun requireGlobalLocationEnabled(
            builder: DialogConfigurationBuilder.() -> Unit
        ): Builder {
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
            activity: ComponentActivity,
            allPermissionGrantedCallback: () -> Unit
        ): PermissionRequester = PermissionRequester(
            activity,
            permissionsToRequest.toList(),
            globalLocationPermissionEnableDialog,
            allPermissionGrantedCallback,
            permissionSnapshotLogger,
            activityResultRegistry ?: activity.activityResultRegistry
        )

        public fun build(activity: ComponentActivity): PermissionRequester = build(activity) {}
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
        public var hasCancelButton: Boolean
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
