package moe.shizuku.manager.filemanager.domain

interface FileManagerRepository {
    suspend fun listFiles(path: String): List<FileItem>
    suspend fun delete(path: String): Boolean
    suspend fun rename(oldPath: String, newPath: String): Boolean
    suspend fun copy(sourcePath: String, destPath: String): Boolean
    suspend fun move(sourcePath: String, destPath: String): Boolean
    suspend fun createFile(path: String): Boolean
    suspend fun createDirectory(path: String): Boolean
}
