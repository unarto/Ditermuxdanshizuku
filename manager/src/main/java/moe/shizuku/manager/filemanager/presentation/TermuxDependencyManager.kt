package moe.shizuku.manager.filemanager.presentation

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TermuxDependencyManagerDialog(
    context: Context,
    viewModel: FileManagerViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Instalasi Paket (Manual)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Instal paket yang dibutuhkan secara manual jika perintah lanjutan (Advanced) gagal dijalankan.", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.executeTermuxScript(context, "pkg install -y git")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Instal Git")
                }
                Button(
                    onClick = {
                        viewModel.executeTermuxScript(context, "pkg install -y zip unzip")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Instal Zip / Unzip")
                }
                Button(
                    onClick = {
                        viewModel.executeTermuxScript(context, "pkg install -y p7zip")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Instal p7zip")
                }
                Button(
                    onClick = {
                        viewModel.executeTermuxScript(context, "pkg install -y unrar")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Instal Unrar")
                }
                Button(
                    onClick = {
                        viewModel.executeTermuxScript(context, "pkg update -y && pkg upgrade -y")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Update & Upgrade Semua Paket")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
