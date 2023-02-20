package kurante.gdxscopedstorage

import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import com.badlogic.gdx.backends.android.AndroidApplication
import kurante.gdxscopedstorage.request.ActivityRequest
import kurante.gdxscopedstorage.request.DocumentTreeCallback
import kurante.gdxscopedstorage.request.DocumentTreeRequest

class ScopedStorageHandler(
    private val application: AndroidApplication,
    private val activityCode: Int,
) {
    constructor(application: AndroidApplication) : this(application, 2801) // Java interop
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

    fun requestDocumentTree(makePersistent: Boolean = true, callback: DocumentTreeCallback) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        var flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        if (makePersistent) flags = flags or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        intent.addFlags(flags)
        // Send request
        val request = DocumentTreeRequest(nextCode(), application, makePersistent, callback)
        requests[request.requestCode] = request
        application.startActivityForResult(intent, request.requestCode)
    }
}