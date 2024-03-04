package com.sonova.android.permissionrequester.sample

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.sonova.android.permissionrequester.PermissionRequester
import com.sonova.android.permissionrequester.sample.ui.theme.PermissionRequesterTheme

class MainActivity : ComponentActivity() {

    private lateinit var permissionRequester: PermissionRequester
    private val permissionGranted = MutableLiveData<Boolean>()
    override fun onCreate(savedInstanceState: Bundle?) {
        permissionRequester = createPermissionRequester()
        super.onCreate(savedInstanceState)
        setContent {
            PermissionRequesterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val grantStatus = permissionGranted.observeAsState(isPermissionGranted()).value
                    PermissionButton(permissionRequester, grantStatus)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            revokeSelfPermissionsOnKill(PERMISSIONS)
        }
    }

    private fun createPermissionRequester() =
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
                val granted = isPermissionGranted()
                permissionGranted.postValue(granted)
            }

    private fun isPermissionGranted(): Boolean {
        return PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val PERMISSIONS = listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}

@Composable
fun PermissionButton(permissionRequester: PermissionRequester, granted: Boolean) {
    Column(modifier = Modifier.padding(24.dp)) {
        val showPermissionDialog = remember { mutableStateOf(true) }
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = "Location permission ${if (granted) "granted" else "denied"}",
            color = if (granted) Color.Green else Color.Red
        )

        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = "Request permission will show " +
                    if (showPermissionDialog.value) "permission dialog" else "rationale"
        )
        Button(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("WithRationale"),
            onClick = {
                showPermissionDialog.value = false
                permissionRequester.request()
            }) {
            Text("Request locations permissions")
        }
        Spacer(modifier = Modifier.height(36.dp))

        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = "Request permission without rationale"
        )
        Button(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("WithoutRationale"),
            onClick = {
                permissionRequester.request(true)
            }) {
            Text("Request locations permissions")
        }
    }
}

private object PreviewActivity: ComponentActivity()
@Preview(showBackground = true)
@Composable
private fun PermissionButtonPreview() {
    PermissionRequesterTheme {
        PermissionButton(
            PermissionRequester.Builder().build(PreviewActivity),
            true
        )
    }
}