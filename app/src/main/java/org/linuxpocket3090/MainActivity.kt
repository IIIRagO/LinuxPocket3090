
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileInputStream
import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LinuxPocketApp()
        }
    }
}

private fun readTarNumber(bytes: ByteArray, start: Int, length: Int): Long {
    val value = String(bytes, start, length)
        .trim('\u0000', ' ')
    return if (value.isEmpty()) 0L else value.toLong(8)
}

private fun extractProotArchive(context: android.content.Context): File {
    val archive = context.assets.open(
        "proot-android-armv7a.tar.gz"
    )

    val root = File(context.filesDir, "proot")
    if (!root.exists() && !root.mkdirs()) {
        throw Exception("Cannot create PRoot directory")
    }

    val allowedFiles = setOf(
        "root/bin/proot",
        "root/bin/proot-userland",
        "root/libexec/proot/loader"
    )

    GZIPInputStream(BufferedInputStream(archive)).use { gzip ->
        while (true) {
            val header = ByteArray(512)
            var offset = 0

            while (offset < 512) {
                val count = gzip.read(header, offset, 512 - offset)
                if (count < 0) break
                offset += count
            }

            if (offset == 0) break
            if (offset != 512) {
                throw Exception("Incomplete TAR header")
            }

            if (header.all { it == 0.toByte() }) break

            val name = String(header, 0, 100)
                .trimEnd('\u0000')
            val prefix = String(header, 345, 155)
                .trimEnd('\u0000')

            val path = if (prefix.isEmpty()) name else "$prefix/$name"
            val size = readTarNumber(header, 124, 12)
            val type = header[156].toInt().toChar()

            if (size < 0 || size > 2_000_000) {
                throw Exception("Unexpected archive file size")
            }

            if (type == '0' || type == '\u0000') {
                if (path !in allowedFiles) {
                    throw Exception("Unexpected archive entry: $path")
                }

                val destination = File(root, path.removePrefix("root/"))
                if (!destination.canonicalPath.startsWith(root.canonicalPath + File.separator)) {
                    throw Exception("Unsafe archive path")
                }

                destination.parentFile?.mkdirs()

                destination.outputStream().use { output ->
                    var remaining = size
                    val buffer = ByteArray(8192)

                    while (remaining > 0) {
                        val count = gzip.read(
                            buffer,
                            0,
                            minOf(buffer.size.toLong(), remaining).toInt()
                        )
                        if (count < 0) throw Exception("Incomplete archive file")
                        output.write(buffer, 0, count)
                        remaining -= count
                    }
                }

                destination.setExecutable(true, true)
            } else {
                var remaining = size
                while (remaining > 0) {
                    val skipped = gzip.skip(remaining)
                    if (skipped <= 0) {
                        if (gzip.read() < 0) throw Exception("Incomplete archive")
                        remaining--
                    } else {
                        remaining -= skipped
                    }
                }
            }

            val padding = (512 - (size % 512)) % 512
            var remainingPadding = padding
            while (remainingPadding > 0) {
                val skipped = gzip.skip(remainingPadding)
                if (skipped <= 0) {
                    if (gzip.read() < 0) throw Exception("Incomplete archive padding")
                    remainingPadding--
                } else {
                    remainingPadding -= skipped
                }
            }
        }
    }

    val proot = File(root, "bin/proot")
    val userland = File(root, "bin/proot-userland")
    val loader = File(root, "libexec/proot/loader")

    if (!proot.isFile || !userland.isFile || !loader.isFile) {
        throw Exception("One or more PRoot files are missing")
    }

    proot.setExecutable(true, true)
    userland.setExecutable(true, true)
    loader.setExecutable(true, true)

    return proot
}

@Composable
fun LinuxPocketApp() {
    val context = LocalContext.current
    var output by remember {
        mutableStateOf(
            "LinuxPocket 3090\nRootless Linux environment\n\nPRoot setup not tested yet."
        )
    }
    var busy by remember { mutableStateOf(false) }

    val storage = StatFs(Environment.getDataDirectory().path)
    val freeStorageGb =
        storage.availableBytes / (1024.0 * 1024.0 * 1024.0)

    val deviceInfo =
        "Android: ${Build.VERSION.RELEASE}\n" +
        "CPU: ${Build.SUPPORTED_ABIS.joinToString()}\n" +
        "Free storage: ${String.format("%.2f", freeStorageGb)} GB"

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("LinuxPocket 3090", fontSize = 25.sp)
            Text("Rootless Linux for Android")
            Text(deviceInfo)

            Spacer(modifier = Modifier.height(16.dp))

            Text("PRoot diagnostics")

            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    output = "Preparing PRoot files..."

                    Thread {
                        val result = try {
                            val proot = extractProotArchive(context)

                            val process = ProcessBuilder(
                                proot.absolutePath,
                                "--version"
                            )
                                .redirectErrorStream(true)
                                .start()

                            val response = process.inputStream
                                .bufferedReader()
                                .use { it.readText() }

                            val exitCode = process.waitFor()

                            "PRoot test finished.\n" +
                            "Exit code: $exitCode\n" +
                            "Output:\n$response"
                        } catch (error: Exception) {
                            "PRoot setup failed:\n${error.message}"
                        }

                        (context as ComponentActivity).runOnUiThread {
                            output = result
                            busy = false
                        }
                    }.start()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Testing..." else "Install and Test PRoot")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                output,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            )
        }
    }
}
