package moe.shizuku.manager.filemanager.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.CheckCircle
import moe.shizuku.manager.filemanager.presentation.components.BreadcrumbNavigation
import moe.shizuku.manager.filemanager.presentation.components.SortDialog
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import moe.shizuku.manager.filemanager.domain.FileItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf("file") } // "file" or "dir"

    var renameDialogItem by remember { mutableStateOf<FileItem?>(null) }
    var showGitCloneDialog by remember { mutableStateOf(false) }
    var showPackageInstaller by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTermuxSettings by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showFindAndReplaceItem by remember { mutableStateOf<FileItem?>(null) }
    
    // Clipboard for copy/move operations
    var clipboardPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var clipboardIsMove by remember { mutableStateOf(false) }
    
    var selectedItemForMenu by remember { mutableStateOf<FileItem?>(null) }
    var chmodDialogItem by remember { mutableStateOf<FileItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var detailsDialogItem by remember { mutableStateOf<FileItem?>(null) }

    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.addSafStorage(uri)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (state.selectedItems.isNotEmpty()) {
                            Text("${state.selectedItems.size} Terpilih")
                        } else if (state.currentPath == "home") {
                            Text("File Manager")
                        } else {
                            BreadcrumbNavigation(state.currentPath) { viewModel.navigateTo(it) }
                        }
                    },
                    navigationIcon = {
                        if (state.selectedItems.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text("Batal")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (state.currentPath == "home") {
                                        onBack()
                                    } else {
                                        viewModel.navigateUp()
                                    }
                                },
                                modifier = Modifier.testTag("back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (state.selectedItems.isNotEmpty()) {
                            TextButton(onClick = { viewModel.selectAll() }) {
                                Text("Semua")
                            }
                            IconButton(onClick = {
                                clipboardPaths = state.selectedItems.map { it.path }
                                clipboardIsMove = false
                                viewModel.clearSelection()
                            }) {
                                Text("C", color = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = {
                                clipboardPaths = state.selectedItems.map { it.path }
                                clipboardIsMove = true
                                viewModel.clearSelection()
                            }) {
                                Text("M", color = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = {
                                state.selectedItems.forEach { viewModel.deleteFile(it.path) }
                                viewModel.clearSelection()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        } else if (clipboardPaths.isNotEmpty()) {
                            TextButton(onClick = {
                                clipboardPaths.forEach { path ->
                                    val dest = if (state.currentPath.endsWith("/")) "${state.currentPath}${path.split("/").last()}" else "${state.currentPath}/${path.split("/").last()}"
                                    if (clipboardIsMove) {
                                        viewModel.moveFile(path, dest)
                                    } else {
                                        viewModel.copyFile(path, dest)
                                    }
                                }
                                clipboardPaths = emptyList()
                            }) {
                                Text("Paste")
                            }
                            TextButton(onClick = { clipboardPaths = emptyList() }) {
                                Text("Cancel")
                            }
                        } else {
                            TextButton(onClick = { viewModel.toggleGridView() }) {
                                Text(if (state.isGridView) "List" else "Grid")
                            }
                            TextButton(onClick = { showSortDialog = true }) {
                                Text("Urut")
                            }
                            IconButton(onClick = { showPackageInstaller = true }) {
                                Icon(Icons.Default.Build, contentDescription = "Packages")
                            }
                            IconButton(onClick = { showTermuxSettings = true }) {
                                Icon(Icons.Default.List, contentDescription = "Termux Settings")
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Cari file...") },
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            if (state.currentPath != "home") {
                FloatingActionButton(onClick = {
                    createType = "file"
                    showCreateDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { safPickerLauncher.launch(null) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Tambah Akses Aplikasi") },
                    text = { Text("Tambah Akses Aplikasi") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredItems) { file ->
                        FileItemGridCell(
                            item = file,
                            isSelected = state.selectedItems.contains(file),
                            onClick = {
                                if (state.selectedItems.isNotEmpty()) {
                                    if (file.lastModified != 0L || file.size != 0L) {
                                        viewModel.toggleSelection(file)
                                    }
                                } else if (file.isDirectory) {
                                    viewModel.navigateTo(file.path)
                                }
                            },
                            onLongClick = {
                                if (file.lastModified != 0L || file.size != 0L) {
                                    if (state.selectedItems.isEmpty()) {
                                        viewModel.toggleSelection(file)
                                    } else {
                                        selectedItemForMenu = file
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.filteredItems) { file ->
                        FileItemRow(
                            item = file,
                            isSelected = state.selectedItems.contains(file),
                            onClick = {
                                if (state.selectedItems.isNotEmpty()) {
                                    if (file.lastModified != 0L || file.size != 0L) {
                                        viewModel.toggleSelection(file)
                                    }
                                } else if (file.isDirectory) {
                                    viewModel.navigateTo(file.path)
                                }
                            },
                            onLongClick = {
                                if (file.lastModified != 0L || file.size != 0L) {
                                    if (state.selectedItems.isEmpty()) {
                                        viewModel.toggleSelection(file)
                                    } else {
                                        selectedItemForMenu = file
                                    }
                                }
                            },
                            onMenuClick = {
                                if (file.lastModified != 0L || file.size != 0L) {
                                    selectedItemForMenu = file
                                }
                            }
                        )
                    }
                }
            }
        }
        
        if (state.error != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text(state.error ?: "Unknown error") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
                }
            )
        }
        
        if (state.isExecutingScript) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Executing Termux Script...") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        state.scriptOutput.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            )
        } else if (state.scriptOutput.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { viewModel.closeScriptDialog() },
                title = { Text("Execution Finished") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        state.scriptOutput.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.closeScriptDialog() }) { Text("Close") }
                }
            )
        }

        if (showCreateDialog) {
            var inputName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Buat Baru") },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = createType == "file",
                                onClick = { createType = "file" }
                            )
                            Text("File")
                            Spacer(Modifier.width(16.dp))
                            RadioButton(
                                selected = createType == "dir",
                                onClick = { createType = "dir" }
                            )
                            Text("Folder")
                        }
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Nama") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (inputName.isNotBlank()) {
                            if (createType == "file") {
                                viewModel.createFile(inputName)
                            } else {
                                viewModel.createDirectory(inputName)
                            }
                        }
                        showCreateDialog = false
                    }) {
                        Text("Buat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Batal") }
                }
            )
        }
        
        if (showGitCloneDialog) {
            var repoUrl by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showGitCloneDialog = false },
                title = { Text("Git Clone") },
                text = {
                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = { repoUrl = it },
                        label = { Text("URL Repository") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (repoUrl.isNotBlank()) {
                            viewModel.executeTermuxScript(context, "git clone '$repoUrl'")
                        }
                        showGitCloneDialog = false
                    }) {
                        Text("Clone")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGitCloneDialog = false }) { Text("Batal") }
                }
            )
        }
        
        if (showPackageInstaller) {
            TermuxDependencyManagerDialog(
                context = context,
                viewModel = viewModel,
                onDismiss = { showPackageInstaller = false }
            )
        }
        
        if (showSortDialog) {
            SortDialog(
                currentOption = state.sortOption,
                currentOrder = state.sortOrder,
                onOptionSelected = { viewModel.setSortOption(it) },
                onOrderToggled = { viewModel.toggleSortOrder() },
                onDismiss = { showSortDialog = false }
            )
        }

        if (showSettings) {
            FileManagerSettingsDialog(
                onDismiss = { showSettings = false }
            )
        }

        if (showTermuxSettings) {
            moe.shizuku.manager.filemanager.presentation.components.TermuxSettingsDialog(
                onDismiss = { showTermuxSettings = false }
            )
        }
        
        if (showFindAndReplaceItem != null) {
            FindAndReplaceManagerDialog(
                context = context,
                viewModel = viewModel,
                targetDirectory = showFindAndReplaceItem!!.path,
                onDismiss = { showFindAndReplaceItem = null }
            )
        }
        
        if (renameDialogItem != null) {
            var inputName by remember { mutableStateOf(renameDialogItem!!.name) }
            AlertDialog(
                onDismissRequest = { renameDialogItem = null },
                title = { Text("Ganti Nama") },
                text = {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nama Baru") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (inputName.isNotBlank() && inputName != renameDialogItem!!.name) {
                            val parent = renameDialogItem!!.path.substringBeforeLast("/")
                            val newPath = "$parent/$inputName"
                            viewModel.renameFile(renameDialogItem!!.path, newPath)
                        }
                        renameDialogItem = null
                    }) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameDialogItem = null }) { Text("Batal") }
                }
            )
        }
        
        if (detailsDialogItem != null) {
            moe.shizuku.manager.filemanager.presentation.components.FileDetailDialog(
                item = detailsDialogItem!!,
                onDismissRequest = { detailsDialogItem = null }
            )
        }
        
        if (selectedItemForMenu != null) {
            moe.shizuku.manager.filemanager.presentation.components.FileContextMenuBottomSheet(
                item = selectedItemForMenu!!,
                sheetState = sheetState,
                viewModel = viewModel,
                currentPath = state.currentPath,
                onDismissRequest = { selectedItemForMenu = null },
                onShowDetails = { detailsDialogItem = it },
                onRename = { renameDialogItem = it },
                onCopy = {
                    clipboardPaths = listOf(it.path)
                    clipboardIsMove = false
                },
                onMove = {
                    clipboardPaths = listOf(it.path)
                    clipboardIsMove = true
                },
                onFindAndReplace = { showFindAndReplaceItem = it },
                onGitClone = { showGitCloneDialog = true },
                onShowChmodDialog = { chmodDialogItem = it }
            )
        }
        
        if (chmodDialogItem != null) {
            moe.shizuku.manager.filemanager.presentation.components.ChmodDialog(
                item = chmodDialogItem!!,
                onDismiss = { chmodDialogItem = null },
                onConfirm = { octalMode ->
                    viewModel.executeTermuxScript(context, "chmod $octalMode '${chmodDialogItem!!.name}'")
                    chmodDialogItem = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dateString = formatter.format(Date(item.lastModified))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp)
            .testTag("file_item_${item.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Text(
                text = if (item.isSymlink) "🔗" else if (item.path.startsWith("content://")) "📦" else if (item.isDirectory) "📁" else "📄",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge
            )
            if (item.extraInfo != null) {
                Text(
                    text = item.extraInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (item.lastModified == 0L && item.size == 0L) {
                Text(
                    text = item.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.permissions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!item.isDirectory) {
                        Text(
                            text = "${item.size} bytes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        if (item.lastModified != 0L || item.size != 0L) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemGridCell(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Text(
                    text = if (item.isSymlink) "🔗" else if (item.path.startsWith("content://")) "📦" else if (item.isDirectory) "📁" else "📄",
                    style = MaterialTheme.typography.displaySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
