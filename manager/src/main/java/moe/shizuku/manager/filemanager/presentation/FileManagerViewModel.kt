package moe.shizuku.manager.filemanager.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.shizuku.manager.filemanager.data.FileManagerRepositoryImpl
import moe.shizuku.manager.filemanager.domain.FileItem
import moe.shizuku.manager.filemanager.domain.FileManagerRepository
import java.io.File

enum class SortOption { NAME, SIZE, DATE, EXTENSION }
enum class SortOrder { ASCENDING, DESCENDING }

data class FileManagerState(
    val currentPath: String = "home",
    val items: List<FileItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isExecutingScript: Boolean = false,
    val scriptOutput: List<String> = emptyList(),
    val sortOption: SortOption = SortOption.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val selectedItems: Set<FileItem> = emptySet(),
    val isGridView: Boolean = false,
    val pinnedFolders: Set<String> = emptySet()
) {
    val filteredItems: List<FileItem>
        get() {
            val filtered = if (searchQuery.isBlank()) items else items.filter { it.name.contains(searchQuery, ignoreCase = true) }
            val sorted = when (sortOption) {
                SortOption.NAME -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                SortOption.SIZE -> filtered.sortedBy { it.size }
                SortOption.DATE -> filtered.sortedBy { it.lastModified }
                SortOption.EXTENSION -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.substringAfterLast('.', "") })
            }
            val ordered = if (sortOrder == SortOrder.ASCENDING) sorted else sorted.reversed()
            return ordered.sortedByDescending { it.isDirectory }
        }
}

