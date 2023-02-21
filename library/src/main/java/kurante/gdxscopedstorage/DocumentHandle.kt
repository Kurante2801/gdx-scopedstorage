package kurante.gdxscopedstorage

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.badlogic.gdx.Files
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import kurante.gdxscopedstorage.util.InvalidFileData
import java.io.*

@Suppress("unused")
class DocumentHandle : FileHandle {
    private val application: AndroidApplication
    var document: DocumentFile private set
    private var invalidFileData: InvalidFileData? = null

    // Constructor needed so document has a private setter
    constructor(application: AndroidApplication, document: DocumentFile) : super() {
        this.application = application
        this.document = document
    }

    constructor(application: AndroidApplication, invalidFileData: InvalidFileData) : this(application, DocumentFile.fromSingleUri(application, Uri.EMPTY)!!) {
        this.invalidFileData = invalidFileData
    }

    companion object {
        // You can construct a DocumentHandle out of its path() function
        @Throws(GdxRuntimeException::class)
        @JvmStatic
        fun valueOf(application: AndroidApplication, uri: String): DocumentHandle {
            try {
                val document = DocumentFile.fromTreeUri(application, Uri.parse(uri))
                return DocumentHandle(application, document!!)
            } catch (e: Exception) {
                throw GdxRuntimeException("Could not create a DocumentHandle", e)
            }
        }

        private val chars = "abcdefghijklmnopqrstuvwxyz".toCharArray()
        @JvmStatic
        fun randomString(length: Int = 6): String {
            val result = CharArray(length)
            for (i in 0 until length)
                result[i] = chars.random()
            return result.joinToString("")
        }

        // Due to SAF, we can't create any file with any arbitrary name
        // so instead we create an empty txt file and rename it to what we want
        @JvmStatic
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

        @JvmStatic
        fun emptyDirectory(document: DocumentFile, preserveTree: Boolean) {
            for (child in document.listFiles()) {
                if (child.isDirectory && preserveTree)
                    emptyDirectory(child, true)
                else
                    child.delete()
            }
        }

        @JvmStatic
        fun copyDirectory(application: AndroidApplication, source: DocumentFile, dest: DocumentFile) {
            if (!source.isDirectory) throw GdxRuntimeException("Source of copyDirectory isn't a directory: $source")
            if (!dest.isDirectory) throw GdxRuntimeException("Destination of copyDirectory isn't a directory: $source")



            for (child in source.listFiles()) {
                if (child.isDirectory) {
                    val other = dest.findFile(child.name!!) ?: dest.createDirectory(child.name!!)
                        ?: throw GdxRuntimeException("Could not create directory: ${child.name}")
                    if (!other.isDirectory) throw GdxRuntimeException("Could not copy directory because destination contains a file with the same name: ${child.name}")
                    copyDirectory(application, child, other)
                } else {
                    val other = dest.findFile(child.name!!) ?: createChild(dest, child.name!!)
                        ?: throw GdxRuntimeException("Could not create file: ${child.name}")
                    if (!other.isFile) throw GdxRuntimeException("Could not copy file because destination contains a directory with the same name: ${child.name}")

                    try {
                        val output = application.contentResolver.openOutputStream(other.uri)
                            ?: throw GdxRuntimeException("Could not open OutputStream: $other")
                        val input = application.contentResolver.openInputStream(child.uri)
                            ?: throw GdxRuntimeException("Could not open InputStream: $other")
                        input.use {
                            it.copyTo(output)
                        }
                    } catch (e: Exception) {
                        throw GdxRuntimeException("Could not copy file: $child -> $other", e)
                    }
                }
            }
        }
    }

    init {
        type = Files.FileType.Absolute
    }

    // You can feed this to DocumentHandle.valueOf
    override fun path(): String = invalidFileData?.path ?: document.uri.path!!

    override fun name(): String = invalidFileData?.name ?: document.name!!

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
        if (!exists()) throw GdxRuntimeException("Android document does not exist: ${invalidFileData?.path ?: document}")
        if (isDirectory) throw GdxRuntimeException("Cannot open a stream to a directory: $document")
        if (!document.canRead()) throw GdxRuntimeException("Can't read document: $document")

