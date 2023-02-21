package kurante.gdxscopedstorage.util

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.RequiresApi
import com.badlogic.gdx.backends.android.AndroidComponentApplication
import kurante.gdxscopedstorage.launchers.ReadWriteCallback

// This class allows you to get READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE permissions
// on Android 10 and below
@RequiresApi(Build.VERSION_CODES.M)
class PermissionsLauncher(private val application: AndroidComponentApplication) {
    private var callback: ((Boolean) -> Unit)? = null
    private val launcher = application.registerForActivityResult(RequestMultiplePermissions()) {
        var success = true
        for (permission in it) {
            if (!permission.value) {
                success = false
                break
            }
        }

        callback?.invoke(success)
        callback = null
    }

    fun checkPermissions(): Boolean {
        return application.checkSelfPermission(READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED &&
                application.checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED
    }

    fun launch(callback: (Boolean) -> Unit) {
        launch(object : ReadWriteCallback {
            override fun run(success: Boolean) {
                callback(success)
            }
        })
    }

    fun launch(callback: ReadWriteCallback) {
        if (checkPermissions())
            callback.run(true)
        else {
            this.callback = { callback.run(it) }
            launcher.launch(arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE))
        }
    }
}