class FileManagerViewModel(
    private val repository: FileManagerRepository,
    private val context: android.content.Context
) : ViewModel() {

    private val _state = MutableStateFlow(FileManagerState())
    val state: StateFlow<FileManagerState> = _state.asStateFlow()
    
    private val prefs = context.getSharedPreferences("saf_storages", android.content.Context.MODE_PRIVATE)
    private val pinnedPrefs = context.getSharedPreferences("pinned_folders", android.content.Context.MODE_PRIVATE)

    init {
        val pinned = pinnedPrefs.getStringSet("pinned", emptySet()) ?: emptySet()
        _state.value = _state.value.copy(pinnedFolders = pinned)
        loadDirectory(_state.value.currentPath)
    }

    fun navigateTo(path: String) {
        loadDirectory(path)
    }

    fun navigateUp() {
        val currentPath = _state.value.currentPath
        if (currentPath == "home") return
        
        if (currentPath == "/storage/emulated/0" || currentPath == "/" || currentPath == "/storage") {
             loadDirectory("home")
             return
        }
        
        val parent = File(currentPath).parent
        if (parent != null) {
            loadDirectory(parent)
        } else {
            loadDirectory("home")
        }
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun setSortOption(option: SortOption) {
        _state.value = _state.value.copy(sortOption = option)
    }

    fun toggleSortOrder() {
        val newOrder = if (_state.value.sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
        _state.value = _state.value.copy(sortOrder = newOrder)
    }

    fun toggleSelection(item: FileItem) {
        val currentSelection = _state.value.selectedItems.toMutableSet()
        if (currentSelection.contains(item)) {
            currentSelection.remove(item)
        } else {
            currentSelection.add(item)
        }
        _state.value = _state.value.copy(selectedItems = currentSelection)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedItems = emptySet())
    }

    fun selectAll() {
        _state.value = _state.value.copy(selectedItems = _state.value.filteredItems.toSet())
    }

    fun toggleGridView() {
        _state.value = _state.value.copy(isGridView = !_state.value.isGridView)
    }

    fun togglePinFolder(path: String) {
        val currentPinned = _state.value.pinnedFolders.toMutableSet()
        if (currentPinned.contains(path)) {
            currentPinned.remove(path)
        } else {
            currentPinned.add(path)
        }
        pinnedPrefs.edit().putStringSet("pinned", currentPinned).apply()
        _state.value = _state.value.copy(pinnedFolders = currentPinned)
        if (_state.value.currentPath == "home") {
            loadDirectory("home")
        }
    }

    private fun getStorageStats(path: String): String? {
        try {
            val stat = android.os.StatFs(path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val total = totalBlocks * blockSize
            val available = availableBlocks * blockSize
            
            fun format(bytes: Long): String {
                if (bytes < 1024) return "$bytes B"
                val kb = bytes / 1024.0
                if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
                val mb = kb / 1024.0
                if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
                val gb = mb / 1024.0
                return String.format(java.util.Locale.US, "%.1f GB", gb)
            }
            return "${format(available)} free of ${format(total)}"
        } catch (e: Exception) {
            return null
        }
    }

    private fun loadDirectory(path: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, currentPath = path, searchQuery = "", selectedItems = emptySet())
            
            if (path == "home") {
                val homeItems = mutableListOf(
                    FileItem("Penyimpanan Internal", "/storage/emulated/0", true, 0, 0, "d", extraInfo = getStorageStats("/storage/emulated/0")),
                    FileItem("Kartu SD", "/storage", true, 0, 0, "d"),
                    FileItem("Termux", "/data/data/com.termux/files/home", true, 0, 0, "d", extraInfo = getStorageStats("/data/data/com.termux/files/home")),
                    FileItem("Root", "/", true, 0, 0, "d", extraInfo = getStorageStats("/"))
                )
                
                // Add SAF storages
                val safUris = prefs.getStringSet("saf_uris", emptySet()) ?: emptySet()
                safUris.forEach { uriStr ->
                    val uri = android.net.Uri.parse(uriStr)
                    val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
                    if (docFile != null && docFile.isDirectory) {
                        homeItems.add(
                            FileItem(docFile.name ?: "Aplikasi Eksternal", uriStr, true, 0, 0, "d")
                        )
                    }
                }
                
                // Add pinned folders
                _state.value.pinnedFolders.forEach { pinnedPath ->
                    val name = if (pinnedPath.startsWith("content://")) {
                        android.net.Uri.decode(pinnedPath).substringAfterLast("/").substringBeforeLast("%3A")
                    } else {
                        pinnedPath.substringAfterLast("/")
                    }
                    homeItems.add(FileItem("📌 $name", pinnedPath, true, 0, 0, "d"))
                }
                
                _state.value = _state.value.copy(isLoading = false, items = homeItems)
                return@launch
            }
            
            try {
                val files = repository.listFiles(path)
                _state.value = _state.value.copy(isLoading = false, items = files)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    fun addSafStorage(uri: android.net.Uri) {
        // Take persistable URI permission
        val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                             android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val safUris = prefs.getStringSet("saf_uris", emptySet())?.toMutableSet() ?: mutableSetOf()
        safUris.add(uri.toString())
        prefs.edit().putStringSet("saf_uris", safUris).apply()
        
        if (_state.value.currentPath == "home") {
            loadDirectory("home")
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            val success = repository.delete(path)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to delete file")
            }
        }
    }

    fun renameFile(oldPath: String, newPath: String) {
        viewModelScope.launch {
            val success = repository.rename(oldPath, newPath)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to rename file")
            }
        }
    }

    fun copyFile(sourcePath: String, destPath: String) {
        viewModelScope.launch {
            val success = repository.copy(sourcePath, destPath)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to copy file")
            }
        }
    }

    fun moveFile(sourcePath: String, destPath: String) {
        viewModelScope.launch {
            val success = repository.move(sourcePath, destPath)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to move file")
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val path = if (_state.value.currentPath.endsWith("/")) "${_state.value.currentPath}$name" else "${_state.value.currentPath}/$name"
            val success = repository.createFile(path)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to create file")
            }
        }
    }

    fun createDirectory(name: String) {
        viewModelScope.launch {
            val path = if (_state.value.currentPath.endsWith("/")) "${_state.value.currentPath}$name" else "${_state.value.currentPath}/$name"
            val success = repository.createDirectory(path)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to create directory")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun executeTermuxScript(context: android.content.Context, command: String) {
        _state.value = _state.value.copy(isExecutingScript = true, scriptOutput = emptyList())
        viewModelScope.launch {
            moe.shizuku.manager.filemanager.data.TermuxScriptExecutor.execute(context, command, _state.value.currentPath).collect { line ->
                _state.value = _state.value.copy(scriptOutput = _state.value.scriptOutput + line)
            }
            _state.value = _state.value.copy(isExecutingScript = false)
            loadDirectory(_state.value.currentPath)
        }
    }

    fun closeScriptDialog() {
        _state.value = _state.value.copy(scriptOutput = emptyList())
    }
}

class FileManagerViewModelFactory(private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileManagerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileManagerViewModel(FileManagerRepositoryImpl(context), context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
