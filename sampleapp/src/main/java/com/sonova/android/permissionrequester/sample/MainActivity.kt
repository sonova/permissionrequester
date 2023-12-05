package com.sonova.android.permissionrequester.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.sonova.android.permissionrequester.PermissionRequester
import com.sonova.android.permissionrequester.sample.ui.theme.PermissionRequesterTheme

class MainActivity : ComponentActivity() {

    private lateinit var permissionRequester: PermissionRequester
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PermissionRequesterTheme {
                // A surface container using the 'background' color from the theme
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        // Open camera
                    } else {
                        // Show dialog
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionButton(permissionRequester)
                }
            }
        }
    }
}

@Composable
fun PermissionButton(permissionRequester: PermissionRequester) {
    Column {
        Button(onClick = { permissionRequester.request() }) {
            Text("Request permission with rationale")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PermissionRequesterTheme {
        PermissionButton(PermissionRequester.Builder().build(LocalContext.current as ComponentActivity))
    }
}