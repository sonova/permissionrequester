# Android Permission Requester Library

## Overview

This library provides a simple and easy-to-use way to handle permissions in Android applications. It
simplifies the process of requesting and checking permissions, making it easier for developers to
manage permissions in their apps.

## Installation

Currently the library is not on maven. Please clone the repository and build your custom `aar` file.
Then you can include it as local library.

## Usage

### Initializing Permissions

To request permissions, simply create a `PermissionRequester` instance before `onCreate`  and pass
in the permission you want
to request with dialog configuration:

```kotlin 
PermissionRequester.Builder()
    .requirePermissions(
        {
            titleResId = R.string.permission_rationale_title
            messageResId = R.string.permission_rationale_description
        },
        {
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
    permissionRequester.request(false)
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
