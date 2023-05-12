package kurante.gdxscopedstorage.request

import android.app.Activity
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import kurante.gdxscopedstorage.DocumentHandle
import kurante.gdxscopedstorage.launchers.ActivityLauncher

class DocumentTreeRequest(
    override val requestCode: Int,
    private val launcher: ActivityLauncher,
    private val makePersistent: Boolean,
    private val callback: DocumentTreeCallback,
) : ActivityRequest {
    override fun onResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            callback.run(null)
            return
        }

        val uri = data?.data
        if (uri == null) {
            callback.run(null)
            return
        }

        if (makePersistent)
            launcher.context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        callback.run(DocumentHandle(DocumentFile.fromTreeUri(launcher.context, uri)!!))
    }
}

// I don't know how to use a Java Runnable with arguments
interface DocumentTreeCallback {
    fun run(handle: DocumentHandle?)
}