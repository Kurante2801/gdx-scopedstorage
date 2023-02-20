package kurante.gdxscopedstorage.request

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import com.badlogic.gdx.backends.android.AndroidApplication
import kurante.gdxscopedstorage.handles.DocumentHandle

class DocumentTreeRequest(
    override val requestCode: Int,
    private val application: AndroidApplication,
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
            application.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        callback.run(DocumentHandle(application, DocumentFile.fromTreeUri(application, uri)!!))
    }
}

// Java interop
interface DocumentTreeCallback {
    fun run(handle: DocumentHandle?)
}