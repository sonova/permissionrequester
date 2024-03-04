# Android Permission Requester Library

## Overview

This library provides a simple and easy-to-use way to handle permissions in Android applications. It
simplifies the process of requesting and checking permissions, making it easier for developers to
manage permissions in their apps.

## Installation

Currently the library is not on maven. Please clone the repository and build your custom `aar` file.
Then you can include it as local library.

## Functionalities

The Android library is designed to streamline the process of handling permissions and location
services within Android applications.
- Requesting permissions from the user
- Displaying rationale dialogs when permissions have been previously requested
- Guiding users to the system app settings to enable permissions if they denied a permission multiple times.
- Global location enabling and disabling, allowing for easily manageable location services

## Usage

### Initializing Permissions

To request permissions, simply create a `PermissionRequester` instance before `onCreate`. For each
permission you request, you'll need to pass:

- Title and description for the rationale dialog
- Title and description for the system app settings invitations g dialog

```kotlin 
PermissionRequester.Builder()
    .requirePermissions(
        rationaleBuilder = {
            titleResId = R.string.permission_rationale_title
            messageResId = R.string.permission_rationale_description
        },
        settingsInvitationBuilder = {
            titleResId = R.string.permission_settings_title
            messageResId = R.string.permission_settings_description
        },
        PERMISSIONS
    )
    .build(this) {
        // what should happen if granted
    }
```

### Requesting Permission

When requesting permission, then call `request` on your `PermissionRequester` instance:

```kotlin 
    permissionRequester.request()
```

If you want to use a custom rationale dialog instead of using built-in dialog,
then pass the value `true` when requesting the permissions.

```kotlin 
    permissionRequester.request(ignoreRationale = true)
```

### Testing

You can mock permission handling in your tests. This has been verified with Espresso and Robolectric
tests.
To use the permission mocking, please use the method:

```kotlin
fun mockPermissions(
    permissionTestConfig: List<PermissionTestConfiguration> = emptyList(),
    buildVersion: Int = Build.VERSION.SDK_INT,
    globalLocationPermissionEnabled: Boolean = true,
    defaultPermissionStatusGranted: Boolean = false
) 
```

## Contributing

Contributions are welcome!

## Changelog

### Version 1.0.0

- Initial release

## Security

If you discover any security-related issues, please use the issue tracker.

## Disclaimer

This library is provided as-is, without any warranty or guarantee of any kind. Use at your own risk.

## License

This library is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
