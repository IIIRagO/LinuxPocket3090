package org.linuxpocket3090

import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LinuxPocketApp()
        }
    }
}

@Composable
fun LinuxPocketApp() {
    var command by remember { mutableStateOf("") }
    val storage = StatFs(Environment.getDataDirectory().path)
val freeStorageGb = storage.availableBytes / (1024.0 * 1024.0 * 1024.0)

val deviceInfo = "Android: ${Build.VERSION.RELEASE}\n" +
    "CPU: ${Build.SUPPORTED_ABIS.joinToString()}\n" +
    "Free storage: ${String.format("%.2f", freeStorageGb)} GB"
    var output by remember {
        mutableStateOf("LinuxPocket 3090\nRootless Linux environment\n\nRuntime not installed yet.")
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "LinuxPocket 3090",
                fontSize = 25.sp
            )

            Text("Rootless Linux for Android")
            Text(deviceInfo)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Terminal")

            Text(
                output,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            )

            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text("Command") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    output += "\n\n$ $command\nLinux runtime is not connected yet."
                    command = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run")
            }
        }
    }
}