        try {
            val input = application.contentResolver.openInputStream(document.uri)
            return input
                ?: throw GdxRuntimeException("Could not open InputStream: $document")
        } catch (e: FileNotFoundException) {
            throw GdxRuntimeException("Error reading document: $document", e)
        }
    }

    override fun readString(): String {
        read().use { input ->
            BufferedReader(InputStreamReader(input)).use {
                return it.readText()
            }
        }
    }

    override fun readString(charset: String?): String {
        val reader = if (charset == null)
            InputStreamReader(read())
        else
            InputStreamReader(read(), charset)

        reader.use { input ->
            return input.readText()
        }
    }

    override fun readBytes(): ByteArray {
        read().use { input ->
            ByteArrayOutputStream().use { output ->
                input.copyTo(output)
                return output.toByteArray()
            }
        }
    }

    override fun write(append: Boolean): OutputStream {
        if (isDirectory) throw GdxRuntimeException("Cannot open a stream to a directory: $document")

        try {
            // This document doesn't exist, so we use the parent's document to write a sub document
            if (!exists()) {
                val parent = invalidFileData?.parent ?: document.parentFile
                    ?: throw GdxRuntimeException("Could not get document's parent (needed to write document because it doesn't exist): $this")
                document = createChild(parent, name())
                    ?: throw GdxRuntimeException("Could not create document to write to: $parent")
                invalidFileData = null // We exist now, so we don't need to hold this anymore
            }

            // https://developer.android.com/reference/android/os/ParcelFileDescriptor#parseMode(java.lang.String)
            val mode = if (append) "wa" else "rwt"
            return application.contentResolver.openOutputStream(document.uri, mode)
                ?: throw GdxRuntimeException("Could not open OutputStream: $document")
        } catch (e: FileNotFoundException) {
            throw GdxRuntimeException("Could not write to document: $document", e)
        }
    }

    override fun writer(append: Boolean, charset: String?): Writer {
        return if (charset == null)
            OutputStreamWriter(write(append))
        else
            OutputStreamWriter(write(append), charset)

    }

    override fun writeBytes(bytes: ByteArray, append: Boolean) {
        write(append).use { output ->
            output.write(bytes)
        }
    }

    override fun list(): Array<FileHandle> {
        if (!exists()) return arrayOf() // Would throwing an exception be more appropriate?
        return document.listFiles().map { DocumentHandle(application, it) }.toTypedArray()
    }

    override fun isDirectory(): Boolean = invalidFileData == null && document.isDirectory

    override fun child(name: String): FileHandle {
        if (!isDirectory) throw GdxRuntimeException("Cannot get the child of a file: $document")

        for (file in document.listFiles()) {
            if (file.name == name)
                return DocumentHandle(application, file)
        }

        // We can't create a DocumentFile out of a file that doesn't exist because we crash
        // so we just hold some data and the parent DocumentFile to create the document when needed
        val path = document.uri.toString() + Uri.encode("/$name")
        return DocumentHandle(application, InvalidFileData(name, path, document))
    }

    override fun sibling(name: String): FileHandle {
        return parent().child(name)
    }

    override fun parent(): FileHandle {
        val parent = invalidFileData?.parent ?: document.parentFile
            ?: throw GdxRuntimeException("Could not get parent document: $document")
        return DocumentHandle(application, parent)
    }

    override fun mkdirs() {
        throw GdxRuntimeException("Cannot mkdirs() a DocumentHandle")
    }

    override fun exists(): Boolean = invalidFileData == null && document.exists()

    override fun delete(): Boolean = if (exists()) document.delete() else false

    override fun deleteDirectory(): Boolean = if (exists()) document.delete() else false

    override fun emptyDirectory() { deleteDirectory() }

    override fun emptyDirectory(preserveTree: Boolean) {
        if (!isDirectory) throw GdxRuntimeException("Tried to empty a file as a directory!: $document")
        if (!exists()) return
        emptyDirectory(document, preserveTree)
    }

    override fun copyTo(dest: FileHandle) {
        if (!exists()) throw GdxRuntimeException("Cannot copyTo from a file that doesn't exist")

        if (dest !is DocumentHandle) {
            super.copyTo(dest)
            return
        }

        if (!isDirectory) {
            if (dest.exists() && dest.isDirectory)
                throw GdxRuntimeException("Destination exists but is a directory: $dest")
            // Copy File
            read().copyTo(dest.write(false))
        } else {
            if (!dest.exists()) {
                dest.document = dest.invalidFileData!!.parent.createDirectory(dest.name())
                    ?: throw GdxRuntimeException("Could not create directory: ${dest.name()}")
                dest.invalidFileData = null
            }
            // Copy directory
            if (!dest.isDirectory)
                throw GdxRuntimeException("Destination exists but is a file: $dest")
            copyDirectory(application, document, dest.document)
        }
    }

    override fun moveTo(dest: FileHandle) {
        copyTo(dest)
        deleteDirectory()
    }

    override fun length(): Long = if (exists()) document.length() else 0L

    override fun lastModified(): Long {
        if (!exists()) throw GdxRuntimeException("Cannot get the last modifier time from a file that doesn't exist")
        return document.lastModified()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DocumentHandle) return false
        if (other === this) return true
        return path() == other.path()
    }

    @Suppress("RedundantOverride")
    override fun hashCode(): Int = super.hashCode()

    override fun toString(): String {
        return if (!exists())
            Uri.decode(invalidFileData!!.path)
        else
            document.uri.path!!
    }
}