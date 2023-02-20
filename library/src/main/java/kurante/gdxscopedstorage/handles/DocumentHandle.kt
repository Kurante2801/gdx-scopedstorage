package kurante.gdxscopedstorage.handles

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.documentfile.provider.DocumentFile
import com.badlogic.gdx.Files
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import java.io.*

class DocumentHandle : FileHandle {
    private val application: AndroidApplication
    var document: DocumentFile private set

    // Constructor needed to make document have a private setter
    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(application: AndroidApplication, document: DocumentFile) : super() {
        this.application = application
        this.document = document
    }

    companion object {
        // You can construct a DocumentHandle out of its path() function
        fun valueOf(application: AndroidApplication, uri: String): DocumentHandle {
            val document = DocumentFile.fromTreeUri(application, Uri.parse(uri))
            return DocumentHandle(application, document!!)
        }

        private val chars = "abcdefghijklmnopqrstuvwxyz".toCharArray()
        fun randomString(length: Int = 6): String {
            val result = CharArray(length)
            for (i in 0 until length)
                result[i] = chars.random()
            return result.joinToString("")
        }

        // Due to SAF, we can't create any file with any arbitrary name
        // so instead we create an empty txt file and rename it to what we want
        fun createChild(parent: DocumentFile, name: String): DocumentFile? {
            // Ensure temp file doesn't exist already
            var tempFile = randomString()
            while (parent.findFile("$tempFile.txt") != null)
                tempFile = randomString()

            val created = parent.createFile("text/plain", "$tempFile.txt")
                ?: return null

            if (!created.renameTo(name)) {
                created.delete()
                return null
            }

            return created
        }

        fun emptyDirectory(document: DocumentFile, preserveTree: Boolean) {
            for (child in document.listFiles()) {
                if (child.isDirectory && preserveTree)
                    emptyDirectory(child, true)
                else
                    child.delete()
            }
        }
    }

    init {
        type = Files.FileType.Absolute
    }

    override fun path(): String = document.uri.toString() // You can feed this to DocumentHandle.valueOf

    override fun name(): String = document.name!!

    override fun extension(): String {
        val name = name()
        val i = name.lastIndexOf('.')
        return if (i == -1) "" else name.substring(i + 1)
    }

    override fun nameWithoutExtension(): String {
        val name = name()
        val i = name.lastIndexOf('.')
        return if (i == -1) name else name.substring(0, i)
    }

    override fun pathWithoutExtension(): String {
        val path = path()
        val i = path.lastIndexOf('.')
        return if (i == -1) path else path.substring(0, i)
    }

    override fun file(): File {
        throw GdxRuntimeException("Can't get the file of a Scoped Storage Document")
    }

    override fun read(): InputStream {
        if (!document.exists()) throw GdxRuntimeException("Android document does not exist: $document")
        if (document.isDirectory) throw GdxRuntimeException("Cannot open a stream to a directory: $document")
        if (!document.canRead()) throw GdxRuntimeException("Can't read document: $document")

        try {
            val input = application.contentResolver.openInputStream(document.uri)
            return input ?: throw GdxRuntimeException("Could not open InputStream: $document")
        } catch (e: FileNotFoundException) {
            throw GdxRuntimeException("Could not open InputStream: $document", e)
        }
    }

    override fun readString(): String {
        read().use { input ->
            BufferedReader(InputStreamReader(input)).use {
                return it.readText()
            }
        }
    }

    override fun write(append: Boolean): OutputStream {
        if (document.isDirectory) throw GdxRuntimeException("Cannot open a stream to a directory: $document")

        try {
            // This document doesn't exist, so we use the parent's document to write a sub document
            if (!exists()) {
                val parent = document.parentFile
                    ?: throw GdxRuntimeException("Could not get document's parent (needed to write document because it doesn't exist): $document")
                document = createChild(parent, name())
                    ?: throw GdxRuntimeException("Could not create document to write to: $document")
            }

            val mode = if (append) "wa" else "wt" // https://developer.android.com/reference/android/os/ParcelFileDescriptor#parseMode(java.lang.String)
            return application.contentResolver.openOutputStream(document.uri, mode)
                ?: throw GdxRuntimeException("Could not open OutputStream: $document")
        } catch (e: FileNotFoundException) {
            throw GdxRuntimeException("Could not write to document: $document", e)
        }
    }

    override fun list(): Array<FileHandle> {
        return document.listFiles().map { DocumentHandle(application, it) }.toTypedArray()
    }

    override fun isDirectory(): Boolean =
        document.isDirectory

    override fun child(name: String): FileHandle {
        val uri = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
            .authority(document.uri.authority).appendPath("tree")
            .appendPath(DocumentsContract.getTreeDocumentId(document.uri)).appendPath("document")
            .appendPath(DocumentsContract.getTreeDocumentId(document.uri)).appendPath(name)
            .build()
        return DocumentHandle(application, DocumentFile.fromTreeUri(application, uri)!!)
    }

    override fun parent(): FileHandle {
        val parent = document.parentFile
            ?: throw GdxRuntimeException("Could not get parent document: $document")
        return DocumentHandle(application, parent)
    }

    override fun mkdirs() {
        TODO("mkdirs is not implemented yet")
    }

    override fun exists(): Boolean = document.exists()

    override fun delete(): Boolean = document.delete()

    // TODO: Test on folder
    override fun deleteDirectory(): Boolean = document.delete()

    override fun emptyDirectory() = emptyDirectory(false)
    override fun emptyDirectory(preserveTree: Boolean) {
        if (isDirectory) throw GdxRuntimeException("Tried to empty a file as a directory!: $document")
        if (!exists()) return
        emptyDirectory(document, preserveTree)
    }

    override fun copyTo(dest: FileHandle) {
        if (dest !is DocumentHandle) {
            super.copyTo(dest)
            return
        }

        if (!isDirectory) {
            read().copyTo(dest.write(false))
            return
        }

        TODO("copyTo is not implemented for directories yet")
    }

    override fun lastModified(): Long = document.lastModified()

    override fun equals(other: Any?): Boolean {
        if (other !is DocumentHandle) return false
        if (other === this) return true
        return path() == other.path()
    }

    override fun hashCode(): Int = super.hashCode()
}