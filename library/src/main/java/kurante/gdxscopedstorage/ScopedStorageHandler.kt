package kurante.gdxscopedstorage

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidComponentApplication
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import kurante.gdxscopedstorage.launchers.ActivityLauncher
import kurante.gdxscopedstorage.launchers.ComponentLauncher
import kurante.gdxscopedstorage.launchers.DefaultLauncher
import kurante.gdxscopedstorage.launchers.ReadWriteCallback
import kurante.gdxscopedstorage.request.ActivityRequest
import kurante.gdxscopedstorage.request.DocumentTreeCallback
import kurante.gdxscopedstorage.request.DocumentTreeRequest
import kurante.gdxscopedstorage.util.PermissionsLauncher

/**
 * Class that handles requesting Document Trees from the user with the startActivityForResult function.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class ScopedStorageHandler {
    companion object {
        const val DEFAULT_STARTING_CODE = 2801
    }

    val launcher: ActivityLauncher
    private val activityCode: Int

    constructor(launcher: ActivityLauncher) : this(launcher, DEFAULT_STARTING_CODE)
    constructor(launcher: ActivityLauncher, activityCode: Int) {
        this.launcher = launcher
        this.activityCode = activityCode
    }

    constructor(application: AndroidApplication) : this(application, DEFAULT_STARTING_CODE)
    constructor(application: AndroidApplication, activityCode: Int) : this(DefaultLauncher(application), activityCode)

    constructor(application: AndroidComponentApplication, launcher: PermissionsLauncher) : this(application, DEFAULT_STARTING_CODE, launcher)
    constructor(application: AndroidComponentApplication, activityCode: Int, launcher: PermissionsLauncher) : this(ComponentLauncher(application, launcher), activityCode)


    private val requests = mutableMapOf<Int, ActivityRequest>()

    // Returns the next code that is not being used to receive an activity result
    private fun nextCode(): Int {
        var code = activityCode
        while (requests.containsKey(code))
            code++
        return code
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val request = requests[requestCode] ?: return
        requests.remove(request.requestCode)
        request.onResult(resultCode, data)
    }

    /**
     * Starts an Intent.ACTION_OPEN_DOCUMENT_TREE. The callback will be called with
     * a FileHandle that can be cast to DocumentHandle on success, or null on failure.
     *
     * If makePersistent is true, the directory the user specified
     * will retain read and write permissions until the app is deleted or permission is revoked,
     * you can make use of this by saving FileHandle.path() and creating a DocumentHandle with the
     * DocumentHandle.valueOf function (passing the FileHandle.path() as second argument)
     */
    fun requestDocumentTree(makePersistent: Boolean = true, callback: (FileHandle?) -> Unit) {
        requestDocumentTree(makePersistent, object : DocumentTreeCallback {
            override fun run(handle: DocumentHandle?) = callback(handle)
        })
    }

    /**
     * Starts an Intent.ACTION_OPEN_DOCUMENT_TREE. The callback will be called with
     * a FileHandle that can be cast to DocumentHandle on success, or null on failure.
     *
     * If makePersistent is true, the directory the user specified
     * will retain read and write permissions until the app is deleted or permission is revoked,
     * you can make use of this by saving FileHandle.path() and creating a DocumentHandle with the
     * DocumentHandle.valueOf function (passing the FileHandle.path() as second argument)
     */
    fun requestDocumentTree(makePersistent: Boolean = true, callback: DocumentTreeCallback) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        var flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        if (makePersistent) flags = flags or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        intent.addFlags(flags)
        // Send request
        val request = DocumentTreeRequest(nextCode(), launcher, makePersistent, callback)
        requests[request.requestCode] = request
        launcher.startActivityForResult(intent, request.requestCode)
    }

    /**
     * Launches a RequestMultiplePermissions with the permissions READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE,
     * this only works from Android 6 to Android 10 (both inclusive) and allows you to use Gdx.files.absolute.
     *
     * You can get an Absolute FileHandle out of a DocumentHandle by calling Gdx.files.absolute(documentHandle.realPath())
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestReadWritePermissions(callback: (Boolean) -> Unit) {
        requestReadWritePermissions(object : ReadWriteCallback {
            override fun run(success: Boolean) = callback(success)
        })
    }

    /**
     * Launches a RequestMultiplePermissions with the permissions READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE,
     * this only works from Android 6 to Android 10 (both inclusive) and allows you to use Gdx.files.absolute.
     *
     * You can get an Absolute FileHandle out of a DocumentHandle by calling Gdx.files.absolute(documentHandle.realPath())
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestReadWritePermissions(callback: ReadWriteCallback) {
        if (launcher !is ComponentLauncher)
            throw GdxRuntimeException("Your AndroidLauncher must inherit from AndroidComponentApplication")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            throw GdxRuntimeException("Cannot request READ_EXTERNAL_STORAGE nor WRITE_EXTERNAL_STORAGE on Android 11 or above")
        launcher.permissionLauncher.launch(callback)
    }
}