package moe.shizuku.manager.filemanager.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import moe.shizuku.manager.filemanager.domain.FileItem
import moe.shizuku.manager.filemanager.presentation.FileManagerViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileContextMenuBottomSheet(
    item: FileItem,
    sheetState: SheetState,
    viewModel: FileManagerViewModel,
    currentPath: String,
    onDismissRequest: () -> Unit,
    onShowDetails: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onMove: (FileItem) -> Unit,
    onFindAndReplace: (FileItem) -> Unit,
    onGitClone: () -> Unit,
    onShowChmodDialog: (FileItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState())) {
            Text(
                text = item.name,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider()
            
            ListItem(
                headlineContent = { Text("Tampilkan detail") },
                modifier = Modifier.clickable {
                    onShowDetails(item)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                }
            )
            
            // ACTION_VIEW (Buka Dengan)
            if (!item.isDirectory) {
                ListItem(
                    headlineContent = { Text("Buka Dengan...") },
                    modifier = Modifier.clickable {
                        try {
                            val file = File(item.path)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Buka dengan"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
            }
            
            // ACTION_SEND (Bagikan)
            if (!item.isDirectory) {
                ListItem(
                    headlineContent = { Text("Bagikan") },
                    modifier = Modifier.clickable {
                        try {
                            val file = File(item.path)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "*/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Bagikan via"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
            }

            ListItem(
                headlineContent = { Text("Salin Path") },
                modifier = Modifier.clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("File Path", item.path))
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                }
            )

            ListItem(
                headlineContent = { Text("Ganti nama") },
                modifier = Modifier.clickable {
                    onRename(item)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                }
            )
            ListItem(
                headlineContent = { Text("Salin") },
                modifier = Modifier.clickable {
                    onCopy(item)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                }
            )
            ListItem(
                headlineContent = { Text("Pindah") },
                modifier = Modifier.clickable {
                    onMove(item)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                }
            )
            ListItem(
                headlineContent = { Text("Hapus") },
                modifier = Modifier.clickable {
                    viewModel.deleteFile(item.path)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                }
            )
            
            HorizontalDivider()
            
            // Termux Advanced Operations
            val ext = item.name.substringAfterLast('.', "")
            
            if (item.isDirectory) {
                ListItem(
                    headlineContent = { 
                        val isPinned = viewModel.state.value.pinnedFolders.contains(item.path)
                        Text(if (isPinned) "Lepas Sematan (Unpin)" else "Sematkan ke Beranda (Pin)") 
                    },
                    modifier = Modifier.clickable {
                        viewModel.togglePinFolder(item.path)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Temukan & Ganti (Recursive)") },
                    modifier = Modifier.clickable {
                        onFindAndReplace(item)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Buka di Terminal") },
                    modifier = Modifier.clickable {
                        // Buka sesi terminal di path ini via Rish/Termux.
                        // Saat ini, jalankan script simpel atau kirim intent ke terminal activity
                        viewModel.executeTermuxScript(context, "cd '${item.path}' && pwd")
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
            }

            if (ext in listOf("zip", "tar", "gz", "7z", "rar", "xz")) {
                ListItem(
                    headlineContent = { Text("Ekstrak (Unpack)") },
                    modifier = Modifier.clickable {
                        val cmd = when(ext) {
                            "zip" -> "unzip '${item.name}'"
                            "tar" -> "tar -xf '${item.name}'"
                            "gz" -> "tar -xzf '${item.name}'"
                            "7z" -> "7z x '${item.name}'"
                            "rar" -> "unrar x '${item.name}'"
                            else -> "tar -xf '${item.name}'"
                        }
                        viewModel.executeTermuxScript(context, cmd)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
            }
            if (item.isDirectory) {
                ListItem(
                    headlineContent = { Text("Kompres ke ZIP") },
                    modifier = Modifier.clickable {
                        viewModel.executeTermuxScript(context, "zip -r '${item.name}.zip' '${item.name}'")
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
                ListItem(
                    headlineContent = { Text("Kompres ke TAR.GZ") },
                    modifier = Modifier.clickable {
                        viewModel.executeTermuxScript(context, "tar -czvf '${item.name}.tar.gz' '${item.name}'")
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
                ListItem(
                    headlineContent = { Text("Kompres ke 7Z") },
                    modifier = Modifier.clickable {
                        viewModel.executeTermuxScript(context, "7z a '${item.name}.7z' '${item.name}'")
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
            }
            
            if (currentPath != "home") {
                ListItem(
                    headlineContent = { Text("Git Clone di sini") },
                    modifier = Modifier.clickable {
                        onGitClone()
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
                ListItem(
                    headlineContent = { Text("Git Commit & Push") },
                    modifier = Modifier.clickable {
                        viewModel.executeTermuxScript(context, "cd '${currentPath}' && git add . && git commit -m 'Auto commit' && git push")
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
            }
            
            if (ext == "apk") {
                ListItem(
                    headlineContent = { Text("Repack APK (Apktool)") },
                    modifier = Modifier.clickable {
                        viewModel.executeTermuxScript(context, "apktool b '${item.name.removeSuffix(".apk")}' -o '${item.name}_repack.apk'")
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                )
            }
            
            ListItem(
                headlineContent = { Text("Ubah Izin (Manajemen Chmod)") },
                modifier = Modifier.clickable {
                    onShowChmodDialog(item)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                }
            )
        }
    }
}
