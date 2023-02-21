package kurante.gdxscopedstorage.util

import androidx.documentfile.provider.DocumentFile

// Data needed to create a new DocumentHandle when calling DocumentHandle.child()
data class InvalidFileData(
    val name: String,
    val path: String,
    val parent: DocumentFile,
)